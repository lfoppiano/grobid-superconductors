package org.grobid.core.engines;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import jep.Interpreter;
import jep.JepConfig;
import jep.JepException;
import jep.SharedInterpreter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SystemUtils;
import org.grobid.core.data.ProcessedParagraph;
import org.grobid.core.jni.PythonEnvironmentConfig;
import org.grobid.core.layout.BoundingBox;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.grobid.core.main.LibraryLoader.*;

@Singleton
public class RulesBasedLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesBasedLinker.class);

    private GrobidSuperconductorsConfiguration configuration;
    private final JepEngine engineController;
    private boolean disabled = false;

    @Inject
    public RulesBasedLinker(GrobidSuperconductorsConfiguration configuration, JepEngine engineController) {
        this.configuration = configuration;
        this.engineController = engineController;
        init();
    }


    public void init() {
        if (this.engineController.getDisabled()) {
            LOGGER.info("The JEP engine wasn't initialised correct. Disabling all dependent modules. ");
            this.disabled = true;
            return;
        }
        try {
            try (Interpreter interp = new SharedInterpreter()) {
                interp.exec("import numpy as np");
                interp.exec("import spacy");
                interp.exec("from linking_module import process_paragraph_json");
            }

        } catch (Exception e) {
            LOGGER.error("Loading JEP native library failed. The linking will be disabled.", e);
            this.disabled = true;
        }
    }

    public List<ProcessedParagraph> process(ProcessedParagraph paragraph) {
        if (CollectionUtils.isEmpty(paragraph.getSpans()) || disabled) {
            return Collections.singletonList(paragraph);
        }

        //Take out the bounding boxes
        Map<String, List<BoundingBox>> backupBoundingBoxes = new HashMap<>();
        paragraph.getSpans().forEach(s -> {
            backupBoundingBoxes.put(String.valueOf(s.getId()), s.getBoundingBoxes());
            s.setBoundingBoxes(new ArrayList<>());
        });

        //Take out the attributes
        Map<String, Map<String, String>> backupAttributes = new HashMap<>();
        paragraph.getSpans().forEach(s -> {
            backupAttributes.put(String.valueOf(s.getId()), s.getAttributes());
        });

        // We convert the initial object in JSON to avoid problems
        Writer jsonWriter = new StringWriter();
        ObjectMapper oMapper = new ObjectMapper();
        try {
            oMapper.writeValue(jsonWriter, paragraph);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String jsonTarget = jsonWriter.toString();

        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from linking_module import process_paragraph_json");
            interp.set("paragraph", jsonTarget);
            interp.exec("extracted_data_from_paragraphs = process_paragraph_json(paragraph)");
            String sentencesAsJson = interp.getValue("extracted_data_from_paragraphs", String.class);

//            System.out.println(sentencesAsJson);

            try {
                TypeReference<List<ProcessedParagraph>> mapType = new TypeReference<List<ProcessedParagraph>>() {
                };
                List<ProcessedParagraph> processed = oMapper.readValue(sentencesAsJson, mapType);

                // put the bounding boxes back where they were
                processed.stream()
                    .forEach(p -> {
                        p.getSpans().stream()
                            .forEach(s -> {
                                s.setBoundingBoxes(backupBoundingBoxes.get(String.valueOf(s.getId())));
                                s.setAttributes(backupAttributes.get(String.valueOf(s.getId())));
                            });
                    });

                return processed;

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (JepException e) {
            e.printStackTrace();
        }

        return Arrays.asList(paragraph);
    }
}
