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
public class LinkingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkingEngine.class);

    private GrobidSuperconductorsConfiguration configuration;
    private boolean disabled = false;

    @Inject
    public LinkingEngine(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        init();
    }


    public void init() {
        File libraryFolder = new File(getLibraryFolder());

        LOGGER.info("Loading JEP native library for the linking module... " + libraryFolder.getAbsolutePath());
        // actual loading will be made at JEP initialization, so we just need to add the path in the
        // java.library.path (JEP will anyway try to load from java.library.path, so explicit file
        // loading here will not help)
        try {
            addLibraryPath(libraryFolder.getAbsolutePath());

            PythonEnvironmentConfig pythonEnvironmentConfig = PythonEnvironmentConfig.getInstanceForVirtualEnv(configuration.getPythonVirtualEnv(), PythonEnvironmentConfig.getActiveVirtualEnv());
            if (pythonEnvironmentConfig.isEmpty()) {
                LOGGER.info("No python environment configured");
            } else {
                LOGGER.info("Configuring python environment: " + pythonEnvironmentConfig.getVirtualEnv());
                LOGGER.info("Adding library paths " + Arrays.toString(pythonEnvironmentConfig.getNativeLibPaths()));
                for (Path path : pythonEnvironmentConfig.getNativeLibPaths()) {
                    if (Files.exists(path)) {
                        addLibraryPath(path.toString());
                    } else {
                        LOGGER.warn(path.toString() + " does not exists. Skipping it. ");
                    }
                }

                if (SystemUtils.IS_OS_MAC) {
                    System.loadLibrary("python" + pythonEnvironmentConfig.getPythonVersion() + "m");
                    System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                } else if (SystemUtils.IS_OS_LINUX) {
                    System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    throw new UnsupportedOperationException("Linking on Windows is not supported.");
                }
            }

            JepConfig config = new JepConfig();
            config.setRedirectOutputStreams(configuration.isPythonRedirectOutput());
            SharedInterpreter.setConfig(config);
            LOGGER.debug("Configuring JEP to redirect python output.");

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
                processed.stream().forEach(p -> {
                    p.getSpans().stream().forEach(s -> {
                        s.setBoundingBoxes(backupBoundingBoxes.get(String.valueOf(s.getId())));
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
