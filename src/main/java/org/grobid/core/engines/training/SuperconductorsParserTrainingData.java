package org.grobid.core.engines.training;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Superconductor;
import org.grobid.core.document.Document;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.QuantityParser;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.sax.TextChunkSaxHandler;
import org.grobid.core.utilities.ChemspotClient;
import org.grobid.core.utilities.TeiUtils;
import org.grobid.core.utilities.UnitUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.engines.AggregatedProcessing.*;

public class SuperconductorsParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParserTrainingData.class);

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private SuperconductorsTrainingFormatter superconductorsTrainingFormatter;

    public SuperconductorsParserTrainingData(ChemspotClient chemspotClient) {
        this(SuperconductorsParser.getInstance(chemspotClient), QuantityParser.getInstance(true));
    }

    public SuperconductorsParserTrainingData(SuperconductorsParser parser, QuantityParser quantityParser) {
        superconductorsParser = parser;
        superconductorsTrainingFormatter = new SuperconductorsTrainingFormatter();
        this.quantityParser = quantityParser;
    }

    /**
     * Process the content of the specified input file and format the result as training data.
     * <p>
     * Input file can be
     * (i) xml (.xml or .tei extension) and it is assumed that we have a patent document,
     * (ii) PDF (.pdf) and it is assumed that we have a scientific article which will be processed by GROBID fulltext first,
     * (iii) some text (.txt extension)
     *
     * @param inputFile       input file
     * @param outputDirectory path to TEI with annotated training data
     * @param id              id
     */
    public void createTraining(String inputFile, String outputDirectory, int id) throws Exception {
        File file = new File(inputFile);
        if (!file.exists()) {
            throw new GrobidException("Cannot create training data because input file can not be accessed: " + inputFile);
        }

        if (inputFile.endsWith(".txt") || inputFile.endsWith(".TXT")) {
//            createTrainingText(file, outputDirectory, id);
            throw new UnsupportedOperationException("Not yet implemented.");
        } else if (inputFile.endsWith(".xml") || inputFile.endsWith(".XML") || inputFile.endsWith(".tei") || inputFile.endsWith(".TEI")) {
            throw new UnsupportedOperationException("Not yet implemented.");
//            createTrainingXML(file, outputDirectory, id);
        } else if (inputFile.endsWith(".pdf") || inputFile.endsWith(".PDF")) {
            createTrainingPDF(file, outputDirectory, id);
        }
    }

//    void createTrainingText(File file, String outputDirectory, int id) throws IOException {
//        String text = FileUtils.readFileToString(file, UTF_8);
//
//        Element quantityNode = teiElement("text");
//        Element quantifiedObjectNode = teiElement("text");
//
//        // for the moment we suppose we have english only...
//        quantityNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));
//
//        // we process the text paragraph by paragraph
//        String lines[] = text.split("\n");
//        StringBuilder paragraph = new StringBuilder();
//        List<Measurement> measurements = null;
//        for (int i = 0; i < lines.length; i++) {
//            String line = lines[i].trim();
//            if (line.length() != 0) {
//                paragraph.append(line).append("\n");
//            }
//            if (((line.length() == 0) || (i == lines.length - 1)) && (paragraph.length() > 0)) {
//
//                measurements = quantityParser.process(text);
//                quantityNode.appendChild(quantityTrainingFormatter.trainingExtraction(measurements, text));
//
//                quantifiedObjectNode.appendChild(substanceTrainingFormatter.trainingExtraction(measurements, text));
//
//                paragraph = new StringBuilder();
//            }
//        }
//        writeOutput(file, outputDirectory, id, quantityNode, text);
//    }

    private void writeOutput(File file,
                             String outputDirectory, int id,
                             Element quantityNode,
                             String features,
                             String plainText) {
        Element quantityDocumentRoot = TeiUtils.getTeiHeader(id);
        quantityDocumentRoot.appendChild(quantityNode);


        //Write the output for the superconductors features
        String featureFileSuperconductors = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".superconductors.features.txt");
        try {
            FileUtils.writeStringToFile(new File(featureFileSuperconductors), features, UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + featureFileSuperconductors);
        }

        //Write the output for superconductors tags XML
        String outputFileQuantity = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".superconductors.tei.xml");
        try {
            FileUtils.writeStringToFile(new File(outputFileQuantity), XmlBuilderUtils.toXml(quantityDocumentRoot), UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + outputFileQuantity);
        }

        //Write the output for plain text
        String outputFilePlainText = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".txt");
        try {
            FileUtils.writeStringToFile(new File(outputFilePlainText), plainText, UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + outputFilePlainText);
        }

    }

    /*private void createTrainingXML(File input, String outputDirectory, int id) {
        List<Measurement> measurements = null;

        Element quantityNode = teiElement("text");
        Element unitNode = teiElement("units");
        Element valueNode = teiElement("values");
        Element quantifiedObjectNode = teiElement("text");

        // for the moment we suppose we have english only...
        quantityNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(input, handler);

            List<String> chunks = handler.getChunks();
            StringBuilder sb = new StringBuilder();
            for (String text : chunks) {

                sb.append(text);
                measurements = quantityParser.process(text);

                if (measurements != null) {
                    System.out.println("\n");
                    for (Measurement measurement : measurements) {
                        System.out.println(measurement.toString());
                    }
                    System.out.println("\n");
                }

                quantityNode.appendChild(quantityTrainingFormatter.trainingExtraction(measurements, text));

                unitTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a -> a.getChildElements().size() != 0)
                        .forEach(unitNode::appendChild);

                valueTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a -> a.getChildElements().size() != 0)
                        .forEach(valueNode::appendChild);

                quantifiedObjectNode.appendChild(substanceTrainingFormatter.trainingExtraction(measurements, text));

            }
            Element quantityRoot = TeiUtils.getTeiHeader(id);
            quantityRoot.appendChild(quantityNode);

            writeOutput(input, outputDirectory, id, quantityNode, unitNode, valueNode, quantifiedObjectNode, sb.toString());
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because input XML file can not be parsed: " + input.getPath(), e);
        }
    }*/

    private void createTrainingPDF(File file, String outputDirectory, int id) {
        // first we apply GROBID fulltext model on the PDF to get the full text TEI
        Document teiDoc = null;
        try {
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .build();
            teiDoc = GrobidFactory.getInstance().createEngine().fullTextToTEIDoc(file, config);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because GROBID Fulltext model failed on the PDF: " + file.getPath(), e);
        }
        if (teiDoc == null) {
            return;
        }

        String teiXML = teiDoc.getTei();

        // we parse this TEI string similarly as for createTrainingXML

        Element textNode = teiElement("text");

        // for the moment we suppose we have english only...
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(new InputSource(new StringReader(teiXML)), handler);

            List<String> chunks = handler.getChunks();

            StringBuilder textAggregation = new StringBuilder();
            StringBuilder features = new StringBuilder();
            for (String text : chunks) {
                textAggregation.append(text);

                Pair<String, List<Superconductor>> stringListPair = superconductorsParser.generateTrainingData(text);
                List<Measurement> measurements = quantityParser.process(text);
                List<Measurement> filteredMeasurements = filterMeasurements(measurements,
                        Arrays.asList(UnitUtilities.Unit_Type.TEMPERATURE, UnitUtilities.Unit_Type.MAGNETIC_FIELD_STRENGTH, UnitUtilities.Unit_Type.PRESSURE)
                );

                features.append(stringListPair.getLeft());
                features.append("\n");
                features.append("\n");

                //With the hammer!
                List<Superconductor> measurementsAdapted = filteredMeasurements.stream().map(m -> {
                    Triple<Integer, Integer, String> measurementData = calculateQuantityExtremitiesOffsetsAndType(m);

                    Superconductor superconductor = new Superconductor();
                    superconductor.setOffsetStart(measurementData.getLeft());
                    superconductor.setOffsetEnd(measurementData.getMiddle());

                    String type = measurementData.getRight();
                    if (StringUtils.equals("temperature", type)) {
                        type = "tcValue";
                    } else if (StringUtils.equals("magnetic field strength", type)) {
                        type = "magnetisation";
                    }
                    superconductor.setType(type);
                    superconductor.setName(text.substring(superconductor.getOffsetStart(), superconductor.getOffsetEnd()));

                    return superconductor;
                }).filter(s -> StringUtils.isNotEmpty(s.getType())).collect(Collectors.toList());

                List<Superconductor> superconductorList = stringListPair.getRight();
                superconductorList.addAll(measurementsAdapted);

                List<Superconductor> sortedEntities = superconductorList.stream().sorted((o1, o2) -> {

                    if (o1.getOffsetStart() > o2.getOffsetStart()) {
                        return 1;
                    } else if (o1.getOffsetStart() < o2.getOffsetStart()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }).collect(Collectors.toList());

                textNode.appendChild(superconductorsTrainingFormatter.trainingExtraction(sortedEntities, text));
            }
            writeOutput(file, outputDirectory, id, textNode, features.toString(), textAggregation.toString());
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because input XML file can not be parsed: " + file.getPath(), e);
        }
    }

    /**
     * Create training data for a list of pdf/text/xml-tei files
     */
    @SuppressWarnings({"UnusedParameters"})
    public int createTrainingBatch(String inputDirectory,
                                   String outputDirectory) {
        try {
            File path = new File(inputDirectory);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            if (!path.isDirectory()) {
                throw new GrobidException("The input path should be a directory.");
            }

            List<File> refFiles = Arrays.stream(Objects.requireNonNull(path.listFiles())).filter(
                    file -> file.getName().endsWith(".pdf") || file.getName().endsWith(".PDF") ||
                            file.getName().endsWith(".txt") || file.getName().endsWith(".TXT") ||
                            file.getName().endsWith(".xml") || file.getName().endsWith(".tei") ||
                            file.getName().endsWith(".XML") || file.getName().endsWith(".TEI")
            ).collect(Collectors.toList());

            LOGGER.info(refFiles.size() + " files to be processed.");

            int n = 0;
            for (final File file : refFiles) {
                try {
                    createTraining(file.getAbsolutePath(), outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                            + file.getPath(), exp);
                }
                n++;
            }

            return refFiles.size();
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }


    /**
     * Returns the offsets and the type of the measurement (pressure, temperature)
     **/
    public static Triple<Integer, Integer, String> calculateQuantityExtremitiesOffsetsAndType(Measurement measurements) {
        Quantity quantity = null;
        Triple<Integer, Integer, String> extremities = null;
        switch (measurements.getType()) {
            case VALUE:
                quantity = measurements.getQuantityAtomic();
                List<LayoutToken> layoutTokens = quantity.getLayoutTokens();

                int quantityStart = layoutTokens.get(0).getOffset();
                int quantityEnd = quantityStart + quantity.getRawValue().length();
//                layoutTokens.get(layoutTokens.size() - 1).getOffset()
//                        + layoutTokens.get(layoutTokens.size() - 1).getText().length();

                if (quantity.getRawUnit() != null) {
                    List<LayoutToken> unitLayoutTokens = quantity.getRawUnit().getLayoutTokens();
                    int unitStart = unitLayoutTokens.get(0).getOffset();
                    int unitEnd = unitStart
//                            + unitLayoutTokens.get(unitLayoutTokens.size() - 1).getText().length();
                            + quantity.getRawUnit().getRawName().length();

                    if (unitStart < quantityStart) {
                        quantityStart = unitStart;
                    } else if (unitEnd > quantityEnd) {
                        quantityEnd = unitEnd;
                    }
                }


                extremities = ImmutableTriple.of(quantityStart, quantityEnd, lowerCase(quantity.getType().getName()));

                break;
            case INTERVAL_BASE_RANGE:
                if (measurements.getQuantityBase() != null && measurements.getQuantityRange() != null) {
                    Quantity quantityBase = measurements.getQuantityBase();
                    Quantity quantityRange = measurements.getQuantityRange();

                    String type = "";
                    if (quantityBase.getType() != null) {
                        type = lowerCase(quantityBase.getType().getName());
                    }

                    int quantityRangeBaseStart = quantityBase.getLayoutTokens().get(0).getOffset();
                    int quantityRangeRangeStart = quantityRange.getLayoutTokens().get(0).getOffset();
                    if (quantityRangeBaseStart > quantityRangeRangeStart) {
                        quantityBase = measurements.getQuantityRange();
                        quantityRange = measurements.getQuantityBase();
                    }

                    int quantityRangeStart = quantityBase.getLayoutTokens().get(0).getOffset();
//                    int quantityRangeEnd = quantityRange.getLayoutTokens().get(quantityRange.getLayoutTokens().size() - 1).getOffset() +
//                            quantityRange.getLayoutTokens().get(quantityRange.getLayoutTokens().size() - 1).getText().length();
                    int quantityRangeEnd = quantityRangeStart + quantityRange.getRawValue().length();


                    // adjust base

                    if (quantityBase.getRawUnit() != null) {
                        List<LayoutToken> quantityBaseTokens = quantityBase.getRawUnit().getLayoutTokens();
                        int unitBaseStart = quantityBaseTokens.get(0).getOffset();
//                        int unitBaseEnd = quantityBaseTokens.get(quantityBaseTokens.size() - 1).getOffset() +
//                                quantityBaseTokens.get(quantityBaseTokens.size() - 1).getText().length();
                        int unitBaseEnd = unitBaseStart + quantityBase.getRawUnit().getRawName().length();

                        if (unitBaseStart < quantityRangeStart) {
                            quantityRangeStart = unitBaseStart;
                        } else if (unitBaseEnd > quantityRangeEnd) {
                            quantityRangeEnd = unitBaseEnd;
                        }

                    }

                    //adjust range

                    if (quantityRange.getRawUnit() != null) {
                        List<LayoutToken> quantityRangeTokens = quantityRange.getRawUnit().getLayoutTokens();
                        int unitRangeStart = quantityRangeTokens.get(0).getOffset();
//                        int unitRangeEnd = quantityRangeTokens.get(quantityRangeTokens.size() - 1).getOffset() +
//                                quantityRangeTokens.get(quantityRangeTokens.size() - 1).getText().length();
                        int unitRangeEnd = quantityRangeStart + quantityRange.getRawUnit().getRawName().length();

                        if (unitRangeStart < quantityRangeStart) {
                            quantityRangeStart = unitRangeStart;
                        } else if (unitRangeEnd > quantityRangeEnd) {
                            quantityRangeEnd = unitRangeEnd;
                        }
                    }

                    extremities = ImmutableTriple.of(quantityRangeStart, quantityRangeEnd, type);

                } else {
                    Quantity quantityTmp;
                    if (measurements.getQuantityBase() == null) {
                        quantityTmp = measurements.getQuantityRange();

                    } else {
                        quantityTmp = measurements.getQuantityBase();
                    }

                    int quantityTmpStart = quantityTmp.getLayoutTokens().get(0).getOffset();
//                    int quantityTmpEnd = quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getOffset() +
//                            quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getText().length();
                    int quantityTmpEnd = quantityTmpStart + quantityTmp.getRawValue().length();

                    if (quantityTmp.getRawUnit() != null) {
                        List<LayoutToken> quantityTmpTokens = quantityTmp.getRawUnit().getLayoutTokens();
                        int unitTmpStart = quantityTmpTokens.get(0).getOffset();
//                        int unitTmpEnd = quantityTmpTokens.get(quantityTmpTokens.size() - 1).getOffset() +
//                                quantityTmpTokens.get(quantityTmpTokens.size() - 1).getText().length();

                        int unitTmpEnd = unitTmpStart + quantityTmp.getRawUnit().getRawName().length();

                        if (unitTmpStart < quantityTmpStart) {
                            quantityTmpStart = unitTmpStart;
                        } else if (unitTmpEnd > quantityTmpEnd) {
                            quantityTmpEnd = unitTmpEnd;
                        }

                    }

                    String type = "";
                    if (quantityTmp.getType() != null) {
                        type = lowerCase(quantityTmp.getType().getName());
                    }

                    extremities = ImmutableTriple.of(quantityTmpStart, quantityTmpEnd, type);
                }

                break;

            case INTERVAL_MIN_MAX:
                if (measurements.getQuantityLeast() != null && measurements.getQuantityMost() != null) {
                    Quantity quantityLeast = measurements.getQuantityLeast();
                    Quantity quantityMost = measurements.getQuantityMost();
                    String type = "";
                    if (quantityLeast.getType() != null) {
                        type = lowerCase(quantityLeast.getType().getName());
                    }

                    int quantityIntervalLeastStart = quantityLeast.getLayoutTokens().get(0).getOffset();
                    int quantityIntervalMostStart = quantityMost.getLayoutTokens().get(0).getOffset();
                    if (quantityIntervalLeastStart > quantityIntervalMostStart) {
                        quantityMost = measurements.getQuantityLeast();
                        quantityLeast = measurements.getQuantityMost();
                    }

                    int quantityIntervalStart = quantityLeast.getLayoutTokens().get(0).getOffset();
//                    int quantityIntervalEnd = quantityMost.getLayoutTokens().get(quantityMost.getLayoutTokens().size() - 1).getOffset() +
//                            quantityMost.getLayoutTokens().get(quantityMost.getLayoutTokens().size() - 1).getText().length();
                    int quantityIntervalEnd = quantityIntervalStart + quantityMost.getRawValue().length();

                    if (quantityLeast.getRawUnit() != null) {
                        List<LayoutToken> quantityLeastTokens = quantityLeast.getRawUnit().getLayoutTokens();
                        int unitLeastStart = quantityLeastTokens.get(0).getOffset();
                        int unitLeastEnd = quantityLeastTokens.get(quantityLeastTokens.size() - 1).getOffset() +
//                                quantityLeastTokens.get(quantityLeastTokens.size() - 1).getText().length();
                                +quantityLeast.getRawUnit().getRawName().length();

                        if (unitLeastStart < quantityIntervalStart) {
                            quantityIntervalStart = unitLeastStart;
                        } else if (unitLeastEnd > quantityIntervalEnd) {
                            quantityIntervalEnd = unitLeastEnd;
                        }
                    }

                    if (quantityMost.getRawUnit() != null) {
                        List<LayoutToken> quantityMostTokens = quantityLeast.getRawUnit().getLayoutTokens();
                        int unitMostStart = quantityMostTokens.get(0).getOffset();
                        int unitMostEnd = quantityMostTokens.get(quantityMostTokens.size() - 1).getOffset() +
//                                quantityMostTokens.get(quantityMostTokens.size() - 1).getText().length();
                                +quantityMost.getRawUnit().getRawName().length();

                        if (unitMostStart < quantityIntervalStart) {
                            quantityIntervalStart = unitMostStart;
                        } else if (unitMostEnd > quantityIntervalEnd) {
                            quantityIntervalEnd = unitMostEnd;
                        }

                    }

                    extremities = ImmutableTriple.of(quantityIntervalStart, quantityIntervalEnd, type);
                } else {
                    Quantity quantityTmp;

                    if (measurements.getQuantityLeast() == null) {
                        quantityTmp = measurements.getQuantityMost();
                    } else {
                        quantityTmp = measurements.getQuantityLeast();
                    }


                    int quantityTmpStart = quantityTmp.getLayoutTokens().get(0).getOffset();
//                    int quantityTmpEnd = quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getOffset() +
//                            quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getText().length();
                    int quantityTmpEnd = quantityTmpStart + quantityTmp.getRawValue().length();

                    if (quantityTmp.getRawUnit() != null) {
                        List<LayoutToken> quantityTmpTokens = quantityTmp.getRawUnit().getLayoutTokens();
                        int unitTmpStart = quantityTmpTokens.get(0).getOffset();
//                        int unitTmpEnd = quantityTmpTokens.get(quantityTmpTokens.size() - 1).getOffset() +
//                                quantityTmpTokens.get(quantityTmpTokens.size() - 1).getText().length();
                        int unitTmpEnd = unitTmpStart + quantityTmp.getRawUnit().getRawName().length();

                        if (unitTmpStart < quantityTmpStart) {
                            quantityTmpStart = unitTmpStart;
                        } else if (unitTmpEnd > quantityTmpEnd) {
                            quantityTmpEnd = unitTmpEnd;
                        }

                    }

                    String type = "";
                    if (quantityTmp.getType() != null) {
                        type = lowerCase(quantityTmp.getType().getName());
                    }

                    extremities = ImmutableTriple.of(quantityTmpStart, quantityTmpEnd, type);
                }
                break;

            case CONJUNCTION:
                List<Quantity> quantityList = measurements.getQuantityList();
                String type = "";
                if (isNotEmpty(quantityList)) {
                    if (quantityList.get(0).getType() != null) {
                        type = lowerCase(quantityList.get(0).getType().getName());
                    }
                }

                if (quantityList.size() > 1) {
                    extremities = ImmutableTriple.of(quantityList.get(0).getLayoutTokens().get(0).getOffset(),
                            quantityList.get(quantityList.size() - 1).getLayoutTokens().get(0).getOffset() +
                                    quantityList.get(quantityList.size() - 1).getLayoutTokens().get(0).getText().length(),
                            type);
                } else {
                    extremities = ImmutableTriple.of(quantityList.get(0).getLayoutTokens().get(0).getOffset(),
                            quantityList.get(0).getLayoutTokens().get(0).getOffset() +
                                    quantityList.get(0).getLayoutTokens().get(0).getText().length(),
                            type);
                }

                break;
        }
        return extremities;
    }
}
