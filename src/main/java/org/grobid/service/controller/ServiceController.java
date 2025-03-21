package org.grobid.service.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.grobid.core.data.ServiceInfo;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Singleton
@Path("/")
public class ServiceController {
    
    private final GrobidSuperconductorsConfiguration configuration;

    @Inject
    public ServiceController(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
    }

    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public ServiceInfo getVersion() {
        return new ServiceInfo(GrobidSuperconductorsConfiguration.getVersion(), GrobidSuperconductorsConfiguration.getRevision());
    }
    
}
