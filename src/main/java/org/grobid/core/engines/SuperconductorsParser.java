package org.grobid.core.engines;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.data.chemspot.Mention;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class SuperconductorsParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsParser.class);

    private static volatile SuperconductorsParser instance;
    public static final String NONE_CHEMSPOT_TYPE = "NONE";
//    private ChemspotClient chemspotClient;

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
//        this.chemspotClient = chemspotClient;
        instance = this;
    }

    public Pair<String, List<Superconductor>> generateTrainingData(List<LayoutToken> layoutTokens) {

        if(isEmpty(layoutTokens))
            return Pair.of("", new ArrayList<>());

        List<Superconductor> measurements = new ArrayList<>();
        String ress = null;

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream().map(layoutToken -> {
                    layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                    return layoutToken;
                }
        ).collect(Collectors.toList());


//        List<Mention> mentions = chemspotClient.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
//        List<String> listChemspotEntities = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

//        mentions.stream().forEach(m -> System.out.println(">>>>>> " + m.getText() + " --> " + m.getType().name()));

        try {
            // string representation of the feature matrix for CRF lib
//            ress = addFeatures(layoutTokensNormalised, listChemspotEntities);
            ress = addFeatures(layoutTokensNormalised);

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

    public List<Superconductor> process(List<LayoutToken> layoutTokens) {

        List<Superconductor> entities = new ArrayList<>();

        // List<LayoutToken> for the selected segment
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream().map(layoutToken -> {
                    layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                    return layoutToken;
                }
        ).collect(Collectors.toList());

        // list of textual tokens of the selected segment
        //List<String> texts = getTexts(tokenizationParts);

//        List<Mention> mentions = chemspotClient.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
//        List<String> listChemspotEntities = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        try {
            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(layoutTokensNormalised);// , listChemspotEntities);

            if (StringUtils.isEmpty(ress))
                return entities;

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantity parsing failed.", e);
            }

            List<Superconductor> localEntities = extractResults(layoutTokensNormalised, res);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }

    protected List<String> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<Mention> mentions) {
        List<String> isChemspotMention = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            tokens.stream().forEach(t -> isChemspotMention.add(NONE_CHEMSPOT_TYPE));

            return isChemspotMention;
        }

        int globalOffset = 0;
        if (CollectionUtils.isNotEmpty(tokens)) {
            globalOffset = tokens.get(0).getOffset();
        }

        int mentionId = 0;
        Mention mention = mentions.get(mentionId);

        for (LayoutToken token : tokens) {
            //normalise the offsets
            int mentionStart = globalOffset + mention.getStart();
            int mentionEnd = globalOffset + mention.getEnd();

            if (token.getOffset() < mentionStart) {
                isChemspotMention.add(NONE_CHEMSPOT_TYPE);
                continue;
            } else {
                if (token.getOffset() >= mentionStart
                        && token.getOffset() + length(token.getText()) <= mentionEnd) {
                    isChemspotMention.add(mention.getType().name());
                    continue;
                }

                if (mentionId == mentions.size() - 1) {
                    isChemspotMention.add(NONE_CHEMSPOT_TYPE);
                    break;
                } else {
                    isChemspotMention.add(NONE_CHEMSPOT_TYPE);
                    mentionId++;
                    mention = mentions.get(mentionId);
                }
            }
        }
        if (tokens.size() > isChemspotMention.size()) {

            for (int counter = isChemspotMention.size(); counter < tokens.size(); counter++) {
                isChemspotMention.add(NONE_CHEMSPOT_TYPE);
            }
        }

        return isChemspotMention;
    }

    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public List<Superconductor> process(String text) {
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
    private String addFeatures(List<LayoutToken> tokens) {//, List<String> chemspotEntities) {
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
                        FeaturesVectorSuperconductors.addFeatures(token, null, previous, null);
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

//        int pos = 0; // position in term of characters for creating the offsets

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

//            OffsetPosition calculatedOffset = calculateOffsets(tokens, theTokens, pos);
//            pos = calculatedOffset.start;
//            int endPos = calculatedOffset.end;

            int startPos = theTokens.get(0).getOffset();
            int endPos = startPos + clusterContent.length();

            Superconductor superconductor = new Superconductor();

            if (clusterLabel.equals(SUPERCONDUCTORS_MATERIAL)) {
                superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_CLASS)) {
                superconductor.setType(SUPERCONDUCTORS_CLASS_LABEL);
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_SAMPLE)) {
                superconductor.setType(SUPERCONDUCTORS_SAMPLE_LABEL);
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_TC)) {
                superconductor.setType(SUPERCONDUCTORS_TC_LABEL);
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_TC_VALUE)) {
                superconductor.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
                superconductor.setName(clusterContent);
                superconductor.setLayoutTokens(theTokens);
                superconductor.setBoundingBoxes(boundingBoxes);
                superconductor.setOffsetStart(startPos);
                superconductor.setOffsetEnd(endPos);
                resultList.add(superconductor);
            } else if (clusterLabel.equals(SUPERCONDUCTORS_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in quantity parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

//            pos = endPos;
        }

        return resultList;
    }

    protected OffsetPosition calculateOffsets(List<LayoutToken> tokens, List<LayoutToken> theTokens, int pos) {
        String text = LayoutTokensUtil.toText(tokens);
        // ignore spaces at the beginning
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

        //Remove the space at the end
        if ((endPos > 0) && (endPos <= text.length()) && (text.charAt(endPos - 1) == ' '))
            endPos--;

        return new OffsetPosition(pos, endPos);
    }
}
