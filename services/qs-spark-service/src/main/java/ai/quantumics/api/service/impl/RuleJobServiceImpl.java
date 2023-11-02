/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.constants.QsConstants;
import ai.quantumics.api.enums.RuleJobStatus;
import ai.quantumics.api.helper.ControllerHelper;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.repo.RuleJobRepository;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.req.CancelJobRequest;
import ai.quantumics.api.req.RuleJobRequest;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleJobService;
import ai.quantumics.api.service.UserServiceV2;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.util.RuleJobHelper;
import ai.quantumics.api.vo.DataSourceDetails;
import ai.quantumics.api.vo.RuleDetails;
import ai.quantumics.api.vo.RuleTypeDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RuleJobServiceImpl implements RuleJobService {


    private final RuleJobRepository ruleJobRepository;
    private final RuleRepository ruleRepository;
    private final DbSessionUtil dbUtil;
    private final ProjectService projectService;
    private final UserServiceV2 userService;
    private final ControllerHelper controllerHelper;
    private final RuleJobHelper ruleJobHelper;

    public RuleJobServiceImpl(RuleJobRepository ruleJobRepositoryCi,
                              DbSessionUtil dbUtilCi,
                              ProjectService projectServiceCi,
                              RuleRepository ruleRepositoryCi,
                              UserServiceV2 userServiceCi,
                              ControllerHelper controllerHelperCi,
                              RuleJobHelper ruleJobHelperCi) {
        this.ruleJobRepository = ruleJobRepositoryCi;
        this.dbUtil = dbUtilCi;
        this.projectService = projectServiceCi;
        this.userService = userServiceCi;
        this.ruleRepository = ruleRepositoryCi;
        this.controllerHelper = controllerHelperCi;
        this.ruleJobHelper = ruleJobHelperCi;
    }


    @Override
    public ResponseEntity<Object> runRuleJob(RuleJobRequest ruleJobRequest, int userId, int projectId) {
        final Map<String, Object> response = new HashMap<>();
		List<Integer> inProcessRules = new ArrayList<>();
        log.info("Invoking RunRuleJob  API for ruleIds {}", ruleJobRequest.toString());
        try {
            dbUtil.changeSchema("public");
            final Projects project = projectService.getProject(projectId, userId);
            if (project == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested project with Id: " + projectId + " for User with Id: " + userId + " not found.");

                return ResponseEntity.ok().body(response);
            }
            QsUserV2 userObj = userService.getUserById(userId);
            if (userObj == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested User with Id: " + userId + " not found.");
                return ResponseEntity.ok().body(response);
            }

            dbUtil.changeSchema(project.getDbSchemaName());
            for (Integer ruleId : ruleJobRequest.getRuleIds()) {

                QsRuleJob ruleJob = ruleJobRepository.findByRuleIdAndActiveIsTrue(ruleId);
                if (ruleJob != null && ruleJob.getJobStatus().equals(RuleJobStatus.INPROCESS.getStatus())) {
					inProcessRules.add(ruleId);
					continue;
                }
                QsRule rule = ruleRepository.findByRuleId(ruleId);
                if (rule == null) {
                    response.put("code", HttpStatus.SC_BAD_REQUEST);
                    response.put("message", "Requested rule with Id: " + ruleId + " not found.");
                }
                if (ruleJob == null && ruleId > 0) {
                    ruleJob = new QsRuleJob();
                    ruleJob.setRuleId(ruleId);
                    ruleJob.setJobStatus(RuleJobStatus.NOT_STARTED.getStatus());
                    ruleJob.setUserId(userId);
                    ruleJob.setActive(true);
                    ruleJob.setCreatedDate(DateTime.now().toDate());
                    ruleJob.setModifiedDate(DateTime.now().toDate());
                    ruleJob.setJobSubmittedDate(DateTime.now().toDate());
                    ruleJob.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
                    ruleJob.setModifiedBy(ruleJob.getCreatedBy());
                } else {
                    ruleJob.setJobStatus(RuleJobStatus.NOT_STARTED.getStatus());
                    ruleJob.setJobOutput(null);
                    ruleJob.setJobSubmittedDate(DateTime.now().toDate());
                    ruleJob.setJobFinishedDate(null);
                    ruleJob.setBatchJobLog(null);
                    ruleJob.setBatchJobId(0);
                    ruleJob.setModifiedDate(DateTime.now().toDate());
                    ruleJob.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
                }
                ruleJob = ruleJobRepository.save(ruleJob);
                RuleDetails ruleDetails = convertToRuleDetails(rule);
                ruleJobHelper.submitRuleJob(ruleJob, ruleDetails, controllerHelper.getFullName(userObj.getQsUserProfile()), projectId);
            }
			if(CollectionUtils.isNotEmpty(inProcessRules)) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Following rules are already in process: " + inProcessRules);
				return ResponseEntity.ok().body(response);
			}
            response.put("code", HttpStatus.SC_OK);
            response.put("message", "Rule Job submitted successfully");
        } catch (final Exception ex) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error while submitting rule job:  " + ex.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }

    @Override
    public ResponseEntity<Object> cancelRuleJobs(CancelJobRequest ruleJobRequest, int userId, int projectId) {
        final Map<String, Object> response = new HashMap<>();
        log.info("Invoking cancelRuleJobs  API for ruleIds {}", ruleJobRequest.toString());
        try {
            dbUtil.changeSchema("public");
            final Projects project = projectService.getProject(projectId, userId);
            if (project == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested project with Id: " + projectId + " for User with Id: " + userId + " not found.");

                return ResponseEntity.ok().body(response);
            }
            QsUserV2 userObj = userService.getUserById(userId);
            if (userObj == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested User with Id: " + userId + " not found.");
                return ResponseEntity.ok().body(response);
            }

            dbUtil.changeSchema(project.getDbSchemaName());
            List<QsRuleJob> ruleJobs = ruleJobRepository.findByJobIdInAndActiveIsTrue(ruleJobRequest.getJobIds());
            for (QsRuleJob ruleJob : ruleJobs) {
                    ruleJob.setUserId(userId);
                    ruleJob.setJobStatus(RuleJobStatus.CANCELLED.getStatus());
                    ruleJob.setModifiedDate(QsConstants.getCurrentUtcDate());
                    ruleJob.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
                    ruleJobRepository.save(ruleJob);
                    if(ruleJob.getBatchJobId() >0) {
                        ruleJobHelper.cancelRuleJob(ruleJob.getBatchJobId());
                    }
            }
            response.put("code", HttpStatus.SC_OK);
            response.put("message", "Rule Jobs Cancelled successfully");
        } catch (final Exception ex) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error while Cancelling rule jobs:  " + ex.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }

    @Override
    public ResponseEntity<Object> fetchRuleJobList(int userId, int projectId) {
        final Map<String, Object> response = new HashMap<>();
        try {
            dbUtil.changeSchema("public");
            final Projects project = projectService.getProject(projectId);
            if (project == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested project with Id: " + projectId + " for User with Id: " + userId + " not found.");

                return ResponseEntity.ok().body(response);
            }
            QsUserV2 userObj = userService.getUserById(userId);
            if (userObj == null) {
                response.put("code", HttpStatus.SC_BAD_REQUEST);
                response.put("message", "Requested User with Id: " + userId + " not found.");

                return ResponseEntity.ok().body(response);
            }

            dbUtil.changeSchema(project.getDbSchemaName());
            List<QsRuleJob> ruleJobList = ruleJobRepository.findAllByActiveTrueOrderByModifiedDateDesc();
            if (CollectionUtils.isNotEmpty(ruleJobList)) {
                ObjectMapper mapper = new ObjectMapper();
                ruleJobList.forEach(ruleJob -> {
                    QsRule rule = ruleRepository.findByRuleId(ruleJob.getRuleId());
                    ruleJob.setRuleName(rule.getRuleName());
                    if (ruleJob.getBatchJobLog() != null) {
                        String batchLog = ruleJob.getBatchJobLog();
                        JsonNode node;
                        try {
                            node = mapper.readValue(batchLog, JsonNode.class);
                            ruleJob.setBatchJobLog(node.toPrettyString());
                        } catch (JsonProcessingException jsonProcessingException) {
                            log.error("Error while setting batch log :" + jsonProcessingException.getMessage());
                        }
                    }
                });
            }

            response.put("code", HttpStatus.SC_OK);
            response.put("message", "Rule Jobs Listed Successfully");
            response.put("projectName", project.getProjectDisplayName());
            response.put("result", ruleJobList);
        } catch (Exception exception) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error -" + exception.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }


    public RuleDetails convertToRuleDetails(QsRule qsRule) {
        Gson gson = new Gson();
        RuleDetails ruleDetails = new RuleDetails();
        ruleDetails.setRuleId(qsRule.getRuleId());
        ruleDetails.setRuleName(qsRule.getRuleName());
        ruleDetails.setRuleDescription(qsRule.getRuleDescription());
        ruleDetails.setSourceAndTarget(qsRule.isSourceAndTarget());
        ruleDetails.setSourceData(gson.fromJson(qsRule.getSourceData(), DataSourceDetails.class));
        ruleDetails.setTargetData(gson.fromJson(qsRule.getTargetData(), DataSourceDetails.class));
        ruleDetails.setRuleDetails(gson.fromJson(qsRule.getRuleDetails(), RuleTypeDetails.class));
        ruleDetails.setUserId(qsRule.getUserId());
        ruleDetails.setStatus(qsRule.getStatus());
        ruleDetails.setCreatedDate(qsRule.getCreatedDate());
        ruleDetails.setModifiedDate(qsRule.getModifiedDate());
        ruleDetails.setCreatedBy(qsRule.getCreatedBy());
        ruleDetails.setModifiedBy(qsRule.getModifiedBy());
        return ruleDetails;
    }

}
