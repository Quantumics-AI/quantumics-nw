package ai.quantumics.api.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleJobDTO {
  private List<RuleTypes> ruleTypes;
  private List<JobStatus> ruleJobStatus;
  private String fromDate;
  private String toDate;
  private String feedName;
}
