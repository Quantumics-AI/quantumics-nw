package ai.quantumics.api.res;

import lombok.Data;

import java.util.Date;

@Data
public class AwsDatasourceResponse {

    private Integer id;
    private String dataSourceName;
    private String accessType;
    private String connectionData;
    private Date createdDate;
    private Date modifiedDate;
}
