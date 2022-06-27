package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.data.document.Span;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesTEIStaxHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAG_TYPES;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;


public class SuperconductorsTrainer extends AbstractTrainerNew {
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
        int positiveExamples = 0;
        int negativeExamples = 0;
        Writer trainingOutputWriter = null;
        Writer evaluationOutputWriter = null;
        Writer negativeExamplesTempWriter = null;
        Path negativeExamplesTempPath = null;

        try {

            Path adaptedCorpusDir = Paths.get(corpusDir.getAbsolutePath());
            LOGGER.info("sourcePathLabel: " + adaptedCorpusDir);
            if (trainingOutputPath != null)
                LOGGER.info("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                LOGGER.info("outputPath for evaluation data: " + evalOutputPath);

            // the file for writing the training data
            OutputStream os2 = null;

            if (trainingOutputPath != null) {
                os2 = newOutputStream(trainingOutputPath.toPath());
                trainingOutputWriter = new OutputStreamWriter(os2, UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;

            if (evalOutputPath != null) {
                os3 = newOutputStream(evalOutputPath.toPath());
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
            negativeExamplesTempPath = Files.createTempFile(Paths.get(GrobidProperties.getTempPath().getAbsolutePath()), "negative_sampling", "temp");
            negativeExamplesTempWriter = new OutputStreamWriter(newOutputStream(negativeExamplesTempPath), UTF_8);

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
                List<String> sentencesFeatureFile = new ArrayList<>();
                String line_ = null;
                boolean end = false;
                try (BufferedReader bis = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(theRawFile.toPath()), UTF_8))) {
                    while ((line_ = bis.readLine()) != null) {
                        if (StringUtils.isNotBlank(line_)) {
                            if (end) {
                                featureFile.add(sentencesFeatureFile);
                                sentencesFeatureFile = new ArrayList<>();
                                end = false;
                            }
                            sentencesFeatureFile.add(line_);
                        } else {
                            end = true;
                        }
                    }
                }


                if (isNotEmpty(sentencesFeatureFile)) {
                    featureFile.add(sentencesFeatureFile);
                }

                List<List<Pair<String, String>>> xmlFile = new ArrayList<>();
                List<Pair<String, String>> sentencesXmlFile = new ArrayList<>();
                for (int idx = 0; idx < labeled.size(); idx++) {
                    String tag = labeled.get(idx).getRight();
                    if (StringUtils.isEmpty(tag)) {
                        xmlFile.add(sentencesXmlFile);
                        sentencesXmlFile = new ArrayList<>();
                    } else {
                        sentencesXmlFile.add(labeled.get(idx));
                    }
                }
                if (isNotEmpty(sentencesXmlFile)) {
                    xmlFile.add(sentencesXmlFile);
                }

                List<List<String>> featureFileAligned = new ArrayList<>();
                List<List<Pair<String, String>>> xmlFileAligned = new ArrayList<>();

                boolean skipFile = false;

                int previousIdx = 0;
                outer:
                for (int i = 0; i < featureFile.size(); i++) {
                    final List<String> featureParagraph = featureFile.get(i);
                    String sentenceBeginning = IntStream.range(0, Math.min(5, featureParagraph.size())).mapToObj(i_ -> featureParagraph.get(i_).split(" ")[0]).collect(Collectors.joining(" "));

                    for (int j = previousIdx; j < xmlFile.size(); j++) {
                        final List<Pair<String, String>> xmlParagraph = xmlFile.get(j);
                        String sentenceBeginningXml = IntStream.range(0, Math.min(5, xmlParagraph.size())).mapToObj(j_ -> xmlParagraph.get(j_).getLeft()).collect(Collectors.joining(" "));
                        if (StringUtils.equals(sentenceBeginning, sentenceBeginningXml)) {
                            featureFileAligned.add(featureParagraph);
                            xmlFileAligned.add(xmlParagraph);
                            previousIdx = j + 1;
                            continue outer;
                        } else {
                            LOGGER.warn("Sentences " + sentenceBeginning + " not found in the xml " + sentenceBeginningXml);
                        }
                    }
                    LOGGER.error("The feature file (" + theRawFile.getName() + ") and the xml file (" + theFile.getName() + ") have different number of paragraphs and cannot be matched back. Skipping it.");
                    LOGGER.error("Sentence beginning: " + sentenceBeginning);
                    skipFile = true;
                    break;
                }

                if (xmlFile.size() != featureFile.size()) {
                    LOGGER.info("Initial sentences: XML: " + xmlFile.size() + ", Features: " + featureFile.size()
                        + ". Output: " + xmlFileAligned.size());
                }

                if (skipFile)
                    continue;

                // At this point the paragraphs are aligned:
                // 1. I need just one index counter
                // 2. I need to find mismatches between tokens
                for (int i = 0; i < featureFileAligned.size(); i++) {
                    sentencesFeatureFile = featureFileAligned.get(i);
                    sentencesXmlFile = xmlFileAligned.get(i);
                    int featureFileIndex = 0;
                    long entityLabels = 0;
                    outer:
                    for (String line : sentencesFeatureFile) {
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
                        for (int labeledIndex = featureFileIndex; labeledIndex < sentencesXmlFile.size(); labeledIndex++) {
                            String localToken = sentencesXmlFile.get(labeledIndex).getLeft();
                            String tag = sentencesXmlFile.get(labeledIndex).getRight();

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
                                        "\t- XML: " + sentencesXmlFile.stream().map(Pair::getLeft).collect(Collectors.joining(" ")) + "\n" +
                                        "\t- fea: " + sentencesFeatureFile.stream().map(s -> s.split(" ")[0]).collect(Collectors.joining(" ")));
//                                    entityLabels = 0;
//                                    output = new StringBuilder();
                                    continue outer;
                                }
                                break;
                            }
                        }
                    }

                    if (isNotBlank(output.toString())) {
                        // Positive sampling goes directly in the output, negative sampling goes to a temporary file
                        // positive sampling
                        output.append("\n");
                        totalExamples++;
                        if (entityLabels > 0) {
                            positiveExamples++;
                            writer.write(output + "\n");
                            writer.flush();
                            writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                        } else {
                            // random/active negative sampling
                            negativeExamples++;
                            negativeExamplesTempWriter.write(output + "\n");
                            negativeExamplesTempWriter.flush();
                        }
                    }
                    output = new StringBuilder();
                }

                writer.write(output + "\n");
                writer.write("\n");
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(evaluationOutputWriter, trainingOutputWriter, negativeExamplesTempWriter);
        }

        // We apply sampling
        assignNegativeExamplesRandomly(negativeExamplesTempPath, positiveExamples * 0.15, negativeExamples, trainingOutputPath, evalOutputPath);


        return totalExamples;
    }


    /**
     * Using a set of negative examples, select those which contradicts a given recognition model.
     * Contradict means that the model predicts incorrectly a software mention and that this
     * particular negative example is particularly relevant to correct this model.
     * <p>
     * Given the max parameter, if the max is not reached, we fill the remaning with random samples.
     * <p>
     * Original: https://github.com/ourresearch/software-mentions/blob/master/src/main/java/org/grobid/trainer/SoftwareTrainer.java
     *
     * @author Patrice Lopez
     */
    public int selectNegativeExamples(File negativeCorpusFile, double max, File outputXMLFile) {
        int totalExamples = 0;
        Writer writer = null;
        try {
            System.out.println("Negative corpus path: " + negativeCorpusFile.getPath());
            System.out.println("selection corpus path: " + outputXMLFile.getPath());

            // the file for writing the training data
            writer = new OutputStreamWriter(new FileOutputStream(outputXMLFile), "UTF8");

            SuperconductorsParser parser = SuperconductorsParser.getInstance(null, null, null);

            if (!negativeCorpusFile.exists()) {
                System.out.println("The XML TEI negative corpus does not exist: " + negativeCorpusFile.getPath());
            } else {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                String tei = FileUtils.readFileToString(negativeCorpusFile, UTF_8);
                org.w3c.dom.Document document = builder.parse(new InputSource(new StringReader(tei)));

                int totalAdded = 0;

                // list of index of nodes to remove
                List<Integer> toAdd = new ArrayList<>();

                NodeList pList = document.getElementsByTagName("p");

                for (int i = 0; i < pList.getLength(); i++) {
                    Element paragraphElement = (Element) pList.item(i);
                    String text = ""; //XMLUtilities.getText(paragraphElement);
                    if (text == null || text.trim().length() == 0) {
                        continue;
                    }

                    // run the mention recognizer and check if we have annotations
                    List<Span> entities = parser.processSingle(text);
                    if (entities != null && entities.size() > 0) {
                        toAdd.add(i);
                        totalAdded++;
                    }
                }

                System.out.println("Number of examples based on active sampling: " + totalAdded);

                List<Integer> toRemove = new ArrayList<Integer>();
                for (int i = 0; i < pList.getLength(); i++) {
                    Element paragraphElement = (Element) pList.item(i);
                    if (totalAdded < max) {
                        toAdd.add(i);
                        totalAdded++;
                    } else if (!toAdd.contains(i)) {
                        toRemove.add(new Integer(i));
                    }
                }

                totalExamples = totalAdded;

                for (int j = toRemove.size() - 1; j > 0; j--) {
                    // remove the specific node
                    Node element = pList.item(toRemove.get(j));
                    if (element == null) {
                        System.out.println("Warning: null element at " + toRemove.get(j));
                        continue;
                    }
                    if (element.getParentNode() != null)
                        element.getParentNode().removeChild(element);
                }

//                writer.write(serialize(document, null));
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while selecting negative examples.", e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return totalExamples;
    }


    /**
     * Select a given number of negative examples among a given (very large) list, in a random
     * manner.
     * <p>
     * Original: https://github.com/ourresearch/software-mentions/blob/master/src/main/java/org/grobid/trainer/SoftwareTrainer.java
     *
     * @author Patrice Lopez
     */
    public int assignNegativeExamplesRandomly(Path negativeExamplesTempFile, double numberOfNegativeExamplesToExtract,
                                              int totalNumberOfNegativeExamplesAvailable, File trainingOutputPath,
                                              File evalOutputPath) {
        int totalExamples = 0;
        Writer writer = null;
        Writer trainingOutputWriter = null;
        Writer evaluationOutputWriter = null;

        try {
            // Reopen the eval / training files
            OutputStream os2 = null;

            if (trainingOutputPath != null) {
                os2 = newOutputStream(trainingOutputPath.toPath());
                trainingOutputWriter = new OutputStreamWriter(os2, UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;

            if (evalOutputPath != null) {
                os3 = newOutputStream(evalOutputPath.toPath());
                evaluationOutputWriter = new OutputStreamWriter(os3, UTF_8);
            }

            //Compute the random examples
//            List<Integer> listNegativeExampleIndex = IntStream.rangeClosed(0, totalNumberOfNegativeExamplesAvailable)
//                .boxed().collect(Collectors.toList());
            List<Integer> negativeExamplesList = new ArrayList<>();
            for (int i =0; i< numberOfNegativeExamplesToExtract; i++){
                int exampleId = (int) (Math.random() * totalNumberOfNegativeExamplesAvailable);
                if (!IterableUtils.contains(negativeExamplesList, exampleId)){
                    negativeExamplesList.add(exampleId);
                }
            }

            // Add other examples if they are not enough
            if (negativeExamplesList.size() < numberOfNegativeExamplesToExtract) {
                for (int i =0; i< totalNumberOfNegativeExamplesAvailable || negativeExamplesList.size() < numberOfNegativeExamplesToExtract; i++){
                    if (!IterableUtils.contains(negativeExamplesList, i)){
                        negativeExamplesList.add(i);
                    }
                }
            }

            // Read the temporary files
            Stream<String> lines = Files.lines(negativeExamplesTempFile);




        } catch (Exception e) {
            throw new GrobidException("An exception occurred while selecting negative examples.", e);
        } finally {
            IOUtils.closeQuietly(writer);
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
     * Add the selected features to the model training for bio entities
     */
    public int createCRFPPDataSingle(File theFile,
                                     File trainingOutputPath) {

        int totalExamples = 0;
        Writer trainingOutputWriter = null;

        try {

            LOGGER.info("sourcePathLabel: " + theFile);
            if (trainingOutputPath != null)
                LOGGER.info("outputPath for training data: " + trainingOutputPath);

            // the file for writing the training data
            OutputStream os2 = null;

            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                trainingOutputWriter = new OutputStreamWriter(os2, UTF_8);
            }


            if (Files.notExists(Paths.get(theFile.getAbsolutePath()))) {
                return 0;
            }

            String name;

            Writer writer = dispatchExample(trainingOutputWriter, null, 1.0);
            StringBuilder output = new StringBuilder();
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
                break;
            }

            if (xmlFile.size() != featureFile.size()) {
                LOGGER.info("Initial paragraphs: XML: " + xmlFile.size() + ", Features: " + featureFile.size()
                    + ". Output: " + xmlFileAligned.size());
            }

            // At this point the paragraphs are aligned:
            // 1. I need just one index counter
            // 2. I need to find mismatches between tokens
            for (int i = 0; i < featureFileAligned.size(); i++) {
                paragraphFeatureFile = featureFileAligned.get(i);
                paragraphXmlFile = xmlFileAligned.get(i);
                int featureFileIndex = 0;
                long entityLabels = 0;
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
                    writer.write(output + "\n");
                    writer.flush();
                    writer = dispatchExample(trainingOutputWriter, null, 1.0);
                }
                output = new StringBuilder();
            }

            writer.write(output + "\n");
            writer.write("\n");

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(trainingOutputWriter);
        }
        return totalExamples;

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