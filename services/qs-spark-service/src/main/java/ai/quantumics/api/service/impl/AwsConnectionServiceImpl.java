package ai.quantumics.api.service.impl;


import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.service.AwsConnectionService;
import kong.unirest.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {

    @Autowired
    private AwsConnectionRepo awsConnectionRepo;

    @Override
    public AWSDatasource saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Projects project) {

        String connectionType = awsDatasourceRequest.getConnectionType();//access Type

        if ("IAM".equals(connectionType)) {
            AWSDatasource awsDatasource = awsDatasourceMapper(awsDatasourceRequest, project);

            return awsConnectionRepo.saveAndFlush(awsDatasource);
        } else {
            throw new InvalidConnectionTypeException("Invalid Connection Type");
        }
    }


    @Override
    public List<AWSDatasource> getAllConnection() {

        return awsConnectionRepo.findAll();
    }

    @Override
    public List<AWSDatasource> getDataSourceByName(String dataSource) {
        return awsConnectionRepo.findByDataSourceName(dataSource);
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
