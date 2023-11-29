/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.controller;

import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRuleType;
import ai.quantumics.api.req.RuleTypesDTO;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleTypeService;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.vo.RuleTypeLevel;
import ai.quantumics.api.vo.RuleTypeResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.PUBLIC_SCHEMA;
import static ai.quantumics.api.constants.QsConstants.RULE_TYPE_NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/api/v1/ruletypes")
@Api(value = "QuantumSpark Service API ")
public class RuleTypeController {
    private final DbSessionUtil dbUtil;
    private final RuleTypeService ruleTypeService;
    private final ProjectService projectService;


    public RuleTypeController(
            final DbSessionUtil dbUtilCi,
            final ProjectService projectServiceCi,
            RuleTypeService ruleTypeServiceCi) {
        dbUtil = dbUtilCi;
        projectService = projectServiceCi;
        ruleTypeService = ruleTypeServiceCi;
    }

    @ApiOperation(value = "RuleType List", response = Json.class)
    @GetMapping("/{projectId}")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List All RuleTypes for Project ID")})
    public ResponseEntity<Object> getRuleTypes(
            @PathVariable(value = "projectId") final int projectId,
            @RequestParam(value = "sourceOnly") final boolean sourceOnly,
            @RequestParam(value = "allRuleTypes", defaultValue = "false") final boolean allRuleTypes) {
        dbUtil.changeSchema("public");
        final Projects project = projectService.getProject(projectId);
        final Map<String, Object> response = new HashMap<>();
        dbUtil.changeSchema(project.getDbSchemaName());
        try {
            List<QsRuleType> qsRuleTypes = ruleTypeService.getActiveRuleTypes(sourceOnly, allRuleTypes);
            prepareResponse(qsRuleTypes, response, project);
        } catch (Exception exception) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error -" + exception.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }
    @ApiOperation(value = "RuleType List", response = Json.class)
    @PostMapping("/filter/{projectId}")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List RuleTypes for Project ID")})
    public ResponseEntity<Object> getFilteredRuleTypes(
            @RequestBody final RuleTypesDTO ruleTypesDTO,
            @PathVariable(value = "projectId") final int projectId) {
        dbUtil.changeSchema(PUBLIC_SCHEMA);
        final Projects project = projectService.getProject(projectId);
        final Map<String, Object> response = new HashMap<>();
        dbUtil.changeSchema(project.getDbSchemaName());
        try {
            List<QsRuleType> qsRuleTypes = ruleTypeService.getFilteredRuleTypes(ruleTypesDTO);
            prepareResponse(qsRuleTypes, response, project);
        } catch (Exception exception) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error -" + exception.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }
    private static void prepareResponse(List<QsRuleType> qsRuleTypes, Map<String, Object> response, Projects project){
        List<RuleTypeResponse> responseList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(qsRuleTypes)) {
            Collections.sort(qsRuleTypes, Comparator.comparingInt(QsRuleType::getId));
            Map<String, List<QsRuleType>> groupedMap = qsRuleTypes.stream()
                    .collect(Collectors.groupingBy(
                            QsRuleType::getRuleTypeName,
                            LinkedHashMap::new, // Use LinkedHashMap to maintain order
                            Collectors.toList()
                    ));
            responseList = groupedMap.entrySet().stream()
                    .map(entry -> {
                        String ruleTypeName = entry.getKey();
                        List<QsRuleType> ruleTypes = entry.getValue();

                        RuleTypeResponse ruleTypeResponse = new RuleTypeResponse();
                        ruleTypeResponse.setRuleTypeName(ruleTypeName);// Assuming id is the same for all in the group

                        List<RuleTypeLevel> ruleTypeLevels = ruleTypes.stream()
                                .map(qsRuleType -> {
                                    RuleTypeLevel level = new RuleTypeLevel();
                                    level.setLevelName(qsRuleType.getLevelName());
                                    level.setColumnLevel(qsRuleType.isColumnLevel());
                                    return level;
                                })
                                .collect(Collectors.toList());

                        ruleTypeResponse.setLevel(ruleTypeLevels);
                        return ruleTypeResponse;
                    })
                    .collect(Collectors.toList());
            response.put("message", "Rule Type Listed Successfully");
        }else{
            response.put("message", RULE_TYPE_NOT_FOUND);
        }
        response.put("code", HttpStatus.SC_OK);
        response.put("projectName", project.getProjectDisplayName());
        response.put("result", responseList);
    }
}
