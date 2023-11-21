package ai.quantumics.api.req;

import ai.quantumics.api.model.QsRuleJob;
import ai.quantumics.api.vo.RuleDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunRuleJobRequest {
  
  private int projectId;
  private QsRuleJob ruleJob;
  private RuleDetails ruleDetails;
  private String modifiedBy;;
  
}
