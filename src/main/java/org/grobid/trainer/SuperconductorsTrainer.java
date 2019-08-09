package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.dbutils.AbstractQueryRunner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.SuperconductorAnnotationStaxHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;

/**
 * @author Patrice Lopez
 */
public class SuperconductorsTrainer extends AbstractTrainer {

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public SuperconductorsTrainer() {
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

            File adaptedCorpusDir = new File(corpusDir.getAbsolutePath() + File.separator + "staging");
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

            // then we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            File[] refFiles = adaptedCorpusDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".tei") || name.toLowerCase().endsWith(".tei.xml")
            );

            if (refFiles == null) {
                return 0;
            }

            LOGGER.info(refFiles.length + " files");

            String name;

            Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
            StringBuilder output = new StringBuilder();
            for (int n = 0; n < refFiles.length; n++) {
                File theFile = refFiles[n];
                name = theFile.getName();
                LOGGER.info(name);

                SuperconductorAnnotationStaxHandler handler = new SuperconductorAnnotationStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                        Arrays.asList("material", "tc", "tcValue", "class"));
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<Pair<String, String>> labeled = handler.getLabeled();

                for (Pair<String, String> example : labeled) {
                    String tag = example.getRight();
                    String token = example.getLeft();

                    if (tag == null || StringUtils.length(StringUtils.trim(tag)) == 0) {
                        output.append("\n");
                        writer.write(output.toString() + "\n");
                        writer.flush();
                        writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                        output = new StringBuilder();
                        continue;
                    }
                    String normalisedToken = UnicodeUtil.normaliseText(token);
                    FeaturesVectorSuperconductors featuresVectorSuperconductors = FeaturesVectorSuperconductors.addFeatures(new LayoutToken(normalisedToken), tag, null, null);
                    output.append(featuresVectorSuperconductors.printVector()).append("\n");

                }
            }

            writer.write(output.toString() + "\n");
            writer.write("\n");

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

        Trainer trainer = new SuperconductorsTrainer();

        GrobidProperties.getInstance();

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        String nFoldCrossValidationReport = AbstractTrainer.runNFoldEvaluation(trainer, 10, true);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logs/superconductors-10fold-cross-validation-" + formatter.format(date) + ".txt"))) {
            writer.write(nFoldCrossValidationReport);
            writer.write("\n");
        } catch (IOException e) {
            throw new GrobidException("Error when saving n-fold cross-validation results into files. ", e);
        }

        AbstractTrainer.runTraining(trainer);
    }
}