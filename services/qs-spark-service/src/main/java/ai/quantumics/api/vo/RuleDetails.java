package ai.quantumics.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleDetails {
    private int ruleId;
    private String ruleName;
    private String ruleDescription;
    private boolean sourceAndTarget;
    private DataSourceDetails sourceData;
    private DataSourceDetails targetData;
    private RuleTypeDetails ruleDetails;
    private int userId;
    private String status;
    private Date createdDate;
    private Date ModifiedDate;
    private String createdBy;
    private String modifiedBy;
}
