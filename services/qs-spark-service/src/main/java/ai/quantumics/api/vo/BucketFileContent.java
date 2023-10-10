package ai.quantumics.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BucketFileContent {
    private List<String> headers;
    private List<ColumnDataType> columnDatatype;
    private List<Map<String, String>> content;
}
