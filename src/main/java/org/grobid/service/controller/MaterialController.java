package org.grobid.service.controller;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.material.Material;
import org.grobid.core.engines.MaterialParser;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Singleton
@Path("/material")
public class MaterialController {


    private final GrobidSuperconductorsConfiguration configuration;
    private MaterialParser materialParser;

    @Inject
    public MaterialController(GrobidSuperconductorsConfiguration configuration, MaterialParser materialParser) {
        this.materialParser = materialParser;
        this.configuration = configuration;
    }

    @Path("parse")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public List<Material> processTextSuperconductorsPost(@FormDataParam("text") String text) {
        return parseMaterial(text);
    }

    @Path("parse")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Material> processTextSuperconductorsGet(@FormDataParam("text") String text) {
        return parseMaterial(text);
    }

    private List<Material> parseMaterial(@FormDataParam("text") String text) {
        String textPreprocessed = text.replace("\r\n", "\n");

        return materialParser.process(textPreprocessed);
    }

}
