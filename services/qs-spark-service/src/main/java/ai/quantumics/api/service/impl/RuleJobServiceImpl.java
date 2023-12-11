/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.constants.QsConstants;
import ai.quantumics.api.enums.BusinessDay;
import ai.quantumics.api.enums.RuleJobStatus;
import ai.quantumics.api.enums.RuleStatus;
import ai.quantumics.api.helper.ControllerHelper;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.repo.RuleJobRepository;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.req.CancelJobRequest;
import ai.quantumics.api.req.RuleData;
import ai.quantumics.api.req.RuleJobRequest;
import ai.quantumics.api.req.RunRuleJobRequest;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleJobService;
import ai.quantumics.api.service.UserServiceV2;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.util.RuleJobHelper;
import ai.quantumics.api.vo.DataSourceDetails;
import ai.quantumics.api.vo.RuleDetails;
import ai.quantumics.api.vo.RuleJobOutput;
import ai.quantumics.api.vo.RuleTypeDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ai.quantumics.api.constants.QsConstants.PUBLIC;

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
        int inProcessRulesCount = 0;
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
            List<String> statuses = Arrays.asList(RuleJobStatus.INPROCESS.getStatus(), RuleJobStatus.NOT_STARTED.getStatus(), RuleJobStatus.IN_QUEUE.getStatus());
            for (RuleData ruleData : ruleJobRequest.getRules()) {
                QsRule rule = ruleRepository.findByRuleId(ruleData.getRuleId());
                if (rule == null) {
                    log.error("Requested rule with Id: {} not found" + ruleData.getRuleId());
                    continue;
                }
                if (!rule.getStatus().equals(RuleStatus.ACTIVE.getStatus())) {
                    log.error("Requested rule with Id: {} is not active." + ruleData.getRuleId());
                    continue;
                }
                LocalDate businessDate = QsConstants.convertToLocalDate(ruleData.getBusinessDate());
                if (StringUtils.isNotEmpty(rule.getRuleRunDays()) && !rule.getRuleRunDays().contains(BusinessDay.valueOf(businessDate.getDayOfWeek().toString()).getDay())) {
                    QsRuleJob ruleJob = new QsRuleJob();
                    ruleJob.setRuleId(ruleData.getRuleId());
                    ruleJob.setJobStatus(RuleJobStatus.FAILED.getStatus());
                    ruleJob.setBatchJobLog("Rule is not configured to run on " + BusinessDay.valueOf(businessDate.getDayOfWeek().toString()).getDay() + ", Please check the rule configuration.");
                    ruleJob.setUserId(userId);
                    ruleJob.setActive(true);
                    ruleJob.setCreatedDate(DateTime.now().toDate());
                    ruleJob.setModifiedDate(DateTime.now().toDate());
                    ruleJob.setJobSubmittedDate(DateTime.now().toDate());
                    ruleJob.setJobFinishedDate(DateTime.now().toDate());
                    ruleJob.setBusinessDate(businessDate);
                    ruleJob.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
                    ruleJob.setModifiedBy(ruleJob.getCreatedBy());
                    ruleJobRepository.save(ruleJob);
                    continue;
                }
                List<QsRuleJob> ruleJobs = ruleJobRepository.findByRuleIdAndActiveIsTrueAndJobStatusInAndBusinessDate(ruleData.getRuleId(), statuses, businessDate);
                if (CollectionUtils.isNotEmpty(ruleJobs)) {
                    inProcessRules.add(ruleData.getRuleId());
                    inProcessRulesCount++;
                    continue;
                }

                QsRuleJob ruleJob = new QsRuleJob();
                ruleJob.setRuleId(ruleData.getRuleId());
                ruleJob.setJobStatus(RuleJobStatus.NOT_STARTED.getStatus());
                ruleJob.setUserId(userId);
                ruleJob.setActive(true);
                ruleJob.setCreatedDate(DateTime.now().toDate());
                ruleJob.setModifiedDate(DateTime.now().toDate());
                ruleJob.setJobSubmittedDate(DateTime.now().toDate());
                ruleJob.setBusinessDate(businessDate);
                ruleJob.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
                ruleJob.setModifiedBy(ruleJob.getCreatedBy());

                ruleJob = ruleJobRepository.save(ruleJob);
                RuleDetails ruleDetails = convertToRuleDetails(rule, ruleJob);
                ruleJobHelper.submitRuleJob(ruleJob, ruleDetails, controllerHelper.getFullName(userObj.getQsUserProfile()), projectId);
            }
            response.put("code", HttpStatus.SC_OK);
            if (inProcessRulesCount > 0) {
                if(inProcessRulesCount == ruleJobRequest.getRules().size()) {
                    response.put("message", "All the selected rules already inprocess");
                } else {
                    response.put("message", inProcessRulesCount + " rules already inprocess, remaining " + (ruleJobRequest.getRules().size() - inProcessRulesCount) + " rules submitted for processing");
                }
            } else {
                response.put("message", ruleJobRequest.getRules().size() + " rules submitted successfully for processing");
            }
        } catch (final Exception ex) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error while submitting rule job:  " + ex.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }

    @Override
    public ResponseEntity<Object> runBatchRuleJob(RunRuleJobRequest ruleJobRequest) {
        final Map<String, Object> response = new HashMap<>();
        try {
            ruleJobHelper.submitRuleJob(ruleJobRequest.getRuleJob(), ruleJobRequest.getRuleDetails(), ruleJobRequest.getModifiedBy(), ruleJobRequest.getProjectId());
            response.put("code", HttpStatus.SC_OK);
            response.put("message", "Rule job submitted successfully for ruleId: " + ruleJobRequest.getRuleJob().getRuleId());
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
                    ruleJob.setModifiedDate(DateTime.now().toDate());
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
                    ruleJob.setRuleTypeName(rule.getRuleTypeName());
                    ruleJob.setRuleLevelName(rule.getLevelName());
                    ruleJob.setRuleStatus(rule.getStatus());
                    if (StringUtils.isNotEmpty(ruleJob.getBatchJobLog())) {
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

    @Override
    public ResponseEntity<Object> getRowCount(String bucketName, String filePath, int userId, int projectId) {
        log.info("Invoking getRowCount API for bucketName {} and filePath {}", bucketName, filePath);
        final Map<String, Object> response = new HashMap<>();
        try {
            dbUtil.changeSchema(PUBLIC);
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
            RuleJobOutput jobOutput = ruleJobHelper.submitRowCountJob(bucketName, filePath, projectId);
            response.put("message", jobOutput.getJobOutput());
        } catch (final Exception ex) {
            response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.put("message", "Error while submitting row count job:  " + ex.getMessage());
        }
        return ResponseEntity.ok().body(response);
    }

    public RuleDetails convertToRuleDetails(QsRule qsRule, QsRuleJob ruleJob) {
        Gson gson = new Gson();
        RuleDetails ruleDetails = new RuleDetails();
        ruleDetails.setRuleId(qsRule.getRuleId());
        ruleDetails.setRuleName(qsRule.getRuleName());
        ruleDetails.setRuleDescription(qsRule.getRuleDescription());
        ruleDetails.setSourceAndTarget(qsRule.isSourceAndTarget());
        ruleDetails.setRuleRunDays(qsRule.getRuleRunDays());
        ruleDetails.setSourceData(gson.fromJson(qsRule.getSourceData(), DataSourceDetails.class));
        if(ruleDetails.getSourceData() != null) {
            String sourceFilePath = qsRule.getSourceFeedName() + "/" + QsConstants.convertToDDMMYYYY(ruleJob.getBusinessDate()) + "/" + qsRule.getSourceFileName();
            ruleDetails.getSourceData().setFilePath(sourceFilePath);
        }
        ruleDetails.setTargetData(gson.fromJson(qsRule.getTargetData(), DataSourceDetails.class));
        if(ruleDetails.getTargetData() != null) {
            String targetFilePath = qsRule.getTargetFeedName() + "/" + QsConstants.convertToDDMMYYYY(ruleJob.getBusinessDate()) + "/" + qsRule.getTargetFileName();
            ruleDetails.getTargetData().setFilePath(targetFilePath);
        }
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
