package ai.quantumics.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMetadata {
    private String fileName;
    private Date fileLastModified;
    private long fileSize;
}
