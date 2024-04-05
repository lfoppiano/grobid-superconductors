package org.grobid.service;

import com.google.inject.AbstractModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.QoSFilter;
import org.grobid.service.command.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import java.util.EnumSet;

public class GrobidSuperconductorsApplication extends Application<GrobidSuperconductorsConfiguration> {
    private static final String RESOURCES = "/service";

    public static void main(String[] args) throws Exception {
        new GrobidSuperconductorsApplication().run(args);
    }

    @Override
    public String getName() {
        return "grobid-superconductors";
    }

    private AbstractModule getGuiceModules() {
        return new SuperconductorsServiceModule();
    }

    @Override
    public void initialize(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        GuiceBundle guiceBundle = GuiceBundle.builder()
            .modules(getGuiceModules())
            .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/web", "/", "index.html", "assets"));
        bootstrap.addCommand(new RunTrainingCommand());
        bootstrap.addCommand(new InterAnnotationAgreementCommand());
        bootstrap.addCommand(new TrainingGenerationCommand());
        bootstrap.addCommand(new PrepareDelftTrainingCommand());
        bootstrap.addCommand(new PrepareMaterialParserTrainingCommand());
    }

    @Override
    public void run(GrobidSuperconductorsConfiguration configuration, Environment environment) {
        String allowedOrigins = configuration.getCorsAllowedOrigins();
        String allowedMethods = configuration.getCorsAllowedMethods();
        String allowedHeaders = configuration.getCorsAllowedHeaders();

        // Enable CORS headers
        final FilterRegistration.Dynamic cors =
            environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeaders);

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        // Enable QoS filter
        final FilterRegistration.Dynamic qosPdf = environment.servlets().addFilter("QOS", QoSFilter.class);
        qosPdf.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "*/process/pdf", "*/process/text");
        qosPdf.setInitParameter("maxRequests", String.valueOf(configuration.getMaxParallelRequests()));
        qosPdf.setInitParameter("waitMs", String.valueOf(0));
        qosPdf.setInitParameter("suspendMs", String.valueOf(0));

        // Enable DDOS
//        final FilterRegistration.Dynamic ddos =environment.servlets().addFilter("DDOS", DoSFilter.class);
//        ddos.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "*/process/pdf", "*/process/text");
//        ddos.setInitParameter("delayMs", String.valueOf(5000));
//        ddos.setInitParameter("maxRequestsPerSec", String.valueOf(configuration.getMaxParallelRequests()));

        environment.jersey().setUrlPattern(RESOURCES + "/*");
        environment.jersey().register(new EmptyOptionalNoContentExceptionMapper());

    }
}
