/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.user;

import ai.quantumics.api.user.enums.AwsAccessType;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class AwsCustomConfiguration {

	@Value("${aws.credentials.accessKey}")
	private String accessKey;

	@Value("${aws.credentials.secretKey}")
	private String secretKey;

	@Value("${qs.cloud.region}")
	private String cloudRegion;


	@Value("${qs.file.max-size}")
	private long maxFileSize;

	@Value("${qs.aws.access.method}")
	private String accessMethod;

	@Value("${aws.access.role}")
	private String roleArn;

	private static final String SESSION_NAME = "qsaiConsumer";

	@Bean
	@Primary
	public AmazonS3 awsS3Client() {
		if(accessMethod.equals(AwsAccessType.KEYS.getAccessType())) {
			return createAmazonS3(accessKey, secretKey);
		} else if(accessMethod.equals(AwsAccessType.IAM.getAccessType())) {
			return createAmazonRoleS3();
		} else if(accessMethod.equals(AwsAccessType.PROFILE.getAccessType())) {
			return createAmazonProfileS3();
		} else {
			return null;
		}
	}

	private AmazonS3 createAmazonS3(String accessKey, String secretKey) {
		final BasicAWSCredentials awsS3Credentials = new BasicAWSCredentials(accessKey, secretKey);
		ClientConfiguration config = new ClientConfiguration();
		config.setMaxConnections(2000);
		return AmazonS3ClientBuilder.standard()
				.withRegion(cloudRegion)
				.withClientConfiguration(config)
				.withCredentials(new AWSStaticCredentialsProvider(awsS3Credentials))
				.build();
	}

	public AmazonS3 createAmazonRoleS3() {

		int sessionDurationSeconds = 3600; // 1 hour

		// Set the desired duration for the assumed role session (in seconds).
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				.withRegion(cloudRegion) // Set your desired region
				.build();
		// Create a session credentials provider that assumes the role with the specified duration.
		AWSCredentialsProvider credentialsProvider = new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, "my-session")
				.withStsClient(stsClient)
				.withRoleSessionDurationSeconds(sessionDurationSeconds)
				.build();

		// Create an Amazon S3 client using the assumed role's credentials.
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion(cloudRegion) // Set your desired region
				.build();
		return s3Client;
	}

	public AmazonS3 createAmazonProfileS3() {
		// Create an S3 client using the default AWS credentials provider chain.
		// The credentials provider chain will automatically use the IAM role attached to the EC2 instance.
		return AmazonS3ClientBuilder.defaultClient();
	}

	@Bean
	RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver multipartResolver() {
		final CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(maxFileSize);
		return multipartResolver;
	}

	private MessageConverter jackson2MessageConverter(final ObjectMapper mapper) {
		final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(mapper);
		converter.setStrictContentTypeMatch(false);
		return converter;
	}

}
