package org.grobid.core.engines.training;

import com.google.common.collect.Iterables;
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
import org.grobid.core.engines.GrobidPDFEngine;
import org.grobid.core.engines.QuantityParser;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.grobid.core.utilities.QuantityOperations.getContainingOffset;
import static org.grobid.core.utilities.QuantityOperations.getOffset;

public class SuperconductorsParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParserTrainingData.class);

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private Map<TrainingOutputFormat, SuperconductorsOutputFormattter> trainingOutputFormatters = new HashMap<>();


    public SuperconductorsParserTrainingData(ChemspotClient chemspotClient) {
        this(SuperconductorsParser.getInstance(chemspotClient), QuantityParser.getInstance(true));
    }

    public SuperconductorsParserTrainingData(SuperconductorsParser parser, QuantityParser quantityParser) {
        superconductorsParser = parser;
        trainingOutputFormatters.put(TrainingOutputFormat.TSV, new SuperconductorsTrainingTSVFormatter());
        trainingOutputFormatters.put(TrainingOutputFormat.XML, new SuperconductorsTrainingXMLFormatter());
        this.quantityParser = quantityParser;
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
        Document document = null;
        try {
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .build();
            document = GrobidFactory.getInstance().createEngine().fullTextToTEIDoc(file, config);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because GROBID Fulltext model failed on the PDF: " + file.getPath(), e);
        }
        if (document == null) {
            return;
        }

        StringBuilder textAggregation = new StringBuilder();
        StringBuilder features = new StringBuilder();
        List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList = new ArrayList<>();

        GrobidPDFEngine.processDocument(document, layoutTokens -> {

            List<LayoutToken> normalisedLayoutTokens = LayoutTokensUtil.dehyphenize(layoutTokens)
                    .stream()
                    .map(m -> {
                        m.setText(StringUtils.replace(m.getText(), "\r\n", " "));
                        m.setText(StringUtils.replace(m.getText(), "\n", " "));
                        return m;
                    })
                    .collect(Collectors.toList());


            String text = LayoutTokensUtil.toText(normalisedLayoutTokens);

            textAggregation.append(text);
            if (!StringUtils.endsWith(text, " ")) {
                textAggregation.append(" ");
            }

            textAggregation.append("\n");

            Pair<String, List<Superconductor>> stringListPair = superconductorsParser.generateTrainingData(normalisedLayoutTokens);
            List<Measurement> measurements = quantityParser.process(normalisedLayoutTokens);
            List<Measurement> filteredMeasurements = MeasurementUtils.filterMeasurements(measurements,
                    Arrays.asList(
                            UnitUtilities.Unit_Type.TEMPERATURE,
                            UnitUtilities.Unit_Type.MAGNETIC_FIELD_STRENGTH,
                            UnitUtilities.Unit_Type.PRESSURE
                    )
            );

            features.append(stringListPair.getLeft());
            features.append("\n");
            features.append("\n");

            List<Superconductor> measurementsAdapted = filteredMeasurements
                    .stream()
                    .map(m -> {
                        OffsetPosition offset = QuantityOperations.getContainingOffset(QuantityOperations.getOffset(m));

                        Superconductor superconductor = new Superconductor();
                        superconductor.setOffsetStart(offset.start);
                        superconductor.setOffsetEnd(offset.end);
                        superconductor.setSource(Superconductor.SOURCE_QUANTITIES);

                        String type = lowerCase(QuantityOperations.getType(m).getName());
                        if (StringUtils.equals("temperature", type)) {
                            type = SUPERCONDUCTORS_TC_VALUE_LABEL;
                        } else if (StringUtils.equals("magnetic field strength", type)) {
                            type = SUPERCONDUCTORS_MAGNETISATION_LABEL;
                        }
                        superconductor.setType(type);

                        List<LayoutToken> quantityLayoutTokens = normalisedLayoutTokens
                                .stream()
                                .filter(l -> l.getOffset() >= superconductor.getOffsetStart()
                                        && l.getOffset() < superconductor.getOffsetEnd())
                                .collect(Collectors.toList());

                        String name = LayoutTokensUtil.toText(quantityLayoutTokens);
                        superconductor.setName(name);

                        return superconductor;
                    })
                    .filter(s -> StringUtils.isNotEmpty(s.getType()))
                    .collect(Collectors.toList());

            List<Superconductor> superconductorList = stringListPair.getRight();
            superconductorList.addAll(measurementsAdapted);
            List<Superconductor> sortedEntities = pruneOverlappingAnnotations(superconductorList);

            Pair<List<Superconductor>, List<LayoutToken>> labeleledText = Pair.of(sortedEntities, normalisedLayoutTokens);
            labeledTextList.add(labeleledText);

        });

        String labelledTextOutput = trainingOutputFormatters.get(outputFormat).format(labeledTextList, id);

        writeOutput(file, outputDirectory, labelledTextOutput, features.toString(), textAggregation.toString(), outputFormat.toString());
    }


    /**
     * Remove overlapping annotations - grobid-quantities has lower priority in this context
     **/
    private List<Superconductor> pruneOverlappingAnnotations(List<Superconductor> superconductorList) {
        //Sorting by offsets
        List<Superconductor> sortedEntities = superconductorList
                .stream()
                .sorted(Comparator.comparingInt(Superconductor::getOffsetStart))
                .collect(Collectors.toList());

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
        return sortedEntities;
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

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
