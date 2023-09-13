package ai.quantumics.api.service;

import ai.quantumics.api.exceptions.ConnectionNotFoundException;
import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;

import java.util.List;

public interface AwsConnectionService {

    AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidConnectionTypeException;

    AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest,Integer id, String userName) throws ConnectionNotFoundException;

    List<AwsDatasourceResponse> getActiveConnections(boolean active);

    AwsDatasourceResponse getConnectionByNameAndActive(String datasourceName, boolean active);

    void deleteConnection(Integer id,boolean active,String userName) throws ConnectionNotFoundException;

}
