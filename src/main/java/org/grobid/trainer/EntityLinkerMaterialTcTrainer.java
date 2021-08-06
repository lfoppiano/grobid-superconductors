package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.data.LinkToken;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.EntityLinkerAnnotationTEIStaxHandler;
import org.grobid.trainer.stax.handler.AnnotationOffsetsTEIExtractionStaxHandler;
import org.grobid.trainer.stax.handler.EntityLinkerAnnotationTEIStaxHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;

public class EntityLinkerMaterialTcTrainer extends AbstractTrainerNew {

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public static String SOURCE = "tcValue";
    public static String DESTINATION = "material";

    public EntityLinkerMaterialTcTrainer() {
        super(SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC);
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

            Path adaptedCorpusDir = Paths.get(corpusDir.getAbsolutePath()
                .replaceFirst(SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC.getModelName(), "superconductors") + File.separator + "final");
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

                EntityLinkerAnnotationTEIStaxHandler handler = 
                    new EntityLinkerAnnotationTEIStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS,
                    SOURCE, DESTINATION);
                XMLStreamReader2 reader = inputFactory.createXMLStreamReader(theFile);
                StaxUtils.traverse(reader, handler);

                List<LinkToken> labeled = handler.getLabeled();

                Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                StringBuilder output = new StringBuilder();
                int materials = 0;
                int tcValues = 0;

                // we get the label in the labelled data file for the same token
                for (LinkToken labeledToken : labeled) {
                    String token = labeledToken.getText();
                    String label = labeledToken.getLinkLabel();
                    String entity_type = GenericTaggerUtils.getPlainLabel(labeledToken.getEntityLabel());
                    if (StringUtils.equalsIgnoreCase(entity_type, "<" + DESTINATION + ">")) {
                        materials++;
                    }

                    if (StringUtils.equalsIgnoreCase(entity_type, "<" + SOURCE + ">")) {
                        tcValues++;
                    }

                    if (token.equals("\n")) {
                        output.append("\n");
                        output.append("\n");
                        if (materials > 0 && tcValues > 0) {
                            writer.write(output.toString());
                            writer.flush();
                            writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                        }
                        output = new StringBuilder();
                        materials = 0;
                        tcValues = 0;
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

        Trainer trainer = new EntityLinkerMaterialTcTrainer();

        AbstractTrainer.runTraining(trainer);
    }

    @Override
    public int createCRFPPDataSingle(File inputFile, File outputDirectory) {
        return 0;
    }
}