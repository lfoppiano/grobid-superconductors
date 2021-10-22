package org.grobid.core.engines;

import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.Material;
import org.grobid.core.utilities.ClassResolverModuleClient;
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
    private final ClassResolverModuleClient client;
    private boolean disabled = false;

    @Inject
    public MaterialClassResolver(GrobidSuperconductorsConfiguration configuration, ClassResolverModuleClient client) {
        this.configuration = configuration;
        this.client = client;
    }

    /**
     * This modifies the material object
     **/
    public Material process(Material material) {
        boolean sampling = false;
        String sampleFormula = material.getFormula() != null ? material.getFormula().getRawValue() : "";
        if (isNotEmpty(material.getResolvedFormulas())) {
            if (material.getResolvedFormulas().size() == 1 && isEmpty(material.getVariables().keySet())) {
                // if there is one resolved formula and no variables, I might need to sample
                sampleFormula = createSample(material);
                if (StringUtils.isEmpty(sampleFormula)) {
                    // no variable in the formula...
                    sampleFormula = material.getResolvedFormulas().get(0).getRawValue();
                }
                sampling = true;
            } else {
                sampleFormula = material.getResolvedFormulas().get(0).getRawValue();
            }
        } else if (material.getFormula() != null && StringUtils.isNotEmpty(material.getFormula().getRawValue())) {
            sampleFormula = createSample(material);
            sampling = true;
        }

        if (StringUtils.isEmpty(sampleFormula)) {
            return material;
        }

        List<String> classes = client.getClassesFromFormula(sampleFormula);

        if (CollectionUtils.isEmpty(classes) && !sampling) {
            String sample = createSample(material);
            classes = client.getClassesFromFormula(sample);
        }

        String classesAsString = String.join(", ", classes);
        material.setClazz(classesAsString);

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
            LOGGER.debug("Something wrong came out from the material sampling in the class detection. " +
                "Input formula: " + sampleMaterial.getFormula() + ", resolvedFormula: " + Arrays.toString(resolvedVariables.toArray()));
        } else {
            LOGGER.debug("No formula came out from the material sampling in the class detection. " +
                "Input formula: " + sampleMaterial.getFormula());
        }
        return sampleFormula;
    }

}
