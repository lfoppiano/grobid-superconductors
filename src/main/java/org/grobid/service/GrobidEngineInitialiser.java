package org.grobid.service;

import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

@Singleton
public class GrobidEngineInitialiser {

    @Inject
    public GrobidEngineInitialiser(GrobidSuperconductorsConfiguration configuration) {
        GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
        String grobidHome = configuration.getGrobidHome();
        if (grobidHome != null) {
            GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
        }
        GrobidProperties.getInstance();
        LibraryLoader.load();
    }
}
