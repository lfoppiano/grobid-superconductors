package org.grobid.service;

import com.google.inject.Module;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.QoSFilter;
import org.grobid.service.command.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class GrobidSuperconductorsApplication extends Application<GrobidSuperconductorsConfiguration> {
    private static final String RESOURCES = "/service";

    public static void main(String[] args) throws Exception {
        new GrobidSuperconductorsApplication().run(args);
    }

    @Override
    public String getName() {
        return "grobid-superconductors";
    }

    private List<? extends Module> getGuiceModules() {
        return Arrays.asList(new SuperconductorsServiceModule());
    }

    @Override
    public void initialize(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        GuiceBundle<GrobidSuperconductorsConfiguration> guiceBundle = GuiceBundle.defaultBuilder(GrobidSuperconductorsConfiguration.class)
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
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        // Enable QoS filter
        final FilterRegistration.Dynamic qos = environment.servlets().addFilter("QOS", QoSFilter.class);
        qos.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
//        final FilterRegistration.Dynamic ddos =environment.servlets().addFilter("DDOS", DoSFilter.class);

        qos.setInitParameter("maxRequests", configuration.getMaxRequests());

        environment.jersey().setUrlPattern(RESOURCES + "/*");
        environment.jersey().register(new EmptyOptionalNoContentExceptionMapper());

    }
}
