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
        DocumentResponse document = aggregatedProcessing.process(uploadedInputStream, disableLinking);

        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();
        for (TextPassage paragraph : document.getParagraphs()) {
            List<Span> linkedSpans = paragraph.getSpans().stream()
                .filter(s -> s.getLinks().size() > 0)
                .collect(Collectors.toList());

            for (Span span : linkedSpans) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }

        // Materials
        List<Span> materials = spansById.entrySet().stream()
            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

//        List<Span> tcValues = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());
//
//        List<Span> pressures = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());

        List<List<String>> outputCSV = new ArrayList<>();
        for (Span m : materials) {
            String formula = "";
            String name = "";
            String cla = "";
            String doping = "";
            String shape = "";
            String fabrication = "";
            String substrate = "";

            for (Map.Entry<String, String> a : m.getAttributes().entrySet()) {
                String[] splits = a.getKey().split("_");
                String prefix = splits[0];
                String propertyName = splits[1];
                String value = a.getValue();

                if (propertyName.equals("formula")) {
                    formula = value;
                } else if (propertyName.equals("name")) {
                    name = value;
                } else if (propertyName.equals("clazz")) {
                    cla = value;
                } else if (propertyName.equals("shape")) {
                    shape = value;
                } else if (propertyName.equals("doping")) {
                    doping = value;
                } else if (propertyName.equals("fabrication")) {
                    fabrication = value;
                } else if (propertyName.equals("substrate")) {
                    substrate = value;
                }
            }

            Map<String, String> linkedToMaterial = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));

            for (Map.Entry<String, String> entry : linkedToMaterial.entrySet()) {
                Span tcValue = spansById.get(entry.getKey());
                List<Span> pressures = tcValue.getLinks().stream()
                    .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                    .map(l -> spansById.get(l.getTargetId()))
                    .collect(Collectors.toList());

                if (isNotEmpty(pressures)) {
                    for (Span pressure : pressures) {
                        outputCSV.add(Arrays.asList(m.getText(), name, formula, doping, shape, cla, fabrication, substrate,
                            tcValue.getText(), pressure.getText(), entry.getValue(), sectionsById.get(m.getId()).getLeft(),
                            sectionsById.get(m.getId()).getRight(), sentenceById.get(m.getId())));
                    }
                } else {
                    outputCSV.add(Arrays.asList(m.getText(), name, formula, doping, shape, cla, fabrication, substrate,
                        tcValue.getText(), "", entry.getValue(), sectionsById.get(m.getId()).getLeft(),
                        sectionsById.get(m.getId()).getRight(), sentenceById.get(m.getId())));
                }

            }
        }

        StringBuilder out = new StringBuilder();
        try {
            final CSVPrinter printer = CSVFormat.DEFAULT
                .withHeader("Raw material", "Name", "Formula", "Doping", "Shape", "Class", "Fabrication", "Substrate", "Critical temperature", "Applied pressure", "Link type", "Section", "Subsection", "Sentence")
                .withQuote('"')
                .withQuoteMode(QuoteMode.ALL)
                .print(out);

            printer.printRecords(outputCSV);
        } catch (IOException e) {
            LOGGER.error("Soemthing wrong when pushing out the CSV", e);
        }

        return out.toString();
    }
}
