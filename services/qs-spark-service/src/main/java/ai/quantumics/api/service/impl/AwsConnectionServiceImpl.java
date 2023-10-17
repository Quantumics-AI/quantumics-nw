package ai.quantumics.api.service.impl;

import ai.quantumics.api.AwsCustomConfiguration;
import ai.quantumics.api.adapter.AwsAdapter;
import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.BucketNotFoundException;
import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.model.ObjectMetadata;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import ai.quantumics.api.vo.BucketFileContent;
import ai.quantumics.api.vo.ColumnDataType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_FAILED;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_SUCCESSFUL;
import static ai.quantumics.api.constants.DatasourceConstants.CORREPTED_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.CSV_EXTENSION;
import static ai.quantumics.api.constants.DatasourceConstants.CSV_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_NOT_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_BUCKET;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.FILE_NAME_NOT_NULL;
import static ai.quantumics.api.constants.DatasourceConstants.Files;
import static ai.quantumics.api.constants.DatasourceConstants.INVALID_ACCESS_TYPE;
import static ai.quantumics.api.constants.QsConstants.DELIMITER;

@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {
    @Autowired
    private AwsConnectionRepo awsConnectionRepo;
    @Autowired
    private AmazonS3 awsS3Client;
    @Autowired
    private AwsCustomConfiguration awsCustomConfiguration;
    private AmazonS3 amazonS3Client;
    @Autowired
    private AwsAdapter awsAdapter;

    @Value("${qs.aws.use.config.buckets}")
    private boolean isUseConfigBuckets;

    @Value("${qs.aws.config.buckets}")
    private String configBucketNames;

    @Override
    public AwsDatasourceResponse saveConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, String userName) throws InvalidAccessTypeException {

        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByConnectionNameIgnoreCaseAndActive(awsDatasourceRequest.getConnectionName().trim(),true);
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
        if(isUseConfigBuckets) {
            if(StringUtils.isEmpty(configBucketNames)) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            List<String> buckets = Arrays.asList(configBucketNames.split(DELIMITER));
            if(CollectionUtils.isEmpty(buckets) || StringUtils.isEmpty(buckets.get(0))) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            AmazonS3 s3Client = awsAdapter.createS3BucketClient(buckets.get(0));
            if(s3Client == null) {
                throw new BadRequestException(CONNECTION_FAILED);
            }
            return buckets;
        } else {
            List<Bucket> buckets = awsS3Client.listBuckets();
            if (buckets.isEmpty()) {
                throw new BucketNotFoundException(EMPTY_BUCKET);
            } else {
                return getBucketsName(buckets);
            }
        }
    }

    @Override
    public String getFoldersAndFilePath(String bucketName) throws IOException {
        List<String> objectNames = new ArrayList<>();
        List<S3ObjectSummary> objectSummaries = new ArrayList<>();
        listObjects(bucketName, "", objectNames,objectSummaries);
        return getFoldersAndFilePathHierarchy(objectNames,objectSummaries);
    }

    @Override
    public String testConnection(String accessMethod) {
        if(isUseConfigBuckets) {
            if(StringUtils.isEmpty(configBucketNames)) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            List<String> buckets = Arrays.asList(configBucketNames.split(DELIMITER));
            if(CollectionUtils.isEmpty(buckets) || StringUtils.isEmpty(buckets.get(0))) {
                throw new BadRequestException(EMPTY_BUCKET);
            }
            AmazonS3 s3Client = awsAdapter.createS3BucketClient(buckets.get(0));
            if(s3Client == null) {
                throw new BadRequestException(CONNECTION_FAILED);
            }
        } else {
            amazonS3Client = awsCustomConfiguration.amazonS3Client(accessMethod);
            amazonS3Client.listBuckets();
        }
        return CONNECTION_SUCCESSFUL;
    }

    @Override
    public BucketFileContent getContent(String bucketName, String file) {
        if(file == null){
            throw new BadRequestException(FILE_NAME_NOT_NULL);
        }else if(!file.endsWith(CSV_EXTENSION)){
            throw new BadRequestException(CSV_FILE);
        }
        List<Map<String, String>> data = new ArrayList<>();

        BucketFileContent bucketFileContent = new BucketFileContent();
        List<String> headers = new ArrayList<>();
        List<ColumnDataType> dataTypes = new ArrayList<>();
        AmazonS3 s3Client = awsAdapter.createS3BucketClient(bucketName);
        S3Object s3Object = s3Client.getObject(bucketName, file);
        S3ObjectInputStream objectInputStream = s3Object.getObjectContent();

        try (CSVReader reader = new CSVReader(new InputStreamReader(objectInputStream))) {
            String[] nextLine;
            String[] headerLine;
            int rowCount = 0;
            headerLine = reader.readNext();
            if(headerLine == null){
                throw new BadRequestException(EMPTY_FILE);
            }
            if(headerLine != null && headerLine.length > 0) {
                headers = Arrays.asList(headerLine);
            }

            if((nextLine = reader.readNext()) != null){
                Map<String, String> row = new HashMap<>();
                ColumnDataType columnDataType = null;
                for (int i = 0; i < nextLine.length ; i++) {
                    String header = headers.get(i);
                    String nxtLine = nextLine[i];
                    columnDataType = new ColumnDataType();
                    row.put(header, nxtLine);
                    columnDataType.setColumnName(header);
                    columnDataType.setDataType(AwsAdapter.getColumnDataType(nxtLine));
                    dataTypes.add(columnDataType);
                }
                data.add(row);
                rowCount++;
            }

            while ((nextLine = reader.readNext()) != null && rowCount < 500) {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < nextLine.length; i++) {
                    row.put(headers.get(i), nextLine[i]);
                }
                data.add(row);
                rowCount++;
            }
            bucketFileContent.setRowCount(rowCount);
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }catch(Exception e){
            throw new BadRequestException(CORREPTED_FILE);
        }
        bucketFileContent.setHeaders(headers);
        bucketFileContent.setContent(data);
        bucketFileContent.setColumnDatatype(dataTypes);
        return bucketFileContent;
    }

    private String getFoldersAndFilePathHierarchy(List<String> objectNames, List<S3ObjectSummary> objectSummaries) throws IOException {
        ObjectNode rootNode = createFolderHierarchy(objectNames, objectSummaries);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    private void listObjects(String bucketName, String prefix, List<String> objectNames, List<S3ObjectSummary> objectSummaries) {
        AmazonS3 s3Client = awsAdapter.createS3BucketClient(bucketName);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);
        ListObjectsV2Result result = s3Client.listObjectsV2(request);

        for (String commonPrefix : result.getCommonPrefixes()) {
            objectNames.add(commonPrefix);
            listObjects(bucketName, commonPrefix, objectNames, objectSummaries);
        }
        objectSummaries.addAll(result.getObjectSummaries());
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

    private static ObjectNode createFolderHierarchy(List<String> objectNames,List<S3ObjectSummary> objectSummaries) {
        ObjectNode rootNode = new ObjectMapper().createObjectNode();
        for(S3ObjectSummary objectSummary :objectSummaries){
            String path = objectSummary.getKey();
            String[] parts = path.split("/");
            addFolder(path, rootNode,  parts, 0,objectSummary);
        }
        return rootNode;
    }

    private static void addFolder(String folder, ObjectNode parentFolders, String[] parts, int depth,S3ObjectSummary objectSummary) {
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
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setFileName(fileName);
                objectMetadata.setFileLastModified(objectSummary.getLastModified());
                objectMetadata.setFileSize(objectSummary.getSize());
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode metaDataNode = objectMapper.valueToTree(objectMetadata);
                JsonNode node = parentFolders.get(Files);
                if(node == null){
                    ArrayNode filesNode = parentFolders.putArray(Files);
                    filesNode.add(metaDataNode);
                } else {
                    ((ArrayNode) node).add(metaDataNode);
                }

            } else {
                childFolders = new ObjectMapper().createObjectNode();
                parentFolders.set(folderName, childFolders);
            }
        } else {
            addFolder(folder, childFolders, parts, depth + 1,objectSummary);
        }
    }
}