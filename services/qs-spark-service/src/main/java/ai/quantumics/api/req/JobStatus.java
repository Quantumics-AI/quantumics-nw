package ai.quantumics.api.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {
    private String selectedStatus;
    private String selectedStatusResult;
}