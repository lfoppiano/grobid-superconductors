package org.grobid.service.configuration;

import io.dropwizard.Configuration;

public class GrobidSuperconductorsConfiguration extends Configuration {

    private String grobidHome;
    private String chemspotUrl;

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
}
