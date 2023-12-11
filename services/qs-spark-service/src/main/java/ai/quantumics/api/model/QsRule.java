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
@Table(name = "qsp_rule")
@Data
public class QsRule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int ruleId;
  private String ruleName;
  private String ruleDescription;
  private boolean sourceAndTarget;
  @Column(columnDefinition = "TEXT")
  private String sourceData;
  @Column(columnDefinition = "TEXT")
  private String targetData;
  @Column(columnDefinition = "TEXT")
  private String ruleDetails;
  private int userId;
  private String status;
  private String ruleRunDays;
  private Date createdDate;
  private Date ModifiedDate;
  private String createdBy;
  private String modifiedBy;
  private int sourceDatasourceId;
  private int targetDatasourceId;
  private String sourceFilePattern;
  private String sourceBucketName;
  private String sourceFeedName;
  private String sourceFileName;
  private String targetFilePattern;
  private String targetBucketName;
  private String targetFeedName;
  private String targetFileName;
  private String ruleTypeName;
  private String levelName;
}
