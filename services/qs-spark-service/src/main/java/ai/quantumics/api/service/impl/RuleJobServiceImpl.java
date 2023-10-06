/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.adapter.AwsAdapter;
import ai.quantumics.api.constants.QsConstants;
import ai.quantumics.api.helper.ControllerHelper;
import ai.quantumics.api.livy.LivyActions;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.repo.RuleJobRepository;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.req.RuleJobRequest;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleJobService;
import ai.quantumics.api.service.UserServiceV2;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.vo.DataSourceDetails;
import ai.quantumics.api.vo.RuleDetails;
import ai.quantumics.api.vo.RuleTypeDetails;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ai.quantumics.api.constants.QsConstants.DQ_ROW_COUNT_TEMPLATE_NAME;

@Slf4j
@Service
public class RuleJobServiceImpl implements RuleJobService {


	private final RuleJobRepository ruleJobRepository;
	private final RuleRepository ruleRepository;
	private final DbSessionUtil dbUtil;
	private final ProjectService projectService;
	private final UserServiceV2 userService;
	private final ControllerHelper controllerHelper;
	private final AwsAdapter awsAdapter;
	private final LivyActions livyActions;

	public static final String SOURCE_BUCKET = "$SOURCE_BUCKET";
	public static final String SOURCE_PATH = "$SOURCE_PATH";
	public static final String TARGET_BUCKET = "$TARGET_BUCKET";
	public static final String TARGET_PATH = "$TARGET_PATH";
	public static final String JOB_ID = "$JOB_ID";
	public static final String RULE_ID = "$RULE_ID";
	public static final String DB_SCHEMA = "$DB_SCHEMA";
	public static final String MODIFIED_BY = "$MODIFIED_BY";
	public static final String RULE_TYPE_NAME = "$RULE_TYPE_NAME";
	public static final String LEVEL_NAME = "$LEVEL_NAME";

	@Value("${qs.rule.etl.output}")
	private String qsEtlScriptBucket;


	public RuleJobServiceImpl(RuleJobRepository ruleJobRepositoryCi,
                              DbSessionUtil dbUtilCi,
                              ProjectService projectServiceCi,
							  RuleRepository ruleRepositoryCi,
                              UserServiceV2 userServiceCi,
							  ControllerHelper controllerHelperCi,
							  AwsAdapter awsAdapterCi,
							  LivyActions livyActionsCi) {
		this.ruleJobRepository = ruleJobRepositoryCi;
		this.dbUtil = dbUtilCi;
		this.projectService = projectServiceCi;
		this.userService = userServiceCi;
		this.ruleRepository = ruleRepositoryCi;
		this.controllerHelper = controllerHelperCi;
		this.awsAdapter = awsAdapterCi;
		this.livyActions = livyActionsCi;
	}


	@Override
	public ResponseEntity<Object> runRuleJob(RuleJobRequest ruleJobRequest, int userId, int projectId) {
		final Map<String, Object> response = new HashMap<>();
		final StringBuilder fileContents = new StringBuilder();
		log.info("Invoking RunRuleJob  API {}", ruleJobRequest.toString());
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
			QsRuleJob ruleJob = ruleJobRepository.findByRuleId(ruleJobRequest.getRuleId());
			//TODO: Validate ruleJob
			if(ruleJob != null && ruleJob.getJobStatus().equals("Inprocess")) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Job already in process for this rule Id: "+ ruleJob.getRuleId());
			}
			if(ruleJob == null) {
				ruleJob = new QsRuleJob();
				ruleJob.setRuleId(ruleJobRequest.getRuleId());
				ruleJob.setJobStatus("Inprocess");
				ruleJob.setUserId(userId);
				ruleJob.setCreatedDate(QsConstants.getCurrentUtcDate());
				ruleJob.setModifiedDate(QsConstants.getCurrentUtcDate());
				ruleJob.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
				ruleJob.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
				ruleJob = ruleJobRepository.save(ruleJob);
			}

			QsRule rule = ruleRepository.findByRuleId(ruleJobRequest.getRuleId());
			if(rule == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested rule with Id: "+ ruleJobRequest.getRuleId() +" not found.");
			}
			RuleDetails ruleDetails = convertToRuleDetails(rule);

			String scriptStr = etlScriptVarsInit(fileContents, project, ruleJob, ruleDetails);
			final String jobName = getJobName(ruleJob);

			final URL s3ContentUpload = awsAdapter.s3ContentUpload(qsEtlScriptBucket, jobName, scriptStr);
			final String scriptFilePath = String.format("%s%s", qsEtlScriptBucket, jobName);
			log.info("uploaded to - s3 Full Path - {} and to path {}", s3ContentUpload, scriptFilePath);

			int batchId = livyActions.invokeRuleJobOperation(ruleJob.getJobId(), scriptFilePath);
			log.info("Batch Id received after the Livy Job submission is: {}", batchId);
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Rule Job submitted successfully");
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while submitting rule job:  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	private String etlScriptVarsInit(
			StringBuilder fileContents,
			Projects project,
			QsRuleJob ruleJob,
			RuleDetails ruleDetails) {

		String temp;

		temp = fileContents.toString().replace(SOURCE_BUCKET, String.format("'%s'", ruleDetails.getSourceData().getBucketName()));
		temp = temp.replace(SOURCE_PATH, String.format("'%s'", ruleDetails.getSourceData().getFilePath()));
		temp = temp.replace(TARGET_BUCKET, String.format("'%s'", ruleDetails.getTargetData().getBucketName()));
		temp = temp.replace(TARGET_PATH, String.format("'%s'", ruleDetails.getTargetData().getFilePath()));
		temp = temp.replace(JOB_ID, String.format("'%s'", ruleJob.getJobId()));
		temp = temp.replace(RULE_ID, String.format("'%s'", ruleJob.getRuleId()));
		temp = temp.replace(DB_SCHEMA, String.format("'%s'", project.getDbSchemaName()));
		temp = temp.replace(MODIFIED_BY, String.format("'%s'", ruleJob.getModifiedBy()));
		temp = temp.replace(RULE_TYPE_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleTypeName()));
		temp = temp.replace(LEVEL_NAME, String.format("'%s'", ruleDetails.getRuleDetails().getRuleLevel().getLevelName()));
		return temp;
	}

	private String getJobName(QsRuleJob qsRuleJob) {
		return String.format("%d-etl-%d.py", new Date().getTime(), qsRuleJob.getRuleId());
	}

	@Override
	public ResponseEntity<Object> fetchRuleJobList(int userId, int projectId) {
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
			List<QsRuleJob> ruleJobList = ruleJobRepository.findAll();
			if(CollectionUtils.isNotEmpty(ruleJobList)) {
				ruleJobList.forEach(ruleJob -> {
					QsRule rule = ruleRepository.findByRuleId(ruleJob.getRuleId());
					ruleJob.setRuleName(rule.getRuleName());
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

	private void readLinesFromTemplate(StringBuilder fileContents) throws Exception {
		File contentSource = ResourceUtils.getFile("./"+DQ_ROW_COUNT_TEMPLATE_NAME);
		log.info("File in classpath Found {} : ", contentSource.exists());
		fileContents.append(new String(Files.readAllBytes(contentSource.toPath())));
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
