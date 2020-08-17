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

        String sampleFormula = material.getFormula();
        if (isNotEmpty(material.getResolvedFormulas())) {
            sampleFormula = material.getResolvedFormulas().get(0);
        }

        if(StringUtils.isEmpty(sampleFormula)) {
            return material;
        }

        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from materialParserWrapper import MaterialParserWrapper");

            interp.set("formula", sampleFormula);
            interp.exec("clazz =  MaterialParserWrapper().formula_to_class(formula)");
            String clazz = interp.getValue("clazz", String.class);

            material.setClazz(clazz);
        } catch (JepException e) {
            e.printStackTrace();
        }

        return material;
    }

}
