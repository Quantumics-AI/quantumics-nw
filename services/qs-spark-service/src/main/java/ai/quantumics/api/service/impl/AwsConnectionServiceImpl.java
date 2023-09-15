package ai.quantumics.api.service.impl;

import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {

    @Autowired
    private AwsConnectionRepo awsConnectionRepo;

    @Override
    public AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidAccessTypeException {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCase(awsDatasourceRequest.getDataSourceName().trim());
        if (dataSources.isPresent()) {
            throw new BadRequestException("Entered Data Source Name is already used.Please enter a different name");
        }

        if (AwsAccessType.getAccessTypeAsMap().containsValue(awsDatasourceRequest.getAccessType())) {

            AWSDatasource awsDatasource = awsConnectionRepo.saveAndFlush(awsDatasourceMapper(awsDatasourceRequest, userName));

            return createResponse(awsDatasource);
        } else {
            throw new InvalidAccessTypeException("Invalid Access Type");
        }
    }

    @Override
    public AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Integer id, String userName) throws DatasourceNotFoundException {

        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new DatasourceNotFoundException("Data source does not exist."));
        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCaseAndActive(awsDatasourceRequest.getDataSourceName().trim(),true);
        if (dataSources.isPresent()) {
            throw new BadRequestException("Data source already exist.");
        }

        dataSource.setDataSourceName(awsDatasourceRequest.getDataSourceName());
        dataSource.setModifiedBy(userName);
        return createResponse(awsConnectionRepo.saveAndFlush(dataSource));
    }

    private AwsDatasourceResponse createResponse(AWSDatasource awsDatasource) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(awsDatasource,AwsDatasourceResponse.class);
    }

    @Override
    public List<AwsDatasourceResponse> getActiveConnections() {
        List<AwsDatasourceResponse> response = new ArrayList<>();
        Optional<List<AWSDatasource>> awsDatasource = awsConnectionRepo.findByActiveOrderByCreatedDateDesc(true);
        awsDatasource.get().forEach(datasource -> {
            response.add(createResponse(datasource));
        });

        return response;
    }
    @Override
    public AwsDatasourceResponse getConnectionByName(String datasourceName) {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByDataSourceNameIgnoreCaseAndActive(datasourceName,true);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException("No record found.");
        }
    }

    @Override
    public AwsDatasourceResponse getConnectionById(Integer id) {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByIdAndActive(id,true);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException("No record found.");
        }
    }

    @Override
    public void deleteConnection(Integer id, String userName) throws DatasourceNotFoundException {
        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new DatasourceNotFoundException("Data source does not exist."));
        dataSource.setActive(false);
        dataSource.setModifiedBy(userName);
        awsConnectionRepo.saveAndFlush(dataSource);
    }

    private AWSDatasource awsDatasourceMapper(AwsDatasourceRequest awsDatasourceRequest, String userName) {

        AWSDatasource awsDatasource = new AWSDatasource();
        awsDatasource.setProjectId(awsDatasourceRequest.getProjectId());
        awsDatasource.setUserId(awsDatasourceRequest.getUserId());
        awsDatasource.setDataSourceName(awsDatasourceRequest.getDataSourceName().trim());
        awsDatasource.setAccessType(awsDatasourceRequest.getAccessType());
        awsDatasource.setConnectionData(awsDatasourceRequest.getConnectionData());
        awsDatasource.setCreatedBy(userName);
        awsDatasource.setActive(true);
        return awsDatasource;
    }
}