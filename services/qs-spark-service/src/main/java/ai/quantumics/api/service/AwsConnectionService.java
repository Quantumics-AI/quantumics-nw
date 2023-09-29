package ai.quantumics.api.service;

import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.impl.Folder;
import com.amazonaws.services.s3.model.Bucket;

import java.io.IOException;
import java.util.List;

public interface AwsConnectionService {

    AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidAccessTypeException;

    AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest,Integer id, String userName) throws DatasourceNotFoundException;

    List<AwsDatasourceResponse> getActiveConnections();

    AwsDatasourceResponse getConnectionByName(String datasourceName);

    AwsDatasourceResponse getConnectionById(Integer id);

    void deleteConnection(Integer id, String userName) throws DatasourceNotFoundException;

    List<String> getBuckets();

    String getFoldersAndFilePath(String bucketName) throws IOException;
}
