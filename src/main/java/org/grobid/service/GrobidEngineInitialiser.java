package org.grobid.service;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GrobidEngineInitialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidEngineInitialiser.class);

    @Inject
    public GrobidEngineInitialiser(GrobidSuperconductorsConfiguration configuration) {
        LOGGER.info("Initialising Grobid");
        GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(ImmutableList.of(configuration.getGrobidHome()));
        GrobidProperties.getInstance(grobidHomeFinder);
        configuration.getModels().stream().forEach(GrobidProperties::addModel);
        if (StringUtils.isNotEmpty(configuration.getConsolidation().service)) {
            GrobidProperties.setGluttonUrl(configuration.getConsolidation().glutton.url);
            GrobidProperties.setConsolidationService(configuration.getConsolidation().service);
        }
        
        //Set the maximum number of Wapiti threads to the maximum concurrent requests 
        GrobidProperties.setWapitiNbThreads(configuration.getMaxParallelRequests());
        LibraryLoader.load();
    }
}
