package ai.quantumics.api.req;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RuleJobRequest {

  private List<Integer> ruleIds;
  private String businessDate;
  
}
