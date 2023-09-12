package ai.quantumics.api.res;

import lombok.Data;

import java.util.Date;

@Data
public class AwsDatasourceResponse {

    private Integer id;
    private String dataSourceName;
    private String connectionType;
    private String iamRole;
    private Date createdDate;
    private Date modifiedDate;
}
