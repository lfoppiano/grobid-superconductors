package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.data.chemDataExtractor.Span;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.trainer.stax.handler.EntityLinkerAnnotationStaxHandler;
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
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class EntityLinkerParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinkerParser.class);

    private static volatile EntityLinkerParser instance;

    public static EntityLinkerParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new EntityLinkerParser();
    }

    @Inject
    public EntityLinkerParser() {
        super(SuperconductorsModels.ENTITY_LINKER);
        instance = this;
    }

    public EntityLinkerParser(GrobidModel model) {
        super(model);
        instance = this;
    }

//    public Pair<String, List<Superconductor>> generateTrainingData(List<LayoutToken> layoutTokens) {
//
//        if (isEmpty(layoutTokens))
//            return Pair.of("", new ArrayList<>());
//
//        List<Superconductor> measurements = new ArrayList<>();
//        String ress = null;
//
//        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);
//
//        //Normalisation
//        List<LayoutToken> layoutTokensNormalised = tokens.stream().map(layoutToken -> {
//                layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));
//
//                return layoutToken;
//            }
//        ).collect(Collectors.toList());
//
//
//        List<Span> mentions = chemicalAnnotator.processText(LayoutTokensUtil.toText(layoutTokensNormalised));
//        List<Boolean> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);
//
////        mentions.stream().forEach(m -> System.out.println(">>>>>> " + m.getText() + " --> " + m.getType().name()));
//
//        try {
//            // string representation of the feature matrix for CRF lib
//            ress = addFeatures(layoutTokensNormalised, listAnnotations);
//
//            String res = null;
//            try {
//                res = label(ress);
//            } catch (Exception e) {
//                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
//            }
//            measurements.addAll(extractResults(tokens, res));
//        } catch (Exception e) {
//            throw new GrobidException("An exception occured while running Grobid.", e);
//        }
//
//        return Pair.of(ress, measurements);
//    }

//    public Pair<String, List<Superconductor>> generateTrainingData(String text) {
//        text = text.replace("\r\t", " ");
//        text = text.replace("\n", " ");
//        text = text.replace("\t", " ");
//
//        List<LayoutToken> layoutTokens = null;
//        try {
//            layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
//        } catch (Exception e) {
//            LOGGER.error("fail to tokenize:, " + text, e);
//        }
//
//        return generateTrainingData(layoutTokens);
//
//    }

    public List<Superconductor> process(List<LayoutToken> layoutTokens, List<Superconductor> annotations) {

        List<Superconductor> entities = new ArrayList<>();

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

        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        try {
            List<Span> mentions = annotations.stream().map(a-> new Span(a.getOffsetStart(), a.getOffsetEnd(), a.getType())).collect(Collectors.toList());
            List<String> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(layoutTokensNormalised, listAnnotations);

            if (StringUtils.isEmpty(ress))
                return entities;

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }

            List<Superconductor> localEntities = extractResults(layoutTokensNormalised, res);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }

    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public List<Superconductor> process(String text, List<Superconductor> annotations) {
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
        return process(tokens, annotations);
    }


    @SuppressWarnings({"UnusedParameters"})
    private String addFeatures(List<LayoutToken> tokens, List<String> annotations) {
        StringBuilder result = new StringBuilder();
        try {
            ListIterator<LayoutToken> it = tokens.listIterator();
            while (it.hasNext()) {
                int index = it.nextIndex();
                LayoutToken token = it.next();

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

                FeaturesVectorEntityLinker featuresVector =
                    FeaturesVectorEntityLinker.addFeatures(token.getText(), null, annotations.get(index));
                result.append(featuresVector.printVector());
                result.append("\n");
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

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.ENTITY_LINKER, result, tokens);
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

            if (clusterLabel.equals(ENTITY_LINKER_LEFT_ATTACHMENT)) {

            } else if (clusterLabel.equals(ENTITY_LINKER_RIGHT_ATTACHMENT)) {

            } else if (clusterLabel.equals(ENTITY_LINKER_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in entity-linker parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

//            pos = endPos;
        }

        return resultList;
    }

    protected List<String> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<Span> mentions) {
        List<String> output = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            tokens.stream().forEach(t -> output.add(OTHER_LABEL));

            return output;
        }

        mentions = mentions.stream()
            .sorted(Comparator.comparingInt(Span::getStart))
            .collect(Collectors.toList());

        int globalOffset = Iterables.getFirst(tokens, new LayoutToken()).getOffset();

        int mentionId = 0;
        Span mention = mentions.get(mentionId);

        for (LayoutToken token : tokens) {
            //normalise the offsets
            int mentionStart = globalOffset + mention.getStart();
            int mentionEnd = globalOffset + mention.getEnd();

            if (token.getOffset() < mentionStart) {
                output.add(OTHER_LABEL);
                continue;
            } else {
                if (token.getOffset() >= mentionStart
                    && token.getOffset() + length(token.getText()) <= mentionEnd) {
                    output.add(mention.getLabel());
                    continue;
                }

                if (mentionId == mentions.size() - 1) {
                    output.add(OTHER_LABEL);
                    break;
                } else {
                    output.add(OTHER_LABEL);
                    mentionId++;
                    mention = mentions.get(mentionId);
                }
            }
        }
        if (tokens.size() > output.size()) {

            for (int counter = output.size(); counter < tokens.size(); counter++) {
                output.add(OTHER_LABEL);
            }
        }

        return output;
    }
}
