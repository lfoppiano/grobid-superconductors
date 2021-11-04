package org.grobid.service;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import org.grobid.core.engines.*;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.utilities.client.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.service.controller.AnnotationController;
import org.grobid.service.controller.HealthCheck;
import org.grobid.service.controller.LinkerController;
import org.grobid.service.controller.MaterialController;
import org.grobid.service.exceptions.mapper.GrobidExceptionMapper;
import org.grobid.service.exceptions.mapper.GrobidExceptionsTranslationUtility;
import org.grobid.service.exceptions.mapper.GrobidServiceExceptionMapper;
import org.grobid.service.exceptions.mapper.WebApplicationExceptionMapper;

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
        binder.bind(StructureIdentificationModuleClient.class);
        binder.bind(ClassResolverModuleClient.class);
        binder.bind(MaterialClassResolver.class);
        binder.bind(ChemicalMaterialParserClient.class);
        binder.bind(MaterialParser.class);
        binder.bind(LinkingModuleClient.class);
        binder.bind(RuleBasedLinker.class);
        binder.bind(CRFBasedLinker.class);
        binder.bind(SuperconductorsParser.class);
        binder.bind(ModuleEngine.class);

        //REST
        binder.bind(AnnotationController.class);
        binder.bind(MaterialController.class);
        binder.bind(LinkerController.class);

        //Exception Mappers
        binder.bind(GrobidServiceExceptionMapper.class);
        binder.bind(GrobidExceptionsTranslationUtility.class);
        binder.bind(GrobidExceptionMapper.class);
        binder.bind(WebApplicationExceptionMapper.class);
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