package org.grobid.core.engines;

import com.google.inject.Singleton;
import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.Material;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Singleton
public class MaterialClassResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialClassResolver.class);

    private GrobidSuperconductorsConfiguration configuration;
    private final JepEngine engineController;
    private boolean disabled = false;

    @Inject
    public MaterialClassResolver(GrobidSuperconductorsConfiguration configuration, JepEngine engineController) {
        this.configuration = configuration;
        this.engineController = engineController;
        init();
    }

    public void init() {
        if (this.engineController.getDisabled()) {
            LOGGER.info("The JEP engine wasn't initialised correct. Disabling all dependent modules. ");
            this.disabled = true;
            return;
        }
        try {
            try (Interpreter interp = new SharedInterpreter()) {
                interp.exec("from materialParserWrapper import MaterialParserWrapper");
            }

        } catch (Exception e) {
            LOGGER.error("Loading JEP native library failed. The linking will be disabled.", e);
            this.disabled = true;
        }
    }

    /**
     * This modifies the material object
     **/
    public Material process(Material material) {
        if (disabled) {
            return material;
        }

        boolean sampling = false;
        String sampleFormula = material.getFormula();
        if (isNotEmpty(material.getResolvedFormulas())) {
            if (material.getResolvedFormulas().size() == 1 && isEmpty(material.getVariables().keySet())) {
                // if there is one resolved formula and no variables, I might need to sample
                sampleFormula = createSample(material);
                if (StringUtils.isEmpty(sampleFormula)) {
                    // no variable in the formula...
                    sampleFormula = material.getResolvedFormulas().get(0);
                }
                sampling = true;
            } else {
                sampleFormula = material.getResolvedFormulas().get(0);
            }
        } else if (StringUtils.isNotEmpty(material.getFormula())) {
            sampleFormula = createSample(material);
            sampling = true;
        }

        if (StringUtils.isEmpty(sampleFormula)) {
            return material;
        }

        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from materialParserWrapper import MaterialParserWrapper");

            interp.set("formula", sampleFormula);
//            interp.exec("clazz =  MaterialParserWrapper().formula_to_class(formula)");
            interp.exec("classes =  MaterialParserWrapper().formula_to_classes(formula)");
            interp.exec("classes_as_string = ', '.join(list(classes.keys()))");
            String clazz = interp.getValue("classes_as_string", String.class);

            if (StringUtils.isEmpty(clazz) && !sampling) {
                String sample = createSample(material);
                interp.set("formula", sample);
                interp.exec("classes =  MaterialParserWrapper().formula_to_classes(formula)");
                interp.exec("classes_as_string = ', '.join(list(classes.keys()))");
                clazz = interp.getValue("classes_as_string", String.class);
            }

            material.setClazz(clazz);
        } catch (JepException e) {
            LOGGER.error("An error occurred when extracting the class from the material, something related to JEP or python.", e);
        }

        return material;
    }

    private String createSample(Material material) {
        String sampleFormula = "";

        Material sampleMaterial = new Material();
        sampleMaterial.setFormula(material.getFormula());
        //Best guesses of possible variables
        sampleMaterial.addVariable("x", Collections.singletonList("0.1"));
        sampleMaterial.addVariable("y", Collections.singletonList("0.1"));
        sampleMaterial.addVariable("z", Collections.singletonList("0.1"));
        sampleMaterial.addVariable("δ", Collections.singletonList("0.1"));
        List<String> resolvedVariables = Material.resolveVariables(sampleMaterial);

        if (resolvedVariables.size() == 1) {
            sampleFormula = resolvedVariables.get(0);
        } else if (resolvedVariables.size() > 1) {
            LOGGER.warn("Something wrong came out from the material sampling in the class detection. " +
                "Input formula: " + sampleMaterial.getFormula() + ", resolvedFormula: " + Arrays.toString(resolvedVariables.toArray()));
        } else {
            LOGGER.warn("No formula came out from the material sampling in the class detection. " +
                "Input formula: " + sampleMaterial.getFormula());
        }
        return sampleFormula;
    }

}
