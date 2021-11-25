package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Figure;
import org.grobid.core.data.document.BiblioInfo;
import org.grobid.core.data.document.DocumentBlock;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.AdditionalLayoutTokensUtil;
import org.grobid.core.utilities.Consolidation;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.SentenceUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GrobidPDFEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidPDFEngine.class);

    private static final List<TaggingLabel> EXCLUDED_TAGGING_LABELS = Arrays.asList(
        TaggingLabels.TABLE_MARKER, TaggingLabels.TABLE, TaggingLabels.CITATION_MARKER, TaggingLabels.FIGURE_MARKER,
        TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION, TaggingLabels.EQUATION_LABEL
    );

    public static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
        TaggingLabels.CITATION_MARKER,
        TaggingLabels.FIGURE_MARKER,
        TaggingLabels.TABLE_MARKER,
        TaggingLabels.EQUATION_MARKER);

    /**
     * @use processDocument(Document doc, GrobidAnalysisConfig config, Consumer < DocumentBlock > closure) {
     */
    @Deprecated
    public static BiblioInfo processDocument(Document doc, Consumer<DocumentBlock> closure) {
        GrobidAnalysisConfig config = GrobidAnalysisConfig.builder()
            .consolidateHeader(1)
            .withSentenceSegmentation(true)
            .build();
        return processDocument(doc, config, closure);
    }

    /**
     * In the following, we process the relevant textual content of the document
     * for refining the process based on structures, we need to filter
     * segment of interest (e.g. header, body, annex) and possibly apply
     * the corresponding model to further filter by structure types
     */
    public static BiblioInfo processDocument(Document doc, GrobidAnalysisConfig config, Consumer<DocumentBlock> closure) {
        EngineParsers parsers = new EngineParsers();

        List<DocumentBlock> documentBlocks = new ArrayList<>();
        BiblioInfo biblioInfo = new BiblioInfo();

        // from the header, we are interested in title, abstract and keywords
        SortedSet<DocumentPiece> headerDocumentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
        if (headerDocumentParts != null) {
            BiblioItem resHeader = new BiblioItem();


            parsers.getHeaderParser().processingHeaderSection(config, doc, resHeader, false);

            // title
            List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
            if (isNotEmpty(titleTokens)) {
                documentBlocks.add(new DocumentBlock(normaliseAndCleanup(titleTokens), DocumentBlock.SECTION_HEADER,
                    DocumentBlock.SUB_SECTION_TITLE
                ));
                biblioInfo.setTitle(resHeader.getTitle());
            }

            // abstract
            List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
            if (isNotEmpty(abstractTokens)) {
                abstractTokens = BiblioItem.cleanAbstractLayoutTokens(abstractTokens);
                Pair<String, List<LayoutToken>> abstractTokenPostProcessed = parsers.getFullTextParser().processShort(abstractTokens, doc);
                List<LayoutToken> restructuredLayoutTokens = abstractTokenPostProcessed.getRight();
//                    addSpaceAtTheEnd(abstractTokens, restructuredLayoutTokens);

                documentBlocks.add(new DocumentBlock(normaliseAndCleanup(restructuredLayoutTokens), DocumentBlock.SECTION_HEADER,
                    DocumentBlock.SUB_SECTION_ABSTRACT));
            }

            // keywords
            List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
            if (isNotEmpty(keywordTokens)) {
                documentBlocks.add(new DocumentBlock(normaliseAndCleanup(keywordTokens), DocumentBlock.SECTION_HEADER,
                    DocumentBlock.SUB_SECTION_KEYWORDS));
            }

            // Other bibliographic data
            if (isNotBlank(resHeader.getAuthors())) {
                String authors = resHeader.getFullAuthors().stream()
                    .map(a -> Stream.of(StringUtils.trimToEmpty(a.getFirstName()), StringUtils.trimToEmpty(a.getMiddleName()), StringUtils.trimToEmpty(a.getLastName()))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining(", "));
                biblioInfo.setAuthors(authors);
            }
            if (isNotBlank(resHeader.getDOI())) {
                biblioInfo.setDoi(resHeader.getDOI());
            }
            if (resHeader.getNormalizedPublicationDate() != null) {
                if (resHeader.getNormalizedPublicationDate().getYear() > 0) {
                    biblioInfo.setYear(resHeader.getNormalizedPublicationDate().getYear());
                }
            }
            if (isNotBlank(resHeader.getPublisher())) {
                biblioInfo.setPublisher(resHeader.getPublisher());
            }
            if (isNotBlank(resHeader.getJournal())) {
                biblioInfo.setJournal(resHeader.getJournal());
            }

        }

        // citation processing
        // consolidation, if selected, is not done individually for each citation but 
        // in a second stage for all citations which is much faster
        List<BibDataSet> resCitations = parsers.getCitationParser().
            processingReferenceSection(doc, parsers.getReferenceSegmenterParser(), 0);

        // consolidate the set
//        if (config.getConsolidateCitations() != 0 && resCitations != null) {
//            Consolidation consolidator = Consolidation.getInstance();
//            if (consolidator.getCntManager() == null)
//                consolidator.setCntManager(Engine.getCntManager());
//            try {
//                Map<Integer,BiblioItem> resConsolidation = consolidator.consolidate(resCitations);
//                for(int i=0; i<resCitations.size(); i++) {
//                    BiblioItem resCitation = resCitations.get(i).getResBib();
//                    BiblioItem bibo = resConsolidation.get(i);
//                    if (bibo != null) {
//                        if (config.getConsolidateCitations() == 1)
//                            BiblioItem.correct(resCitation, bibo);
//                        else if (config.getConsolidateCitations() == 2)
//                            BiblioItem.injectIdentifiers(resCitation, bibo);
//                    }
//                }
//            } catch(Exception e) {
//                throw new GrobidException(
//                    "An exception occured while running consolidation on bibliographical references.", e);
//            }
//        }

        doc.setBibDataSets(resCitations);
        // we can process all the body, in the future figure and table could be the
        // object of more refined processing
        SortedSet<DocumentPiece> bodyDocumentParts = doc.getDocumentPart(SegmentationLabels.BODY);
        if (bodyDocumentParts != null) {
            Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, bodyDocumentParts);

            String fulltextTaggedRawResult = null;
            if (featSeg != null) {
                String featureText = featSeg.getLeft();
                LayoutTokenization layoutTokenization = featSeg.getRight();

                if (StringUtils.isNotEmpty(featureText)) {
                    fulltextTaggedRawResult = parsers.getFullTextParser().label(featureText);
//                    postProcessCallout??
                }

                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, fulltextTaggedRawResult,
                    layoutTokenization.getTokenization(), true);

                TaggingTokenCluster previousCluster = null;

                List<LayoutToken> outputBodyLayoutTokens = new ArrayList<>();
                List<List<LayoutToken>> markersLayoutTokens = new ArrayList<>();

                //Iterate and exclude figures, tables, equations and citation markers
                for (TaggingTokenCluster cluster : clusteror.cluster()) {
                    final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();

                    //We collect all the markers that will be used as sentence segmentation 
                    if (MARKER_LABELS.contains(cluster.getTaggingLabel())) {
                        List<LayoutToken> tokens = cluster.concatTokens();
                        TaggingLabel label = cluster.getTaggingLabel();
                        markersLayoutTokens.add(tokens);
                    }

                    if (EXCLUDED_TAGGING_LABELS.contains(cluster.getTaggingLabel())) {
                        if (MARKER_LABELS.contains(cluster.getTaggingLabel())) {
                            List<LayoutToken> normalisedLayoutTokens = normaliseAndCleanup(cluster.concatTokens());
                            outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                        }
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
                                    documentBlocks.add(new DocumentBlock(normaliseAndCleanup(outputBodyLayoutTokens), DocumentBlock.SECTION_BODY,
                                        DocumentBlock.SUB_SECTION_FIGURE, new ArrayList<>(), markersLayoutTokens));
                                    outputBodyLayoutTokens = new ArrayList<>();
                                    markersLayoutTokens = new ArrayList<>();
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
                                //Since we merge sections and paragraphs, we avoid adding sections titles if
                                // there is no text already in the same block
                                if (isNotEmpty(outputBodyLayoutTokens)) {
                                    documentBlocks.add(new DocumentBlock(normaliseAndCleanup(outputBodyLayoutTokens), DocumentBlock.SECTION_BODY,
                                        DocumentBlock.SUB_SECTION_PARAGRAPH, new ArrayList<>(), markersLayoutTokens));

                                    outputBodyLayoutTokens = new ArrayList<>();
                                    markersLayoutTokens = new ArrayList<>();
                                }
                            } else {
                                // is new paragraph?
                                if (isNewParagraph(previousCluster, normalisedLayoutTokens)) {

                                    if (isNotEmpty(outputBodyLayoutTokens)) {
                                        documentBlocks.add(new DocumentBlock(normaliseAndCleanup(outputBodyLayoutTokens), DocumentBlock.SECTION_BODY,
                                            DocumentBlock.SUB_SECTION_PARAGRAPH, new ArrayList<>(), markersLayoutTokens));
                                        outputBodyLayoutTokens = new ArrayList<>();
                                        markersLayoutTokens = new ArrayList<>();
                                    }
                                }

                                outputBodyLayoutTokens.addAll(normalisedLayoutTokens);
                            }
                            previousCluster = cluster;
                        }
                    }
                }

                if (isNotEmpty(outputBodyLayoutTokens)) {
                    documentBlocks.add(new DocumentBlock(normaliseAndCleanup(outputBodyLayoutTokens), DocumentBlock.SECTION_BODY, "", new ArrayList<>(), markersLayoutTokens));
                }
            }
            // we don't process references (although reference titles could be relevant)
            // acknowledgement?

            // we can process annexes
            SortedSet<DocumentPiece> annexDocumentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (annexDocumentParts != null) {

                List<LayoutToken> tokens = Document.getTokenizationParts(annexDocumentParts, doc.getTokenizations());
                Pair<String, List<LayoutToken>> annex = parsers.getFullTextParser().processShort(tokens, doc);
                if (annex != null) {
                    List<LayoutToken> restructuredLayoutTokens = annex.getRight();
//                    addSpaceAtTheEnd(tokens, restructuredLayoutTokens);
                    documentBlocks.add(new DocumentBlock(normaliseAndCleanup(restructuredLayoutTokens), DocumentBlock.SECTION_ANNEX,
                        DocumentBlock.SUB_SECTION_PARAGRAPH, new ArrayList<>(), new ArrayList<>()));
                }
            }

            // Sentence splitting using reference, then remove them from the text

            List<DocumentBlock> documentBlocksBySentences = new ArrayList<>();

            documentBlocks.stream().forEach(documentBlock -> {

                List<Pair<Integer, Integer>> markersExtremitiesAsIndex = new ArrayList<>();
                List<OffsetPosition> markersPositionsAsOffsetsInText = new ArrayList<>();

                String section = documentBlock.getSection();
                String subSection = documentBlock.getSubSection();

                if (isNotEmpty(documentBlock.getMarkers())) {
                    markersExtremitiesAsIndex = documentBlock
                        .getMarkers()
                        .stream()
                        .map(markerLayoutTokens -> AdditionalLayoutTokensUtil.getExtremitiesAsIndex(documentBlock.getLayoutTokens(),
                            AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(markerLayoutTokens),
                            AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(markerLayoutTokens)))
                        .collect(Collectors.toList());
                    
                    // We need adjust overlapping markers
                    if (markersExtremitiesAsIndex.size() > 1) {
                        for (int i = 0; i < markersExtremitiesAsIndex.size() - 1; i++) {
                            if (markersExtremitiesAsIndex.get(i).getRight() > markersExtremitiesAsIndex.get(i + 1).getLeft()) {
                                markersExtremitiesAsIndex.set(i, Pair.of(markersExtremitiesAsIndex.get(i).getLeft(), markersExtremitiesAsIndex.get(i + 1).getLeft()));
                            }
                        }
                    }

                    markersPositionsAsOffsetsInText = getMarkersAsOffsets(documentBlock, markersExtremitiesAsIndex);
                }

                List<Pair<Integer, Integer>> indexesPairs = getSentencesOffsetsAsIndexes(documentBlock, markersPositionsAsOffsetsInText);

                int cumulatedIndexes = 0;
                for (Pair<Integer, Integer> pair : indexesPairs) {
                    DocumentBlock newDocumentBlock = new DocumentBlock(documentBlock);
                    List<LayoutToken> sentenceTokens = documentBlock.getLayoutTokens().subList(pair.getLeft(), pair.getRight());
                    final Integer cumulatedIndexes_ = cumulatedIndexes;
                    List<Integer> indexesContainingReferenceMarkers = markersExtremitiesAsIndex.stream()
                        .filter(m -> m.getLeft() > cumulatedIndexes_ && m.getRight() <= cumulatedIndexes_ + sentenceTokens.size())
                        .flatMap(p -> IntStream.range(p.getLeft(), p.getRight()).boxed().collect(Collectors.toList()).stream())
                        .collect(Collectors.toList());

                    cumulatedIndexes += sentenceTokens.size();

                    //We remove the markers from the layout token list 
                    final List<LayoutToken> newSentenceTokens = new ArrayList<>();
                    IntStream.range(0, sentenceTokens.size())
                        .forEach(index -> {
                            if (!indexesContainingReferenceMarkers.contains(index)) {
                                newSentenceTokens.add(sentenceTokens.get(index));
                            }
                        });
                    newDocumentBlock.setLayoutTokens(newSentenceTokens);
                    newDocumentBlock.setSection(section);
                    newDocumentBlock.setSubSection(subSection);
                    documentBlocksBySentences.add(newDocumentBlock);
                }
            });


            // process
            IntStream.range(0, documentBlocksBySentences.size())
                .forEach(i -> closure.accept(documentBlocksBySentences.get(i)));
        }

        return biblioInfo;
    }

    private static List<Pair<Integer, Integer>> getSentencesOffsetsAsIndexes(DocumentBlock documentBlock, List<OffsetPosition> markersPositionsAsOffsetsInText) {
        List<String> tokensAsStringList = documentBlock.getLayoutTokens().stream().map(LayoutToken::getText).collect(Collectors.toList());
        String text = String.join("", tokensAsStringList);
        List<OffsetPosition> sentencesPositions = SentenceUtilities.getInstance()
            .runSentenceDetection(text, markersPositionsAsOffsetsInText, documentBlock.getLayoutTokens(), new Language("en"));

        List<Pair<Integer, Integer>> indexesPairs = AdditionalLayoutTokensUtil
            .fromOffsetsToIndexesOfTokensWithSpaces(sentencesPositions, tokensAsStringList);
        return indexesPairs;
    }

    private static List<OffsetPosition> getMarkersAsOffsets(DocumentBlock documentBlock, List<Pair<Integer, Integer>> markersExtremitiesAsIndex) {
        int lastIndex = 0;
        List<OffsetPosition> markersPositionsAsOffsetsInText = new ArrayList<>();

        for (Pair<Integer, Integer> markersExtremities : markersExtremitiesAsIndex) {
            List<LayoutToken> layoutTokensFirst = documentBlock.getLayoutTokens().subList(lastIndex, markersExtremities.getLeft());
            String textBefore = layoutTokensFirst
                .stream()
                .map(LayoutToken::getText)
                .collect(Collectors.joining(""));

            List<LayoutToken> layoutTokensSecond = documentBlock.getLayoutTokens().subList(markersExtremities.getLeft(), markersExtremities.getRight());
            String textAfter = layoutTokensSecond
                .stream()
                .map(LayoutToken::getText)
                .collect(Collectors.joining(""));

            markersPositionsAsOffsetsInText.add(new OffsetPosition(textBefore.length(), textBefore.length() + textAfter.length()));
            lastIndex = markersExtremities.getRight();
        }
        return markersPositionsAsOffsetsInText;
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
                    LOGGER.trace("Correcting offsets " + i + " from " + cleanedTokens.get(i).getOffset() + " to " + expectedFollowingOffset);
                    cleanedTokens.get(i).setOffset(expectedFollowingOffset);
                }
            });

        return cleanedTokens;
    }

}
