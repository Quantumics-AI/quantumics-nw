package ai.quantumics.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceDetails {
    private String dataSourceType;
    private String subDataSourceType;
    private int dataSourceId;
    private String bucketName;
    private String filePath;
    private String filePattern;
}
