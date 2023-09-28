package ai.quantumics.api.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RuleStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DELETED("Deleted");

    private String ruleStatus;

    RuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }

    public String getStatus() {
        return ruleStatus;
    }

    public static Map<RuleStatus,String> getAccessTypeAsMap(){
        return Arrays.stream(RuleStatus.values())
                .collect(Collectors.toMap( Function.identity(), RuleStatus::getStatus));
    }
}
