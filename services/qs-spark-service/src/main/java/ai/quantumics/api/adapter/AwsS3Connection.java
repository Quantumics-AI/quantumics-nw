package ai.quantumics.api.adapter;

import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.req.AwsDatasourceRequest;
import com.amazonaws.services.s3.AmazonS3;

public interface AwsS3Connection {
    AmazonS3 getS3Connection(String connectionData, String cloudRegion) throws InvalidAccessTypeException;
}
