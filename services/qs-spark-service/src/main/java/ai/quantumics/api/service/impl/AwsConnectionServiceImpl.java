package ai.quantumics.api.service.impl;

import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.BucketNotFoundException;
import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.*;

@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {

    @Autowired
    private AwsConnectionRepo awsConnectionRepo;
    @Autowired
    private AmazonS3 awsS3Client;

    @Override
    public AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidAccessTypeException {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByConnectionNameIgnoreCase(awsDatasourceRequest.getConnectionName().trim());
        if (dataSources.isPresent()) {
            throw new BadRequestException(DATA_SOURCE_EXIST);
        }

        if (AwsAccessType.getAccessTypeAsMap().containsValue(awsDatasourceRequest.getAccessType())) {

            AWSDatasource awsDatasource = awsConnectionRepo.saveAndFlush(awsDatasourceMapper(awsDatasourceRequest, userName));

            return createResponse(awsDatasource);
        } else {
            throw new InvalidAccessTypeException(INVALID_ACCESS_TYPE);
        }
    }

    @Override
    public AwsDatasourceResponse updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Integer id, String userName) throws DatasourceNotFoundException {

        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new DatasourceNotFoundException(DATA_SOURCE_NOT_EXIST));
        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByConnectionNameIgnoreCase(awsDatasourceRequest.getConnectionName().trim());
        if (dataSources.isPresent()) {
            throw new BadRequestException(DATA_SOURCE_EXIST);
        }

        dataSource.setConnectionName(awsDatasourceRequest.getConnectionName());
        dataSource.setModifiedBy(userName);
        dataSource.setModifiedDate(DateTime.now().toDate());
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

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByConnectionNameIgnoreCaseAndActive(datasourceName,true);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException(DATA_SOURCE_NOT_EXIST);
        }
    }

    @Override
    public AwsDatasourceResponse getConnectionById(Integer id) {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByIdAndActive(id,true);
        if (dataSources.isPresent()) {
            return createResponse(dataSources.get());
        }else{
            throw new BadRequestException(DATA_SOURCE_NOT_EXIST);
        }
    }

    @Override
    public void deleteConnection(Integer id, String userName) throws DatasourceNotFoundException {
        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new DatasourceNotFoundException(DATA_SOURCE_NOT_EXIST));
        dataSource.setActive(false);
        dataSource.setModifiedBy(userName);
        dataSource.setModifiedDate(DateTime.now().toDate());
        awsConnectionRepo.saveAndFlush(dataSource);
    }

    @Override
    public List<String> getBuckets() {
        List<Bucket> buckets = awsS3Client.listBuckets();
        if(buckets.isEmpty()){
            throw new BucketNotFoundException(EMPTY_BUCKET);
        }else {
            return getBucketsName(buckets);
        }
    }

    private List<String> getBucketsName(final List<Bucket> buckets) {
        return buckets.stream().map(Bucket::getName).collect(Collectors.toList());
    }

    private AWSDatasource awsDatasourceMapper(AwsDatasourceRequest awsDatasourceRequest, String userName) {

        AWSDatasource awsDatasource = new AWSDatasource();
        awsDatasource.setUserId(awsDatasourceRequest.getUserId());
        awsDatasource.setProjectId(awsDatasourceRequest.getProjectId());
        awsDatasource.setConnectionName(awsDatasourceRequest.getConnectionName().trim());
        awsDatasource.setSubDataSource(awsDatasourceRequest.getSubDataSource());
        awsDatasource.setAccessType(awsDatasourceRequest.getAccessType());
        awsDatasource.setBucketName(awsDatasourceRequest.getBucketName().trim());
        awsDatasource.setCreatedBy(userName);
        awsDatasource.setCreatedDate(DateTime.now().toDate());
        awsDatasource.setActive(true);
        return awsDatasource;
    }
}