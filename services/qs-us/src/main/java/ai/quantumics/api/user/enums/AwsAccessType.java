package ai.quantumics.api.user.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AwsAccessType {

    IAM("IAM"),
    RESOURCE_POLICY("RESOURCE_POLICY"),
    ACCESS_SECRET_KEY("ACCESS_KEY/SECRET_KEY"),
    KEYS("KEYS"),
    PROFILE("PROFILE");

    private String accessType;

    AwsAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessType() {
        return accessType;
    }

    public static Map<AwsAccessType,String> getAccessTypeAsMap(){
        return Arrays.stream(AwsAccessType.values())
                .collect(Collectors.toMap( Function.identity(),AwsAccessType::getAccessType));
    }
}
