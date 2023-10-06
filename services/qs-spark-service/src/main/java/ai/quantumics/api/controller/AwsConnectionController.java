package ai.quantumics.api.controller;

import ai.quantumics.api.enums.AwsAccessType;
import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.exceptions.InvalidAccessTypeException;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.req.AwsDatasourceRequest;
import ai.quantumics.api.res.AwsDatasourceResponse;
import ai.quantumics.api.service.AwsConnectionService;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.util.ValidatorUtils;
import ai.quantumics.api.vo.BucketFileContent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ai.quantumics.api.constants.DatasourceConstants.DATA_SOURCE_DELETED;
import static ai.quantumics.api.constants.DatasourceConstants.PUBLIC_SCHEMA;

@RestController
@RequestMapping("/api/v1/aws")
public class AwsConnectionController {
    private final AwsConnectionService awsConnectionService;
    private final DbSessionUtil dbUtil;
    private final ValidatorUtils validatorUtils;

    public AwsConnectionController(AwsConnectionService awsConnectionService, DbSessionUtil dbUtil,
                                   ValidatorUtils validatorUtils) {
        this.awsConnectionService = awsConnectionService;
        this.dbUtil = dbUtil;
        this.validatorUtils = validatorUtils;
    }

    @PostMapping("/save")
    public ResponseEntity<AwsDatasourceResponse> saveConnection(@RequestBody @Valid AwsDatasourceRequest awsDatasourceRequest)
            throws InvalidAccessTypeException {

            dbUtil.changeSchema(PUBLIC_SCHEMA);
            QsUserV2 user = validatorUtils.checkUser(awsDatasourceRequest.getUserId());
            Projects project = validatorUtils.checkProject(awsDatasourceRequest.getProjectId());
            dbUtil.changeSchema(project.getDbSchemaName());
            final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();
            return ResponseEntity.status(HttpStatus.CREATED).body(awsConnectionService.saveConnectionInfo(awsDatasourceRequest, userName));
    }

    @GetMapping("/getConnections/{userId}/{projectId}")
    public ResponseEntity<List<AwsDatasourceResponse>> getConnectionInfo(@PathVariable(value = "userId") final int userId,
                                                                 @PathVariable(value = "projectId") final int projectId) throws Exception {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getActiveConnections());
    }

    @GetMapping("/getConnectionByName/{userId}/{projectId}/{datasourceName}")
    public ResponseEntity<AwsDatasourceResponse> getConnectionByName(
            @PathVariable(value = "userId") final int userId,
            @PathVariable(value = "projectId") final int projectId,
            @PathVariable(value = "datasourceName") final String datasourceName)
            throws Exception {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getConnectionByName(datasourceName.trim()));

    }

    @GetMapping("/getConnectionById/{userId}/{projectId}/{id}")
    public ResponseEntity<AwsDatasourceResponse> getConnectionById(
            @PathVariable(value = "userId") final int userId,
            @PathVariable(value = "projectId") final int projectId,
            @PathVariable(value = "id") final int id)
            throws Exception {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getConnectionById(id));

    }

    @DeleteMapping("/delete/{userId}/{projectId}/{id}")
    public ResponseEntity<Object> deleteConnection(@PathVariable(value = "userId") final int userId,
                                                                        @PathVariable(value = "projectId") final int projectId,
                                                                        @PathVariable(value = "id") final int id) throws DatasourceNotFoundException {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();
        awsConnectionService.deleteConnection(id, userName);
        return returnResInstance(HttpStatus.OK, DATA_SOURCE_DELETED);
    }

    @GetMapping("/getAwsAccessTypes")
    public ResponseEntity<Map<AwsAccessType,String>> getAwsAccessTypes() {
        return ResponseEntity.status(HttpStatus.OK).body(AwsAccessType.getAccessTypeAsMap());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<AwsDatasourceResponse> updateConnection(@RequestBody @Valid  AwsDatasourceRequest awsDatasourceRequest,
                                                                  @PathVariable(value = "id") final int id)
            throws DatasourceNotFoundException {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        QsUserV2 user = validatorUtils.checkUser(awsDatasourceRequest.getUserId());
        Projects project = validatorUtils.checkProject(awsDatasourceRequest.getProjectId());
        dbUtil.changeSchema(project.getDbSchemaName());
        final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.updateConnectionInfo(awsDatasourceRequest, id, userName));
    }

    @GetMapping("/buckets/{userId}/{projectId}")
    public ResponseEntity<List<String>> getBuckets(@PathVariable(value = "userId") final int userId,
                                                   @PathVariable(value = "projectId") final int projectId) {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        List<String> bucketsName = awsConnectionService.getBuckets();
        return ResponseEntity.status(HttpStatus.OK).body(bucketsName);
    }

    @GetMapping(value="/buckets/{userId}/{projectId}/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFoldersAndFilePath(@PathVariable(value = "userId") final int userId,
                                                   @PathVariable(value = "projectId") final int projectId,
                                                   @PathVariable(value = "bucketName") final String bucketName) throws IOException {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getFoldersAndFilePath(bucketName));
    }

    @PostMapping("/testConnection/{userId}/{projectId}")
    public ResponseEntity<Object> testConnection(@RequestBody AwsDatasourceRequest awsDatasourceRequest,
                                                 @PathVariable(value = "userId") final int userId,
                                                 @PathVariable(value = "projectId") final int projectId) {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return returnResInstance(HttpStatus.OK, awsConnectionService.testConnection(awsDatasourceRequest.getAccessType().trim()));
    }

    @GetMapping("/content/{userId}/{projectId}")
    public ResponseEntity<Object> getFileContent(@PathVariable(value = "userId") final int userId,
                                                   @PathVariable(value = "projectId") final int projectId,
                                                       @RequestParam(value = "bucket") final String bucket,
                                                       @RequestParam(value = "file") final String file) {

        dbUtil.changeSchema(PUBLIC_SCHEMA);
        validatorUtils.checkUser(userId);
        validatorUtils.checkProject(projectId);
        BucketFileContent content = awsConnectionService.getContent(bucket, file);
        return ResponseEntity.status(HttpStatus.OK).body(content);
    }

    private ResponseEntity<Object> returnResInstance(HttpStatus code, String message) {
        HashMap<String, Object> genericResponse = new HashMap<>();
        genericResponse.put("code", code.value());
        genericResponse.put("message", message);
        return ResponseEntity.status(code).body(genericResponse);
    }
}
