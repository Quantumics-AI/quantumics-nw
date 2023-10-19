package ai.quantumics.api.service.impl;

import ai.quantumics.api.AwsCustomConfiguration;
import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.service.AwsConnectionServiceV2;
import ai.quantumics.api.util.AwsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.util.Arrays;
import java.util.List;

import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_FAILED;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_SUCCESSFUL;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_BUCKET;
import static ai.quantumics.api.constants.QsConstants.DELIMITER;
@Slf4j
@Service
public class AwsConnectionServiceImplV2 implements AwsConnectionServiceV2 {
    @Autowired
    private AwsConnectionRepo awsConnectionRepo;
    @Autowired
    private AwsCustomConfiguration awsCustomConfiguration;
    @Autowired
    private S3Client s3ClientV2;
    @Autowired
    private AwsUtils awsUtils;

    @Value("${qs.aws.use.config.buckets}")
    private boolean isUseConfigBuckets;

    @Value("${qs.aws.config.buckets}")
    private String configBucketNames;

    @Override
    public String testConnection(String accessMethod) {
        log.debug("Inside AwsConnectionServiceImplV2.testConnection method with access method {}", accessMethod);
        if(isUseConfigBuckets) {
            log.debug("Inside if block {}",isUseConfigBuckets);
            if(StringUtils.isEmpty(configBucketNames)) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            List<String> buckets = Arrays.asList(configBucketNames.split(DELIMITER));
            log.debug("List of buckets {}", buckets);
            if(CollectionUtils.isEmpty(buckets) || StringUtils.isEmpty(buckets.get(0))) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            S3Client s3Client = awsUtils.createS3BucketClientV2(buckets.get(0));
            log.debug("Returned/Newly created s3Client {}",s3Client);
            if(s3Client == null) {
                throw new BadRequestException(CONNECTION_FAILED);
            }
        } else {
            log.debug("Inside else block {}",isUseConfigBuckets);
            ListBucketsResponse listBucketsResponse = s3ClientV2.listBuckets();
            log.debug("Buckets response {}",listBucketsResponse);
        }
        return CONNECTION_SUCCESSFUL;
    }
}