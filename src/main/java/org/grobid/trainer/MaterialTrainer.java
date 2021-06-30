package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.MaterialParser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorMaterial;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesStaxHandler;
import org.grobid.trainer.stax.handler.AnnotationValuesTEIStaxHandler;
import org.grobid.trainer.stax.handler.MaterialAnnotationStaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;

public class MaterialTrainer extends AbstractTrainerNew {
    public static final List<String> TOP_LEVEL_ANNOTATION_DEFAULT_TAGS = Arrays.asList("material");
    public static final List<String> ANNOTATION_DEFAULT_TAGS = Arrays.asList("formula", "variable",
        "value", "name", "shape", "doping", "fabrication", "substrate");

    protected static final Logger LOGGER = LoggerFactory.getLogger(MaterialTrainer.class);
    private final WstxInputFactory inputFactory = new WstxInputFactory();

    public MaterialTrainer() {
        super(SuperconductorsModels.MATERIAL);
        // adjusting CRF training parameters for this model
        epsilon = 0.000001;
        window = 20;
//        algorithm = "rprop";
    }

    /**
     * Processes the XML files of the superconductors model, and extract all the content labeled as material
     */
    public void createTrainingDataFromSuperconductors(String inputDirectory, String outputDirectory, boolean recursive) {
        Path inputPath = Paths.get(inputDirectory);

        MaterialParser materialParser = MaterialParser.getInstance(null);

        int maxDept = recursive ? Integer.MAX_VALUE : 1;
        List<File> refFiles = new ArrayList<>();
        try {
            refFiles = Files.walk(inputPath, maxDept)
                .filter(path -> Files.isRegularFile(path)
                    && (StringUtils.endsWithIgnoreCase(path.getFileName().toString(), ".tei.xml")))
                .map(Path::toFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return;
        }

        if (isEmpty(refFiles)) {
            return;
        }

        for (File inputFile : refFiles) {
            Writer outputWriter = null;
            try {
                // the file for writing the training data
                Path relativeOutputPath = Paths.get(outputDirectory, String.valueOf(Paths.get(inputDirectory).relativize(Paths.get(inputFile.getAbsolutePath()))));
                Files.createDirectories(relativeOutputPath.getParent());
                OutputStream os2 = new FileOutputStream(relativeOutputPath.toString().replace("superconductors.tei.xml", "material.tei.xml"));
                outputWriter = new OutputStreamWriter(os2, UTF_8);

                outputWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
                outputWriter.write("<materials>\n");

                AnnotationValuesTEIStaxHandler target = new AnnotationValuesTEIStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS, Collections.singletonList("material"));

                InputStream inputStream = new FileInputStream(inputFile.getAbsolutePath());
                XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

                StaxUtils.traverse(reader, target);

                List<Pair<String, String>> materials = target.getLabeledEntities();

                for (Pair<String, String> material : materials) {
                    String materialName = material.getLeft();
                    String tagged = materialParser.generateTrainingData(materialName);

                    outputWriter.write("\t<material>");
                    outputWriter.write(tagged);
                    outputWriter.write("</material>\n");
                }

                outputWriter.write("</materials>\n");
            } catch (IOException | XMLStreamException e) {
                e.printStackTrace();
            } finally {
                closeQuietly(outputWriter);
            }
        }
    }

    /**
     * Add the selected features to the model training
     */
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        int totalExamples = 0;
        Writer trainingOutputWriter = null;
        Writer evaluationOutputWriter = null;

        try {

            Path adaptedCorpusDir = Paths.get(corpusDir.getAbsolutePath() + File.separator + "final");
            LOGGER.info("sourcePathLabel: " + adaptedCorpusDir);
            if (trainingOutputPath != null)
                LOGGER.info("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                LOGGER.info("outputPath for evaluation data: " + evalOutputPath);

            // the file for writing the training data
            OutputStream os2 = null;

            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                trainingOutputWriter = new OutputStreamWriter(os2, UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;

            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                evaluationOutputWriter = new OutputStreamWriter(os3, UTF_8);
            }

            List<File> refFiles = Files.walk(adaptedCorpusDir, Integer.MAX_VALUE)
                .filter(path -> Files.isRegularFile(path)
                    && (StringUtils.endsWithIgnoreCase(path.getFileName().toString(), ".xml")))
                .map(Path::toFile)
                .collect(Collectors.toList());

            LOGGER.info(refFiles.size() + " files to be processed.");

            if (isEmpty(refFiles)) {
                return 0;
            }

            LOGGER.info(refFiles.size() + " files");

            String name;

            for (int n = 0; n < refFiles.size(); n++) {
                File theFile = refFiles.get(n);
                name = theFile.getName();
                LOGGER.info(name);

                MaterialAnnotationStaxHandler handler = new MaterialAnnotationStaxHandler(
                    TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                    ANNOTATION_DEFAULT_TAGS);
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<List<Pair<String, String>>> labeled = handler.getLabeled();

                int q = 0;

                // we get the label in the labelled data file for the same token
                for (List<Pair<String, String>> labeledMaterial : labeled) {
                    Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                    StringBuilder output = new StringBuilder();
                    for (Pair<String, String> materialComponent : labeledMaterial) {

                        String token = materialComponent.getLeft();
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        String tag = materialComponent.getRight();

                        if (tag == null) {
                            continue;
                        }

                        output.append(FeaturesVectorMaterial.addFeatures(token, tag).printVector());
                        output.append("\n");
                    }

                    output.append("\n");
                    output.append("\n");
                    writer.write(output.toString());
                    writer.flush();


                    continue;
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            closeQuietly(evaluationOutputWriter, trainingOutputWriter);
        }
        return totalExamples;
    }

    /**
     * Add the selected features to the model training for bio entities
     */
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {

        return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);

    }

    /**
     * Command line execution. Assuming grobid-home is in ../grobid-home.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        GrobidProperties.getInstance();

        Trainer trainer = new MaterialTrainer();

        AbstractTrainer.runTraining(trainer);
    }

    @Override
    public int createCRFPPDataSingle(File inputFile, File outputDirectory) {
        return 0;
    }
}