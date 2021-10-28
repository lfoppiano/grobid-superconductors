package org.grobid.core.data.material;

import java.util.HashMap;
import java.util.Map;

public class Formula {
    
    private String rawValue;
    private Map<String, String> formulaComposition = new HashMap<>();

    public Formula(String rawValue) {
        this.rawValue = rawValue;
    }

    public Formula(String rawValue, Map<String, String> formulaComposition) {
        this.rawValue = rawValue;
        this.formulaComposition = formulaComposition;
    }

    public Formula() {
        
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
}
