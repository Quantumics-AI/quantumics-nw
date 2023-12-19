/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.enums.RuleJobStatus;
import ai.quantumics.api.enums.RuleStatus;
import ai.quantumics.api.helper.ControllerHelper;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.repo.RuleJobRepository;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.req.RuleTypes;
import ai.quantumics.api.req.RuleTypesDTO;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleService;
import ai.quantumics.api.service.RuleTypeService;
import ai.quantumics.api.service.UserServiceV2;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.util.PatternUtils;
import ai.quantumics.api.util.RuleJobHelper;
import ai.quantumics.api.util.ValidatorUtils;
import ai.quantumics.api.vo.DataSourceDetails;
import ai.quantumics.api.vo.RuleDetails;
import ai.quantumics.api.vo.RuleTypeDetails;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.DatasourceConstants.BUCKETNAME;
import static ai.quantumics.api.constants.DatasourceConstants.ERROR_FETCHING_RULE;
import static ai.quantumics.api.constants.DatasourceConstants.EXPECTED_IN_FILE_PATTERN;
import static ai.quantumics.api.constants.DatasourceConstants.FEED_NAME;
import static ai.quantumics.api.constants.DatasourceConstants.FILENAME;
import static ai.quantumics.api.constants.DatasourceConstants.FILE_NOT_ALIGN_WITH_PATTERN;
import static ai.quantumics.api.constants.DatasourceConstants.PUBLIC_SCHEMA;
import static ai.quantumics.api.constants.DatasourceConstants.RULE_NAME_EXIST;
import static ai.quantumics.api.constants.DatasourceConstants.RULE_NAME_NOT_EXIST;
import static ai.quantumics.api.constants.QsConstants.RULE_LEVEL_ALL;
@Slf4j
@Service
public class RuleServiceImpl implements RuleService {


	private final RuleRepository ruleRepository;
	private final DbSessionUtil dbUtil;
	private final RuleTypeService ruleTypeService;
	private final ProjectService projectService;
	private final ControllerHelper controllerHelper;
	private final UserServiceV2 userService;
	private final ValidatorUtils validatorUtils;
	private final RuleJobRepository ruleJobRepository;
	private final RuleJobHelper ruleJobHelper;

	public RuleServiceImpl(RuleRepository ruleRepositoryCi,
						   DbSessionUtil dbUtilCi,
						   RuleTypeService ruleTypeServiceCi,
						   ProjectService projectServiceCi,
						   ControllerHelper controllerHelperCi,
						   UserServiceV2 userServiceCi,
						   ValidatorUtils validatorUtilsCi,
						   RuleJobRepository ruleJobRepositoryCi,
						   RuleJobHelper ruleJobHelperCi) {
		this.ruleRepository = ruleRepositoryCi;
		this.dbUtil = dbUtilCi;
		this.ruleTypeService = ruleTypeServiceCi;
		this.projectService = projectServiceCi;
		this.controllerHelper = controllerHelperCi;
		this.userService = userServiceCi;
		this.validatorUtils = validatorUtilsCi;
		this.ruleJobRepository = ruleJobRepositoryCi;
		this.ruleJobHelper = ruleJobHelperCi;
	}


	@Override
	public ResponseEntity<Object> saveRule(RuleDetails ruleDetails, int userId, int projectId) {
		final Map<String, Object> response = new HashMap<>();
		log.info("Invoking saveRule  API {}", ruleDetails.toString());
		try {
			String targetFeedName = null;
			String targetFileName = null;
			String targetBucketName = null;
			String targetFilePattern = null;

			DataSourceDetails sourceDatails = ruleDetails.getSourceData();
			if(sourceDatails == null){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Source details can't be null");
				return ResponseEntity.ok().body(response);
			}

			String sourceFilePattern = sourceDatails.getFilePattern();
			if(sourceFilePattern == null){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Source file pattern can't be null");
				return ResponseEntity.ok().body(response);
			}
			String filePath = sourceDatails.getFilePath();
			if(filePath == null){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "File path can't be null");
				return ResponseEntity.ok().body(response);
			}
			String sourceFilePath = "s3://" + sourceDatails.getBucketName() + "/" + filePath;

			// Split strings
			String[] sourceFilePatternList = sourceFilePattern.split("/");
			// Check for the presence of elements
			List<String> missingElements = PatternUtils.findMissingElements(EXPECTED_IN_FILE_PATTERN, sourceFilePatternList);

			// Output the results
			if (!missingElements.isEmpty()) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", missingElements + " are missing in the file pattern");
				return ResponseEntity.ok().body(response);
			}
			String[] sourceFilePathList = sourceFilePath.split("/");
			// Check if both arrays have the same size
			if (sourceFilePatternList.length != sourceFilePathList.length) {
				log.info(FILE_NOT_ALIGN_WITH_PATTERN);
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", FILE_NOT_ALIGN_WITH_PATTERN);
				return ResponseEntity.ok().body(response);
			}

			if(ruleDetails.isSourceAndTarget()) {
				DataSourceDetails targetDetails = ruleDetails.getTargetData();
				if(targetDetails == null){
					response.put("code", HttpStatus.SC_BAD_REQUEST);
					response.put("message", "Target details can't be null");
					return ResponseEntity.ok().body(response);
				}
				targetFilePattern = targetDetails.getFilePattern();
				if(targetFilePattern == null){
					response.put("code", HttpStatus.SC_BAD_REQUEST);
					response.put("message", "Target file pattern can't be null");
					return ResponseEntity.ok().body(response);
				}
				String selectedFilePath = targetDetails.getFilePath();
				if(selectedFilePath == null){
					response.put("code", HttpStatus.SC_BAD_REQUEST);
					response.put("message", "Target file path can't be null");
					return ResponseEntity.ok().body(response);
				}
				String targetFilePath = "s3://" + targetDetails.getBucketName() + "/" + selectedFilePath;
                // Split strings
				String[] targetFilePatternList = targetFilePattern.split("/");
				// Check for the presence of elements
				List<String> missingElementsTarget = PatternUtils.findMissingElements(EXPECTED_IN_FILE_PATTERN, targetFilePatternList);

				// Output the results
				if (!missingElementsTarget.isEmpty()) {
					response.put("code", HttpStatus.SC_BAD_REQUEST);
					response.put("message", missingElements + " are missing in the file pattern");
					return ResponseEntity.ok().body(response);
				}
				String[] targetFilePathList = targetFilePath.split("/");
				// Check if both arrays have the same size
				if (targetFilePatternList.length != targetFilePathList.length) {
					log.info(FILE_NOT_ALIGN_WITH_PATTERN);
					response.put("code", HttpStatus.SC_BAD_REQUEST);
					response.put("message", FILE_NOT_ALIGN_WITH_PATTERN);
					return ResponseEntity.ok().body(response);
				}
				// Find the indices of expected elements
				Map<String, Integer> indicesTarget = PatternUtils.findIndicesOfElements(EXPECTED_IN_FILE_PATTERN, targetFilePatternList);
				targetBucketName = PatternUtils.getValueAtIndex(targetFilePathList, indicesTarget.get(BUCKETNAME));
				targetFeedName = PatternUtils.getValueAtIndex(targetFilePathList, indicesTarget.get(FEED_NAME));
				targetFileName = PatternUtils.getValueAtIndex(targetFilePathList, indicesTarget.get(FILENAME));
			}
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId, ruleDetails.getUserId());
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + ruleDetails.getUserId() +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(ruleDetails.getUserId());
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}

			// Find the indices of expected elements
			Map<String, Integer> indices = PatternUtils.findIndicesOfElements(EXPECTED_IN_FILE_PATTERN, sourceFilePatternList);
			String sourceBucketName = PatternUtils.getValueAtIndex(sourceFilePathList, indices.get(BUCKETNAME));
			String sourceFeedName = PatternUtils.getValueAtIndex(sourceFilePathList, indices.get(FEED_NAME));
			String sourceFileName = PatternUtils.getValueAtIndex(sourceFilePathList, indices.get(FILENAME));

			dbUtil.changeSchema(project.getDbSchemaName());
			Gson gson = new Gson();

			QsRule qsRule = new QsRule();
			qsRule.setRuleName(ruleDetails.getRuleName());
			qsRule.setRuleDescription(ruleDetails.getRuleDescription());
			qsRule.setSourceAndTarget(ruleDetails.isSourceAndTarget());
			qsRule.setSourceData(gson.toJson(ruleDetails.getSourceData()));
			if(ruleDetails.isSourceAndTarget()) {
				qsRule.setTargetData(gson.toJson(ruleDetails.getTargetData()));
				qsRule.setTargetDatasourceId(ruleDetails.getTargetData().getDataSourceId());
				qsRule.setTargetFilePattern(targetFilePattern);
				qsRule.setTargetBucketName(targetBucketName);
				qsRule.setTargetFeedName(targetFeedName);
				qsRule.setTargetFileName(targetFileName);
			}
			qsRule.setRuleDetails(gson.toJson(ruleDetails.getRuleDetails()));
			qsRule.setRuleTypeName(ruleDetails.getRuleDetails().getRuleTypeName());
			qsRule.setLevelName(ruleDetails.getRuleDetails().getRuleLevel().getLevelName());
			qsRule.setUserId(ruleDetails.getUserId());
			qsRule.setStatus(RuleStatus.ACTIVE.getStatus());
			qsRule.setCreatedDate(DateTime.now().toDate());
			qsRule.setModifiedDate(DateTime.now().toDate());
			qsRule.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
			qsRule.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
			qsRule.setSourceDatasourceId(ruleDetails.getSourceData().getDataSourceId());
			qsRule.setSourceFilePattern(sourceFilePattern);
			qsRule.setSourceBucketName(sourceBucketName);
			qsRule.setSourceFeedName(sourceFeedName);
			qsRule.setSourceFileName(sourceFileName);
			qsRule.setRuleRunDays(ruleDetails.getRuleRunDays());
			ruleRepository.save(qsRule);
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Data saved successfully");
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while saving rule :  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	@Override
	public ResponseEntity<Object> editRule(RuleDetails ruleDetails, int userId, int projectId) {
		final Map<String, Object> response = new HashMap<>();
		log.info("Invoking editRule  API {}", ruleDetails.toString());
		try {
			dbUtil.changeSchema("public");
			if(ruleDetails.getRuleId() == 0){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Invalid Rule Id");

				return ResponseEntity.ok().body(response);
			}
 			final Projects project = projectService.getProject(projectId, ruleDetails.getUserId());
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + ruleDetails.getUserId() +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(ruleDetails.getUserId());
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}
			dbUtil.changeSchema(project.getDbSchemaName());
			QsRule qsRule = ruleRepository.findByRuleId(ruleDetails.getRuleId());
			if(qsRule == null){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "No Rule found with Id: "+ ruleDetails.getRuleId());

				return ResponseEntity.ok().body(response);
			}

			Gson gson = new Gson();

			qsRule.setRuleName(ruleDetails.getRuleName());
			qsRule.setRuleDescription(ruleDetails.getRuleDescription());
			qsRule.setSourceAndTarget(ruleDetails.isSourceAndTarget());
			qsRule.setSourceData(gson.toJson(ruleDetails.getSourceData()));
			if(ruleDetails.isSourceAndTarget()) {
				qsRule.setTargetData(gson.toJson(ruleDetails.getTargetData()));
			}
			qsRule.setRuleDetails(gson.toJson(ruleDetails.getRuleDetails()));
			qsRule.setRuleTypeName(ruleDetails.getRuleDetails().getRuleTypeName());
			qsRule.setLevelName(ruleDetails.getRuleDetails().getRuleLevel().getLevelName());
			qsRule.setUserId(ruleDetails.getUserId());
			qsRule.setStatus(ruleDetails.getStatus());
			qsRule.setRuleRunDays(ruleDetails.getRuleRunDays());
			qsRule.setModifiedDate(DateTime.now().toDate());
			qsRule.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
			ruleRepository.save(qsRule);
			if(!RuleStatus.ACTIVE.getStatus().equals(ruleDetails.getStatus())){
				cancelJobs(qsRule.getRuleId(), userId, projectId);
			}
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Data updated successfully");
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while updating rule :  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	@Async("qsThreadPool")
	private void cancelJobs(int ruleId, int userId, int projectId) {
		log.info("Invoking cancelRuleJobs for ruleId {}", ruleId);
		try {
			dbUtil.changeSchema("public");
			QsUserV2 userObj = userService.getUserById(userId);
			if (userObj == null) {
				log.error("Requested User with Id: " + userId + " not found.");
				return;
			}
			final Projects project = projectService.getProject(projectId, userId);
			if (project == null) {
				log.error("Requested project with Id: " + projectId + " for User with Id: " + userId + " not found.");
				return;
			}
			dbUtil.changeSchema(project.getDbSchemaName());
			List<String> statuses = Arrays.asList(RuleJobStatus.INPROCESS.getStatus(), RuleJobStatus.NOT_STARTED.getStatus(), RuleJobStatus.IN_QUEUE.getStatus());
			List<QsRuleJob> ruleJobs = ruleJobRepository.findByRuleIdAndActiveIsTrueAndJobStatusIn(ruleId, statuses);
			List<QsRuleJob> ruleJobsToUpdate = new ArrayList<>();
			List<Integer> batchJobIds = new ArrayList<>();
			if(org.apache.commons.collections4.CollectionUtils.isEmpty(ruleJobs)) {
				log.info("No rule jobs found for ruleId {} to cancel", ruleId);
				return;
			}
			for (QsRuleJob ruleJob : ruleJobs) {
				ruleJob.setUserId(userId);
				ruleJob.setJobStatus(RuleJobStatus.CANCELLED.getStatus());
				ruleJob.setModifiedDate(DateTime.now().toDate());
				ruleJob.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
				ruleJobsToUpdate.add(ruleJob);
				if(ruleJob.getBatchJobId() >0) {
					batchJobIds.add(ruleJob.getBatchJobId());
				}
			}
			if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(ruleJobsToUpdate)) {
				ruleJobRepository.saveAll(ruleJobsToUpdate);
			}
			if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(batchJobIds)) {
				for (int batchJobId : batchJobIds) {
					ruleJobHelper.cancelRuleJob(batchJobId);
				}
			}
		} catch (final Exception ex) {
			log.error("Error while Cancelling rule jobs: " + ex.getMessage());
		}

	}

	@Override
	public ResponseEntity<Object> getRuleList(int userId, int projectId, String status, int page, int pageSize)  {
		final Map<String, Object> response = new HashMap<>();
		try {
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId);
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + userId +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(userId);
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}

			dbUtil.changeSchema(project.getDbSchemaName());
			Pageable paging = PageRequest.of(page-1, pageSize);
			Page<QsRule> rulesPage = ruleRepository.findAllByStatusOrderByCreatedDateDesc(status, paging);

			Page<RuleDetails> ruleDetailsPage = rulesPage.map(this::convertToRuleDetails);

			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Rules Listed Successfully");
			response.put("projectName", project.getProjectDisplayName());
			response.put("result", ruleDetailsPage);
		} catch (Exception exception) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error -" + exception.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	@Override
	public ResponseEntity<Object> getRule(int userId, int projectId, int ruleId) {
		final Map<String, Object> response = new HashMap<>();
		try {
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId, userId);
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + userId +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(userId);
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}
			dbUtil.changeSchema(project.getDbSchemaName());
			QsRule qsRule = ruleRepository.findByRuleId(ruleId);
			if(qsRule == null){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "No Rule found with Id: "+ ruleId);

				return ResponseEntity.ok().body(response);
			}

			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Rules Fetched Successfully");
			response.put("projectName", project.getProjectDisplayName());
			response.put("result", convertToRuleDetails(qsRule));
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while Fetching rule :  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}
	@Override
	public ResponseEntity<Object> searchRule(int userId, int projectId, String ruleName, List<String> status, int page, int pageSize) {
		final Map<String, Object> response = new HashMap<>();
		Page<QsRule> qsRule;
		try {
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId, userId);
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + userId +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(userId);
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}
			dbUtil.changeSchema(project.getDbSchemaName());
			Pageable paging = PageRequest.of(page-1, pageSize);
			if(CollectionUtils.isEmpty(status)) {
				qsRule = ruleRepository.findByRuleNameContainingIgnoreCaseOrderByCreatedDateDesc(ruleName, paging);
			}else{
				qsRule = ruleRepository.findByStatusInAndRuleNameContainingIgnoreCaseOrderByCreatedDateDesc(status, ruleName, paging);
			}

			if(qsRule.isEmpty()){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "No Rule found with Id: "+ ruleName);

				return ResponseEntity.ok().body(response);
			}
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Rules Fetched Successfully");
			response.put("projectName", project.getProjectDisplayName());
			response.put("result", qsRule.map(this::convertToRuleDetails));
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while Fetching rule :  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	@Override
	public ResponseEntity<Object> getRuleByName(int userId, int projectId, String ruleName, List<String> status) {
		final Map<String, Object> response = new HashMap<>();
		List<QsRule> qsRule;
		try {
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId, userId);
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + userId +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(userId);
			if(userObj == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested User with Id: "+ userId  +" not found.");

				return ResponseEntity.ok().body(response);
			}
			dbUtil.changeSchema(project.getDbSchemaName());
			qsRule = ruleRepository.findByRuleNameIgnoreCaseAndStatusIn(ruleName,status);
			if(CollectionUtils.isEmpty(qsRule)){
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", RULE_NAME_NOT_EXIST);
				response.put("isExist",false);

				return ResponseEntity.ok().body(response);
			}
			response.put("code", HttpStatus.SC_OK);
			response.put("message", RULE_NAME_EXIST);
			response.put("isExist",true);
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", ERROR_FETCHING_RULE + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}
	@Override
	public ResponseEntity<Object> filterByRuleType(int userId, int projectId, RuleTypesDTO ruleTypesDTO, int page, int pageSize, List<String> status) {
		final Map<String, Object> response = new HashMap<>();
		try {
			dbUtil.changeSchema(PUBLIC_SCHEMA);
			validatorUtils.checkUser(userId);
			final Projects project = validatorUtils.checkProject(projectId);
			dbUtil.changeSchema(project.getDbSchemaName());
			List<RuleTypes> ruleTypes = ruleTypesDTO.getRuleTypes();
			List<QsRule> filteredResponse;
			if(!CollectionUtils.isEmpty(ruleTypes)){
				Map<String, String> ruleTypeAndLevelMap = ruleTypes.stream().collect(Collectors.toMap(RuleTypes::getRuleTypeName, RuleTypes::getRuleLevel));
				List<String> ruleTypeList = ruleTypes.stream().map(RuleTypes::getRuleTypeName).collect(Collectors.toList());
				List<QsRule> dbResultList = ruleRepository.findByRuleTypeNameInAndStatusInOrderByCreatedDateDesc(ruleTypeList, status);
				filteredResponse = dbResultList.stream()
						.filter(dbResult -> {
							String ruleLevel = ruleTypeAndLevelMap.get(dbResult.getRuleTypeName());
							return RULE_LEVEL_ALL.equals(ruleLevel) || dbResult.getLevelName().equals(ruleLevel);
						}).collect(Collectors.toList());
			}else{
				filteredResponse = ruleRepository.findAllByStatusInOrderByCreatedDateDesc(status);
			}

			Page<QsRule> paginatedFilteredResponse = getPaginatedFilteredResponse(filteredResponse, page-1, pageSize);
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Rules Listed Successfully");
			response.put("projectName", project.getProjectDisplayName());
			response.put("result", paginatedFilteredResponse.map(this::convertToRuleDetails));
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
		ruleDetails.setRuleRunDays(qsRule.getRuleRunDays());
		ruleDetails.setCreatedDate(qsRule.getCreatedDate());
		ruleDetails.setModifiedDate(qsRule.getModifiedDate());
		ruleDetails.setCreatedBy(qsRule.getCreatedBy());
		ruleDetails.setModifiedBy(qsRule.getModifiedBy());
		return ruleDetails;
	}

	// Convert List<QsRule> to Page<QsRule>
	public <T> Page<T> getPaginatedFilteredResponse(List<T> filteredResponse, int pageNo, int pageSize) {
		int responseSize = filteredResponse.size();
		int start = pageNo * pageSize;
		PageRequest pageRequest = PageRequest.of(pageNo, pageSize);
		//Added equals condition to avoid extra calculations
		if (start >= responseSize) {
			return new PageImpl<>(List.of(), pageRequest, 0);
		}
		int end = Math.min((start + pageSize), responseSize);
		return new PageImpl<>(filteredResponse.subList(start, end), pageRequest, filteredResponse.size());
	}
}
