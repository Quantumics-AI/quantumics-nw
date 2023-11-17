package ai.quantumics.api.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ai.quantumics.api.constants.DatasourceConstants.INDEX_NOT_FOUND;

public class PatternUtils {
    public static int getIndex(String[] array, String target) {
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (target.equals(array[i])) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static String getValueAtIndex(String[] array, int index) {
        if (index >= 0 && index < array.length) {
            return array[index];
        } else {
            return INDEX_NOT_FOUND;
        }
    }

    public static List<String> findMissingElements(List<String> expectedElements, String[] targetFilePatternList) {
        List<String> missingElements = new ArrayList<>(expectedElements);

        for (String part : targetFilePatternList) {
            missingElements.remove(part);
        }
        return missingElements;
    }

    public static Map<String, Integer> findIndicesOfElements(List<String> expectedElements, String[] targetFilePatternList) {
        Map<String, Integer> indices = new LinkedHashMap<>();

        for (String expectedElement : expectedElements) {
            int index = indexOfElement(targetFilePatternList, expectedElement);
            if (index != -1) {
                indices.put(expectedElement, index);
            }
        }

        return indices;
    }

    private static int indexOfElement(String[] array, String element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(element)) {
                return i;
            }
        }
        return -1;
    }
}
