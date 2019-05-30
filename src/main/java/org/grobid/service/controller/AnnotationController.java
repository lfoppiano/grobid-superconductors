package org.grobid.service.controller;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.OutputResponse;
import org.grobid.core.engines.AggregatedProcessing;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Singleton
@Path("/")
public class AnnotationController {

    private static final String PATH_BASE = "/";
    private static final String PATH_IS_ALIVE = "isalive";

    private AggregatedProcessing aggregatedProcessing;

    @Inject
    public AnnotationController(GrobidSuperconductorsConfiguration configuration, AggregatedProcessing aggregatedProcessing) {
        this.aggregatedProcessing = aggregatedProcessing;
    }

    @Path(PATH_IS_ALIVE)
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public static Response isAlive() {
        Response response = null;
        try {

            String retVal = null;
            try {
                retVal = Boolean.valueOf(true).toString();
            } catch (Exception e) {
                retVal = Boolean.valueOf(false).toString();
            }
            response = Response.status(Response.Status.OK).entity(retVal).build();
        } catch (Exception e) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    @Path("annotateSuperconductorsPDF")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @POST
    public String processPDF(@FormDataParam("input") InputStream uploadedInputStream,
                             @FormDataParam("input") FormDataContentDisposition fileDetail) {
        long start = System.currentTimeMillis();
        OutputResponse extractedEntities = aggregatedProcessing.process(uploadedInputStream);
        long end = System.currentTimeMillis();

        extractedEntities.setRuntime(end - start);

        return extractedEntities.toJson();
    }

    @Path("processSuperconductorsText")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public String processTextSuperconductors(@FormDataParam("text") String text) {

        String textPreprocessed = text.replace("\r\n", "\n");

        long start = System.currentTimeMillis();
        OutputResponse extractedEntities = aggregatedProcessing.process(textPreprocessed);
        long end = System.currentTimeMillis();

        extractedEntities.setRuntime(end - start);

        return extractedEntities.toJson();
    }
}
