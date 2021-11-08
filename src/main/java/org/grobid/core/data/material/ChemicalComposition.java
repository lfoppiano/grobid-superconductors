package org.grobid.core.data.material;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChemicalComposition {
    private Map<String, String> composition = new LinkedHashMap<>();

    private String formula;

    private String name;

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getComposition() {
        return composition;
    }

    public void setComposition(Map<String, String> composition) {
        this.composition = composition;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(formula) && StringUtils.isBlank(name) && composition.keySet().size() == 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChemicalComposition.class.getSimpleName() + "[", "]")
            .add("composition=" + composition)
            .add("formula='" + formula + "'")
            .add("name='" + name + "'")
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChemicalComposition that = (ChemicalComposition) o;
        return Objects.equals(composition, that.composition) && Objects.equals(formula, that.formula) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composition, formula, name);
    }
}
