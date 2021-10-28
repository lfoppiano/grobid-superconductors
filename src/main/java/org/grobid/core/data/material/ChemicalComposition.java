package org.grobid.core.data.material;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Map;
import java.util.StringJoiner;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChemicalComposition {
    private Map<String, String> elements;

    @JsonProperty("elements_vars")
    private Map<String, Object> elementsVars;

    @JsonProperty("amounts_vars")
    private Map<String, Object> amountsVars;

    private String formula;

    @JsonProperty("oxygen_deficiency")
    private Map<String, String> oxygenDeficency;

    public Map<String, String> getElements() {
        return elements;
    }

    public void setElements(Map<String, String> elements) {
        this.elements = elements;
    }

    public Map<String, Object> getAmountsVars() {
        return amountsVars;
    }

    public void setAmountsVars(Map<String, Object> amountsVars) {
        this.amountsVars = amountsVars;
    }

    public Map<String, Object> getElementsVars() {
        return elementsVars;
    }

    public void setElementsVars(Map<String, Object> elementsVars) {
        this.elementsVars = elementsVars;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public Map<String, String> getOxygenDeficency() {
        return oxygenDeficency;
    }

    public void setOxygenDeficency(Map<String, String> oxygenDeficency) {
        this.oxygenDeficency = oxygenDeficency;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChemicalComposition.class.getSimpleName() + "[", "]")
            .add("elements=" + ArrayUtils.toString(elements))
            .add("elementsVars=" +  ArrayUtils.toString(elementsVars))
            .add("amountsVars=" +  ArrayUtils.toString(amountsVars))
            .add("formula='" + formula + "'")
            .add("oxygenDeficency=" +  ArrayUtils.toString(oxygenDeficency))
            .toString();
    }
}
