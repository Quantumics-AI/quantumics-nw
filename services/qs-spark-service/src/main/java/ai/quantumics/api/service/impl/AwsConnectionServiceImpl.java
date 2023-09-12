package ai.quantumics.api.service.impl;

import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.ConnectionNotFoundException;
import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ai.quantumics.api.enums.AwsAccessType.IAM;

@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {

    @Autowired
    private AwsConnectionRepo awsConnectionRepo;

    @Override
    public AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Projects project) throws InvalidConnectionTypeException {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceName(awsDatasourceRequest.getDataSourceName());
        if (dataSources.isPresent()) {
            throw new BadRequestException("Data source name already exist.");
        }

        String connectionType = awsDatasourceRequest.getConnectionType();//access Type

        if (IAM.getAccessType().equals(connectionType)) {
            AWSDatasource awsDatasource = awsConnectionRepo.saveAndFlush(awsDatasourceMapper(awsDatasourceRequest, project));

            return createResponse(awsDatasource);
        } else {
            throw new InvalidConnectionTypeException("Invalid Connection Type");
        }
    }

    @Override
    public AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Integer id, Projects project) throws ConnectionNotFoundException {

        AWSDatasource dataSource = awsConnectionRepo.findById(id).orElseThrow(() -> new ConnectionNotFoundException("Connection not found"));
        dataSource.setDataSourceName(awsDatasourceRequest.getDataSourceName());
        dataSource.setModifiedBy(project.getModifiedBy());
        dataSource.setModifiedDate(DateTime.now().toDate());
        return createResponse(awsConnectionRepo.save(dataSource));
    }

    private AwsDatasourceResponse createResponse(AWSDatasource awsDatasource) {
        AwsDatasourceResponse response = new AwsDatasourceResponse();
        response.setId(awsDatasource.getId());
        response.setDataSourceName(awsDatasource.getDataSourceName());
        response.setConnectionType(awsDatasource.getConnectionType());
        response.setIamRole(awsDatasource.getCredentialOrRole());
        response.setCreatedDate(awsDatasource.getCreatedDate());
        response.setModifiedDate(awsDatasource.getModifiedDate());
        return response;
    }


    @Override
    public List<AwsDatasourceResponse> getAllConnection() {
        List<AwsDatasourceResponse> response = new ArrayList<>();
        List<AWSDatasource> awsDatasource = awsConnectionRepo.findAll();

        awsDatasource.forEach(datasource -> {
            response.add(createResponse(datasource));
        });

        return response;
    }

    @Override
    public AwsDatasourceResponse getConnectionByName(String datasourceName) {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceName(datasourceName);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException("No record found.");
        }
    }

    @Override
    public void deleteConnection(Integer id) throws ConnectionNotFoundException {
        AWSDatasource connection = awsConnectionRepo.findById(id).orElseThrow(() -> new ConnectionNotFoundException("Connection not found"));
        awsConnectionRepo.delete(connection);
    }

    private AWSDatasource awsDatasourceMapper(AwsDatasourceRequest awsDatasourceRequest, Projects project) {
        AWSDatasource awsDatasource = new AWSDatasource();
        String iamRole = "{" + "IAMRole " + ": " + awsDatasourceRequest.getIamRole() + "}";
        awsDatasource.setProjectId(awsDatasourceRequest.getProjectId());
        awsDatasource.setUserId(awsDatasourceRequest.getUserId());
        awsDatasource.setDataSourceName(awsDatasourceRequest.getDataSourceName());
        awsDatasource.setConnectionType(awsDatasourceRequest.getConnectionType());
        awsDatasource.setCredentialOrRole(iamRole);
        awsDatasource.setCreatedBy(project.getCreatedBy());
        awsDatasource.setModifiedBy(project.getModifiedBy());

        return awsDatasource;
    }
}