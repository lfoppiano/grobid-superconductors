package org.grobid.service;

import com.google.inject.Provides;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.grobid.core.engines.*;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.utilities.client.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.service.controller.*;
import org.grobid.service.exceptions.mapper.GrobidExceptionMapper;
import org.grobid.service.exceptions.mapper.GrobidExceptionsTranslationUtility;
import org.grobid.service.exceptions.mapper.GrobidServiceExceptionMapper;
import org.grobid.service.exceptions.mapper.WebApplicationExceptionMapper;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;


public class SuperconductorsServiceModule extends DropwizardAwareModule<GrobidSuperconductorsConfiguration> {

    @Override
    public void configure() {
        // Generic modules
        bind(GrobidEngineInitialiser.class);
        bind(HealthCheck.class);

        //Core services
        bind(ChemspotClient.class);
        bind(ChemDataExtractorClient.class);
        bind(StructureIdentificationModuleClient.class);
        bind(ClassResolverModuleClient.class);
        bind(MaterialClassResolver.class);
        bind(ChemicalMaterialParserClient.class);
        bind(MaterialParser.class);
        bind(LinkingModuleClient.class);
        bind(RuleBasedLinker.class);
        bind(CRFBasedLinker.class);
        bind(SuperconductorsParser.class);
        bind(ModuleEngine.class);

        //REST
        bind(AnnotationController.class);
        bind(MaterialController.class);
        bind(LinkerController.class);
        bind(ServiceController.class);

        //Exception Mappers
        bind(GrobidServiceExceptionMapper.class);
        bind(GrobidExceptionsTranslationUtility.class);
        bind(GrobidExceptionMapper.class);
        bind(WebApplicationExceptionMapper.class);
    }

    @Provides
    Client provideClient() {
        return ClientBuilder.newClient();
    }

}