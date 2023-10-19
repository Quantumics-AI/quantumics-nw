package ai.quantumics.api.util;

import ai.quantumics.api.exceptions.BucketNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.quantumics.api.constants.DatasourceConstants.BUCKET_NOT_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.REGION_PATTERN;

@Slf4j
@Component
public class AwsUtils {
	@Autowired
	private S3Client s3ClientV2;

	public S3Client createS3BucketClientV2(String bucketName){
		log.info("Inside AwsUtils.createS3BucketClientV2 method with abucket name {}", bucketName);

		S3Client s3Client = s3ClientV2;
		try {
			HeadBucketRequest bucketLocationRequest =  HeadBucketRequest.builder().bucket(bucketName).build();
			//GetBucketLocationRequest bucketLocationRequest = GetBucketLocationRequest.builder().bucket(bucketName).build();
			HeadBucketResponse headBucketResponse = s3Client.headBucket(bucketLocationRequest);
			//GetBucketLocationResponse bucketLocation = s3Client.getBucketLocation(bucketLocationRequest);
			log.info("Bucket location(s) {}",headBucketResponse);
		}catch(AwsServiceException exception){
			log.info("AwsServiceException occurs {}",exception.getStackTrace().toString());
			String errorMessage = exception.awsErrorDetails().errorMessage();
			log.info("Error message {}",errorMessage);
			String region = getRegionFromMessage(errorMessage);
			log.info("Region is {}",region);
			if(region == null){
				log.error("Error while creating s3 client {}", errorMessage);
				throw new BucketNotFoundException(BUCKET_NOT_EXIST);
			}
			s3Client = S3Client
					.builder()
					.region(Region.of(region))
					.build();
			log.info("New s3client {}",s3Client);
			return s3Client;
		}catch(Exception e){
			log.info("Exception occurs while creating client");
			log.info(e.getMessage());
		}
		log.info("Returning client {}",s3Client);
		return s3Client;
	}

	private String getRegionFromMessage(String errorMessage) {
		log.info("Inside AwsUtils.getRegionFromMessage {}",errorMessage);
		String expectedRegion = null;
		// Define a regular expression pattern to match the expected region
		Pattern pattern = Pattern.compile(REGION_PATTERN);
		Matcher matcher = pattern.matcher(errorMessage);

		// Find the expected region
		if (matcher.find()) {
			expectedRegion = matcher.group(1);
			log.info("Expected region if match found {}",expectedRegion);
		}
		log.info("Expected region {}",expectedRegion);
		return expectedRegion;
	}
}
