package ai.quantumics.api.req;

import lombok.Data;

@Data
public class AwsDatasourceRequest {

    private int projectId;
    private int userId;
    private String dataSourceName;
    private String connectionType;
    private String iamRole;
}
