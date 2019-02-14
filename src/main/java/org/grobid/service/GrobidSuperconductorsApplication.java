package org.grobid.service;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.grobid.service.command.TrainingGenerationCommand;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

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
        return Lists.newArrayList(new SuperconductorsServiceModule());
    }

    @Override
    public void initialize(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap) {
        GuiceBundle<GrobidSuperconductorsConfiguration> guiceBundle = GuiceBundle.defaultBuilder(GrobidSuperconductorsConfiguration.class)
                .modules(getGuiceModules())
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/web", "/", "index.html", "assets"));
        bootstrap.addCommand(new TrainingGenerationCommand());
    }

    @Override
    public void run(GrobidSuperconductorsConfiguration configuration, Environment environment) {
        environment.jersey().setUrlPattern(RESOURCES + "/*");
    }
}
