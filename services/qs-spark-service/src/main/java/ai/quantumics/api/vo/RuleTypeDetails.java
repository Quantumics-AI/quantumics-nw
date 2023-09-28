package ai.quantumics.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleTypeDetails {
    private String ruleTypeName;
    private RuleLevel ruleLevel;
}
