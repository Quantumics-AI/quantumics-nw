package ai.quantumics.api.service;

import java.util.List;

public interface AwsConnectionServiceV2 {
    String testConnection(String accessMethod);
    List<String> getBuckets();
}
