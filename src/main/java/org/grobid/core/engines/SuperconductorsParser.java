package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.ModuleEngine.getFormattedString;
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

        List<Span> entities = new ArrayList<>();
        String features = null;

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream()
            .map(layoutToken -> {
                    layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                    return layoutToken;
                }
            )
            .collect(Collectors.toList());

        List<ChemicalSpan> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
        List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

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
            throw new GrobidException("An exception occurred while running Grobid.", e);
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

    public List<Span> processSingle(List<LayoutToken> layoutTokens) {

        List<Span> entities = new ArrayList<>();

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = normalizeAndRetokenizeLayoutTokens(layoutTokens);

        List<ChemicalSpan> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
        List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

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


    public List<List<Span>> processText(List<String> text) {
        List<List<LayoutToken>> tokens = text.stream()
            .map(SuperconductorsParser::textToLayoutTokens)
            .collect(Collectors.toList());

        return process(tokens);
    }

    public static List<LayoutToken> textToLayoutTokens(String text) {
        if (isBlank(text)) {
            return new ArrayList<>();
        }

        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> layoutTokens = new ArrayList<>();
        try {
            layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        if (isEmpty(layoutTokens)) {
            return new ArrayList<>();
        }

        return layoutTokens;
    }


    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public List<Span> processSingle(String text) {
        return processSingle(textToLayoutTokens(text));
    }

    public List<List<Span>> process(List<List<LayoutToken>> layoutTokensBatch) {
        List<List<LayoutToken>> normalisedTokens = layoutTokensBatch.stream()
            .map(SuperconductorsParser::normalizeAndRetokenizeLayoutTokens)
            .collect(Collectors.toList());

        List<String> tokensWithFeatures = normalisedTokens.stream()
            .map(lt -> {
                List<ChemicalSpan> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(lt));
                List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(lt, mentions);

                //TODO: remove this hack! :-) 
                return addFeatures(lt, listAnnotations) + "\n";
            })
            .collect(Collectors.toList());

        // labeled result from CRF lib
        String labellingResult = null;
        try {
            labellingResult = label(tokensWithFeatures);
        } catch (Exception e) {
            throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
        }

        List<String> resultingBlocks = Arrays.asList(labellingResult.split("\n\n"));
        List<List<Span>> localEntities = extractParallelResults(normalisedTokens, resultingBlocks);

        return localEntities;
    }

    public static List<LayoutToken> normalizeAndRetokenizeLayoutTokens(List<LayoutToken> layoutTokens) {
        List<LayoutToken> layoutTokensPreNormalised = normalizeLayoutTokens(layoutTokens);

        List<LayoutToken> layoutTokensNormalised = DeepAnalyzer.getInstance()
            .retokenizeLayoutTokens(layoutTokensPreNormalised);

        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        return layoutTokensNormalised;
    }

    public static List<LayoutToken> normalizeLayoutTokens(List<LayoutToken> layoutTokens) {
        return layoutTokens.stream()
            .map(layoutToken -> {
                    LayoutToken newOne = new LayoutToken(layoutToken);
                    newOne.setText(UnicodeUtil.normaliseText(layoutToken.getText()));
                    return newOne;
                }
            ).collect(Collectors.toList());
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

    public List<List<Span>> extractParallelResults(List<List<LayoutToken>> tokens, List<String> results) {
        List<List<Span>> spans = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            spans.add(extractResults(tokens.get(i), results.get(i)));
        }

        return spans;
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
                List<Material> parsedMaterials = materialParser.process(theTokens);
                int i = 0;
                for (Material parsedMaterial : parsedMaterials) {
                    superconductor.getAttributes().putAll(Material.asAttributeMap(parsedMaterial, "material" + i));
                    i++;
                }
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
