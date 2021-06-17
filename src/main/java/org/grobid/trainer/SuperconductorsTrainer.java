package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.engines.SentenceSegmenter;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
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


public class SuperconductorsTrainer extends AbstractTrainer {
    public static final String FOLD_TYPE_PARAGRAPH = "paragraph";
    public static final String FOLD_TYPE_DOCUMENT = "document";

    private WstxInputFactory inputFactory = new WstxInputFactory();
    private SentenceSegmenter segmenter; 

    public SuperconductorsTrainer() {
        super(SuperconductorsModels.SUPERCONDUCTORS);
        // adjusting CRF training parameters for this model
        epsilon = 0.000001;
        window = 20;
        this.segmenter = new SentenceSegmenter();
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

                // we can now add the features
                // we open the featured file
                File theRawFile = new File(theFile.getAbsolutePath().replace(".tei.xml", ".features.txt"));
                if (!theRawFile.exists()) {
                    LOGGER.warn("Raw file " + theRawFile + " does not exist. Please have a look!");
                    continue;
                }

                List<List<String>> featureFile = new ArrayList<>();
                List<String> paragraphFeatureFile = new ArrayList<>();
                String line_ = null;
                boolean end = false;
                try (BufferedReader bis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(theRawFile), UTF_8))) {
                    while ((line_ = bis.readLine()) != null) {
                        if (StringUtils.isNotBlank(line_)) {
                            if (end) {
                                featureFile.add(paragraphFeatureFile);
                                paragraphFeatureFile = new ArrayList<>();
                                end = false;
                            }
                            paragraphFeatureFile.add(line_);
                        } else {
                            end = true;
                        }
                    }
                }


                if (isNotEmpty(paragraphFeatureFile)) {
                    featureFile.add(paragraphFeatureFile);
                }

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

                List<List<String>> featureFileAligned = new ArrayList<>();
                List<List<Pair<String, String>>> xmlFileAligned = new ArrayList<>();

                boolean skipFile = false;

                int previousIdx = 0;
                outer:
                for (int i = 0; i < featureFile.size(); i++) {
                    final List<String> featureParagraph = featureFile.get(i);
                    String paragraphBeginning = IntStream.range(0, Math.min(5, featureParagraph.size())).mapToObj(i_ -> featureParagraph.get(i_).split(" ")[0]).collect(Collectors.joining(" "));

                    for (int j = previousIdx; j < xmlFile.size(); j++) {
                        final List<Pair<String, String>> xmlParagraph = xmlFile.get(j);
                        String paragraphBeginningXml = IntStream.range(0, Math.min(5, xmlParagraph.size())).mapToObj(j_ -> xmlParagraph.get(j_).getLeft()).collect(Collectors.joining(" "));
                        if (StringUtils.equals(paragraphBeginning, paragraphBeginningXml)) {
                            featureFileAligned.add(featureParagraph);
                            xmlFileAligned.add(xmlParagraph);
                            previousIdx = j + 1;
                            continue outer;
                        } else {
                            LOGGER.warn("Paragraphs " + paragraphBeginning + " not found in the xml " + paragraphBeginningXml);
                        }
                    }
                    LOGGER.error("The feature file (" + theRawFile.getName() + ") and the xml file (" + theFile.getName() + ") have different number of paragraphs and cannot be matched back. Skipping it.");
                    LOGGER.error("Paragraph beginning: " + paragraphBeginning);
                    skipFile = true;
                    break;
                }

                if (xmlFile.size() != featureFile.size()) {
                    LOGGER.info("Initial paragraphs: XML: " + xmlFile.size() + ", Features: " + featureFile.size()
                        + ". Output: " + xmlFileAligned.size());
                }

                if (skipFile)
                    continue;

                // At this point the paragraphs are aligned:
                // 1. I need just one index counter
                // 2. I need to find mismatches between tokens
                for (int i = 0; i < featureFileAligned.size(); i++) {
                    paragraphFeatureFile = featureFileAligned.get(i);
                    paragraphXmlFile = xmlFileAligned.get(i);
                    int featureFileIndex = 0;
                    long entityLabels = 0;
                    
                    List<String> tmpLayoutTokens = paragraphXmlFile.stream().map(Pair::getLeft).collect(Collectors.toList());
                    List<OffsetPosition> sentencesAsOffsets = this.segmenter.getSentencesAsOffsets(tmpLayoutTokens);

                    outer:
                    for (String line : paragraphFeatureFile) {
                        int secondFeatureTokenIndex = line.indexOf('\t');
                        if (secondFeatureTokenIndex == -1) {
                            secondFeatureTokenIndex = line.indexOf(' ');
                        }
                        String token = null;
                        if (secondFeatureTokenIndex != -1) {
                            token = line.substring(0, secondFeatureTokenIndex).trim();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been generated by a recent version of grobid
                            token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        }
                        // we get the label in the labelled data file for the same token
                        for (int labeledIndex = featureFileIndex; labeledIndex < paragraphXmlFile.size(); labeledIndex++) {
                            String localToken = paragraphXmlFile.get(labeledIndex).getLeft();
                            String tag = paragraphXmlFile.get(labeledIndex).getRight();

                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been generated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                            if (localToken.equals(token)) {
                                line = line.replace("\t", " ").replace("  ", " ");
                                output.append(line).append(" ").append(tag).append("\n");
                                if (!tag.equals(TaggingLabels.OTHER_LABEL)) {
                                    entityLabels++;
                                }
                                featureFileIndex = labeledIndex + 1;
                                labeledIndex = featureFileIndex + 10;
                                break;
                            }

                            if (labeledIndex - featureFileIndex > 5) {
                                if (isNotBlank(line)) {
                                    LOGGER.info("Out of sync. Moving to the next paragraph. Faulty paragraph: \n" +
                                        "\t- XML: " + paragraphXmlFile.stream().map(Pair::getLeft).collect(Collectors.joining(" ")) + "\n" +
                                        "\t- fea: " + paragraphFeatureFile.stream().map(s -> s.split(" ")[0]).collect(Collectors.joining(" ")));
//                                    entityLabels = 0;
//                                    output = new StringBuilder();
                                    continue outer;
                                }
                                break;
                            }
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

        Trainer trainer = new SuperconductorsTrainer();

        AbstractTrainer.runTraining(trainer);
    }
}