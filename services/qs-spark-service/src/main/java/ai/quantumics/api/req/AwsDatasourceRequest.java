package ai.quantumics.api.req;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AwsDatasourceRequest {

    private Integer projectId;
    private Integer userId;
    @NotNull(message="Data Source Name is mandatory")
    @Size(min = 3, max = 50, message = "Data Source Name must be between 3 and 50 Characters long")
    private String dataSourceName;
    @NotNull(message="Access Type is mandatory")
    private String accessType;
    @NotNull(message="Data Source Name is mandatory")
    @Size(max = 100, message = "IAM name can have alphabets, number and special characters only and maximum 100 Characters long")
    private String connectionData;

}
