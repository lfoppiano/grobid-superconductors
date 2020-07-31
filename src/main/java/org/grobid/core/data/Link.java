package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Link {
    private String targetId;
    private String targetText;
    private String targetType;
    private String type;

    public static final String MATERIAL_TC_TYPE = "<material-tc>";
    public static final String TC_PRESSURE_TYPE = "<tc-pressure>";
    public static final String TC_ME_METHOD_TYPE = "<tc-me_method>";

    public Link() {
    }

    public Link(String targetId, String targetText, String targetType, String type) {
        this.targetId = targetId;
        this.targetText = targetText;
        this.targetType = targetType;
        this.type = type;
    }

    public static String getLinkType(String type1, String type2) {
        if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)) {
            return MATERIAL_TC_TYPE;
        } else if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL)) {
            return MATERIAL_TC_TYPE;
        } else if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_PRESSURE_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)) {
            return TC_PRESSURE_TYPE;
        } else if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_PRESSURE_LABEL)) {
            return TC_PRESSURE_TYPE;
        } else if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)) {
            return TC_ME_METHOD_TYPE;
        } else if (StringUtils.equals(type1, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL)
            && StringUtils.equals(type2, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL)) {
            return TC_ME_METHOD_TYPE;
        } else {
            throw new RuntimeException("Wrongly labelled entity. Something is wrong somewhere up the chain. ");
        }
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }
}
