package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.SuperconductorAnnotationStaxHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;
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

            Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
            StringBuilder output = new StringBuilder();
            for (int n = 0; n < refFiles.size(); n++) {
                File theFile = refFiles.get(n);
                name = theFile.getName();
                LOGGER.info(name);

                SuperconductorAnnotationStaxHandler handler = new SuperconductorAnnotationStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                    ANNOTATION_DEFAULT_TAGS);
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<Pair<String, String>> labeled = handler.getLabeled();

                // we can now add the features
                // we open the featured file
                File theRawFile = new File(theFile.getAbsolutePath().replace(".tei.xml", ".features.txt"));
                if (!theRawFile.exists()) {
                    LOGGER.warn("Raw file " + theRawFile + " does not exist. Please have a look!");
                    continue;
                }
                int q = 0;
                BufferedReader bis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(theRawFile), UTF_8));
                String line;
                while ((line = bis.readLine()) != null) {
                    int ii = line.indexOf('\t');
                    if (ii == -1) {
                        ii = line.indexOf(' ');
                    }
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii).trim();
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been generated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }
                    // we get the label in the labelled data file for the same token
                    for (int pp = q; pp < labeled.size(); pp++) {
                        String tag = labeled.get(pp).getRight();

                        if (tag == null || StringUtils.length(StringUtils.trim(tag)) == 0) {
                            output.append("\n");
                            writer.write(output.toString() + "\n");
                            writer.flush();
                            writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                            output = new StringBuilder();
                            continue;
                        }
                        String localToken = labeled.get(pp).getLeft();
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been gnerated by a recent version of grobid
                        localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                        if (localToken.equals(token)) {
                            line = line.replace("\t", " ").replace("  ", " ");
                            output.append(line).append(" ").append(tag).append("\n");
                            q = pp + 1;
                            pp = q + 10;
                        }

                        if (pp - q > 5) {
                            break;
                        }
                    }
                }
                bis.close();

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

        Trainer trainer = new SuperconductorsTrainer();

        AbstractTrainer.runTraining(trainer);
    }
}