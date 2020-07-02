package org.grobid.service.controller;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.DocumentResponse;
import org.grobid.core.engines.AggregatedProcessing;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Singleton
@Path("/")
public class AnnotationController {

    private AggregatedProcessing aggregatedProcessing;

    @Inject
    public AnnotationController(GrobidSuperconductorsConfiguration configuration, AggregatedProcessing aggregatedProcessing) {
        this.aggregatedProcessing = aggregatedProcessing;
    }

    @Path("/process/text")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public DocumentResponse processTextSuperconductors(@FormDataParam("text") String text,
                                                       @FormDataParam("disableLinking") boolean disableLinking) {
        String textPreprocessed = text.replace("\r\n", "\n");

        long start = System.currentTimeMillis();
        DocumentResponse extractedEntities = aggregatedProcessing.process(textPreprocessed, disableLinking);
        long end = System.currentTimeMillis();

        extractedEntities.setRuntime(end - start);

        return extractedEntities;
    }

    @Path("/process/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public DocumentResponse processPdfSuperconductors(@FormDataParam("input") InputStream uploadedInputStream,
                                                      @FormDataParam("input") FormDataContentDisposition fileDetail,
                                                      @FormDataParam("disableLinking") boolean disableLinking) {
        long start = System.currentTimeMillis();
        DocumentResponse response = aggregatedProcessing.process(uploadedInputStream, disableLinking);
        long end = System.currentTimeMillis();

        response.setRuntime(end - start);

        return response;
    }
}
