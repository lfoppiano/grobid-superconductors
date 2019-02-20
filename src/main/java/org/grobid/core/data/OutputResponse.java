package org.grobid.core.data;

import java.util.List;

public class OutputResponse {

    private List<Superconductor> superconductors;
    private List<Measurement> temperatures;
    private List<Abbreviation> abbreviations;


    public List<Superconductor> getSuperconductors() {
        return superconductors;
    }

    public void setSuperconductors(List<Superconductor> superconductors) {
        this.superconductors = superconductors;
    }

    public List<Measurement> getTemperatures() {
        return temperatures;
    }

    public void setTemperatures(List<Measurement> temperatures) {
        this.temperatures = temperatures;
    }

    public List<Abbreviation> getAbbreviations() {
        return abbreviations;
    }

    public void setAbbreviations(List<Abbreviation> abbreviations) {
        this.abbreviations = abbreviations;
    }
}
