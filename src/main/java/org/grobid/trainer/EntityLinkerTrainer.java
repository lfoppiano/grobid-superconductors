package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.EntityLinkerAnnotationStaxHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class EntityLinkerTrainer extends AbstractTrainer {

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public EntityLinkerTrainer() {
        super(SuperconductorsModels.ENTITY_LINKER);
        // adjusting CRF training parameters for this model
        epsilon = 0.000001;
        window = 40;
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

            Path adaptedCorpusDir = Paths.get(corpusDir.getAbsolutePath().replaceFirst("entityLinker", "superconductors") + File.separator + "final");
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

                EntityLinkerAnnotationStaxHandler handler = new EntityLinkerAnnotationStaxHandler("p",
                    "tcValue", "material");
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<Triple<String, String, String>> labeled = handler.getLabeled();

                int q = 0;

                Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                StringBuilder output = new StringBuilder();

                // we get the label in the labelled data file for the same token
                for (Triple<String, String, String> labeledToken : labeled) {
                    String token = labeledToken.getLeft();
                    String label = labeledToken.getMiddle();
                    String entity_type = labeledToken.getRight();

                    if(token.equals("\n")) {
                        output.append("\n");
                        output.append("\n");
                        writer.write(output.toString());
                        writer.flush();
                        output = new StringBuilder();
                        writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                        continue;
                    }

                    token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    output.append(FeaturesVectorEntityLinker.addFeatures(token, label, entity_type).printVector());
                    output.append("\n");
                }

                writer.write(output.toString());
                writer.write("\n");
                writer.write("\n");
                writer.flush();

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

        Trainer trainer = new EntityLinkerTrainer();

        AbstractTrainer.runTraining(trainer);
    }
}