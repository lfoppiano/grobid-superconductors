package org.grobid.service.controller;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.grobid.service.GrobidSuperconductorsApplication;
import org.grobid.service.configuration.GrobidQuantitiesConfiguration;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("health")
@Singleton
@Produces(APPLICATION_JSON)
public class HealthCheck extends com.codahale.metrics.health.HealthCheck {

    @Inject
    private GrobidSuperconductorsConfiguration configuration;

    @jakarta.inject.Inject
    public HealthCheck(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
    }

    @GET
    public Response alive() {
        return Response.ok().build();
    }

    @Override
    protected Result check() throws Exception {
        return configuration.getGrobidHome() != null ? Result.healthy() :
            Result.unhealthy("Grobid home is null in the configuration");
    }
}
