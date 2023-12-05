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
import com.amazonaws.services.s3.model.HeadBucketRequest;
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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.http.HttpStatus;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import static ai.quantumics.api.constants.DatasourceConstants.COMMA_DELIMITER;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_FAILED;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_SUCCESSFUL;
import static ai.quantumics.api.constants.DatasourceConstants.CORREPTED_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.CSV_EXTENSION;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_NOT_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_UPDATED;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_BUCKET;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_FILE;
import static ai.quantumics.api.constants.DatasourceConstants.EMPTY_REGIONS;
import static ai.quantumics.api.constants.DatasourceConstants.FILE_NAME_NOT_NULL;
import static ai.quantumics.api.constants.DatasourceConstants.Files;
import static ai.quantumics.api.constants.DatasourceConstants.INVALID_ACCESS_TYPE;
import static ai.quantumics.api.constants.DatasourceConstants.INVALID_FILE_EXTENSION;
import static ai.quantumics.api.constants.DatasourceConstants.NOT_WELL_FORMATTED;
import static ai.quantumics.api.constants.DatasourceConstants.PARQUET_EXTENSION;
import static ai.quantumics.api.constants.DatasourceConstants.POUND_DELIMITTER;
import static ai.quantumics.api.constants.DatasourceConstants.REGION_PROPERTY_KEY;
import static ai.quantumics.api.constants.DatasourceConstants.REGION_PROPERTY_MISSING;
import static ai.quantumics.api.constants.DatasourceConstants.RULE_ATTACHED;
import static ai.quantumics.api.constants.QsConstants.DELIMITER;
import static ai.quantumics.api.constants.QsConstants.PARQUET_DATE;
import static ai.quantumics.api.constants.QsConstants.PARQUET_DATE_PATTERN;
import static ai.quantumics.api.constants.QsConstants.PARQUET_TIMESTAMP;
import static ai.quantumics.api.constants.QsConstants.PARQUET_TIMESTAMP_PATTERN;

@Slf4j
@Service
public class AwsConnectionServiceImpl implements AwsConnectionService {
    @Autowired
    private AwsConnectionRepo awsConnectionRepo;
    @Autowired
    private AmazonS3 awsS3Client;
    @Autowired
    private AwsCustomConfiguration awsCustomConfiguration;
    @Autowired
    private AwsAdapter awsAdapter;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private Environment environment;

    @Value("${qs.aws.use.config.buckets}")
    private boolean isUseConfigBuckets;

    @Value("${qs.aws.config.buckets}")
    private String configBucketNames;

    @Value("${qs.client.name}")
    private String clientName;

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
    public String getFoldersAndFilePath(String bucketName, String region, String accessType) throws IOException {
        List<S3ObjectSummary> objectSummaries = new ArrayList<>();
        listObjects(bucketName, "",objectSummaries, region, accessType);
        return getFoldersAndFilePathHierarchy(objectSummaries);
    }

    @Override
    public String testConnection(AwsDatasourceRequest request) {
        String region = request.getRegion();
        try {
            if(!region.equals(awsS3Client.getRegionName())){
                awsS3Client = awsCustomConfiguration.amazonS3Client(request.getAccessType().trim(), region);
            }
            //s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
            HeadBucketRequest bucketLocationRequest =  new HeadBucketRequest(request.getBucketName());
            awsS3Client.headBucket(bucketLocationRequest);
            log.info("Connection established success");
        }catch(Exception e){
            log.info(e.getMessage());
            throw new BadRequestException(CONNECTION_FAILED);
        }
        return CONNECTION_SUCCESSFUL;
    }

    @Override
    public BucketFileContent getContent(String bucketName, String file, String region, String accessType) {
        if(file == null){
            throw new BadRequestException(FILE_NAME_NOT_NULL);
        }else if(!(file.endsWith(CSV_EXTENSION))&& !(file.endsWith(PARQUET_EXTENSION))){
            throw new BadRequestException(INVALID_FILE_EXTENSION);
        }
        List<Map<String, String>> data = new ArrayList<>();

        BucketFileContent bucketFileContent = new BucketFileContent();
        List<String> headers = new ArrayList<>();
        List<ColumnDataType> dataTypes = new ArrayList<>();
        if(file.endsWith(PARQUET_EXTENSION)) {
            readParquetContent(data, bucketFileContent, headers, dataTypes, bucketName, file);
        } else {
            if(!region.equals(awsS3Client.getRegionName())){
                awsS3Client = awsCustomConfiguration.amazonS3Client(accessType, region);
            }
            S3Object s3Object = awsS3Client.getObject(bucketName, file);
            S3ObjectInputStream objectInputStream = s3Object.getObjectContent();

            return readCSVContent(data, bucketFileContent, headers, dataTypes, objectInputStream);
        }
        return bucketFileContent;
    }

    private static BucketFileContent readCSVContent(List<Map<String, String>> data, BucketFileContent bucketFileContent, List<String> headers, List<ColumnDataType> dataTypes, S3ObjectInputStream objectInputStream) {
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

    private static BucketFileContent readParquetContent(List<Map<String, String>> data, BucketFileContent bucketFileContent, List<String> headers, List<ColumnDataType> dataTypes, String bucketName, String file) {
        try {
            log.info("Reading parquet file method starts");
            // Parse the S3 URI
            String s3Uri = "s3a://" + bucketName + "/" + file;
            Configuration hadoopConfig = new Configuration();
            //Uncomment below lines to work in local
            /*AWSCredentials awsCredentials = DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
            hadoopConfig.set("fs.s3a.access.key", awsCredentials.getAWSAccessKeyId());
            hadoopConfig.set("fs.s3a.secret.key", awsCredentials.getAWSSecretKey());
            hadoopConfig.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");*/
            // Create InputFile
            InputFile inputFile = HadoopInputFile.fromPath(new Path(s3Uri), hadoopConfig);
            ParquetFileReader parquetFileReader = ParquetFileReader.open(inputFile);

            // Get the Parquet schema (MessageType)
            ParquetMetadata parquetMetadata = parquetFileReader.getFooter();
            MessageType schema = parquetMetadata.getFileMetaData().getSchema();

            // Get column names and data types
            List<ColumnDescriptor> columns = schema.getColumns();
            // Print column names and data types
            for (ColumnDescriptor column : columns) {
                headers.add(column.getPath()[0]);
                PrimitiveType primitiveType = column.getPrimitiveType();
                LogicalTypeAnnotation logicalTypeAnnotation = primitiveType.getLogicalTypeAnnotation();
                String logicalType = (logicalTypeAnnotation != null) ? logicalTypeAnnotation.toString() : primitiveType.getPrimitiveTypeName() != null ? primitiveType.getPrimitiveTypeName().toString() : "null";
                ColumnDataType columnDataType = new ColumnDataType();
                columnDataType.setColumnName(column.getPath()[0]);
                columnDataType.setDataType(getColumnDataType(logicalType));
                dataTypes.add(columnDataType);
            }

            int rowCount = 0;
            GroupReadSupport readSupport = new GroupReadSupport();
            readSupport.init(hadoopConfig, null, parquetMetadata.getFileMetaData().getSchema());

            try (ParquetReader<Group> parquetReader = ParquetReader.builder(readSupport, new Path(s3Uri)).build()) {
                Group group;
                while ( (group = parquetReader.read()) != null && rowCount < 500) {
                    Map<String, String> row = new HashMap<>();
                    for (ColumnDescriptor column : columns) {
                        String columnName = column.getPath()[0];
                        if (group.getFieldRepetitionCount(column.getPath().length) > 0) {
                            Object value = getParquetColumnValue(group, column);
                            row.put(columnName, value.toString());
                        } else {
                            row.put(columnName, "");
                        }
                    }
                    data.add(row);
                    rowCount++;
                }
            } catch (IOException e) {
                log.error("Error reading Parquet file {}", e.getMessage());
                e.printStackTrace();
            } catch(Exception e){
                log.error("Error reading Parquet file {}", e.getMessage());
                throw new BadRequestException(CORREPTED_FILE);
            }
            bucketFileContent.setHeaders(headers);
            bucketFileContent.setColumnDatatype(dataTypes);
            bucketFileContent.setContent(data);
            bucketFileContent.setRowCount(rowCount);
            return bucketFileContent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch(Exception e){
            throw new BadRequestException(CORREPTED_FILE);
        }
    }


    private static Object getParquetColumnValue(Group group, ColumnDescriptor column) {
        String columnName = column.getPath()[0];
        PrimitiveType.PrimitiveTypeName type = column.getPrimitiveType().getPrimitiveTypeName();

        switch (type) {
            case BINARY:
                try {
                    return group.getBinary(columnName, 0).toStringUsingUTF8();
                } catch (Exception e) {
                    return "";
                }
            case INT32:
                try {
                    if(column.getPrimitiveType().getLogicalTypeAnnotation() != null && column.getPrimitiveType().getLogicalTypeAnnotation().toString().equals(PARQUET_DATE)) {
                        int daysSinceEpoch = group.getInteger(columnName, 0);
                        LocalDate date = java.time.LocalDate.ofEpochDay(daysSinceEpoch);
                        String formattedDate = date.format(java.time.format.DateTimeFormatter.ofPattern(PARQUET_DATE_PATTERN));
                        return formattedDate;
                    }
                    return group.getInteger(columnName, 0);
                } catch (Exception e) {
                    return "";
                }
            case INT64:
                try {
                    if(column.getPrimitiveType().getLogicalTypeAnnotation() != null && column.getPrimitiveType().getLogicalTypeAnnotation().toString().contains(PARQUET_TIMESTAMP)) {
                        long timestampInSeconds = group.getLong(columnName, 0);
                        Instant instant = Instant.ofEpochMilli(timestampInSeconds/1000);
                        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PARQUET_TIMESTAMP_PATTERN);
                        String formattedDateTime = formatter.format(zonedDateTime);
                        return formattedDateTime;
                    }
                    return group.getLong(columnName, 0);
                } catch (Exception e) {
                    return "";
                }
            case FLOAT:
                try {
                    return group.getFloat(columnName, 0);
                } catch (Exception e) {
                    return "";
                }
            case DOUBLE:
                try {
                    return group.getDouble(columnName, 0);
                } catch (Exception e) {
                    return "";
                }
            case BOOLEAN:
                try {
                    return group.getBoolean(columnName, 0);
                }                catch (Exception e) {
                    return "";
                }
            default:
                throw new UnsupportedOperationException("Unsupported Parquet column type: " + type);
        }
    }

    public static String getColumnDataType(String value) {
        String dataType = "string"; // default type

        if(!StringUtils.isEmpty(value)) {
            if (value.equals("STRING")) {
                dataType = "string";
            } else if (value.contains("INT")) {
                dataType = "int";
            } else if (value.equals("FLOAT")) {
                dataType = "float";
            } else if (value.equals("DOUBLE")) {
                dataType = "double";
            } else if (value.equals("BOOLEAN")) {
                dataType = "boolean";
            } else if (value.equals("DATE") || value.contains("TIMESTAMP")) {
                dataType = "date";
            }
        }
        return dataType;
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
    public List<String> getRegions() {
        if(StringUtils.isEmpty(clientName)) {
            throw new BadRequestException(CLIENT_NAME_NOT_CONFIGURED);
        }
        String regionList = environment.getProperty(String.format(REGION_PROPERTY_KEY, clientName.toLowerCase()));
        if (regionList == null) {
            throw new BadRequestException(REGION_PROPERTY_MISSING);
        }
        List<String> regions = Arrays.asList(regionList.split(COMMA_DELIMITER));
        if(CollectionUtils.isEmpty(regions) || StringUtils.isEmpty(regions.get(0))) {
            throw new BadRequestException(EMPTY_REGIONS);
        }
        return regions;
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

    private void listObjects(String bucketName, String prefix, List<S3ObjectSummary> objectSummaries, String region, String accessType) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);
        if(!region.equals(awsS3Client.getRegionName())){
            awsS3Client = awsCustomConfiguration.amazonS3Client(accessType, region);
        }
        ListObjectsV2Result result = awsS3Client.listObjectsV2(request);

        for (String commonPrefix : result.getCommonPrefixes()) {
            listObjects(bucketName, commonPrefix, objectSummaries, region, accessType);
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
