package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Span;
import org.grobid.core.data.chemDataExtractor.ChemicalSpan;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.AggregatedProcessing.getFormattedString;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class SuperconductorsParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParser.class);

    private static volatile SuperconductorsParser instance;
    public static final String NONE_CHEMSPOT_TYPE = "NONE";
    private final MaterialParser materialParser;
    private final ChemDataExtractorClient chemicalAnnotator;

    public static SuperconductorsParser getInstance(ChemDataExtractorClient chemspotClient, MaterialParser materialParser) {
        if (instance == null) {
            synchronized (SuperconductorsParser.class) {
                if (instance == null) {
                    instance = new SuperconductorsParser(chemspotClient, materialParser);
                }
            }
        }
        return instance;
    }

    @Inject
    public SuperconductorsParser(ChemDataExtractorClient chemicalAnnotator, MaterialParser materialParser) {
        this(SuperconductorsModels.SUPERCONDUCTORS, chemicalAnnotator, materialParser);
        instance = this;
    }

    public SuperconductorsParser(GrobidModel model, ChemDataExtractorClient chemicalAnnotator, MaterialParser materialParser) {
        super(model);
        this.chemicalAnnotator = chemicalAnnotator;
        this.materialParser = materialParser;
        instance = this;
    }

    public Pair<String, List<Span>> generateTrainingData(List<LayoutToken> layoutTokens) {

        if (isEmpty(layoutTokens))
            return Pair.of("", new ArrayList<>());

        List<Span> measurements = new ArrayList<>();
        String features = null;

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream().map(layoutToken -> {
                layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                return layoutToken;
            }
        ).collect(Collectors.toList());


        List<ChemicalSpan> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
        List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

//        mentions.stream().forEach(m -> System.out.println(">>>>>> " + m.getText() + " --> " + m.getType().name()));

        try {
            // string representation of the feature matrix for CRF lib
            features = addFeatures(layoutTokensNormalised, listAnnotations);

            String labelledResults = null;
            try {
                labelledResults = label(features);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }
            entities.addAll(extractResults(tokens, labelledResults));
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return Pair.of(features, entities);
    }

    public Pair<String, List<Span>> generateTrainingData(String text) {
        text = text.replace("\r\t", " ");
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

    public List<Span> process(List<LayoutToken> layoutTokens) {

        List<Span> entities = new ArrayList<>();

        //Normalisation
        List<LayoutToken> layoutTokensPreNormalised = layoutTokens.stream()
            .map(layoutToken -> {
                    LayoutToken newOne = new LayoutToken(layoutToken);
                    newOne.setText(UnicodeUtil.normaliseText(layoutToken.getText()));
//                        .replaceAll("\\p{C}", " ")));
                    return newOne;
                }
            ).collect(Collectors.toList());

        // List<LayoutToken> for the selected segment
        List<LayoutToken> layoutTokensNormalised = DeepAnalyzer.getInstance()
            .retokenizeLayoutTokens(layoutTokensPreNormalised);

        // list of textual tokens of the selected segment
        //List<String> texts = getTexts(tokenizationParts);

        List<ChemicalSpan> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
        List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        try {
            // string representation of the feature matrix for CRF lib
            String inputWithFeatures = addFeatures(layoutTokensNormalised, listAnnotations);

            if (StringUtils.isEmpty(inputWithFeatures))
                return entities;

            // labeled result from CRF lib
            String labelingResult = null;
            try {
                labelingResult = label(inputWithFeatures);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }

            List<Span> localEntities = extractResults(layoutTokensNormalised, labelingResult);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }

    protected List<Boolean> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<ChemicalSpan> mentions) {
        List<Boolean> isChemicalEntity = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            tokens.stream().forEach(t -> isChemicalEntity.add(Boolean.FALSE));

            return isChemicalEntity;
        }

        mentions = mentions.stream()
            .sorted(Comparator.comparingInt(ChemicalSpan::getStart))
            .collect(Collectors.toList());

        int globalOffset = Iterables.getFirst(tokens, new LayoutToken()).getOffset();

        int mentionId = 0;
        ChemicalSpan mention = mentions.get(mentionId);

        for (LayoutToken token : tokens) {
            //normalise the offsets
            int mentionStart = globalOffset + mention.getStart();
            int mentionEnd = globalOffset + mention.getEnd();

            if (token.getOffset() < mentionStart) {
                isChemicalEntity.add(Boolean.FALSE);
                continue;
            } else {
                if (token.getOffset() >= mentionStart
                    && token.getOffset() + length(token.getText()) <= mentionEnd) {
                    isChemicalEntity.add(true);
                    continue;
                }

                if (mentionId == mentions.size() - 1) {
                    isChemicalEntity.add(Boolean.FALSE);
                    break;
                } else {
                    isChemicalEntity.add(Boolean.FALSE);
                    mentionId++;
                    mention = mentions.get(mentionId);
                }
            }
        }
        if (tokens.size() > isChemicalEntity.size()) {

            for (int counter = isChemicalEntity.size(); counter < tokens.size(); counter++) {
                isChemicalEntity.add(Boolean.FALSE);
            }
        }

        return isChemicalEntity;
    }

    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public List<Span> process(String text) {
        if (isBlank(text)) {
            return new ArrayList<>();
        }

        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> tokens = new ArrayList<>();
        try {
            tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        if (isEmpty(tokens)) {
            return new ArrayList<>();
        }
        return process(tokens);
    }


    @SuppressWarnings({"UnusedParameters"})
    private String addFeatures(List<LayoutToken> tokens, List<Boolean> isChemicalEntity) {
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

                FeaturesVectorSuperconductors featuresVector =
                    FeaturesVectorSuperconductors.addFeatures(token, null, previous, isChemicalEntity.get(index).toString());
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
    public List<Span> extractResults(List<LayoutToken> tokens, String result) {
        List<Span> resultList = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.SUPERCONDUCTORS, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int tokenStartPos = 0; // position in term of layout token index

        String source = SuperconductorsModels.SUPERCONDUCTORS.getModelName();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();
            List<BoundingBox> boundingBoxes = null;

            if (!clusterLabel.equals(SUPERCONDUCTORS_OTHER))
                boundingBoxes = BoundingBoxCalculator.calculate(cluster.concatTokens());

            int startPos = theTokens.get(0).getOffset();
            int endPos = startPos + clusterContent.length();

            int tokenEndPos = tokenStartPos + theTokens.size();

            Span superconductor = new Span();
            superconductor.setSource(source);

            if (clusterLabel.equals(SUPERCONDUCTORS_MATERIAL)) {
                superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setStructuredMaterials(materialParser.process(theTokens));
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                superconductor.setFormattedText(getFormattedString(theTokens));
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_CLASS)) {
                superconductor.setType(SUPERCONDUCTORS_CLASS_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                superconductor.setFormattedText(getFormattedString(theTokens));
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_MEASUREMENT_METHOD)) {
                superconductor.setType(SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_TC)) {
                superconductor.setType(SUPERCONDUCTORS_TC_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_TC_VALUE)) {
                superconductor.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_PRESSURE)) {
                superconductor.setType(SUPERCONDUCTORS_PRESSURE_LABEL);
                superconductor.setText(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                superconductor.setTokenStart(tokenStartPos);
                superconductor.setTokenEnd(tokenEndPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in superconductors parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

            tokenStartPos = tokenEndPos;
        }

        return resultList;
    }
}
