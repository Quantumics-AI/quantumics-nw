/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.user.adapter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class AwsAdapter {

	private static final String S3proto = "s3://";
	private static final String hyphen = "-";
	private static final String path_sep = "/";
	private final AmazonS3 amazonS3Client;

	public AwsAdapter(
			AmazonS3 amazonS3ClientCi
			) {
		amazonS3Client = amazonS3ClientCi;
	}

	public String storeImageInS3Async(final String imagesBucketName,
			final MultipartFile inputFile, final String fileName, final String contentType) throws Exception{

		log.info("\nReceived file upload request for the file: {}", fileName);

		Instant now = Instant.now();
		TransferManager transferManager = null;
		InputStream is = null;
		try {

			final ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(contentType);
			objectMetadata.setContentLength(inputFile.getSize());
			is = inputFile.getInputStream();

			log.info("Bucket Name: {} and File Name: {}", imagesBucketName, fileName);

			// Enable Transfer Acceleration before uploading the file to the bucket.
			amazonS3Client.setBucketAccelerateConfiguration(
					new SetBucketAccelerateConfigurationRequest(imagesBucketName,
							new BucketAccelerateConfiguration(
									BucketAccelerateStatus.Enabled)));

			// Verify whether the transfer acceleration is enabled on the bucket or not...
			String accelerateStatus = amazonS3Client.getBucketAccelerateConfiguration(
					new GetBucketAccelerateConfigurationRequest(imagesBucketName))
					.getStatus();

			log.info("Transfer acceleration status of the bucket {} is {}", imagesBucketName, accelerateStatus);

			// Approach 1:
			transferManager = TransferManagerBuilder.standard()
					.withMultipartUploadThreshold((long) (1 * 1024 * 1025))
					.withS3Client(amazonS3Client)
					.build(); // TODO: We have to create a Singleton instance of TransferManager instance and close it as part of shutdown hook.


			Upload upload = transferManager.upload(new PutObjectRequest(imagesBucketName, fileName, is, objectMetadata)
					.withCannedAcl(CannedAccessControlList.PublicRead));



			upload.waitForCompletion();
			// After the upload is complete, release the TransferManager resources by calling shutdownNow() API of AWS SDK..
			transferManager.shutdownNow(false); // Shutdown only the TransferManager and not the underlying S3Client instance.

			log.info("Uploaded file to destination : {}/{}", imagesBucketName, fileName);
			log.info("Elapsed time to upload file in AWS S3 bucket is: {} msecs", (Duration.between(now, Instant.now()).toMillis()));

			URL s3FileUrl = amazonS3Client.getUrl(imagesBucketName, fileName);
			return s3FileUrl.toString();
		}
		catch (AmazonClientException | IOException | InterruptedException exception) {
			throw new RuntimeException("Error while uploading file." + exception.getMessage());
		}
	}


}
