package org.grobid.core.engines.label;

import org.grobid.core.engines.SuperconductorsModels;

/**
 * Created by lfoppiano on 28/11/16.
 */
public class SuperconductorsTaggingLabels extends TaggingLabels {
    private SuperconductorsTaggingLabels() {
        super();
    }

    private static final String SUPERCONDUCTORS_VALUE_NAME_LABEL = "<supercon>";
    private static final String SUPERCONDUCTORS_OTHER_LABEL = "<other>";

    public static final TaggingLabel SUPERCONDUCTOR_VALUE_NAME = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_VALUE_NAME_LABEL);
    public static final TaggingLabel SUPERCONDUCTOR_OTHER = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, OTHER_LABEL);

    static {
        //Quantity
        register(SUPERCONDUCTOR_VALUE_NAME);
        register(SUPERCONDUCTOR_OTHER);
    }
}
