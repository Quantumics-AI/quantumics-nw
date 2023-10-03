package ai.quantumics.api.res;

import lombok.Data;

import java.util.Date;

@Data
public class AwsDatasourceResponse {

    private Integer id;
    private String connectionName;
    private String subDataSource;
    private String accessType;
    private String bucketName;
    private Date createdDate;
    private Date modifiedDate;
}
