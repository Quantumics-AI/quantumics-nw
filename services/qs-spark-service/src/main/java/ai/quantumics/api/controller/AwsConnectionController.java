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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/saveConnection")
    public ResponseEntity<AwsDatasourceResponse> saveConnection(@RequestBody @Valid AwsDatasourceRequest awsDatasourceRequest)
            throws InvalidAccessTypeException {

            dbUtil.changeSchema("public");
            QsUserV2 user = validatorUtils.checkUser(awsDatasourceRequest.getUserId());
            Projects project = validatorUtils.checkProject(awsDatasourceRequest.getProjectId());
            dbUtil.changeSchema(project.getDbSchemaName());
            final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();

            AwsDatasourceResponse response = awsConnectionService.saveConnectionInfo(awsDatasourceRequest, userName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getConnections/{userId}/{projectId}")
    public ResponseEntity<List<AwsDatasourceResponse>> getConnectionInfo(@PathVariable(value = "userId") final int userId,
                                                                 @PathVariable(value = "projectId") final int projectId) throws Exception {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getActiveConnections());
    }

    @GetMapping("/getConnections/{userId}/{projectId}/{datasourceName}")
    public ResponseEntity<AwsDatasourceResponse> getConnectionByName(
            @PathVariable(value = "userId") final int userId,
            @PathVariable(value = "projectId") final int projectId,
            @PathVariable(value = "datasourceName") final String datasourceName)
            throws Exception {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());

        AwsDatasourceResponse allConnection = awsConnectionService.getConnectionByName(datasourceName.trim());

        return ResponseEntity.status(HttpStatus.OK).body(allConnection);

    }

    @GetMapping("/getConnection/{userId}/{projectId}/{id}")
    public ResponseEntity<AwsDatasourceResponse> getConnectionById(
            @PathVariable(value = "userId") final int userId,
            @PathVariable(value = "projectId") final int projectId,
            @PathVariable(value = "id") final int id)
            throws Exception {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());

        AwsDatasourceResponse allConnection = awsConnectionService.getConnectionById(id);

        return ResponseEntity.status(HttpStatus.OK).body(allConnection);

    }

    @DeleteMapping("/deleteConnection/{userId}/{projectId}/{id}")
    public ResponseEntity<Object> deleteConnection(@PathVariable(value = "userId") final int userId,
                                                                        @PathVariable(value = "projectId") final int projectId,
                                                                        @PathVariable(value = "id") final int id) throws DatasourceNotFoundException {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();
        awsConnectionService.deleteConnection(id, userName);
        return returnResInstance(HttpStatus.OK, "Data source deleted successfully.");
    }

    @GetMapping("/getAwsAccessTypes")
    public ResponseEntity<Map<AwsAccessType,String>> getAwsAccessTypes() {
        return ResponseEntity.status(HttpStatus.OK).body(AwsAccessType.getAccessTypeAsMap());
    }

    @PutMapping("/updateConnection/{id}")
    public ResponseEntity<AwsDatasourceResponse> updateConnection(@RequestBody @Valid  AwsDatasourceRequest awsDatasourceRequest,
                                                                  @PathVariable(value = "id") final int id)
            throws DatasourceNotFoundException {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(awsDatasourceRequest.getUserId());
        Projects project = validatorUtils.checkProject(awsDatasourceRequest.getProjectId());
        dbUtil.changeSchema(project.getDbSchemaName());
        final String userName = user.getQsUserProfile().getUserFirstName() + " "
                + user.getQsUserProfile().getUserLastName();
        AwsDatasourceResponse response = awsConnectionService.updateConnectionInfo(awsDatasourceRequest, id, userName);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    private ResponseEntity<Object> returnResInstance(HttpStatus code, String message) {
        HashMap<String, Object> genericResponse = new HashMap<>();
        genericResponse.put("code", code.value());
        genericResponse.put("message", message);
        return ResponseEntity.status(code).body(genericResponse);
    }
}
