package org.grobid.core.engines.training;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Superconductor;
import org.grobid.core.document.Document;
import org.grobid.core.engines.QuantityParser;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.sax.TextChunkSaxHandler;
import org.grobid.core.utilities.ChemspotClient;
import org.grobid.core.utilities.MeasurementOperations;
import org.grobid.core.utilities.MeasurementUtils;
import org.grobid.core.utilities.UnitUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

public class SuperconductorsParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParserTrainingData.class);

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private Map<TrainingOutputFormat, SuperconductorsOutputFormattter> superconductorsTrainingXMLFormatter = new HashMap<>();
    private MeasurementOperations measurementOperations;


    public SuperconductorsParserTrainingData(ChemspotClient chemspotClient) {
        this(SuperconductorsParser.getInstance(chemspotClient), QuantityParser.getInstance(true));
    }

    public SuperconductorsParserTrainingData(SuperconductorsParser parser, QuantityParser quantityParser) {
        superconductorsParser = parser;
        superconductorsTrainingXMLFormatter.put(TrainingOutputFormat.TSV, new SuperconductorsTrainingTSVFormatter());
        superconductorsTrainingXMLFormatter.put(TrainingOutputFormat.XML, new SuperconductorsTrainingXMLFormatter());
        this.quantityParser = quantityParser;
        this.measurementOperations = new MeasurementOperations();
    }

    private void writeOutput(File file,
                             String outputDirectory,
                             String labelledText,
                             String features,
                             String plainText,
                             String outputFormat) {

        //Write the output for the superconductors features
        String featureFileSuperconductors = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".superconductors.features.txt");
        try {
            FileUtils.writeStringToFile(new File(featureFileSuperconductors), features, UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + featureFileSuperconductors);
        }

        //Write the output for the labeled text
        String outputFileQuantity = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".superconductors." + StringUtils.lowerCase(outputFormat));
        try {
            FileUtils.writeStringToFile(new File(outputFileQuantity), labelledText, UTF_8);
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

    private void createTrainingPDF(File file, String outputDirectory, TrainingOutputFormat outputFormat, int id) {
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

        StringBuilder textAggregation = new StringBuilder();
        StringBuilder features = new StringBuilder();
        List<Pair<List<Superconductor>, String>> labeledTextList = new ArrayList<>();

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();
            handler.addFilteredTag("ref");

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(new InputSource(new StringReader(teiXML)), handler);

            List<String> chunks = handler.getChunks();

            for (String text : chunks) {
                textAggregation.append(text);
                if (!StringUtils.endsWith(text, " ")) {
                    textAggregation.append(" ");
                }
                textAggregation.append("\n");

                Pair<String, List<Superconductor>> stringListPair = superconductorsParser.generateTrainingData(text);
                List<Measurement> measurements = quantityParser.process(text);
                List<Measurement> filteredMeasurements = MeasurementUtils.filterMeasurements(measurements,
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
                    superconductor.setSource(Superconductor.SOURCE_QUANTITIES);

                    String type = measurementData.getRight();
                    if (StringUtils.equals("temperature", type)) {
                        type = SUPERCONDUCTORS_TC_VALUE_LABEL;
                    } else if (StringUtils.equals("magnetic field strength", type)) {
                        type = SUPERCONDUCTORS_MAGNETISATION_LABEL;
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

//                sortedEntities = sortedEntities.stream().distinct().collect(Collectors.toList());

                List<Superconductor> toBeRemoved = new ArrayList<>();

                Superconductor previous = null;
                boolean first = true;
                for (Superconductor current : sortedEntities) {

                    if (first) {
                        first = false;
                        previous = current;
                    } else {
                        if (current.getOffsetEnd() < previous.getOffsetEnd() || previous.getOffsetEnd() > current.getOffsetStart()) {
                            System.out.println("Overlapping. " + current.getName() + " <" + current.getType() + "> with " + previous.getName() + " <" + previous.getType() + ">");

                            if (current.getSource().equals(Superconductor.SOURCE_QUANTITIES)) {
                                toBeRemoved.add(previous);
                            } else if (previous.getSource().equals(Superconductor.SOURCE_QUANTITIES)) {
                                toBeRemoved.add(previous);
                            } else {
                                toBeRemoved.add(previous);
                            }
                        }
                        previous = current;
                    }
                }

                sortedEntities.removeAll(toBeRemoved);

                Pair<List<Superconductor>, String> labeleledText = new ImmutablePair<>(sortedEntities, text);
                labeledTextList.add(labeleledText);
            }

        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because input XML file can not be parsed: ", e);
        }

        String labelledTextOutput = superconductorsTrainingXMLFormatter.get(outputFormat).format(labeledTextList, id);

        writeOutput(file, outputDirectory, labelledTextOutput, features.toString(), textAggregation.toString(), outputFormat.toString());
    }

    /**
     * Create training data for a list of pdf/text/xml-tei files
     */
    @SuppressWarnings({"UnusedParameters"})
    public int createTrainingBatch(String inputDirectory,
                                   String outputDirectory,
                                   TrainingOutputFormat outputFormat) {
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
                    file -> file.getName().endsWith(".pdf") || file.getName().endsWith(".PDF"))
                    .collect(Collectors.toList());

            LOGGER.info(refFiles.size() + " files to be processed.");

            int n = 0;
            for (final File file : refFiles) {
                try {
                    if (!file.exists()) {
                        throw new GrobidException("Cannot create training data because input file can not be accessed: " + file.getAbsolutePath());
                    }
                    createTrainingPDF(file, outputDirectory, outputFormat, n);
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
    public Triple<Integer, Integer, String> calculateQuantityExtremitiesOffsetsAndType(Measurement measurements) {
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
                    int unitEnd = unitStart + quantity.getRawUnit().getRawName().length();

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
                    int quantityRangeEnd = quantityRangeStart + quantityRange.getRawValue().length();


                    // adjust base

                    if (quantityBase.getRawUnit() != null) {
                        List<LayoutToken> quantityBaseTokens = quantityBase.getRawUnit().getLayoutTokens();
                        int unitBaseStart = quantityBaseTokens.get(0).getOffset();
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
                    int quantityTmpEnd = quantityTmpStart + quantityTmp.getRawValue().length();

                    if (quantityTmp.getRawUnit() != null) {
                        List<LayoutToken> quantityTmpTokens = quantityTmp.getRawUnit().getLayoutTokens();
                        int unitTmpStart = quantityTmpTokens.get(0).getOffset();
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
                    int quantityIntervalEnd = quantityIntervalStart + quantityMost.getRawValue().length();

                    if (quantityLeast.getRawUnit() != null) {
                        List<LayoutToken> quantityLeastTokens = quantityLeast.getRawUnit().getLayoutTokens();
                        int unitLeastStart = quantityLeastTokens.get(0).getOffset();
                        int unitLeastEnd = unitLeastStart + quantityLeast.getRawUnit().getRawName().length();

                        if (unitLeastStart < quantityIntervalStart) {
                            quantityIntervalStart = unitLeastStart;
                        } else if (unitLeastEnd > quantityIntervalEnd) {
                            quantityIntervalEnd = unitLeastEnd;
                        }
                    }

                    if (quantityMost.getRawUnit() != null) {
                        List<LayoutToken> quantityMostTokens = quantityLeast.getRawUnit().getLayoutTokens();
                        int unitMostStart = quantityMostTokens.get(0).getOffset();
                        int unitMostEnd = unitMostStart + quantityMost.getRawUnit().getRawName().length();

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
                    int quantityTmpEnd = quantityTmpStart + quantityTmp.getRawValue().length();

                    if (quantityTmp.getRawUnit() != null) {
                        List<LayoutToken> quantityTmpTokens = quantityTmp.getRawUnit().getLayoutTokens();
                        int unitTmpStart = quantityTmpTokens.get(0).getOffset();
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

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
