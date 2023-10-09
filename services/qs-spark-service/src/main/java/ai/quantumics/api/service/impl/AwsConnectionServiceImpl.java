package ai.quantumics.api.service.impl;

import ai.quantumics.api.AwsCustomConfiguration;
import ai.quantumics.api.adapter.AwsAdapter;
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
import ai.quantumics.api.vo.BucketFileContent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.*;

@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {
    @Autowired
    private AwsConnectionRepo awsConnectionRepo;
    @Autowired
    private AmazonS3 awsS3Client;
    @Autowired
    private AwsCustomConfiguration awsCustomConfiguration;
    private AmazonS3 amazonS3Client;

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

    @Override
    public String getFoldersAndFilePath(String bucketName) throws IOException {
        List<String> objectNames = new ArrayList<>();
        listObjects(bucketName, "", objectNames);
        return getFoldersAndFilePathHierarchy(objectNames);
    }

    @Override
    public String testConnection(String accessMethod) {
        amazonS3Client = awsCustomConfiguration.amazonS3Client(accessMethod);
        amazonS3Client.listBuckets();
        return CONNECTION_SUCCESSFUL;
    }

    @Override
    public BucketFileContent getContent(String bucketName, String file) {
        List<Map<String, String>> data = new ArrayList<>();
        BucketFileContent bucketFileContent = new BucketFileContent();
        List<String> headers = new ArrayList<>();

        S3Object s3Object = awsS3Client.getObject(bucketName, file);
        S3ObjectInputStream objectInputStream = s3Object.getObjectContent();

        try (CSVReader reader = new CSVReader(new InputStreamReader(objectInputStream))) {
            String[] nextLine;
            String[] headerLine;
            int rowCount = 0;
            headerLine = reader.readNext();
            if(headerLine != null && headerLine.length > 0) {
                headers = Arrays.asList(headerLine);
            }
            while ((nextLine = reader.readNext()) != null && rowCount < 500) {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < nextLine.length; i++) {
                    row.put(headers.get(i), nextLine[i]);
                }
                data.add(row);
                rowCount++;
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        bucketFileContent.setHeaders(headers);
        bucketFileContent.setContent(data);
        return bucketFileContent;
    }

    private String getFoldersAndFilePathHierarchy(List<String> objectNames) throws IOException {
        ObjectNode rootNode = createFolderHierarchy(objectNames);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    private void listObjects(String bucketName, String prefix, List<String> objectNames) {
        AmazonS3 s3Client = AwsAdapter.createNewS3Client(awsS3Client,bucketName);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);
        ListObjectsV2Result result = s3Client.listObjectsV2(request);

        for (String commonPrefix : result.getCommonPrefixes()) {
            objectNames.add(commonPrefix);
            listObjects(bucketName, commonPrefix, objectNames);
        }
        objectNames.addAll(Arrays.asList(result.getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .toArray(String[]::new)));
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

    private static ObjectNode createFolderHierarchy(List<String> objectNames) {
        ObjectNode rootNode = new ObjectMapper().createObjectNode();
        for (String path : objectNames) {
            String[] parts = path.split("/");
            addFolder(path, rootNode,  parts, 0);
        }
        return rootNode;
    }

    private static void addFolder(String folder, ObjectNode parentFolders, String[] parts, int depth) {
        if (depth >= parts.length) {
            return;
        }
        String folderName = parts[depth];
        ObjectNode childFolders = (ObjectNode) parentFolders.get(folderName);

        if (childFolders == null && depth != parts.length - 1) {
            childFolders = new ObjectMapper().createObjectNode();
            parentFolders.set(folderName, childFolders);
        }

        if (depth == parts.length - 1) {
            String fileName = parts[parts.length - 1];
            if(!folder.endsWith("/")){
                JsonNode node = parentFolders.get(Files);
                if(node == null){
                    ArrayNode filesNode = parentFolders.putArray(Files);
                    filesNode.add(fileName);
                } else {
                    ((ArrayNode) node).add(fileName);
                }
            } else {
                childFolders = new ObjectMapper().createObjectNode();
                parentFolders.set(folderName, childFolders);
            }
        } else {
            addFolder(folder, childFolders, parts, depth + 1);
        }
    }
}