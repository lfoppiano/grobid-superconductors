package org.grobid.service.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.DocumentResponse;
import org.grobid.core.data.TextPassage;
import org.grobid.core.data.Span;
import org.grobid.core.engines.AggregatedProcessing;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
@Path("/")
public class AnnotationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

    private AggregatedProcessing aggregatedProcessing;

    @Inject
    public AnnotationController(GrobidSuperconductorsConfiguration configuration, AggregatedProcessing aggregatedProcessing) {
        this.aggregatedProcessing = aggregatedProcessing;
    }

    @Path("/annotations/feedback")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public void annotationFeedback(@FormParam("name") String name,
                                   @FormParam("pk") String key,
                                   @FormParam("value") String value) {

        // pk can be used to add more information - at the moment is the same as name
        LOGGER.debug("Received feedback on annotation for " + name + " with value " + value);

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

    @Path("/process/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/csv")
    @POST
    public String processPdfSuperconductorsCSV(@FormDataParam("input") InputStream uploadedInputStream,
                                               @FormDataParam("input") FormDataContentDisposition fileDetail,
                                               @FormDataParam("disableLinking") boolean disableLinking) {
        long start = System.currentTimeMillis();
        DocumentResponse documentResponse = processPdfSuperconductors(uploadedInputStream, fileDetail, disableLinking);
        long end = System.currentTimeMillis();

        documentResponse.setRuntime(end - start);

        return documentResponse.toCsv();
    }
}
