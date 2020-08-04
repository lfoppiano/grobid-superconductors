package org.grobid.service;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import org.grobid.core.engines.AggregatedProcessing;
import org.grobid.core.engines.MaterialParser;
import org.grobid.core.engines.RulesBasedLinker;
import org.grobid.core.engines.CRFBasedLinker;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.grobid.core.utilities.ChemspotClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.service.controller.AnnotationController;
import org.grobid.service.controller.HealthCheck;
import org.grobid.service.controller.LinkerController;
import org.grobid.service.controller.MaterialController;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;


public class SuperconductorsServiceModule extends DropwizardAwareModule<GrobidSuperconductorsConfiguration> {

    @Override
    public void configure(Binder binder) {
        // Generic modules
        binder.bind(GrobidEngineInitialiser.class);
        binder.bind(HealthCheck.class);

        //Core services
        binder.bind(ChemspotClient.class);
        binder.bind(ChemDataExtractorClient.class);
        binder.bind(SuperconductorsParser.class);
        binder.bind(MaterialParser.class);
        binder.bind(RulesBasedLinker.class);
        binder.bind(CRFBasedLinker.class);
        binder.bind(AggregatedProcessing.class);

        //REST
        binder.bind(AnnotationController.class);
        binder.bind(MaterialController.class);
        binder.bind(LinkerController.class);
    }

    @Provides
    protected ObjectMapper getObjectMapper() {
        return getEnvironment().getObjectMapper();
    }

    @Provides
    protected MetricRegistry provideMetricRegistry() {
        return getMetricRegistry();
    }

    //for unit tests
    protected MetricRegistry getMetricRegistry() {
        return getEnvironment().metrics();
    }

    @Provides
    Client provideClient() {
        return ClientBuilder.newClient();
    }

}