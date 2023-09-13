package ai.quantumics.api.service.impl;

import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.ConnectionNotFoundException;
import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import ai.quantumics.api.service.UserServiceV2;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
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
    public AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidConnectionTypeException {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCaseAndActive(awsDatasourceRequest.getDataSourceName(),true);
        if (dataSources.isPresent()) {
            throw new BadRequestException("Data source name already exist.");
        }

        String connectionType = awsDatasourceRequest.getConnectionType();

        if (IAM.getAccessType().equals(connectionType)) {
            AWSDatasource awsDatasource = awsConnectionRepo.saveAndFlush(awsDatasourceMapper(awsDatasourceRequest, userName));

            return createResponse(awsDatasource);
        } else {
            throw new InvalidConnectionTypeException("Invalid Connection Type");
        }
    }

    @Override
    public AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Integer id, String userName) throws ConnectionNotFoundException {

        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new ConnectionNotFoundException("Connection not found"));
        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCaseAndActive(awsDatasourceRequest.getDataSourceName(),true);
        if (dataSources.isPresent()) {
            throw new BadRequestException("Data source name already exist.");
        }

        dataSource.setDataSourceName(awsDatasourceRequest.getDataSourceName());
        dataSource.setModifiedBy(userName);
        dataSource.setModifiedDate(DateTime.now().toDate());
        return createResponse(awsConnectionRepo.save(dataSource));
    }

    private AwsDatasourceResponse createResponse(AWSDatasource awsDatasource) {
        ModelMapper mapper = new ModelMapper();
        AwsDatasourceResponse response = mapper.map(awsDatasource,AwsDatasourceResponse.class);
        response.setIamRole(awsDatasource.getCredentialOrRole());
        return response;
    }

    @Override
    public List<AwsDatasourceResponse> getConnections(int userId, int projectId, boolean active) {
        List<AwsDatasourceResponse> response = new ArrayList<>();
        List<AWSDatasource> awsDatasource = awsConnectionRepo.findByUserIdAndProjectIdAndActiveOrderByCreatedDateDesc(userId,projectId,active);
        awsDatasource.forEach(datasource -> {
            response.add(createResponse(datasource));
        });

        return response;
    }
    @Override
    public AwsDatasourceResponse getConnectionByName(String datasourceName, boolean active) {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCaseAndActive(datasourceName,active);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException("No record found.");
        }
    }

    @Override
    public void deleteConnection(Integer id,boolean active,String userName) throws ConnectionNotFoundException {
        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new ConnectionNotFoundException("Connection not found"));
        dataSource.setActive(false);
        dataSource.setModifiedBy(userName);
        dataSource.setModifiedDate(DateTime.now().toDate());
        awsConnectionRepo.saveAndFlush(dataSource);
    }

    private AWSDatasource awsDatasourceMapper(AwsDatasourceRequest awsDatasourceRequest, String userName) {

        AWSDatasource awsDatasource = new AWSDatasource();

        String iamRole = "{" + "IAMRole " + ": " + awsDatasourceRequest.getIamRole() + "}";
        awsDatasource.setProjectId(awsDatasourceRequest.getProjectId());
        awsDatasource.setUserId(awsDatasourceRequest.getUserId());
        awsDatasource.setDataSourceName(awsDatasourceRequest.getDataSourceName());
        awsDatasource.setConnectionType(awsDatasourceRequest.getConnectionType());
        awsDatasource.setCredentialOrRole(iamRole);
        awsDatasource.setCreatedBy(userName);
        awsDatasource.setCreatedDate(DateTime.now().toDate());
        awsDatasource.setActive(true);
        return awsDatasource;
    }
}