package org.grobid.service.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.DocumentResponse;
import org.grobid.core.engines.ModuleEngine;
import org.grobid.core.engines.TabularDataEngine;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
@Path("/")
public class AnnotationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

    private ModuleEngine moduleEngine;

    @Inject
    public AnnotationController(GrobidSuperconductorsConfiguration configuration, ModuleEngine moduleEngine) {
        this.moduleEngine = moduleEngine;
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
        DocumentResponse extractedEntities = moduleEngine.process(textPreprocessed, disableLinking);
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
        DocumentResponse response = moduleEngine.process(uploadedInputStream, disableLinking);
        long end = System.currentTimeMillis();

        response.setRuntime(end - start);

        return response;
    }

    @Path("/process/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/csv")
    @POST
    public Optional<String> processPdfSuperconductorsCSV(@FormDataParam("input") InputStream uploadedInputStream,
                                                         @FormDataParam("input") FormDataContentDisposition fileDetail,
                                                         @FormDataParam("disableLinking") boolean disableLinking,
                                                         @FormDataParam("extractAllEntities") boolean extractAllEntities) {
        long start = System.currentTimeMillis();
        DocumentResponse documentResponse = processPdfSuperconductors(uploadedInputStream, fileDetail, disableLinking);
        long end = System.currentTimeMillis();

        documentResponse.setRuntime(end - start);
        String csvOutput = "";
        if (!extractAllEntities) {
            csvOutput = documentResponse.toCsv();
        } else {
            csvOutput = documentResponse.toCsvAll();
        }

        if (StringUtils.isBlank(csvOutput) || csvOutput.split("\n").length == 1) {
            return Optional.empty();
        }

        return Optional.of(csvOutput);
    }

    @Path("/process/json")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Optional<List<SuperconEntry>> processJsonToTabular(@FormDataParam("input") DocumentResponse jsonResponse,
                                                              @FormDataParam("outputAll") boolean outputEverything) {
        List<SuperconEntry> superconEntries = new ArrayList<>();
        if (!outputEverything) {
            superconEntries = TabularDataEngine.computeTabularData(jsonResponse.getPassages());
        } else {
            superconEntries = TabularDataEngine.extractEntities(jsonResponse.getPassages());
        }

        if (CollectionUtils.isEmpty(superconEntries)) {
            return Optional.empty();
        }

        return Optional.of(superconEntries);

    }
}
