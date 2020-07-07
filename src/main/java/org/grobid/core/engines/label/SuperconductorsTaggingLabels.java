package org.grobid.core.engines.label;

import org.grobid.core.engines.SuperconductorsModels;

/**
 * Created by lfoppiano on 28/11/16.
 */
public class SuperconductorsTaggingLabels extends TaggingLabels {
    private SuperconductorsTaggingLabels() {
        super();
    }

    public static final String SUPERCONDUCTORS_CLASS_LABEL = "<class>";
    public static final String SUPERCONDUCTORS_MATERIAL_LABEL = "<material>";
    public static final String SUPERCONDUCTORS_SAMPLE_LABEL = "<sample>";
    public static final String SUPERCONDUCTORS_TC_LABEL = "<tc>";
    public static final String SUPERCONDUCTORS_TC_VALUE_LABEL = "<tcValue>";
    public static final String SUPERCONDUCTORS_PRESSURE_LABEL = "<pressure>";
    public static final String SUPERCONDUCTORS_MAGNETISATION_LABEL = "<magnetisation>";
    public static final String SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL = "<me_method>";

    public static final TaggingLabel SUPERCONDUCTORS_CLASS = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_CLASS_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_MATERIAL = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_MATERIAL_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_SAMPLE = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_SAMPLE_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_TC = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_TC_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_TC_VALUE = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_TC_VALUE_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_PRESSURE = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_PRESSURE_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_MAGNETISATION = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_MAGNETISATION_LABEL);
    public static final TaggingLabel SUPERCONDUCTORS_MEASUREMENT_METHOD = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL);

    public static final TaggingLabel SUPERCONDUCTORS_OTHER = new TaggingLabelImpl(SuperconductorsModels.SUPERCONDUCTORS, OTHER_LABEL);

    public static final String MATERIAL_NAME_LABEL = "<name>";
    public static final String MATERIAL_FORMULA_LABEL = "<formula>";
    public static final String MATERIAL_SHAPE_LABEL = "<shape>";
    public static final String MATERIAL_DOPING_LABEL = "<doping>";
    public static final String MATERIAL_VARIABLE_LABEL = "<variable>";
    public static final String MATERIAL_VALUE_LABEL = "<value>";
    public static final String MATERIAL_SUBSTRATE_LABEL = "<substrate>";
    public static final String MATERIAL_FABRICATION_LABEL = "<fabrication>";

    public static final TaggingLabel MATERIAL_NAME = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_NAME_LABEL);
    public static final TaggingLabel MATERIAL_FORMULA = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_FORMULA_LABEL);
    public static final TaggingLabel MATERIAL_SHAPE = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_SHAPE_LABEL);
    public static final TaggingLabel MATERIAL_DOPING = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_DOPING_LABEL);
    public static final TaggingLabel MATERIAL_VARIABLE = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_VARIABLE_LABEL);
    public static final TaggingLabel MATERIAL_VALUE = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_VALUE_LABEL);
    public static final TaggingLabel MATERIAL_SUBSTRATE = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_SUBSTRATE_LABEL);
    public static final TaggingLabel MATERIAL_FABRICATION = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, MATERIAL_FABRICATION_LABEL);
    public static final TaggingLabel MATERIAL_OTHER = new TaggingLabelImpl(SuperconductorsModels.MATERIAL, OTHER_LABEL);

    static {
        //Superconductor
        register(SUPERCONDUCTORS_CLASS);
        register(SUPERCONDUCTORS_MATERIAL);
        register(SUPERCONDUCTORS_SAMPLE);
        register(SUPERCONDUCTORS_TC);
        register(SUPERCONDUCTORS_TC_VALUE);
        register(SUPERCONDUCTORS_PRESSURE);
        register(SUPERCONDUCTORS_MAGNETISATION);
        register(SUPERCONDUCTORS_OTHER);

        //Material
        register(MATERIAL_SHAPE);
        register(MATERIAL_VARIABLE);
        register(MATERIAL_VALUE);
        register(MATERIAL_FORMULA);
        register(MATERIAL_NAME);
        register(MATERIAL_DOPING);
        register(MATERIAL_SUBSTRATE);
        register(MATERIAL_FABRICATION);
        register(MATERIAL_OTHER);
    }
}
