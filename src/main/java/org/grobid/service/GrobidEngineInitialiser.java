package org.grobid.service;

import org.grobid.core.engines.Engine;
import org.grobid.core.factory.AbstractEngineFactory;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.NoSuchElementException;

@Singleton
public class GrobidEngineInitialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidEngineInitialiser.class);


    @Inject
    public GrobidEngineInitialiser(GrobidSuperconductorsConfiguration configuration) {
        GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
        String grobidHome = configuration.getGrobidHome();
        if (grobidHome != null) {
            GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
        }
        GrobidProperties.getInstance();
        GrobidProperties.setContextExecutionServer(true);
        AbstractEngineFactory.init();
        Engine engine = null;
        try {
            // this will init or not all the models in memory
            engine = Engine.getEngine(false);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time.");
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs when initiating the grobid engine. ", exp);
        } finally {
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }
    }
}
