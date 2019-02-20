package org.grobid.core.engines.label;

import org.grobid.core.engines.AbbreviationsModels;

/**
 * Created by lfoppiano on 28/11/16.
 */
public class AbbreviationsTaggingLabels extends TaggingLabels {
    private AbbreviationsTaggingLabels() {
        super();
    }

    private static final String ABBREVIATION_NAME_LABEL = "<acronym>";

    public static final TaggingLabel ABBREVIATION_VALUE_NAME = new TaggingLabelImpl(AbbreviationsModels.ABBREVIATIONS, ABBREVIATION_NAME_LABEL);
    public static final TaggingLabel ABBREVIATION_OTHER = new TaggingLabelImpl(AbbreviationsModels.ABBREVIATIONS, OTHER_LABEL);

    static {
        //Quantity
        register(ABBREVIATION_VALUE_NAME);
        register(ABBREVIATION_OTHER);
    }
}
