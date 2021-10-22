package org.grobid.core.engines.training;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.DocumentBlock;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Span;
import org.grobid.core.document.Document;
import org.grobid.core.engines.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SuperconductorsParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParserTrainingData.class);

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private StructureIdentificationModuleClient structureIdentificationModuleClient;
    private Map<TrainingOutputFormat, SuperconductorsOutputFormattter> trainingOutputFormatters = new HashMap<>();


    public SuperconductorsParserTrainingData(ChemDataExtractorClient chemspotClient, StructureIdentificationModuleClient structureIdentificationModuleClient) {
        this(SuperconductorsParser.getInstance(chemspotClient, MaterialParser.getInstance(null, null), structureIdentificationModuleClient),
            QuantityParser.getInstance(true));
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
        GrobidAnalysisConfig config =
            new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                .build();

        Document document = null;
        try {
            document = GrobidFactory.getInstance().createEngine().fullTextToTEIDoc(file, config);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because GROBID Fulltext model failed on the PDF: " + file.getPath(), e);
        }
        if (document == null) {
            return;
        }

        StringBuilder textAggregation = new StringBuilder();
        StringBuilder features = new StringBuilder();
        List<DocumentBlock> labeledTextList = new ArrayList<>();

        GrobidPDFEngine.processDocument(document, config, documentBlock -> {

            // Re-tokenise now
            final List<LayoutToken> normalisedLayoutTokens = DeepAnalyzer.getInstance()
                .retokenizeLayoutTokens(documentBlock.getLayoutTokens());

            // Trying to fix the eventual offset mismatches by rewrite offsets
            IntStream
                .range(1, normalisedLayoutTokens.size())
                .forEach(i -> {
                    int expectedFollowingOffset = normalisedLayoutTokens.get(i - 1).getOffset()
                        + StringUtils.length(normalisedLayoutTokens.get(i - 1).getText());

                    if (expectedFollowingOffset != normalisedLayoutTokens.get(i).getOffset()) {
                        throw new RuntimeException("Cross-validating offset. Error at element " + i + " offset: "
                            + normalisedLayoutTokens.get(i).getOffset() + " but should be " + expectedFollowingOffset);
                    }
                });

            String text = LayoutTokensUtil.toText(normalisedLayoutTokens);

            textAggregation.append(text);
            if (!StringUtils.endsWith(text, " ")) {
                textAggregation.append(" ");
            }

            textAggregation.append("\n");

            Pair<String, List<Span>> stringListPair = superconductorsParser.generateTrainingData(normalisedLayoutTokens);
            List<Span> entityList = stringListPair.getRight();
            features.append(stringListPair.getLeft());
            features.append("\n");
            features.append("\n");

            List<Measurement> measurements = quantityParser.process(normalisedLayoutTokens);

            List<Measurement> temperatures = MeasurementUtils
                .filterMeasurementsByUnitType(measurements, UnitUtilities.Unit_Type.TEMPERATURE);
            List<Span> temperaturesAsSpan = temperatures.stream()
                .flatMap(p -> Stream.of(MeasurementUtils.toSpan(p, normalisedLayoutTokens, "tcValue")))
                .collect(Collectors.toList());

            entityList.addAll(temperaturesAsSpan);

            List<Measurement> pressures = MeasurementUtils
                .filterMeasurementsByUnitType(measurements, UnitUtilities.Unit_Type.PRESSURE);
            List<Span> pressuresAsSpan = pressures.stream()
                .flatMap(p -> Stream.of(MeasurementUtils.toSpan(p, normalisedLayoutTokens, "pressure")))
                .collect(Collectors.toList());
            entityList.addAll(pressuresAsSpan);

            List<Span> sortedEntities = ModuleEngine.pruneOverlappingAnnotations(entityList);

            DocumentBlock newDocumentBlock = new DocumentBlock(documentBlock);
            newDocumentBlock.setLayoutTokens(normalisedLayoutTokens);
            newDocumentBlock.setSpans(sortedEntities);

            labeledTextList.add(newDocumentBlock);

        });

        String labelledTextOutput = trainingOutputFormatters.get(outputFormat).format(labeledTextList, id);

        writeOutput(file, outputDirectory, labelledTextOutput, features.toString(), textAggregation.toString(), outputFormat.toString());
    }

    /**
     * Create training data for a list of pdf/text/xml-tei files
     */
    @SuppressWarnings({"UnusedParameters"})
    public int createTrainingBatch(String inputDirectory,
                                   String outputDirectory,
                                   TrainingOutputFormat outputFormat,
                                   boolean recursive) {
        try {
            Path inputDirectoryPath = Paths.get(inputDirectory);
            if (!Files.exists(inputDirectoryPath)) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            if (!Files.isDirectory(inputDirectoryPath)) {
                throw new GrobidException("The input path should be a directory.");
            }

            int maxDept = recursive ? Integer.MAX_VALUE : 1;

            List<File> refFiles = Files.walk(inputDirectoryPath, maxDept)
                .filter(path -> Files.isRegularFile(path)
                    && (StringUtils.endsWithIgnoreCase(path.getFileName().toString(), ".pdf")))
                .map(Path::toFile)
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
                    LOGGER.error("An error occurred while processing the following pdf: "
                        + file.getPath(), exp);
                }
                n++;
            }

            return refFiles.size();
        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
