package ai.quantumics.api.service;

import ai.quantumics.api.exceptions.ConnectionNotFoundException;
import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;

import java.util.List;

public interface AwsConnectionService {

    AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Projects project) throws InvalidConnectionTypeException;

    AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest,Integer id, Projects project) throws ConnectionNotFoundException;

    List<AwsDatasourceResponse> getAllConnection();

    AwsDatasourceResponse getConnectionByName(String datasourceName);

    void deleteConnection(Integer id) throws ConnectionNotFoundException;

}
