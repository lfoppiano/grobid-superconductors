package org.grobid.service.controller;

import org.grobid.core.data.ServiceInfo;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
