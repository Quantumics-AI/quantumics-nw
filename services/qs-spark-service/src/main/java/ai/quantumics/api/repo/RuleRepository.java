/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.repo;

import ai.quantumics.api.model.QsRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<QsRule, Integer> {
    Page<QsRule> findAllByStatusOrderByCreatedDateDesc(String status, Pageable pageable);

    QsRule findByRuleId(int ruleId);
    List<QsRule> findByRuleNameIgnoreCaseAndStatusIn(String ruleName,List<String> status);

    List<QsRule> findByStatusInAndSourceDatasourceIdOrStatusInAndTargetDatasourceId(List<String> sources, int source,List<String> targets, int target);
    Page<QsRule> findByRuleNameContainingIgnoreCase(String ruleName, Pageable pageable);
    Page<QsRule> findByStatusInAndRuleNameContainingIgnoreCase(List<String> status, String ruleName, Pageable pageable);
}