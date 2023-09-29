package ai.quantumics.api.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AwsDatasourceRequest {

    private Integer userId;
    private Integer projectId;
    @NotNull(message="Data Source Name is mandatory")
    @Size(min = 3, max = 50, message = "Connection Name must be between 3 and 50 Characters long")
    private String connectionName;
    @NotNull(message="Data Source Name is mandatory")
    private String subDataSource;
    @NotNull(message="Access Type is mandatory")
    private String accessType;
    private String bucketName;
}
