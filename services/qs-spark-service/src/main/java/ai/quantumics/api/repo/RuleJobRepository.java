/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.repo;

import ai.quantumics.api.model.QsRuleJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RuleJobRepository extends JpaRepository<QsRuleJob, Integer> {
    QsRuleJob findByRuleIdAndActiveIsTrue(int ruleId);
    QsRuleJob findByJobIdAndActiveIsTrue(int jobId);
    List<QsRuleJob> findByJobIdInAndActiveIsTrue(List<Integer> jobIds);
    List<QsRuleJob> findAllByActiveTrueOrderByModifiedDateDesc();
    List<QsRuleJob> findByRuleIdAndActiveIsTrueAndJobStatusInAndBusinessDate(int ruleId, List<String> statuses, LocalDate businessDate);
    List<QsRuleJob> findByRuleId(int ruleId);
}