package ai.quantumics.api.service.impl;

import ai.quantumics.api.AwsCustomConfiguration;
import ai.quantumics.api.adapter.AwsAdapter;
import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.enums.RuleStatus;
import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.BucketNotFoundException;
import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.model.AWSDatasource;
import ai.quantumics.api.model.ObjectMetadata;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.repo.AwsConnectionRepo;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.res.BucketDetails;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.CLIENT_NAME_NOT_CONFIGURED;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_FAILED;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_SUCCESSFUL;
import static ai.quantumics.api.constants.DatasourceConstants.CORREPTED_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.CSV_EXTENSION;
import static ai.quantumics.api.constants.DatasourceConstants.CSV_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_NOT_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_UPDATED;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_BUCKET;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_BUCKET_REGIONS;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.FILE_NAME_NOT_NULL;
import static ai.quantumics.api.constants.DatasourceConstants.Files;
import static ai.quantumics.api.constants.DatasourceConstants.INVALID_ACCESS_TYPE;
import static ai.quantumics.api.constants.DatasourceConstants.NATWEST;
import static ai.quantumics.api.constants.DatasourceConstants.NOT_WELL_FORMATTED;
import static ai.quantumics.api.constants.DatasourceConstants.NO_IMPLEMENTATION_AVAILABLE;
import static ai.quantumics.api.constants.DatasourceConstants.POUND_DELIMITTER;
import static ai.quantumics.api.constants.DatasourceConstants.QUANTUMICS;
import static ai.quantumics.api.constants.DatasourceConstants.RULE_ATTACHED;
import static ai.quantumics.api.constants.QsConstants.DELIMITER;
@Slf4j
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
    @Autowired
    private RuleRepository ruleRepository;

    @Value("${qs.aws.use.config.buckets}")
    private boolean isUseConfigBuckets;

    @Value("${qs.aws.config.buckets}")
    private String configBucketNames;

    @Value("${qs.client.name}")
    private String clientName;

    @Value("${qs.aws.quantumics.bucket.region}")
    private String quantumicsBucketRegionNames;

    @Value("${qs.aws.natwest.bucket.region}")
    private String natwestBucketRegionNames;
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
    public ResponseEntity<Object> updateConnectionInfo(AwsDatasourceRequest awsDatasourceRequest, Integer id, String userName) throws DatasourceNotFoundException {
        final Map<String, Object> response = new HashMap<>();
        AWSDatasource dataSource = awsConnectionRepo.findByIdAndActive(id,true).orElseThrow(() -> new DatasourceNotFoundException(DATA_SOURCE_NOT_EXIST));
        Optional<AWSDatasource> sameDataSource = awsConnectionRepo.findByIdAndConnectionNameIgnoreCaseAndActiveTrue(id, awsDatasourceRequest.getConnectionName());
        if (sameDataSource.isPresent()) {
            dataSource.setModifiedBy(userName);
            dataSource.setModifiedDate(DateTime.now().toDate());
            response.put("code", HttpStatus.SC_OK);
            response.put("message", DATA_SOURCE_UPDATED);
            response.put("result", createResponse(awsConnectionRepo.saveAndFlush(dataSource)));
            return ResponseEntity.ok().body(response);
        }
        Optional<AWSDatasource> dataSources = awsConnectionRepo.findByConnectionNameIgnoreCaseAndActiveTrue(awsDatasourceRequest.getConnectionName().trim());
        if (dataSources.isPresent()) {
            response.put("code", HttpStatus.SC_OK);
            response.put("message", DATA_SOURCE_EXIST);
            return ResponseEntity.ok().body(response);
        }

        dataSource.setConnectionName(awsDatasourceRequest.getConnectionName());
        dataSource.setModifiedBy(userName);
        dataSource.setModifiedDate(DateTime.now().toDate());
        response.put("code", HttpStatus.SC_OK);
        response.put("message", DATA_SOURCE_UPDATED);
        response.put("result", createResponse(awsConnectionRepo.saveAndFlush(dataSource)));
        return ResponseEntity.ok().body(response);
    }

    private AwsDatasourceResponse createResponse(AWSDatasource awsDatasource) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(awsDatasource,AwsDatasourceResponse.class);
    }

    @Override
    public Page<AwsDatasourceResponse> getActiveConnections(int page, int pageSize) {
        final Map<String, Object> response = new HashMap<>();
        Pageable paging = PageRequest.of(page-1, pageSize);
        Page<AWSDatasource> awsDatasource = awsConnectionRepo.findByActiveTrueOrderByCreatedDateDesc(paging);
        return awsDatasource.map(this::createResponse);
    }
    @Override
    public ResponseEntity<Object> getConnectionByName(String datasourceName, int page, int pageSize, boolean filter) {
        final Map<String, Object> response = new HashMap<>();
        Page<AWSDatasource> dataSources;
        Pageable paging = PageRequest.of(page-1, pageSize);
        if (filter){
            dataSources = awsConnectionRepo.findByActiveTrueAndConnectionNameStartingWithIgnoreCaseOrActiveTrueAndConnectionNameEndingWithIgnoreCaseOrderByCreatedDateDesc(datasourceName, datasourceName, paging);
        }else{
            dataSources = awsConnectionRepo.findByConnectionNameIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(datasourceName, paging);
        }

        if (!dataSources.isEmpty()) {
            response.put("code", HttpStatus.SC_OK);
            response.put("message", DATA_SOURCE_EXIST);
            response.put("result", dataSources.map(this::createResponse));
            response.put("isExist",true);
        }else{
            response.put("code", HttpStatus.SC_BAD_REQUEST);
            response.put("message", DATA_SOURCE_NOT_EXIST);
            response.put("isExist",false);
        }
        return ResponseEntity.ok().body(response);
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
        List<String> status = Arrays.asList(RuleStatus.ACTIVE.getStatus(),RuleStatus.INACTIVE.getStatus());
        List<QsRule> rules = ruleRepository.findByStatusInAndSourceDatasourceIdOrStatusInAndTargetDatasourceId(status,id,status,id);

        if(!CollectionUtils.isEmpty(rules)) {
            throw new BadRequestException(String.format(RULE_ATTACHED, rules.size()));
        }

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
        List<S3ObjectSummary> objectSummaries = new ArrayList<>();
        listObjects(bucketName, "",objectSummaries);
        return getFoldersAndFilePathHierarchy(objectSummaries);
    }

    @Override
    public String testConnection(AwsDatasourceRequest request) {
        awsS3Client = awsCustomConfiguration.amazonS3Client(request.getAccessType().trim(), request.getRegion());
        awsAdapter.createS3BucketClient(request.getBucketName());
        return CONNECTION_SUCCESSFUL;
    }
    public String testsConnection(AwsDatasourceRequest request) {
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
            try {
                amazonS3Client = awsCustomConfiguration.amazonS3Client(request.getAccessType(), request.getRegion());
                amazonS3Client.listBuckets();
            } catch(Exception e){
                    log.info(e.getMessage());
                    throw new BadRequestException(CONNECTION_FAILED);
                }
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
        //AmazonS3 s3Client = awsAdapter.createS3BucketClient(bucketName);
        S3Object s3Object = awsS3Client.getObject(bucketName, file);
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

    @Override
    public List<AwsDatasourceResponse> searchConnection(String datasourceName) {
        List<AwsDatasourceResponse> response = new ArrayList<>();
        List<AWSDatasource> awsDatasource = awsConnectionRepo.findByActiveAndConnectionNameStartingWithIgnoreCaseOrActiveAndConnectionNameEndingWithIgnoreCase(true, datasourceName,true, datasourceName);
        if(CollectionUtils.isEmpty(awsDatasource)){
            throw new BadRequestException(DATA_SOURCE_NOT_EXIST);
        }
        awsDatasource.forEach(datasource -> {
            response.add(createResponse(datasource));
        });
        return response;
    }

    @Override
    public List<BucketDetails> getBucketRegions() {
        if(StringUtils.isEmpty(clientName)) {
            throw new BadRequestException(CLIENT_NAME_NOT_CONFIGURED);
        }
        switch (clientName) {
            case QUANTUMICS:
                List<String> qsBucketRegions = Arrays.asList(quantumicsBucketRegionNames.split(DELIMITER));
                if(CollectionUtils.isEmpty(qsBucketRegions) || StringUtils.isEmpty(qsBucketRegions.get(0))) {
                    throw new BadRequestException(EMPTY_BUCKET_REGIONS);
                }
                return parseBucketRegions(qsBucketRegions);
            case NATWEST:
                List<String> nwBucketRegions = Arrays.asList(natwestBucketRegionNames.split(DELIMITER));
                if(CollectionUtils.isEmpty(nwBucketRegions) || StringUtils.isEmpty(nwBucketRegions.get(0))) {
                    throw new BadRequestException(EMPTY_BUCKET_REGIONS);
                }
                return parseBucketRegions(nwBucketRegions);
            default:
                throw new BadRequestException(String.format(NO_IMPLEMENTATION_AVAILABLE, clientName));
        }
    }

    private static List<BucketDetails> parseBucketRegions(List<String> bucketRegions) {
        List<BucketDetails> formattedBuckets = new ArrayList<>();
        for (String bucketRegion : bucketRegions) {
            String[] parts = bucketRegion.split(POUND_DELIMITTER);
            if (parts.length == 2) {
                formattedBuckets.add(new BucketDetails(parts[0], parts[1]));
            }else{
                throw new BadRequestException(NOT_WELL_FORMATTED);
            }
        }
        return formattedBuckets;
    }
    private String getFoldersAndFilePathHierarchy(List<S3ObjectSummary> objectSummaries) throws IOException {
        log.info("Getting folder and file path hierarchy of S3ObjectSummary {}", objectSummaries);
        ObjectNode rootNode = createFolderHierarchy(objectSummaries);
        ObjectMapper objectMapper = new ObjectMapper();

        // Sort files within each folder and subfolder
        rootNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject()) {
                ObjectNode objectNode = (ObjectNode) entry.getValue();
                sortFiles(objectNode, objectMapper);
            }
        });
        return rootNode.toPrettyString();
    }

    private static void sortFiles(ObjectNode objectNode, ObjectMapper objectMapper) {
        log.info("Sorting objectNode {}",objectNode);
        if (objectNode.has(Files) && objectNode.get(Files).isArray()) {
            ArrayNode filesArray = (ArrayNode) objectNode.get(Files);
            List<JsonNode> filesList = new ArrayList<>();
            filesArray.elements().forEachRemaining(filesList::add);
            Collections.sort(filesList, Comparator
                    .<JsonNode, Long>comparing(fileNode -> fileNode.get("fileLastModified").asLong())
                    .reversed());
            // Clear the existing array and add the sorted elements
            filesArray.removeAll();
            filesList.forEach(filesArray::add);
        }
        // Recursively sort files in subfolders
        objectNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject()) {
                sortFiles((ObjectNode) entry.getValue(), objectMapper);
            }
        });
    }

    private void listObjects(String bucketName, String prefix, List<S3ObjectSummary> objectSummaries) {
        //AmazonS3 s3Client = awsAdapter.createS3BucketClient(bucketName);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);
        ListObjectsV2Result result = awsS3Client.listObjectsV2(request);

        for (String commonPrefix : result.getCommonPrefixes()) {
            listObjects(bucketName, commonPrefix, objectSummaries);
        }
        objectSummaries.addAll(result.getObjectSummaries());
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
        awsDatasource.setRegion(awsDatasourceRequest.getRegion());
        awsDatasource.setCreatedBy(userName);
        awsDatasource.setCreatedDate(DateTime.now().toDate());
        awsDatasource.setActive(true);
        return awsDatasource;
    }

    private static ObjectNode createFolderHierarchy(List<S3ObjectSummary> objectSummaries) {
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
