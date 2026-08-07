package org.grobid.service;

import com.google.inject.AbstractModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.eclipse.jetty.server.handler.QoSHandler;
import org.grobid.service.command.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.service.controller.HealthCheck;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        environment.healthChecks().register("health-check", new HealthCheck(configuration));

        // Enable CORS via Jetty 12's CrossOriginHandler (replaces the removed
        // org.eclipse.jetty.servlets.CrossOriginFilter). Inserted above the application
        // context so it applies to all served paths.
        CrossOriginHandler cors = new CrossOriginHandler();
        cors.setAllowedOriginPatterns(toSet(configuration.getCorsAllowedOrigins()));
        cors.setAllowedMethods(toSet(configuration.getCorsAllowedMethods()));
        cors.setAllowedHeaders(toSet(configuration.getCorsAllowedHeaders()));
        cors.setAllowCredentials(false);
        environment.getApplicationContext().insertHandler(cors);

        // Limit concurrent requests via Jetty 12's QoSHandler (replaces the removed
        // org.eclipse.jetty.servlets.QoSFilter). A non-positive value means unlimited.
        int maxParallelRequests = configuration.getMaxParallelRequests();
        if (maxParallelRequests > 0) {
            QoSHandler qos = new QoSHandler();
            qos.setMaxRequestCount(maxParallelRequests);
            environment.getApplicationContext().insertHandler(qos);
        }

        environment.jersey().setUrlPattern(RESOURCES + "/*");
        environment.jersey().register(new EmptyOptionalNoContentExceptionMapper());

    }

    /**
     * Splits a comma-separated config value (e.g. "OPTIONS,GET,POST") into a set of trimmed,
     * non-empty entries preserving order, as expected by {@link CrossOriginHandler}.
     */
    private static Set<String> toSet(String csv) {
        if (csv == null) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
