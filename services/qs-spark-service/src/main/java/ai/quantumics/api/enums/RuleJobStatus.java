package ai.quantumics.api.enums;

public enum RuleJobStatus {

    INPROCESS("Inprocess"),
    COMPLETE("Complete"),
    FAILED("Failed");

    private String ruleJobStatus;

    RuleJobStatus(String ruleJobStatus) {
        this.ruleJobStatus = ruleJobStatus;
    }

    public String getStatus() {
        return ruleJobStatus;
    }
}
