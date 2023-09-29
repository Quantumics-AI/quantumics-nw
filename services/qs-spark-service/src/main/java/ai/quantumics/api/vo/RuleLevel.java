package ai.quantumics.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleLevel {
    private String levelName;
    private boolean columnLevel;
    private int acceptance;
    private List<String> columns;
}
