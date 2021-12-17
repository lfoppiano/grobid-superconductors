package org.grobid.service;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;

@Singleton
public class GrobidEngineInitialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidEngineInitialiser.class);

    @Inject
    public GrobidEngineInitialiser(GrobidSuperconductorsConfiguration configuration) {
        LOGGER.info("Initialising Grobid (GrobidHome=" + configuration.getGrobidHome() + ")");
        GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(ImmutableList.of(configuration.getGrobidHome()));
        GrobidProperties.getInstance(grobidHomeFinder);
        configuration.getModels().stream().forEach(GrobidProperties::addModel);
        if (StringUtils.isNotEmpty(configuration.getConsolidation().service)) {
            GrobidProperties.setGluttonUrl(configuration.getConsolidation().glutton.url);
            GrobidProperties.setConsolidationService(configuration.getConsolidation().service);
        }

        //Set the maximum number of Wapiti threads to the maximum concurrent requests

        Class<?> clazz = null; // if you know class name dynamically i.e. at runtime
        try {
            clazz = Class.forName("org.grobid.core.utilities.GrobidProperties");
            Field field = clazz.getDeclaredField("grobidConfig");
            field.setAccessible(true);
            GrobidConfig grobidConfig = (GrobidConfig) field.get("grobidConfig");
            grobidConfig.grobid.concurrency = configuration.getMaxParallelRequests();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Invalid operation when hacking the GrobidProperties", e);
        }

        LibraryLoader.load();

        LOGGER.info("Finishing initialising grobid. ");
        LOGGER.info("Configuration: ");
        LOGGER.info("Grobid Home: " + GrobidProperties.getGrobidHome() + " (Grobid Home=" + configuration.getGrobidHome() + ")");
    }
}
