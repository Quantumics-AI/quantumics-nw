package ai.quantumics.api.enums;

public enum AwsAccessType {

    IAM("IAM"),
    ACCESS_KEY("ACCESS_KEY"),
    ACCESS_SECRET_KEY("ACCESS_SECRET_KEY");

    private String accessType;

    AwsAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessType() {
        return accessType;
    }

}