package ai.quantumics.api.controller;

import ai.quantumics.api.exceptions.BadRequestException;
import ai.quantumics.api.exceptions.InvalidConnectionTypeException;
import ai.quantumics.api.helper.ControllerHelper;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/aws")
public class AwsConnectionController {
    private final AwsConnectionService awsConnectionService;
    private final DbSessionUtil dbUtil;
    private final ValidatorUtils validatorUtils;

    private final ControllerHelper controllerHelper;

    public AwsConnectionController(AwsConnectionService awsConnectionService, DbSessionUtil dbUtil,
                                   ValidatorUtils validatorUtils, ControllerHelper controllerHelper) {
        this.awsConnectionService = awsConnectionService;
        this.dbUtil = dbUtil;
        this.validatorUtils = validatorUtils;
        this.controllerHelper = controllerHelper;
    }

    @PostMapping("/saveConnection")
    public ResponseEntity<AwsDatasourceResponse> saveConnection(@RequestBody @Valid AwsDatasourceRequest awsDatasourceRequest)
            throws Exception {

            dbUtil.changeSchema("public");
            QsUserV2 user = validatorUtils.checkUser(awsDatasourceRequest.getUserId());
            Projects project = validatorUtils.checkProject(awsDatasourceRequest.getProjectId());
            controllerHelper.getProjects(project.getProjectId(), user.getUserId());
            dbUtil.changeSchema(project.getDbSchemaName());
            AwsDatasourceResponse response = awsConnectionService.saveConnectionInfo(awsDatasourceRequest, project);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getConnections/{userId}/{projectId}")
    public ResponseEntity<List<AwsDatasourceResponse>> getConnectionInfo(@PathVariable(value = "userId") final int userId,
                                                                 @PathVariable(value = "projectId") final int projectId) throws Exception {

        dbUtil.changeSchema("public");
        QsUserV2 user = validatorUtils.checkUser(userId);
        Projects project = validatorUtils.checkProject(projectId);
        dbUtil.changeSchema(project.getDbSchemaName());
        return ResponseEntity.status(HttpStatus.OK).body(awsConnectionService.getAllConnection());
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

        AwsDatasourceResponse allConnection = awsConnectionService.getConnectionByName(datasourceName);

        return ResponseEntity.status(HttpStatus.OK).body(allConnection);

    }
}
