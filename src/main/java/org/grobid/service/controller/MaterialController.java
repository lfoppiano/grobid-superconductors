package org.grobid.service.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.material.Material;
import org.grobid.core.engines.MaterialParser;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import java.util.Arrays;
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


    @Path("multiparse")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public List<List<Material>> processTextSuperconductorsPost2(@FormDataParam("texts") String texts) {
        return parseMaterials(texts);
    }

    @Path("multiparse")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<List<Material>> processTextSuperconductorsGet2(@FormDataParam("texts") String texts) {
        return parseMaterials(texts);
    }

    private List<Material> parseMaterial(@FormDataParam("text") String text) {
        String textPreprocessed = text.replace("\r\n", "\n");

        return materialParser.process(textPreprocessed);
    }

    private List<List<Material>> parseMaterials(@FormDataParam("text") String text) {
        String textPreprocessed = text.replace("\r\n", "\n");

        List<String> list = Arrays.asList(textPreprocessed.split("\n"));
        return materialParser.processParallel(list);
    }

}
