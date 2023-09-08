package ai.quantumics.api.enums;

public enum AwsAccessType {

    IAM("IAM"),
    RESOURCE_POLICY("RESOURCE_POLICY"),
    ACCESS_SECRET_KEY("ACCESS_KEY/SECRET_KEY");

    private String accessType;

    AwsAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessType() {
        return accessType;
    }

}
