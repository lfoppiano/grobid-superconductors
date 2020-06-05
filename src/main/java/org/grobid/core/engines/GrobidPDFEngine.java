package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Figure;
import org.grobid.core.data.Table;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.training.SuperconductorsParserTrainingData;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class GrobidPDFEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidPDFEngine.class);

    private static final List<TaggingLabel> EXCLUDED_TAGGING_LABELS = Arrays.asList(
        TaggingLabels.TABLE_MARKER, TaggingLabels.CITATION_MARKER, TaggingLabels.FIGURE_MARKER,
        TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION, TaggingLabels.EQUATION_LABEL
    );

    public static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
        TaggingLabels.CITATION_MARKER,
        TaggingLabels.FIGURE_MARKER,
        TaggingLabels.TABLE_MARKER,
        TaggingLabels.EQUATION_MARKER);

    /**
     * In the following, we process the relevant textual content of the document
     * for refining the process based on structures, we need to filter
     * segment of interest (e.g. header, body, annex) and possibly apply
     * the corresponding model to further filter by structure types
     */
    public static void processDocument(Document doc, Consumer<List<LayoutToken>> closure) {
        EngineParsers parsers = new EngineParsers();

        List<List<LayoutToken>> outputLayoutTokens = new ArrayList<>();

        // from the header, we are interested in title, abstract and keywords
        SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
        if (documentParts != null) {
            Pair<String, List<LayoutToken>> headerStruct = parsers.getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
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
                if (isNotEmpty(titleTokens)) {
                    outputLayoutTokens.add(normaliseAndCleanup(titleTokens));
                }

                // abstract
                List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                if (isNotEmpty(abstractTokens)) {
                    abstractTokens = BiblioItem.cleanAbstractLayoutTokens(abstractTokens);
                    Pair<String, List<LayoutToken>> abstractTokenPostProcessed = parsers.getFullTextParser().processShort(abstractTokens, doc);
                    List<LayoutToken> restructuredLayoutTokens = abstractTokenPostProcessed.getRight();
//                    addSpaceAtTheEnd(abstractTokens, restructuredLayoutTokens);
                    outputLayoutTokens.add(normaliseAndCleanup(restructuredLayoutTokens));
                }

                // keywords
                List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                if (isNotEmpty(keywordTokens)) {
                    outputLayoutTokens.add(normaliseAndCleanup(keywordTokens));
                }
            }
        }

        // we can process all the body, in the future figure and table could be the
        // object of more refined processing
        documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
        if (documentParts != null) {
            Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, documentParts);

            String fulltextTaggedRawResult = null;
            if (featSeg != null) {
                String featureText = featSeg.getLeft();
                LayoutTokenization layoutTokenization = featSeg.getRight();

                if (StringUtils.isNotEmpty(featureText)) {
                    fulltextTaggedRawResult = parsers.getFullTextParser().label(featureText);
                }

                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, fulltextTaggedRawResult,
                    layoutTokenization.getTokenization(), true);

                TaggingTokenCluster previousCluster = null;

                List<LayoutToken> outputBodyLayoutTokens = new ArrayList<>();

                //Iterate and exclude figures, tables, equations and citation markers
                for (TaggingTokenCluster cluster : clusteror.cluster()) {
                    final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();

                    if (EXCLUDED_TAGGING_LABELS.contains(cluster.getTaggingLabel())) {
                        previousCluster = cluster;
                    } else if (cluster.getTaggingLabel().equals(TaggingLabels.FIGURE)) {
                        //apply the figure model to only get the caption
                        final Figure processedFigure = parsers.getFigureParser()
                            .processing(cluster.concatTokens(), cluster.getFeatureBlock());

                        List<LayoutToken> tokens = processedFigure.getCaptionLayoutTokens();
                        List<LayoutToken> normalisedLayoutTokens = normaliseAndCleanup(tokens);
                        if (isNotEmpty(normalisedLayoutTokens)) {
                            if (!isNewParagraphFigureCaption(previousCluster, normalisedLayoutTokens)) {
                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            } else {
                                if (isNotEmpty(outputBodyLayoutTokens)) {
                                    outputLayoutTokens.add(normaliseAndCleanup(outputBodyLayoutTokens));
                                    outputBodyLayoutTokens = new ArrayList<>();
                                }
                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            }
                            previousCluster = cluster;
                        }

                    } /*else if (cluster.getTaggingLabel().equals(TaggingLabels.TABLE)) {
                        //apply the table model to only get the caption/description
                        final Table processedTable = parsers.getTableParser().processing(cluster.concatTokens(), cluster.getFeatureBlock());
                        List<LayoutToken> tokens = processedTable.getFullDescriptionTokens();
                        List<LayoutToken> normalisedLayoutTokens = normaliseAndCleanup(tokens);
                        if(isNotEmpty(normalisedLayoutTokens)) {
                            *//*
                            if (!isNewParagraphTableCaption(previousCluster, normalisedLayoutTokens)) {
                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            } else {
                                if (isNotEmpty(outputBodyLayoutTokens)) {
                                    outputLayoutTokens.add(outputBodyLayoutTokens);
                                    outputBodyLayoutTokens = new ArrayList<>();
                                }
                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            }
                             *//*
                            if (isNotEmpty(outputBodyLayoutTokens)) {
                                outputLayoutTokens.add(outputBodyLayoutTokens);
                                outputBodyLayoutTokens = new ArrayList<>();
                            }
                            outputLayoutTokens.add(normalisedLayoutTokens);
                            previousCluster = cluster;
                        }
                    } */ else {
                        // extract all the layout tokens from the cluster as a list
                        List<LayoutToken> tokens = labeledTokensContainers.stream()
                            .map(LabeledTokensContainer::getLayoutTokens)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                        Pair<String, List<LayoutToken>> body = parsers.getFullTextParser().processShort(tokens, doc);
                        List<LayoutToken> restructuredTokens = body.getRight();
                        //addSpaceAtTheEnd(tokens, restructuredTokens);
                        List<LayoutToken> normalisedLayoutTokens = normaliseAndCleanup(restructuredTokens);

                        if (isNotEmpty(normalisedLayoutTokens)) {
                            if (cluster.getTaggingLabel().equals(TaggingLabels.SECTION)) {
                                if (isNotEmpty(outputBodyLayoutTokens)) {
                                    outputLayoutTokens.add(normaliseAndCleanup(outputBodyLayoutTokens));
                                    outputBodyLayoutTokens = new ArrayList<>();
                                }
                            } else {
                                // is new paragraph?
                                if (isNewParagraph(previousCluster, normalisedLayoutTokens)) {

                                    if (isNotEmpty(outputBodyLayoutTokens)) {
                                        outputLayoutTokens.add(normaliseAndCleanup(outputBodyLayoutTokens));
                                        outputBodyLayoutTokens = new ArrayList<>();
                                    }
                                }

                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            }
                            previousCluster = cluster;
                        }
                    }

                }

                if (isNotEmpty(outputBodyLayoutTokens)) {
                    outputLayoutTokens.add(normaliseAndCleanup(outputBodyLayoutTokens));
                }
            }
            // we don't process references (although reference titles could be relevant)
            // acknowledgement?

            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {

                List<LayoutToken> tokens = Document.getTokenizationParts(documentParts, doc.getTokenizations());
                Pair<String, List<LayoutToken>> annex = parsers.getFullTextParser().processShort(tokens, doc);
                if (annex != null) {
                    List<LayoutToken> restructuredLayoutTokens = annex.getRight();
//                    addSpaceAtTheEnd(tokens, restructuredLayoutTokens);
                    outputLayoutTokens.add(normaliseAndCleanup(restructuredLayoutTokens));
                }
            }

            // process
            outputLayoutTokens.stream().forEach(closure::accept);
        }
    }

    /**
     * Check if to create a new paragraphs - applied to the PARAGRAPH label
     */
    public static boolean isNewParagraph(TaggingTokenCluster previousCluster, List<LayoutToken> currentClusterLayoutTokens) {

        // create a new paragraph when:
        // previous cluster is not empty  -> I don't need a new paragraph at the beginning
        // previous cluster is not a marker, a table, a figure, an equation or equation label  -> they might be actual text.
        boolean contentResult = (previousCluster != null
            && (!MARKER_LABELS.contains(previousCluster.getTaggingLabel())
//            && previousCluster.getTaggingLabel() != TaggingLabels.TABLE
            && previousCluster.getTaggingLabel() != TaggingLabels.FIGURE
            && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION
            && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION_LABEL
        )
        ) || isEmpty(currentClusterLayoutTokens);

        return contentResult;

        // working with coordinates.

//        LabeledTokensContainer lastContainer = Iterables.getLast(previousCluster.getLabeledTokensContainers());
//        for (int x = lastContainer.getLayoutTokens().size() - 1; x >= 0; x--) {
//            if(lastContainer.getLayoutTokens().get(x).getText())
//        }
//        LayoutToken lastClusterLastLayoutToken = Iterables.getLast(.getLayoutTokens());
//        double lastClusterLastTokenYCoordinate = lastClusterLastLayoutToken.getY();
//        double lastClusterLastTokenPage = lastClusterLastLayoutToken.getPage();
//
//        LayoutToken currentClusterFirstLayoutToken = Iterables.getFirst(currentClusterLayoutTokens, new LayoutToken());
//        double currentClusterFirstTokenYCoordinate = currentClusterFirstLayoutToken.getY();
//        double currentClusterFirstTokenPage = currentClusterFirstLayoutToken.getPage();
//
//        if (currentClusterFirstTokenPage != lastClusterLastTokenPage ||
//            lastClusterLastTokenYCoordinate > currentClusterFirstTokenYCoordinate) {
//            return contentResult;
//        } else {
//            return lastClusterLastTokenYCoordinate != currentClusterFirstTokenYCoordinate;
//        }
    }

    /**
     * Check if to create a new paragraphs - applied to the TABLE label
     */
    public static boolean isNewParagraphTableCaption(TaggingTokenCluster previousCluster, List<LayoutToken> currentClusterLayoutTokens) {

        boolean contentResult = previousCluster != null
            &&
            (
                !MARKER_LABELS.contains(previousCluster.getTaggingLabel())
                    && (previousCluster.getTaggingLabel() != TaggingLabels.FIGURE
                    && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION
                    && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION_LABEL
                )
            ) || isEmpty(currentClusterLayoutTokens);

        if (previousCluster == null) {
            return contentResult;
        }

        // working with coordinates.
        LayoutToken lastClusterLastLayoutToken = Iterables.getLast(Iterables.getLast(previousCluster.getLabeledTokensContainers()).getLayoutTokens());
        double lastClusterLastTokenYCoordinate = lastClusterLastLayoutToken.getY();
        double lastClusterLastTokenPage = lastClusterLastLayoutToken.getPage();

        LayoutToken currentClusterFirstLayoutToken = Iterables.getFirst(currentClusterLayoutTokens, new LayoutToken());
        double currentClusterFirstTokenYCoordinate = currentClusterFirstLayoutToken.getY();
        double currentClusterFirstTokenPage = currentClusterFirstLayoutToken.getPage();

        if (currentClusterFirstTokenPage != lastClusterLastTokenPage ||
            lastClusterLastTokenYCoordinate > currentClusterFirstTokenYCoordinate) {
            return contentResult;
        } else {
            return lastClusterLastTokenYCoordinate != currentClusterFirstTokenYCoordinate;
        }
    }

    /**
     * Check if to create a new paragraphs - applied to the FIGURE label
     */
    public static boolean isNewParagraphFigureCaption(TaggingTokenCluster previousCluster, List<LayoutToken> currentClusterLayoutTokens) {

        boolean contentResult = previousCluster != null
            &&
            (
                !MARKER_LABELS.contains(previousCluster.getTaggingLabel())
                    && (previousCluster.getTaggingLabel() != TaggingLabels.TABLE
                    && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION
                    && previousCluster.getTaggingLabel() != TaggingLabels.EQUATION_LABEL
                )
            ) || isEmpty(currentClusterLayoutTokens);

        if (previousCluster == null) {
            return contentResult;
        }

        // working with coordinates.
        double lastClusterLastTokenYCoordinate = Iterables.getLast(Iterables.getLast(previousCluster.getLabeledTokensContainers()).getLayoutTokens()).getY();
        double currentClusterFirstTokenYCoordinate = Iterables.getFirst(currentClusterLayoutTokens, new LayoutToken()).getY();

        if (lastClusterLastTokenYCoordinate > currentClusterFirstTokenYCoordinate) {
            return contentResult;
        } else {
            return lastClusterLastTokenYCoordinate != currentClusterFirstTokenYCoordinate;
        }
    }

    private static void addSpaceAtTheEnd(List<LayoutToken> originalLayoutTokens, List<LayoutToken> normalisedLayoutTokens) {
        if (isEmpty(originalLayoutTokens) || isEmpty(normalisedLayoutTokens)) return;

        if (Iterables.getLast(originalLayoutTokens).getText().equals(" ")
            && !Iterables.getLast(normalisedLayoutTokens).getText().equals(" ")) {
            LayoutToken copyLastToken = new LayoutToken(Iterables.getLast(originalLayoutTokens));

            copyLastToken.setX(-1);
            copyLastToken.setY(-1);
            copyLastToken.setText(" ");
            copyLastToken.setOffset(copyLastToken.getOffset() + copyLastToken.getText().length());

            normalisedLayoutTokens.add(copyLastToken);
        }
    }

    private static boolean shouldAttachedToPreviousChunk(List<LayoutToken> normalisedLayoutTokens) {
        return isNotEmpty(normalisedLayoutTokens)
            && (
            !Character.isUpperCase(normalisedLayoutTokens.get(0).getText().codePointAt(0))
                || StringUtils.equalsIgnoreCase(normalisedLayoutTokens.get(0).getText(), " ")
        );
    }

    /**
     * transform breaklines in spaces and remove duplicated spaces
     */
    protected static List<LayoutToken> normaliseAndCleanup(List<LayoutToken> layoutTokens) {
        if (isEmpty(layoutTokens)) {
            return new ArrayList<>();
        }

        //De-hypenisation and converting break lines with spaces.
        List<LayoutToken> bodyLayouts = layoutTokens
            .stream()
            .map(m -> {
                m.setText(StringUtils.replace(m.getText(), "\r\n", " "));
                m.setText(StringUtils.replace(m.getText(), "\n", " "));
                return m;
            })
            .collect(Collectors.toList());

        //Removing duplicated spaces
        List<LayoutToken> cleanedTokens = IntStream
            .range(0, bodyLayouts.size())
            .filter(i -> (
                    (i < bodyLayouts.size() - 1 && (!bodyLayouts.get(i).getText().equals("\n") || !bodyLayouts.get(i).getText().equals(" "))
                        && !StringUtils.equals(bodyLayouts.get(i).getText(), bodyLayouts.get(i + 1).getText())
                    ) || i == bodyLayouts.size() - 1
                )
            )
            .mapToObj(bodyLayouts::get)
            .collect(Collectors.toList());

        // Correcting offsets after having removed certain tokens
        IntStream
            .range(1, cleanedTokens.size())
            .forEach(i -> {
                int expectedFollowingOffset = cleanedTokens.get(i - 1).getOffset()
                    + StringUtils.length(cleanedTokens.get(i - 1).getText());

                if (expectedFollowingOffset != cleanedTokens.get(i).getOffset()) {
                    LOGGER.debug("Correcting offsets " + i + " from " + cleanedTokens.get(i).getOffset() + " to " + expectedFollowingOffset);
                    cleanedTokens.get(i).setOffset(expectedFollowingOffset);
                }
            });

        return cleanedTokens;
    }

}
