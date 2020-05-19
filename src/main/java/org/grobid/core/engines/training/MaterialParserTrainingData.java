package org.grobid.core.engines.training;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Superconductor;
import org.grobid.core.document.Document;
import org.grobid.core.engines.GrobidPDFEngine;
import org.grobid.core.engines.MaterialParser;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MAGNETISATION_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;

public class MaterialParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialParserTrainingData.class);

    private MaterialParser materialParser;
//    private Map<TrainingOutputFormat, SuperconductorsOutputFormattter> trainingOutputFormatters = new HashMap<>();


    public MaterialParserTrainingData(ChemDataExtractorClient chemspotClient) {
//        this(SuperconductorsParser.getInstance(chemspotClient), QuantityParser.getInstance(true));
    }

    public MaterialParserTrainingData(MaterialParser parser) {
        materialParser = parser;
//        trainingOutputFormatters.put(TrainingOutputFormat.TSV, new SuperconductorsTrainingTSVFormatter());
//        trainingOutputFormatters.put(TrainingOutputFormat.XML, new SuperconductorsTrainingXMLFormatter());
    }

    private void writeOutput(File file,
                             String outputDirectory,
                             String labelledText,
                             String features,
                             String plainText,
                             String outputFormat) {

        //Write the output for the labeled text
        String fileLabelledPath = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".material." + StringUtils.lowerCase(outputFormat));
        try {
            FileUtils.writeStringToFile(new File(fileLabelledPath), labelledText, UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + fileLabelledPath);
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

        GrobidPDFEngine.processDocument(document, preprocessedLayoutToken -> {

            // Re-tokenise now
            final List<LayoutToken> normalisedLayoutTokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(preprocessedLayoutToken);

            // Trying to fix the eventual offset mismatches by rewrite offsets
            IntStream
                .range(1, normalisedLayoutTokens.size())
                .forEach(i -> {
                    int expectedFollowingOffset = normalisedLayoutTokens.get(i - 1).getOffset()
                        + StringUtils.length(normalisedLayoutTokens.get(i - 1).getText());

                    if (expectedFollowingOffset != normalisedLayoutTokens.get(i).getOffset()) {
                        throw new RuntimeException("Crossvalidating offset. Error at element " + i + " offset: " + normalisedLayoutTokens.get(i).getOffset() + " but should be " + expectedFollowingOffset);
//                        normalisedLayoutTokens.get(i).setOffset(expectedFollowingOffset);
                    }
                });

            String text = LayoutTokensUtil.toText(normalisedLayoutTokens);

            textAggregation.append(text);
            if (!StringUtils.endsWith(text, " ")) {
                textAggregation.append(" ");
            }

            textAggregation.append("\n");

            Pair<String, List<Material>> stringListPair = materialParser.generateTrainingData(normalisedLayoutTokens);

        });

//        String labelledTextOutput = trainingOutputFormatters.get(outputFormat).format(labeledTextList, id);

//        writeOutput(file, outputDirectory, labelledTextOutput, features.toString(), textAggregation.toString(), outputFormat.toString());
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

}
