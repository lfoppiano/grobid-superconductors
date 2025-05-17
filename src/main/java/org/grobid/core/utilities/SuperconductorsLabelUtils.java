package org.grobid.core.utilities;

public class SuperconductorsLabelUtils {
    public static String getPlainLabelName(String label) {
        if (label == null) {
            return label;
        }
        return label.replaceAll("<|>", "");
    }
}
