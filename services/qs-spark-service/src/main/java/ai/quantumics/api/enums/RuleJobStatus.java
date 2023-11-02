package ai.quantumics.api.enums;

public enum RuleJobStatus {
    NOT_STARTED("Not Started"),
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
}
