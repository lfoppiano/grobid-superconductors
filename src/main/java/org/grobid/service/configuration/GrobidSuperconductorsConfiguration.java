package org.grobid.service.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.apache.commons.io.IOUtils;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GrobidSuperconductorsConfiguration extends Configuration {

    public static final Logger LOGGER = LoggerFactory.getLogger(GrobidSuperconductorsConfiguration.class);

    private String grobidHome;
    private String chemspotUrl;
    private String chemDataExtractorUrl;
    private String pythonVirtualEnv;

    // Version
    private static String VERSION = null;
    private static final String UNKNOWN_VERSION_STR = "unknown";
    private static final String GROBID_VERSION_FILE = "/version.txt";

    // CORS
    @JsonProperty
    private String corsAllowedOrigins = "*";
    @JsonProperty
    private String corsAllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";
    @JsonProperty
    private String corsAllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin";

    private boolean pythonRedirectOutput = false;

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

    public boolean isPythonRedirectOutput() {
        return pythonRedirectOutput;
    }

    public void setPythonRedirectOutput(boolean pythonRedirectOutput) {
        this.pythonRedirectOutput = pythonRedirectOutput;
    }

    public static String getVersion() {
        if (VERSION != null) {
            return VERSION;
        }
        synchronized (GrobidProperties.class) {
            if (VERSION == null) {
                String grobidVersion = UNKNOWN_VERSION_STR;
                try (InputStream is = GrobidProperties.class.getResourceAsStream(GROBID_VERSION_FILE)) {
                    grobidVersion = IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.error("Cannot read Grobid version from resources", e);
                }
                VERSION = grobidVersion;
            }
        }
        return VERSION;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }
}
