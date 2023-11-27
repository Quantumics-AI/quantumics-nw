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
import ai.quantumics.api.service.RuleTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
