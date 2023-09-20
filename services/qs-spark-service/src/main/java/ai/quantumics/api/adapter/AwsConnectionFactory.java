package ai.quantumics.api.adapter;

import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;

import static ai.quantumics.api.constants.DatasourceConstants.INVALID_ACCESS_TYPE;

public class AwsConnectionFactory {

    public AwsS3Connection getAwsConnector(String connectorType) throws InvalidAccessTypeException{

        if(AwsAccessType.IAM.getAccessType().equalsIgnoreCase(connectorType)) {
            return new AwsS3IamRoleConnection();
        }else{
            throw new InvalidAccessTypeException(INVALID_ACCESS_TYPE);
        }
    }
}
