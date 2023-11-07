package ai.quantumics.api.service;

import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.vo.BucketFileContent;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface AwsConnectionService {

    AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidAccessTypeException;

    ResponseEntity<Object> updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest,Integer id, String userName) throws DatasourceNotFoundException;

    Page<AwsDatasourceResponse> getActiveConnections(int page, int pageSize);

    ResponseEntity<Object> getConnectionByName(String datasourceName, int page, int pageSize, boolean filter);

    AwsDatasourceResponse getConnectionById(Integer id);

    void deleteConnection(Integer id, String userName) throws DatasourceNotFoundException;

    List<String> getBuckets();

    String getFoldersAndFilePath(String bucketName) throws IOException;

    String testConnection(String accessMethod);

    BucketFileContent getContent(String bucketName, String file);
    List<AwsDatasourceResponse> searchConnection(String datasourceName);
}
