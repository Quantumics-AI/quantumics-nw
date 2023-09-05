package ai.quantumics.api.service;


import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.req.AwsDatasourceRequest;

import java.util.List;

public interface AwsConnectionService {

    AWSDatasource saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Projects project);

    List<AWSDatasource> getAllConnection();

    List<AWSDatasource> getDataSourceByName(String dataSource);

}
