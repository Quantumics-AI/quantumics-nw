/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "qsp_rule_job")
@Data
public class QsRuleJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int jobId;
  private int ruleId;
  @Transient
  private String ruleName;
  private String jobStatus;
  @Column(columnDefinition = "TEXT")
  private String jobOutput;
  private String batchJobLog;
  private int userId;
  private boolean active;
  private Date jobSubmittedDate;
  private Date jobFinishedDate;
  private Date createdDate;
  private Date modifiedDate;
  private String createdBy;
  private String modifiedBy;
}
