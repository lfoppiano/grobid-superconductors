package org.grobid.core.data.material;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Formula {
    
    private String rawValue;
    private Map<String, String> formulaComposition = new LinkedHashMap<>();

    public Formula(String rawValue) {
        this.rawValue = rawValue;
    }

    public Formula(String rawValue, Map<String, String> formulaComposition) {
        this.rawValue = rawValue;
        this.formulaComposition = formulaComposition;
    }

    public Formula() {
        
    }

    @Override
    public String toString() {
//        Map<String, String> sorted = formulaComposition.entrySet().stream()
//            .sorted(Map.Entry.comparingByKey())
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        if (formulaComposition != null && CollectionUtils.isNotEmpty(formulaComposition.keySet())) {
            return Joiner.on(",").withKeyValueSeparator("=").join(formulaComposition);
        } else {
            return rawValue;
        }
    }

    public Map<String, String> getFormulaComposition() {
        return formulaComposition;
    }

    public void setFormulaComposition(Map<String, String> formulaComposition) {
        this.formulaComposition = formulaComposition;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formula formula = (Formula) o;
        boolean areCompositionsEqual = Objects.equals(formulaComposition, formula.formulaComposition);
        if(areCompositionsEqual && formulaComposition == null) {
            return Objects.equals(rawValue, formula.rawValue);
        } else {
            return areCompositionsEqual;   
        }
    }

    @Override
    public int hashCode() {
        if (formulaComposition == null) {
             return Objects.hash(rawValue);
        } else {
            return Objects.hash(formulaComposition);
        }
    }
}
