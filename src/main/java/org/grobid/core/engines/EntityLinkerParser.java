package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.internal.bytebuddy.implementation.bind.annotation.Super;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Measurement;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.grobid.core.utilities.MeasurementUtils.*;

@Singleton
public class EntityLinkerParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinkerParser.class);

    private static volatile EntityLinkerParser instance;

    private List<String> annotationLinks = new ArrayList<>();

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
        annotationLinks = Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL);
    }

    public EntityLinkerParser(GrobidModel model, List<String> validLinkAnnotations) {
        super(model);
        instance = this;
        annotationLinks = validLinkAnnotations;
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
            List<Superconductor> filteredAnnotations = annotations.stream().filter(a -> annotationLinks.contains(a.getType())).collect(Collectors.toList());
            List<Span> mentions = filteredAnnotations.stream().map(a -> new Span(a.getOffsetStart(), a.getOffsetEnd(), a.getType())).collect(Collectors.toList());
            List<String> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(layoutTokensNormalised, listAnnotations);

            if (StringUtils.isEmpty(ress))
                return annotations;

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }

            List<Superconductor> localLinkedEntities = extractResults(layoutTokensNormalised, res, filteredAnnotations);

            for(Superconductor annotation : annotations) {
                for (Superconductor localEntity : localLinkedEntities) {
                    if (localEntity.equals(annotation)) {
                        annotation.setLinkedEntity(localEntity.getLinkedEntity());
                        break;
                    }
                }
            }


        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return annotations;
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
    public List<Superconductor> extractResults(List<LayoutToken> tokens, String result, List<Superconductor> annotations) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.ENTITY_LINKER, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int pos = 0; // position in term of characters for creating the offsets

        boolean insideLink = false;
        List<TaggingTokenCluster> detectedClusters = clusters.stream().filter(a -> !a.getTaggingLabel().getLabel().equals(OTHER_LABEL)).collect(Collectors.toList());
        if (detectedClusters.size() != annotations.size()) {
            LOGGER.warn("Linking seems not correct. Expected annotations: " + annotations.size() + ", output links: " + detectedClusters.size());
        }
        Superconductor leftSide = null;

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

            if (clusterLabel.equals(ENTITY_LINKER_LEFT_ATTACHMENT)) {
                if (insideLink) {
                    LOGGER.info("Linking to the left " + clusterContent);

//                    Pair<Integer, Integer> extremitiesAsIndex = getExtremitiesAsIndex(tokens, startPos, endPos);
//                    List<LayoutToken> link = new ArrayList<>();
//                    for (int x = extremitiesAsIndex.getLeft(); x < extremitiesAsIndex.getRight(); x++) {
//                        link.add(tokens.get(x));
//                    }

                    int layoutTokenListStartOffset = getLayoutTokenListStartOffset(theTokens);
                    int layoutTokenListEndOffset = getLayoutTokenListEndOffset(theTokens);
                    List<Superconductor> collect = annotations.stream().filter(a -> {
                        int supLayoutStart = getLayoutTokenListStartOffset(a.getLayoutTokens());
                        int supLayoutEnd = getLayoutTokenListEndOffset(a.getLayoutTokens());

                        return supLayoutStart == layoutTokenListStartOffset && supLayoutEnd == layoutTokenListEndOffset;
                    }).collect(Collectors.toList());

                    if(collect.size() == 1) {
                        LOGGER.info("Link left -> " + collect.get(0).getName());
                        leftSide.setLinkedEntity(collect.get(0));
                        collect.get(0).setLinkedEntity(leftSide);

                    } else {
                        LOGGER.error("Cannot find the link ... no matching in the original list of tokens... dammit!");
                    }

                    insideLink = false;
                } else {
                    LOGGER.warn("Something is wrong, there is link to the left but not to the right. ");
                }

            } else if (clusterLabel.equals(ENTITY_LINKER_RIGHT_ATTACHMENT)) {
                if (!insideLink) {
                    LOGGER.info("Linking on the right " + clusterContent);
                    int layoutTokenListStartOffset = getLayoutTokenListStartOffset(theTokens);
                    int layoutTokenListEndOffset = getLayoutTokenListEndOffset(theTokens);
                    List<Superconductor> collect = annotations.stream().filter(a -> {
                        int supLayoutStart = getLayoutTokenListStartOffset(a.getLayoutTokens());
                        int supLayoutEnd = getLayoutTokenListEndOffset(a.getLayoutTokens());

                        return supLayoutStart == layoutTokenListStartOffset && supLayoutEnd == layoutTokenListEndOffset;
                    }).collect(Collectors.toList());
//
//                    Pair<Integer, Integer> extremitiesAsIndex = getExtremitiesAsIndex(tokens, startPos, endPos);
//                    List<LayoutToken> link = new ArrayList<>();
//                    for (int x = extremitiesAsIndex.getLeft(); x < extremitiesAsIndex.getRight(); x++) {
//                        link.add(tokens.get(x));
//                    }
                    if(collect.size() == 1) {
                        LOGGER.info("Link right -> " + collect.get(0).getName());
                        leftSide = collect.get(0);
                    } else {
                        LOGGER.error("Cannot find the link ... no matching in the original list of tokens... dammit!");
                    }

                    insideLink = true;
                } else {
                    LOGGER.warn("Something is wrong, there is link it means I should link on the left . ");
                }

            } else if (clusterLabel.equals(ENTITY_LINKER_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in entity-linker parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

//            pos = endPos;
        }

        return annotations;
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
