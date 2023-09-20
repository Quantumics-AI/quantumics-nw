package ai.quantumics.api.adapter;

import ai.quantumics.api.model.IamConnection;
import ai.quantumics.api.req.AwsDatasourceRequest;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.gson.Gson;

import static ai.quantumics.api.constants.DatasourceConstants.ROLE_ARN_SESSION_NAME;

public class AwsS3IamRoleConnection implements AwsS3Connection{
    @Override
    public AmazonS3 getS3Connection(String connectionData, String cloudRegion) {
        Gson gson = new Gson();
        String roleArn = gson.fromJson(connectionData,IamConnection.class).getIam();
        AWSCredentialsProvider credentialsProvider = getAwsCredentialsProvider(roleArn, cloudRegion);

        // Create an Amazon S3 client using the assumed role's credentials.
        return AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(cloudRegion)
                .build();
    }

    public AWSCredentialsProvider getAwsCredentialsProvider(String roleArn, String cloudRegion) {
        int sessionDurationSeconds = 3600; // 1 hour

        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(cloudRegion)
                .build();
        // Create a session credentials provider that assumes the role with the specified duration.
        return new STSAssumeRoleSessionCredentialsProvider
                .Builder(roleArn, ROLE_ARN_SESSION_NAME)
                .withStsClient(stsClient)
                .withRoleSessionDurationSeconds(sessionDurationSeconds)
                .build();
    }
}

