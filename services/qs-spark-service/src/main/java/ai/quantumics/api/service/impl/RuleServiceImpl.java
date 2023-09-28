/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.constants.QsConstants;
import ai.quantumics.api.enums.RuleStatus;
import ai.quantumics.api.helper.ControllerHelper;
import ai.quantumics.api.model.Projects;
import ai.quantumics.api.model.QsRule;
import ai.quantumics.api.model.QsUserV2;
import ai.quantumics.api.repo.RuleRepository;
import ai.quantumics.api.service.ProjectService;
import ai.quantumics.api.service.RuleService;
import ai.quantumics.api.service.RuleTypeService;
import ai.quantumics.api.service.UserServiceV2;
import ai.quantumics.api.util.DbSessionUtil;
import ai.quantumics.api.vo.DataSourceDetails;
import ai.quantumics.api.vo.RuleDetails;
import ai.quantumics.api.vo.RuleTypeDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RuleServiceImpl implements RuleService {


	private final RuleRepository ruleRepository;
	private final DbSessionUtil dbUtil;
	private final RuleTypeService ruleTypeService;
	private final ProjectService projectService;
	private final ControllerHelper controllerHelper;
	private final UserServiceV2 userService;

	public RuleServiceImpl(RuleRepository ruleRepositoryCi,
						   DbSessionUtil dbUtilCi,
						   RuleTypeService ruleTypeServiceCi,
						   ProjectService projectServiceCi,
						   ControllerHelper controllerHelperCi,
						   UserServiceV2 userServiceCi) {
		this.ruleRepository = ruleRepositoryCi;
		this.dbUtil = dbUtilCi;
		this.ruleTypeService = ruleTypeServiceCi;
		this.projectService = projectServiceCi;
		this.controllerHelper = controllerHelperCi;
		this.userService = userServiceCi;
	}


	@Override
	public ResponseEntity<Object> saveRule(RuleDetails ruleDetails, int projectId) {
		final Map<String, Object> response = new HashMap<>();
		log.info("Invoking saveRule  API {}", ruleDetails.toString());
		try {
			dbUtil.changeSchema("public");
			final Projects project = projectService.getProject(projectId, ruleDetails.getUserId());
			if(project == null) {
				response.put("code", HttpStatus.SC_BAD_REQUEST);
				response.put("message", "Requested project with Id: "+ projectId +" for User with Id: " + ruleDetails.getUserId() +" not found.");

				return ResponseEntity.ok().body(response);
			}
			QsUserV2 userObj = userService.getUserById(ruleDetails.getUserId());

			dbUtil.changeSchema(project.getDbSchemaName());
			//ObjectMapper objectMapper = new ObjectMapper();
			Gson gson = new Gson();

			QsRule qsRule = new QsRule();
			qsRule.setRuleName(ruleDetails.getRuleName());
			qsRule.setRuleDescription(ruleDetails.getRuleDescription());
			qsRule.setSourceAndTarget(ruleDetails.isSourceAndTarget());
			/*qsRule.setSourceData(objectMapper.writeValueAsString(ruleDetails.getSourceData()));
			qsRule.setTargetData(objectMapper.writeValueAsString(ruleDetails.getTargetData()));
			qsRule.setRuleDetails(objectMapper.writeValueAsString(ruleDetails.getRuleDetails()));*/
			qsRule.setSourceData(gson.toJson(ruleDetails.getSourceData()));
			if(ruleDetails.isSourceAndTarget()) {
				qsRule.setTargetData(gson.toJson(ruleDetails.getTargetData()));
			}
			qsRule.setRuleDetails(gson.toJson(ruleDetails.getRuleDetails()));
			qsRule.setUserId(ruleDetails.getUserId());
			qsRule.setStatus(RuleStatus.ACTIVE.getStatus());
			qsRule.setCreatedDate(QsConstants.getCurrentUtcDate());
			qsRule.setModifiedDate(QsConstants.getCurrentUtcDate());
			qsRule.setCreatedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
			qsRule.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
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
	public ResponseEntity<Object> editRule(RuleDetails ruleDetails, int projectId) {
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
			qsRule.setUserId(ruleDetails.getUserId());
			qsRule.setStatus(ruleDetails.getStatus());
			qsRule.setModifiedDate(QsConstants.getCurrentUtcDate());
			qsRule.setModifiedBy(controllerHelper.getFullName(userObj.getQsUserProfile()));
			ruleRepository.save(qsRule);
			response.put("code", HttpStatus.SC_OK);
			response.put("message", "Data updated successfully");
		} catch (final Exception ex) {
			response.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
			response.put("message", "Error while updating rule :  " + ex.getMessage());
		}
		return ResponseEntity.ok().body(response);
	}

	@Override
	public ResponseEntity<Object> getRuleList(int projectId, String status, int page, int pageSize)  {
		dbUtil.changeSchema("public");
		final Projects project = projectService.getProject(projectId);
		final Map<String, Object> response = new HashMap<>();
		dbUtil.changeSchema(project.getDbSchemaName());
		try {
			Pageable paging = PageRequest.of(page-1, pageSize);
			Page<QsRule> rulesPage = ruleRepository.findAllByStatus(status, paging);

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
