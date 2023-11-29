/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.service.impl;

import ai.quantumics.api.model.QsRuleType;
import ai.quantumics.api.repo.RuleTypeRepository;
import ai.quantumics.api.req.RuleTypes;
import ai.quantumics.api.req.RuleTypesDTO;
import ai.quantumics.api.service.RuleTypeService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.quantumics.api.constants.QsConstants.DATA_COMPLETENESS;
import static ai.quantumics.api.constants.QsConstants.DATA_PROFILER;
import static ai.quantumics.api.constants.QsConstants.DUPLICATE_VALUE;
import static ai.quantumics.api.constants.QsConstants.NULL_VALUE;
import static ai.quantumics.api.constants.QsConstants.RULE_LEVEL_ALL;
import static ai.quantumics.api.constants.QsConstants.ZERO_ROW_CHECK;

@Service
public class RuleTypeServiceImpl implements RuleTypeService {

	@Autowired
	private RuleTypeRepository ruleTypeRepository;

	@Override
	public List<QsRuleType> getActiveRuleTypes(boolean sourceOnly, boolean allRuleTypes) {
		if(allRuleTypes){
			return ruleTypeRepository.findByActiveIsTrue();
		}
		return ruleTypeRepository.findBySourceOnlyAndActiveIsTrue(sourceOnly);
	}
	@Override
	public List<QsRuleType> getFilteredRuleTypes(RuleTypesDTO ruleTypesDTO) {
		List<QsRuleType> filteredResponse = new ArrayList<>();
		List<RuleTypes> ruleTypes = ruleTypesDTO.getRuleTypes();
		if(CollectionUtils.isNotEmpty(ruleTypes)){
			List<String> ruleTypeList = ruleTypes.stream().map(RuleTypes::getRuleTypeName).collect(Collectors.toList());
			List<QsRuleType> ruleList = ruleTypeRepository.findByRuleTypeNameInAndActiveIsTrue(ruleTypeList);
			ruleTypes.forEach(ruleType -> {
				if (DATA_COMPLETENESS.equals(ruleType.getRuleTypeName()) || DATA_PROFILER.equals(ruleType.getRuleTypeName()) || DUPLICATE_VALUE.equals(ruleType.getRuleTypeName())) {
					filterByRulesAndLevels(ruleType, ruleList, filteredResponse);
				} else if (NULL_VALUE.equals(ruleType.getRuleTypeName()) || ZERO_ROW_CHECK.equals(ruleType.getRuleTypeName())) {
					filterByRuleTypes(ruleType, ruleList, filteredResponse);
				}
			});
		}else{
			return ruleTypeRepository.findByActiveIsTrue();
		}
		return filteredResponse;
	}
	private static void filterByRulesAndLevels(RuleTypes ruleType, List<QsRuleType> ruleList, List<QsRuleType> filteredResponse){
		List<QsRuleType> filteredList;
		if(ruleType.getRuleLevel().equals(RULE_LEVEL_ALL)){
			filteredList = ruleList.stream().filter(x -> ruleType.getRuleTypeName().equals(x.getRuleTypeName())).collect(Collectors.toList());
		}else {
			filteredList = ruleList.stream().filter(x -> ruleType.getRuleLevel().equals(x.getLevelName())).collect(Collectors.toList());
		}
		filteredResponse.addAll(filteredList);
	}

	private static void filterByRuleTypes(RuleTypes ruleType, List<QsRuleType> ruleList, List<QsRuleType> filteredResponse){
		List<QsRuleType> filteredList = ruleList.stream().filter(x -> ruleType.getRuleTypeName().equals(x.getRuleTypeName())).collect(Collectors.toList());
		filteredResponse.addAll(filteredList);
	}
}
