package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabelImpl;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesTEIStaxHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAG_TYPES;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;


public class SuperconductorsNoFeaturesTrainer extends AbstractTrainerNew {
    public static final String FOLD_TYPE_PARAGRAPH = "paragraph";
    public static final String FOLD_TYPE_DOCUMENT = "document";

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public SuperconductorsNoFeaturesTrainer() {
        super(SuperconductorsModels.SUPERCONDUCTORS);
        // adjusting CRF training parameters for this model
        epsilon = 0.000001;
        window = 20;
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

            Path adaptedCorpusDir = Paths.get(corpusDir.getAbsolutePath(), "final");
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

            Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
            StringBuilder output = new StringBuilder();
            for (int n = 0; n < refFiles.size(); n++) {
                File theFile = refFiles.get(n);
                name = theFile.getName();
                LOGGER.info(name);

                AnnotationValuesTEIStaxHandler handler = new AnnotationValuesTEIStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS,
                    ANNOTATION_DEFAULT_TAG_TYPES);
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<Pair<String, String>> labeled = handler.getLabeledStream();

                List<List<Pair<String, String>>> xmlFile = new ArrayList<>();
                List<Pair<String, String>> paragraphXmlFile = new ArrayList<>();
                for (int idx = 0; idx < labeled.size(); idx++) {
                    String tag = labeled.get(idx).getRight();
                    if (StringUtils.isEmpty(tag)) {
                        xmlFile.add(paragraphXmlFile);
                        paragraphXmlFile = new ArrayList<>();
                    } else {
                        paragraphXmlFile.add(labeled.get(idx));
                    }
                }
                if (isNotEmpty(paragraphXmlFile)) {
                    xmlFile.add(paragraphXmlFile);
                }

                // we can now add the features
                for (List<Pair<String, String>> paragraph : xmlFile) {
                    long entityLabels = 0;
                    for (Pair<String, String> token : paragraph) {
                        String value = token.getLeft();
                        String label = token.getRight();
                        value = UnicodeUtil.normaliseTextAndRemoveSpaces(value);

                        TaggingLabel taggingLabel = TaggingLabels.labelFor(SuperconductorsModels.SUPERCONDUCTORS, label);
                        FeaturesVectorSuperconductors featuresVectorSuperconductors = FeaturesVectorSuperconductors.addFeatures(new LayoutToken(value, taggingLabel), label, null, "false");
                        output.append(featuresVectorSuperconductors.printVector()).append("\n");
                        if (!token.getRight().equals(TaggingLabels.OTHER_LABEL)) {
                            entityLabels++;
                        }
                    }

                    if (isNotBlank(output.toString()) && entityLabels > 0) {
                        output.append("\n");
                        writer.write(output.toString() + "\n");
                        writer.flush();
                        writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                    }
                    output = new StringBuilder();
                }

                writer.write(output.toString() + "\n");
                writer.write("\n");
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(evaluationOutputWriter, trainingOutputWriter);
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

        Trainer trainer = new SuperconductorsNoFeaturesTrainer();

        AbstractTrainer.runTraining(trainer);
    }

    @Override
    public int createCRFPPDataSingle(File inputFile, File outputDirectory) {
        return 0;
    }
}