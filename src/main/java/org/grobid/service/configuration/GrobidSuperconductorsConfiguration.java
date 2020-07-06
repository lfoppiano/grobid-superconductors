package org.grobid.service.configuration;

import io.dropwizard.Configuration;

public class GrobidSuperconductorsConfiguration extends Configuration {

    private String grobidHome;
    private String chemspotUrl;
    private String chemDataExtractorUrl;
    private String grobidQuantitiesUrl;
    private String pythonVirtualEnv;

    public String getGrobidHome() {
        return grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public String getChemspotUrl() {
        return chemspotUrl;
    }

    public void setChemspotUrl(String chemspotUrl) {
        this.chemspotUrl = chemspotUrl;
    }

    public String getGrobidQuantitiesUrl() {
        return grobidQuantitiesUrl;
    }

    public void setGrobidQuantitiesUrl(String grobidQuantitiesUrl) {
        this.grobidQuantitiesUrl = grobidQuantitiesUrl;
    }

    public String getChemDataExtractorUrl() {
        return chemDataExtractorUrl;
    }

    public void setChemDataExtractorUrl(String chemDataExtractorUrl) {
        this.chemDataExtractorUrl = chemDataExtractorUrl;
    }

    public String getPythonVirtualEnv() {
        return pythonVirtualEnv;
    }

    public void setPythonVirtualEnv(String pythonVirtualEnv) {
        this.pythonVirtualEnv = pythonVirtualEnv;
    }
}
