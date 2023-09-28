package ai.quantumics.api.controller;

import ai.quantumics.api.service.RuleService;
import ai.quantumics.api.vo.RuleDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

@Slf4j
@RestController
@RequestMapping("/api/v1/qsrules")
@Api(value = "QuantumSpark Service API ")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(
            RuleService ruleServiceCi) {
        ruleService = ruleServiceCi;
    }

    @ApiOperation(value = "rules", response = Json.class)
    @PostMapping("/{projectId}")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "OKAY"),
                    @ApiResponse(code = 409, message = "Failure message")
            })
    public ResponseEntity<Object> saveRule(
            @RequestBody final RuleDetails ruleDetails,
            @PathVariable(value = "projectId") final int projectId)
            throws Exception {
        return ruleService.saveRule(ruleDetails, projectId);
    }

    @ApiOperation(value = "rules", response = Json.class)
    @PutMapping("/{projectId}")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "OKAY"),
                    @ApiResponse(code = 409, message = "Failure message")
            })
    public ResponseEntity<Object> editRule(
            @RequestBody final RuleDetails ruleDetails,
            @PathVariable(value = "projectId") final int projectId)
            throws Exception {
        return ruleService.editRule(ruleDetails, projectId);
    }

    @ApiOperation(value = "Rules", response = Json.class)
    @GetMapping("/{projectId}")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List All Rules for Project ID")})
    public ResponseEntity<Object> getFolders(
            @PathVariable(value = "projectId") final int projectId,
            @RequestParam(value = "status") final String status,
            @RequestParam(name = "page", required = true) int page,
            @RequestParam(name = "size", required = true) int size) {
        return ruleService.getRuleList(projectId, status, page, size);
    }



}
