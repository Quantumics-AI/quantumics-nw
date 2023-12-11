package ai.quantumics.api.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RuleJobStatus {
    NOT_STARTED("Not Started"),
    IN_QUEUE("In Queue"),
    INPROCESS("Inprocess"),
    COMPLETE("Complete"),
    CANCELLED("Cancelled"),
    FAILED("Failed");

    private String ruleJobStatus;

    RuleJobStatus(String ruleJobStatus) {
        this.ruleJobStatus = ruleJobStatus;
    }

    public String getStatus() {
        return ruleJobStatus;
    }

    public static List<String> getStatusList(){
        return Arrays.stream(RuleJobStatus.values())
                .map(RuleJobStatus::getStatus)
                .collect(Collectors.toList());
    }
}
