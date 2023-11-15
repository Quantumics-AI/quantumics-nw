package ai.quantumics.api.util;

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
}
