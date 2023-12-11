package ai.quantumics.api.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleTypesDTO {
    private List<RuleTypes> ruleTypes;
}