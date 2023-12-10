package ai.quantumics.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QsRuleJobResponse {
  private int jobId;
  private int ruleId;
  private String jobStatus;
  private String jobOutput;
  private String batchJobLog;
  private int batchJobId;
  private int userId;
  private boolean active;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
  private LocalDate businessDate;
  private Date jobSubmittedDate;
  private Date jobFinishedDate;
  private Date createdDate;
  private Date modifiedDate;
  private String createdBy;
  private String modifiedBy;
  private String ruleName;
  private String ruleTypeName;
  private String ruleLevelName;
  private String ruleStatus;
  private String sourceFeedName;
  private String targetFeedName;
}