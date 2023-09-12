package ai.quantumics.api.req;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AwsDatasourceRequest {

    @NotNull(message="Project id can't be null")
    private Integer projectId;
    @NotNull(message="User id can't be null")
    private Integer userId;
    @NotNull(message="Data source name can't be null")
    private String dataSourceName;
    @NotNull(message="Connection type can't be null")
    private String connectionType;
    @NotNull(message="Iam role can't be null")
    private String iamRole;
}
