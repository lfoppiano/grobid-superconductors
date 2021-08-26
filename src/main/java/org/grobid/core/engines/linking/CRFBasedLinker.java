package org.grobid.core.engines.linking;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Link;
import org.grobid.core.data.Span;
import org.grobid.core.data.chemDataExtractor.ChemicalSpan;
import org.grobid.core.engines.SuperconductorsParser;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.AdditionalLayoutTokensUtil;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.OTHER_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_OTHER;

@Singleton
public class CRFBasedLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRFBasedLinker.class);

    private static volatile CRFBasedLinker instance;
    public static final String MATERIAL_TCVALUE_ID = "material-tcValue";
    public static final String TCVALUE_PRESSURE_ID = "tcValue-pressure";
    public static final String TCVALUE_ME_METHOD_ID = "tcValue-me_method";

    private Map<String, EntityLinker> annotationLinks = new HashMap<>();

    public static CRFBasedLinker getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new CRFBasedLinker();
    }

    @Inject
    public CRFBasedLinker() {
        annotationLinks.put(MATERIAL_TCVALUE_ID, EntityLinker_MaterialTcValue.getInstance());
        annotationLinks.put(TCVALUE_PRESSURE_ID, EntityLinker_TcValuePressure.getInstance());
        annotationLinks.put(TCVALUE_ME_METHOD_ID, EntityLinker_TcValueMe_Method.getInstance());
    }

    public CRFBasedLinker(Map<String, EntityLinker> annotationsLinks) {
        this.annotationLinks = annotationsLinks;
    }

    public List<Span> process(List<LayoutToken> layoutTokens, List<Span> annotations, String linkerType) {

        if (!this.annotationLinks.containsKey(linkerType)) {
            throw new RuntimeException("the linker type " + linkerType + "does not exists. ");
        }

        EntityLinker linkingImplementation = this.annotationLinks.get(linkerType);
        List<Span> taggedAnnotations = linkingImplementation.markLinkableEntities(annotations);

        List<Span> linkableAnnotations = taggedAnnotations.stream()
            .filter(Span::isLinkable)
            .collect(Collectors.toList());

        //Normalisation
//        List<LayoutToken> layoutTokensNormalised = SuperconductorsParser.normalizeAndRetokenizeLayoutTokens(layoutTokens);
        List<LayoutToken> layoutTokensPreNormalised = layoutTokens.stream()
            .map(layoutToken -> {
                    LayoutToken newOne = new LayoutToken(layoutToken);
                    newOne.setText(UnicodeUtil.normaliseText(layoutToken.getText()));
//                        .replaceAll("\\p{C}", " ")));
                    return newOne;
                }
            ).collect(Collectors.toList());

        List<LayoutToken> layoutTokensNormalised = DeepAnalyzer.getInstance()
            .retokenizeLayoutTokens(layoutTokensPreNormalised);

        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        try {
            List<Span> filteredAnnotations = linkableAnnotations.stream()
                .filter(a -> linkingImplementation.getAnnotationsToBeLinked().contains(a.getType()))
                .collect(Collectors.toList());

            List<ChemicalSpan> mentions = filteredAnnotations.stream()
                .map(a -> new ChemicalSpan(a.getOffsetStart(), a.getOffsetEnd(), a.getType()))
                .collect(Collectors.toList());

            List<String> listAnnotations = synchroniseLayoutTokensWithMentions(layoutTokensNormalised, mentions);

            // string representation of the feature matrix for CRF lib
            String ress = linkingImplementation.addFeatures(layoutTokensNormalised, listAnnotations);

            if (StringUtils.isEmpty(ress))
                return new ArrayList<>();

            // labeled result from CRF lib
            String res = null;
            try {
                res = linkingImplementation.label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }

            List<Span> localLinkedEntities = linkingImplementation.extractResults(layoutTokensNormalised, res, filteredAnnotations);

            //I modify the taggedAnnotations which are anyway a copy of the spans 
            for (Span annot : taggedAnnotations) {
                for (Span localEntity : localLinkedEntities) {
                    if (localEntity.equals(annot) && isNotEmpty(localEntity.getLinks())) {
                        annot.setLinks(localEntity.getLinks());
                        annot.setLinkable(localEntity.isLinkable());
                        break;
                    }
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }

        return taggedAnnotations;
    }

    public List<Span> process(String text, List<Span> annotations, String linkerType) {
        List<LayoutToken> tokens = SuperconductorsParser.textToLayoutTokens(text);

        // I need to fill up the annotations' layout tokens
        for (Span span : annotations) {
            Pair<Integer, Integer> extremitiesAsIndex = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, span.getOffsetStart(), span.getOffsetEnd());
            // The +1 is simulating the fact that the layout token are coming from an upstream model,
            // which usually includes the final space of the output. A "feature" that comes from the Clusteror.
            int endExtremities = extremitiesAsIndex.getRight();
            if (endExtremities < tokens.size() && tokens.get(endExtremities).getText().equals(" ")) {
                endExtremities = extremitiesAsIndex.getRight() + 1;
            }
            span.setLayoutTokens(tokens.subList(extremitiesAsIndex.getLeft(), endExtremities));
            span.setTokenStart(extremitiesAsIndex.getLeft());
            span.setTokenEnd(extremitiesAsIndex.getRight());
        }

        if (isEmpty(tokens)) {
            return new ArrayList<>();
        }

        return process(tokens, annotations, linkerType);
    }


    /**
     * Extract identified quantities from a labeled text.
     */
    public static List<Span> extractResults(List<LayoutToken> tokens, String result, List<Span> annotations,
                                            GrobidModel model, TaggingLabel leftTaggingLabel,
                                            TaggingLabel rightTaggingLabel, TaggingLabel otherTaggingLabel) {

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(model, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int pos = 0; // position in term of characters for creating the offsets

        boolean insideLink = false;
        List<TaggingTokenCluster> detectedClusters = clusters.stream()
            .filter(a -> !a.getTaggingLabel().getLabel().equals(OTHER_LABEL))
            .collect(Collectors.toList());

        if (detectedClusters.size() != annotations.size()) {
            LOGGER.info("Some annotation will not be linked. Input entities: " + annotations.size() + ", output links: " + detectedClusters.size());
        }
        Span leftSide = null;

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

            if (clusterLabel.equals(leftTaggingLabel)) {
                if (insideLink) {
                    LOGGER.info("Found link-left label with content " + clusterContent);

//                    Pair<Integer, Integer> extremitiesAsIndex = getExtremitiesAsIndex(tokens, startPos, endPos);
//                    List<LayoutToken> link = new ArrayList<>();
//                    for (int x = extremitiesAsIndex.getLeft(); x < extremitiesAsIndex.getRight(); x++) {
//                        link.add(tokens.get(x));
//                    }

                    int layoutTokenListStartOffset = AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(theTokens);
                    int layoutTokenListEndOffset = AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(theTokens);
                    List<Span> collect = annotations.stream().filter(a -> {
                        int supLayoutStart = AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(a.getLayoutTokens());
                        int supLayoutEnd = AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(a.getLayoutTokens());

                        return supLayoutStart == layoutTokenListStartOffset && supLayoutEnd == layoutTokenListEndOffset;
                    }).collect(Collectors.toList());

                    if (collect.size() == 1) {
                        Span rightSide = collect.get(0);

                        if (rightSide.getType().equals(leftSide.getType())) {
                            LOGGER.warn("Linking two entities of the same type. Ignoring.");
                        } else {
                            LOGGER.info("Link left -> " + rightSide.getText());
                            leftSide.addLink(new Link(String.valueOf(rightSide.getId()), rightSide.getText(), rightSide.getType(), "crf"));
                            rightSide.addLink(new Link(String.valueOf(leftSide.getId()), leftSide.getText(), leftSide.getType(), "crf"));
                            // After linking I remove the references to both sides
                            leftSide = null;
                            rightSide = null;
                        }
                    } else {
                        LOGGER.error("Cannot find the span corresponding to the link. Ignoring it. ");
                    }

                    insideLink = false;
                } else {
                    LOGGER.warn("Something is wrong, there is link to the left but not to the right. Ignoring it. ");
                }

            } else if (clusterLabel.equals(rightTaggingLabel)) {
                LOGGER.info("Found link-right label with content " + clusterContent);

                int layoutTokenListStartOffset = AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(theTokens);
                int layoutTokenListEndOffset = AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(theTokens);
                List<Span> spansCorrespondingToCurrentLink = annotations.stream().filter(a -> {
                    int supLayoutStart = AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(a.getLayoutTokens());
                    int supLayoutEnd = AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(a.getLayoutTokens());

                    return supLayoutStart == layoutTokenListStartOffset && supLayoutEnd == layoutTokenListEndOffset;
                }).collect(Collectors.toList());
//
//                    Pair<Integer, Integer> extremitiesAsIndex = getExtremitiesAsIndex(tokens, startPos, endPos);
//                    List<LayoutToken> link = new ArrayList<>();
//                    for (int x = extremitiesAsIndex.getLeft(); x < extremitiesAsIndex.getRight(); x++) {
//                        link.add(tokens.get(x));
//                    }
                if (!insideLink) {
                    if (spansCorrespondingToCurrentLink.size() == 1) {
                        LOGGER.info("Link right -> " + spansCorrespondingToCurrentLink.get(0).getText());
                        leftSide = spansCorrespondingToCurrentLink.get(0);
                        insideLink = true;
                    } else {
                        LOGGER.error("Cannot find the span corresponding to the link. Ignoring it. ");
                        insideLink = false;
                    }
                } else {
                    LOGGER.warn("Something is wrong, there is a link, but this means I should link on the left. Let's ignore the previous stored link and start from scratch. ");
                    if (spansCorrespondingToCurrentLink.size() == 1) {
                        LOGGER.info("Link right -> " + spansCorrespondingToCurrentLink.get(0).getText());
                        leftSide = spansCorrespondingToCurrentLink.get(0);
                        insideLink = true;
                    } else {
                        LOGGER.error("Cannot find the span corresponding to the link. Ignoring it. ");
                        insideLink = false;
                    }

                }

            } else if (clusterLabel.equals(otherTaggingLabel)) {

            } else {
                LOGGER.error("Warning: unexpected label in entity-linker parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }

        return annotations;
    }

    public static List<String> synchroniseLayoutTokensWithMentions(List<LayoutToken> tokens, List<ChemicalSpan> mentions) {
        List<String> output = new ArrayList<>();

        if (CollectionUtils.isEmpty(mentions)) {
            tokens.stream().forEach(t -> output.add(OTHER_LABEL));

            return output;
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


    public Map<String, EntityLinker> getLinkingEngines() {
        return annotationLinks;
    }
}
