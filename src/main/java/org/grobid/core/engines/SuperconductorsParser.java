package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.data.chemspot.Mention;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.ChemspotClient;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnitUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTOR_OTHER;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTOR_VALUE_NAME;

@Singleton
public class SuperconductorsParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParser.class);

    private static volatile SuperconductorsParser instance;
    private EngineParsers parsers;
    private ChemspotClient chemspotClient;

    public static SuperconductorsParser getInstance(ChemspotClient chemspotClient) {
        if (instance == null) {
            getNewInstance(chemspotClient);
        }
        return instance;
    }

    private static synchronized void getNewInstance(ChemspotClient chemspotClient) {
        instance = new SuperconductorsParser(chemspotClient);
    }

    @Inject
    public SuperconductorsParser(ChemspotClient chemspotClient) {
        super(SuperconductorsModels.SUPERCONDUCTORS);
        this.chemspotClient = chemspotClient;
    }

    public Pair<String, List<Superconductor>> generateTrainingData(List<LayoutToken> layoutTokens) {

        List<Superconductor> measurements = new ArrayList<>();
        String ress = null;

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        List<Mention> mentions = chemspotClient.processText(LayoutTokensUtil.toText(layoutTokens));
        List<Boolean> listChemspotEntities = synchroniseLayoutTokensWithMentions(tokens, mentions);

        try {
            // string representation of the feature matrix for CRF lib
            ress = addFeatures(tokens, listChemspotEntities);

            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantity parsing failed.", e);
            }
            measurements.addAll(extractResults(tokens, res));
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return Pair.of(ress, measurements);
    }

    public Pair<String, List<Superconductor>> generateTrainingData(String text) {
        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> layoutTokens = null;
        try {
            layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        return generateTrainingData(layoutTokens);

    }

    public Pair<List<Superconductor>, List<Measurement>> process(List<LayoutToken> layoutTokens) {

        List<Superconductor> entities = new ArrayList<>();

        // List<LayoutToken> for the selected segment
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        // list of textual tokens of the selected segment
        //List<String> texts = getTexts(tokenizationParts);

        List<Mention> mentions = chemspotClient.processText(LayoutTokensUtil.toText(layoutTokens));
        List<Boolean> listChemspotEntities = synchroniseLayoutTokensWithMentions(tokens, mentions);


        if (isEmpty(tokens))
            return new ImmutablePair<>(entities, new ArrayList<>());

        try {
            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(tokens, listChemspotEntities);

            if (StringUtils.isEmpty(ress))
                return new ImmutablePair<>(entities, new ArrayList<>());

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantity parsing failed.", e);
            }

            List<Superconductor> localEntities = extractResults(tokens, res);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        QuantityParser quantityParser = QuantityParser.getInstance();
        List<Measurement> process = quantityParser.process(layoutTokens);

        List<Measurement> temperatures = process.stream().filter(measurement -> {
            switch (measurement.getType()) {
                case VALUE:
                    return UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityAtomic().getType());
                case CONJUNCTION:
                    return measurement.getQuantityList()
                            .stream().anyMatch(quantity -> UnitUtilities.Unit_Type.TEMPERATURE.equals(quantity.getType()));
                case INTERVAL_BASE_RANGE:
                    return UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityBase().getType()) ||
                            UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityRange());

                case INTERVAL_MIN_MAX:
                    return (measurement.getQuantityMost() != null && UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityMost().getType())) ||
                            (measurement.getQuantityLeast() != null && UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityLeast()));

            }

            return false;
        }).collect(Collectors.toList());

        return new ImmutablePair<>(entities, temperatures);
    }

    private List<Boolean> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<Mention> mentions) {
        List<Boolean> isChemspotMention = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            for (LayoutToken token : tokens) {
                isChemspotMention.add(false);
            }

            return isChemspotMention;
        }

        int mentionId = 0;
        Mention mention = mentions.get(mentionId);

        for (LayoutToken token : tokens) {
            if (token.getOffset() < mention.getStart()) {
                isChemspotMention.add(false);
                continue;
            } else if (token.getOffset() >= mention.getStart()
                    && token.getOffset() + length(token.getText()) <= mention.getEnd()) {
                isChemspotMention.add(true);
            } else {
                if (mentionId == mentions.size() - 1) {
                    isChemspotMention.add(false);
                    break;
                }
                mentionId++;
                mention = mentions.get(mentionId);

                if (token.getOffset() < mention.getStart()) {
                    isChemspotMention.add(false);
                    continue;
                } else if (token.getOffset() >= mention.getStart()
                        && token.getOffset() + length(token.getText()) < mention.getEnd()) {
                    isChemspotMention.add(true);
                } else {
                    LOGGER.error("Something is really wrong here. ");
                }
            }
        }
        if (tokens.size() > isChemspotMention.size()) {

            for (int counter = isChemspotMention.size(); counter < tokens.size(); counter++) {
                isChemspotMention.add(false);
            }
        }

        return isChemspotMention;
    }

    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public Pair<List<Superconductor>, List<Measurement>> process(String text) {
        if (isBlank(text)) {
            return null;
        }

        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> tokens = null;
        try {
            tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        if ((tokens == null) || (tokens.size() == 0)) {
            return new ImmutablePair<>(new ArrayList<>(), new ArrayList<>());
        }
        return process(tokens);
    }

    public Pair<Pair<List<Superconductor>, List<Measurement>>, Document> extractFromPDF(File file) throws IOException {
        parsers = new EngineParsers();

        List<Superconductor> superconductorNamesList = new ArrayList<>();
        List<Measurement> temperaturesList = new ArrayList<>();

        Document doc = null;
        try {
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .build();
            DocumentSource documentSource =
                    DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            // In the following, we process the relevant textual content of the document

            // for refining the process based on structures, we need to filter
            // segment of interest (e.g. header, body, annex) and possibly apply
            // the corresponding model to further filter by structure types

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                org.apache.commons.lang3.tuple.Pair<String, List<LayoutToken>> headerStruct = parsers.getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
                List<LayoutToken> tokenizationHeader = headerStruct.getRight();
                String header = headerStruct.getLeft();
                String labeledResult = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    labeledResult = parsers.getHeaderParser().label(header);

                    BiblioItem resHeader = new BiblioItem();
                    //parsers.getHeaderParser().processingHeaderSection(false, doc, resHeader);
                    resHeader.generalResultMapping(doc, labeledResult, tokenizationHeader);

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        Pair<List<Superconductor>, List<Measurement>> result = process(titleTokens);
                        superconductorNamesList.addAll(result.getLeft());
                        temperaturesList.addAll(result.getRight());
                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        Pair<List<Superconductor>, List<Measurement>> result = process(abstractTokens);
                        superconductorNamesList.addAll(result.getLeft());
                        temperaturesList.addAll(result.getRight());
                    }

                    // keywords
                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                    if (keywordTokens != null) {
                        Pair<List<Superconductor>, List<Measurement>> result = process(keywordTokens);
                        superconductorNamesList.addAll(result.getLeft());
                        temperaturesList.addAll(result.getRight());
                    }
                }
            }

            // we can process all the body, in the future figure and table could be the
            // object of more refined processing
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                org.apache.commons.lang3.tuple.Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, documentParts);

                String fulltextTaggedRawResult = null;
                if (featSeg != null) {
                    String featureText = featSeg.getLeft();
                    LayoutTokenization layoutTokenization = featSeg.getRight();

                    if (StringUtils.isNotEmpty(featureText)) {
                        fulltextTaggedRawResult = parsers.getFullTextParser().label(featureText);
                    }

                    TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, fulltextTaggedRawResult,
                            layoutTokenization.getTokenization(), true);

                    //Iterate and exclude figures and tables
                    for (TaggingTokenCluster cluster : Iterables.filter(clusteror.cluster(),
                            new TaggingTokenClusteror
                                    .LabelTypeExcludePredicate(TaggingLabels.TABLE_MARKER, TaggingLabels.EQUATION, TaggingLabels.CITATION_MARKER,
                                    TaggingLabels.FIGURE_MARKER, TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION_LABEL))) {

                        if (cluster.getTaggingLabel().equals(TaggingLabels.FIGURE)) {
                            //apply the figure model to only get the caption
                            final Figure processedFigure = parsers.getFigureParser()
                                    .processing(cluster.concatTokens(), cluster.getFeatureBlock());

                            Pair<List<Superconductor>, List<Measurement>> result = process(processedFigure.getCaptionLayoutTokens());
                            superconductorNamesList.addAll(result.getLeft());
                            temperaturesList.addAll(result.getRight());
                        } else if (cluster.getTaggingLabel().equals(TaggingLabels.TABLE)) {
                            //apply the table model to only get the caption/description 
                            final Table processedTable = parsers.getTableParser().processing(cluster.concatTokens(), cluster.getFeatureBlock());
                            Pair<List<Superconductor>, List<Measurement>> result = process(processedTable.getFullDescriptionTokens());
                            superconductorNamesList.addAll(result.getLeft());
                            temperaturesList.addAll(result.getRight());
                        } else {
                            final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();

                            // extract all the layout tokens from the cluster as a list
                            List<LayoutToken> tokens = labeledTokensContainers.stream()
                                    .map(LabeledTokensContainer::getLayoutTokens)
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList());

                            Pair<List<Superconductor>, List<Measurement>> result = process(tokens);
                            superconductorNamesList.addAll(result.getLeft());
                            temperaturesList.addAll(result.getRight());

                        }

                    }
                }
            }

            // we don't process references (although reference titles could be relevant)
            // acknowledgement?

            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {

                Pair<List<Superconductor>, List<Measurement>> result = processDocumentPart(documentParts, doc);
                superconductorNamesList.addAll(result.getLeft());
                temperaturesList.addAll(result.getRight());
            }

        } catch (Exception e) {
            throw new GrobidException("Cannot process pdf file: " + file.getPath(), e);
        }

        // for next line, comparable measurement needs to be implemented
        //Collections.sort(measurements);
        return new ImmutablePair<>(new ImmutablePair<>(superconductorNamesList, temperaturesList), doc);
    }

    /**
     * Process with the quantity model a segment coming from the segmentation model
     */
    private Pair<List<Superconductor>, List<Measurement>> processDocumentPart(SortedSet<DocumentPiece> documentParts,
                                                     Document doc) {
        // List<LayoutToken> for the selected segment
        List<LayoutToken> layoutTokens
                = doc.getTokenizationParts(documentParts, doc.getTokenizations());
        return process(layoutTokens);
    }


    public int batchProcess(String inputDirectory,
                            String outputDirectory,
                            boolean isRecursive) throws IOException {
        return 0;
    }


    @SuppressWarnings({"UnusedParameters"})
    private String addFeatures(List<LayoutToken> tokens, List<Boolean> chemspotEntities) {
        StringBuilder result = new StringBuilder();
        try {
            LayoutToken previous = new LayoutToken();
            ListIterator<LayoutToken> it = tokens.listIterator();
            while (it.hasNext()) {
                int index = it.nextIndex();
                LayoutToken token = it.next();

                if (token.getText().trim().equals("@newline")) {
                    result.append("\n");
                    continue;
                }

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

//                // parano normalisation
//                text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
//                if (text.trim().length() == 0) {
//                    continue;
//                }


                FeaturesVectorSuperconductors featuresVector =
                        FeaturesVectorSuperconductors.addFeatures(token, null, previous, chemspotEntities.get(index));
                result.append(featuresVector.printVector());
                result.append("\n");
                previous = token;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return result.toString();
    }

    /**
     * Extract identified quantities from a labeled text.
     */
    public List<Superconductor> extractResults(List<LayoutToken> tokens, String result) {
        List<Superconductor> resultList = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.SUPERCONDUCTORS, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int pos = 0; // position in term of characters for creating the offsets

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();
            List<BoundingBox> boundingBoxes = null;

            if (!clusterLabel.equals(SUPERCONDUCTOR_OTHER))
                boundingBoxes = BoundingBoxCalculator.calculate(cluster.concatTokens());

            String text = LayoutTokensUtil.toText(tokens);
            if ((pos < text.length() - 1) && (text.charAt(pos) == ' '))
                pos += 1;
            int endPos = pos;
            boolean start = true;
            for (LayoutToken token : theTokens) {
                if (token.getText() != null) {
                    if (start && token.getText().equals(" ")) {
                        pos++;
                        endPos++;
                        continue;
                    }
                    if (start)
                        start = false;
                    endPos += token.getText().length();
                }
            }

            if ((endPos > 0) && (endPos <= text.length()) && (text.charAt(endPos - 1) == ' '))
                endPos--;

            Superconductor superconductor = null;

            if (clusterLabel.equals(SUPERCONDUCTOR_VALUE_NAME)) {
                superconductor = new Superconductor();
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(pos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTOR_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in quantity parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

            pos = endPos;
        }

        return resultList;
    }
}
