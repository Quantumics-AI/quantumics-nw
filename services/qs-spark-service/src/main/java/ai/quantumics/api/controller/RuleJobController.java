/*

 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.controller;

import ai.quantumics.api.enums.RuleJobStatus;
import ai.quantumics.api.req.CancelJobRequest;
import ai.quantumics.api.req.RuleJobRequest;
import ai.quantumics.api.service.RuleJobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import java.util.List;

@Slf4j
@RestController
@Api(value = "RulJob API ")
@RequestMapping("/api/v1/rulejob")
public class RuleJobController {

  private final RuleJobService ruleJobService;

  public RuleJobController(
          RuleJobService ruleJobServiceCi) {
    ruleJobService = ruleJobServiceCi;
  }

  @ApiOperation(value = "rulejob", response = Json.class)
  @PostMapping("/{userId}/{projectId}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "OKAY"),
                  @ApiResponse(code = 409, message = "Failure message")
          })
  public ResponseEntity<Object> runRuleJob(
          @RequestBody final RuleJobRequest ruleJobRequest,
          @PathVariable(value = "userId") final int userId,
          @PathVariable(value = "projectId") final int projectId)
          throws Exception {
    return ruleJobService.runRuleJob(ruleJobRequest, userId, projectId);
  }

  @ApiOperation(value = "rulejob", response = Json.class)
  @PutMapping("/{userId}/{projectId}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "OKAY"),
                  @ApiResponse(code = 409, message = "Failure message")
          })
  public ResponseEntity<Object> cancelJobs(
          @RequestBody final CancelJobRequest ruleJobRequest,
          @PathVariable(value = "userId") final int userId,
          @PathVariable(value = "projectId") final int projectId)
          throws Exception {
    return ruleJobService.cancelRuleJobs(ruleJobRequest, userId, projectId);
  }

  @ApiOperation(value = "RuleJobs", response = Json.class)
  @GetMapping("/{userId}/{projectId}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "List All Rule jobs for Project ID")})
  public ResponseEntity<Object> getRuleJobs(
          @PathVariable(value = "projectId") final int projectId,
          @PathVariable(value = "userId") final int userId) {
    return ruleJobService.fetchRuleJobList(userId, projectId);
  }

  @ApiOperation(value = "rowCountJob", response = Json.class)
  @GetMapping("/rowCount/{userId}/{projectId}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "OKAY"),
                  @ApiResponse(code = 409, message = "Failure message")
          })
  public ResponseEntity<Object> rowCount(
          @RequestParam(value = "bucketName") final String bucketName,
          @RequestParam(value = "filePath") final String filePath,
          @PathVariable(value = "userId") final int userId,
          @PathVariable(value = "projectId") final int projectId)
          throws Exception {
    return ruleJobService.getRowCount(bucketName, filePath, userId, projectId);
  }
  @GetMapping("/getRuleJobStatus")
  public ResponseEntity<List<String>> getRuleJobStatus() {
    return ResponseEntity.status(HttpStatus.OK).body(RuleJobStatus.getStatusList());
  }
}
