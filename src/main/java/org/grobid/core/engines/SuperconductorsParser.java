package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.external.chemDataExtractor.ChemicalSpan;
import org.grobid.core.data.material.ChemicalComposition;
import org.grobid.core.data.material.Formula;
import org.grobid.core.data.material.Material;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.client.ChemDataExtractorClient;
import org.grobid.core.utilities.client.StructureIdentificationModuleClient;
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
    private final StructureIdentificationModuleClient structureIdentificationModuleClient;

    public static SuperconductorsParser getInstance(ChemDataExtractorClient chemspotClient, MaterialParser materialParser,
                                                    StructureIdentificationModuleClient structureIdentificationModuleClient) {
        if (instance == null) {
            synchronized (SuperconductorsParser.class) {
                if (instance == null) {
                    instance = new SuperconductorsParser(chemspotClient, materialParser, structureIdentificationModuleClient);
                }
            }
        }
        return instance;
    }

    @Inject
    public SuperconductorsParser(ChemDataExtractorClient chemicalAnnotator, MaterialParser materialParser, StructureIdentificationModuleClient structureIdentificationModuleClient) {
        this(SuperconductorsModels.SUPERCONDUCTORS, chemicalAnnotator, materialParser, structureIdentificationModuleClient);
        instance = this;
    }

    public SuperconductorsParser(GrobidModel model, ChemDataExtractorClient chemicalAnnotator, MaterialParser materialParser, StructureIdentificationModuleClient structureIdentificationModuleClient) {
        super(model);
        this.chemicalAnnotator = chemicalAnnotator;
        this.materialParser = materialParser;
        this.structureIdentificationModuleClient = structureIdentificationModuleClient;
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

    public List<Span> extractSpans(List<LayoutToken> tokens, List<ChemicalSpan> mentions) {
        List<Span> output = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            return output;
        }

        mentions = mentions.stream()
            .sorted(Comparator.comparingInt(ChemicalSpan::getStart))
            .collect(Collectors.toList());

        int globalOffset = Iterables.getFirst(tokens, new LayoutToken()).getOffset();

        int mentionId = 0;
        ChemicalSpan mention = mentions.get(mentionId);
        Span currentSpan = null;

        boolean newMention = true;
        int tokenId = 0;
        for (LayoutToken token : tokens) {
            //normalise the offsets
            int mentionStart = globalOffset + mention.getStart();
            int mentionEnd = globalOffset + mention.getEnd();

            if (token.getOffset() < mentionStart) {
                tokenId++;
                continue;
            } else {
                if (token.getOffset() >= mentionStart
                    && token.getOffset() + length(token.getText()) <= mentionEnd) {
                    if (newMention) {
                        currentSpan = new Span();
                        currentSpan.setType(mention.getLabel());
                        currentSpan.setTokenStart(tokenId);
                        currentSpan.setOffsetStart(mention.getStart());
                        currentSpan.setOffsetEnd(mention.getEnd());
                        output.add(currentSpan);
                        newMention = false;
                    }

                    currentSpan.getLayoutTokens().add(token);

//                    if (StringUtils.isNotBlank(mention.getType())) {
//                        currentSpan.setType(mention.getType());
//                    }
                    tokenId++;
                    continue;
                }

                if (mentionId == mentions.size() - 1) {
                    currentSpan.setTokenEnd(tokenId);
                    break;
                } else {
                    currentSpan.setTokenEnd(tokenId);
                    mentionId++;
                    mention = mentions.get(mentionId);
                    newMention = true;
                    currentSpan.getId();
                }
            }
            tokenId++;
        }
//        if (tokens.size() > output.size()) {
//
//            for (int counter = output.size(); counter < tokens.size(); counter++) {
//            }
//        }

        output.stream()
            .forEach(s -> {
                s.setText(LayoutTokensUtil.toText(s.getLayoutTokens()));
                s.getId();
            });

        return output;
    }

    protected List<Boolean> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<ChemicalSpan> mentions) {
        List<Boolean> isChemicalEntity = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            tokens.stream()
                .forEach(t -> isChemicalEntity.add(Boolean.FALSE));

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

        List<String> texts = normalisedTokens.stream()
            .map(LayoutTokensUtil::toText)
            .collect(Collectors.toList());

        List<List<ChemicalSpan>> structures = structureIdentificationModuleClient.extractStructuresMulti(texts);
        List<List<ChemicalSpan>> mentions = chemicalAnnotator.processBulk(texts);
        List<String> tokensWithFeatures = new ArrayList<>();

        List<List<Span>> structuredCumulatedSpans = new ArrayList<>();

        for (int i = 0; i < normalisedTokens.size(); i++) {
            List<Boolean> listChemicalAnnotations = synchroniseLayoutTokensWithMentions(normalisedTokens.get(i), mentions.get(i));

            if (CollectionUtils.isEmpty(structures)) {
                LOGGER.debug("Structures extraction (crystal structure and space groups) disabled. ");
            } else {
                List<Span> structureSpan = extractSpans(normalisedTokens.get(i), structures.get(i));
                structureSpan.stream().forEach(s -> s.setLinkable(true));
                structuredCumulatedSpans.add(structureSpan);
            }

            //TODO: remove this hack! :-) 
            //TODO: one day, son... One day... 
            tokensWithFeatures.add(addFeatures(normalisedTokens.get(i), listChemicalAnnotations) + "\n");
        }


        // labeled result from CRF lib
        String labellingResult = null;
        try {
            labellingResult = label(tokensWithFeatures);
        } catch (Exception e) {
            throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
        }

        List<String> resultingBlocks = Arrays.asList(labellingResult.split("\n\n"));
        List<List<Span>> localEntities = extractParallelResults(normalisedTokens, resultingBlocks);

        // add the entities from the extracted structures to the list of entities 
        for (int i = 0; i < normalisedTokens.size(); i++) {
            if (CollectionUtils.isEmpty(structures)) {
                LOGGER.debug("Structures extraction (crystal structure and space groups) disabled. ");
            } else {
                localEntities.get(i).addAll(structuredCumulatedSpans.get(i));
            }
        }

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
                boolean hasFormula = false;

                List<Material> parsedMaterials = materialParser.process(theTokens);
//                superconductor.getOriginalMaterials().addAll(parsedMaterials);
                int i = 0;
                for (Material parsedMaterial : parsedMaterials) {
                    if (parsedMaterial.getFormula() != null && StringUtils.isNotBlank(parsedMaterial.getFormula().getRawValue())) {
                        hasFormula = true;
                    }
                    superconductor.getAttributes().putAll(Material.asAttributeMap(parsedMaterial, "material" + i));
                    i++;
                }
                String rawMaterialString = hasFormula ? materialParser.postProcessFormula(clusterContent) : clusterContent;
                superconductor.setText(rawMaterialString);

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
                
                if (materialParser != null && materialParser.getChemicalMaterialParserClient() != null) {
                    ChemicalComposition chemicalComposition = materialParser.getChemicalMaterialParserClient().convertNameToFormula(clusterContent);
                    if (!chemicalComposition.isEmpty()) {
                        Material classAsMaterial = new Material();
                        classAsMaterial.setName(clusterContent);
                        if (CollectionUtils.isNotEmpty(chemicalComposition.getComposition().keySet())) {
                            classAsMaterial.setFormula(new Formula(chemicalComposition.getFormula(), chemicalComposition.getComposition()));
                        }
                        superconductor.getAttributes().putAll(Material.asAttributeMap(classAsMaterial, "class"));
                    }
                }
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
