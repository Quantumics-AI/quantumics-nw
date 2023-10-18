/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api;

import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClient;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.AWSGlueClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static ai.quantumics.api.constants.DatasourceConstants.INVALID_ACCESS_TYPE;

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

  @Bean
  public AmazonAthena awsAthenaClient() {
    if(accessMethod.equals(AwsAccessType.KEYS.getAccessType())) {
      return createAmazonAthena(accessKey, secretKey);
    } else if(accessMethod.equals(AwsAccessType.IAM.getAccessType())) {
      return createAmazonRoleAthena();
    } else if(accessMethod.equals(AwsAccessType.PROFILE.getAccessType())) {
      return createAmazonProfileAthena();
    } else {
      return null;
    }
  }

  private AmazonAthena createAmazonAthena(String accessKey, String secretKey) {
    final BasicAWSCredentials awsAthenaCredentials =
            new BasicAWSCredentials(accessKey, secretKey);

    return AmazonAthenaClient.builder()
            .withRegion(cloudRegion)
            .withCredentials(new AWSStaticCredentialsProvider(awsAthenaCredentials))
            .build();
  }

  public AmazonAthena createAmazonRoleAthena() {

    AWSCredentialsProvider credentialsProvider = getAwsCredentialsProvider();

    return AmazonAthenaClient.builder()
            .withRegion(cloudRegion)
            .withCredentials(credentialsProvider)
            .build();
  }

  public AmazonAthena createAmazonProfileAthena() {
    return AmazonAthenaClientBuilder.defaultClient();
  }

  @Bean
  public AmazonS3 awsS3Client() {
    return amazonS3Client(accessMethod);
  }

  public AmazonS3 amazonS3Client(String accessMethod) {
    if(accessMethod.equals(AwsAccessType.KEYS.getAccessType())) {
      return createAmazonS3(accessKey, secretKey);
    } else if(accessMethod.equals(AwsAccessType.IAM.getAccessType())) {
      return createAmazonRoleS3();
    } else if(accessMethod.equals(AwsAccessType.PROFILE.getAccessType())) {
      return createAmazonProfileS3();
    } else {
      throw new InvalidAccessTypeException(INVALID_ACCESS_TYPE);
    }
  }

  public AmazonS3 createAmazonS3(String accessKey, String secretKey) {
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

    AWSCredentialsProvider credentialsProvider = getAwsCredentialsProvider();

    // Create an Amazon S3 client using the assumed role's credentials.
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(cloudRegion) // Set your desired region
            .build();
    return s3Client;
  }

  public AWSCredentialsProvider getAwsCredentialsProvider() {
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
    return credentialsProvider;
  }

  public AmazonS3 createAmazonProfileS3() {
    // Create an S3 client using the default AWS credentials provider chain.
    // The credentials provider chain will automatically use the IAM role attached to the EC2 instance.
    return AmazonS3ClientBuilder.defaultClient();
  }

  @Bean
  public AWSGlue glueClient() {
    if(accessMethod.equals(AwsAccessType.KEYS.getAccessType())) {
      return createAWSGlue(accessKey, secretKey);
    } else if(accessMethod.equals(AwsAccessType.IAM.getAccessType())) {
      return createAmazonRoleGlue();
    } else if(accessMethod.equals(AwsAccessType.PROFILE.getAccessType())) {
      return createAmazonProfileGlue();
    } else {
      return null;
    }
  }

  private AWSGlue createAWSGlue(String accessKey, String secretKey) {
    final AWSCredentials awsGlueCredentials = new BasicAWSCredentials(accessKey, secretKey);
    return AWSGlueClientBuilder.standard()
            .withRegion(cloudRegion)
            .withCredentials(new AWSStaticCredentialsProvider(awsGlueCredentials))
            .build();
  }

  public AWSGlue createAmazonRoleGlue() {

    AWSCredentialsProvider credentialsProvider = getAwsCredentialsProvider();

    return AWSGlueClientBuilder.standard()
            .withRegion(cloudRegion)
            .withCredentials(credentialsProvider)
            .build();
  }

  public AWSGlue createAmazonProfileGlue() {
    return AWSGlueClientBuilder.defaultClient();
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

  @Bean
  public S3Client s3Client() {
    return s3Client(accessMethod);
  }

  public S3Client s3Client(String accessMethod) {
    if(accessMethod.equals(AwsAccessType.PROFILE.getAccessType())) {
      return createProfileS3();
    } else {
      throw new InvalidAccessTypeException(INVALID_ACCESS_TYPE);
    }
  }

  public S3Client createProfileS3() {
    return S3Client.builder().region(Region.US_EAST_1).build();
  }

}