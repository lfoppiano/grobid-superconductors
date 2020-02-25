package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wipo.analyzers.wipokr.utils.StringUtil.*;

@Singleton
public class AggregatedProcessing {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedProcessing.class);

    private EngineParsers parsers;

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private SentenceSegmenter sentenceSegmenter;


    public AggregatedProcessing(SuperconductorsParser superconductorsParser, QuantityParser quantityParser) {
        this.superconductorsParser = superconductorsParser;
        this.quantityParser = quantityParser;
        this.sentenceSegmenter = new SentenceSegmenter();
        parsers = new EngineParsers();
    }

    @Inject
    public AggregatedProcessing(SuperconductorsParser superconductorsParser) {
        this(superconductorsParser, QuantityParser.getInstance(true));
    }

    private List<Superconductor> linkSuperconductorsWithTc(List<Superconductor> superconductors, List<Measurement> measurements, List<LayoutToken> tokens) {
        if (measurements.size() == 0)
            return superconductors;


        List<Measurement> criticalTemperatures = measurements
            .stream()
            .filter(measurement -> measurement.getQuantifiedObject() != null
                && StringUtils.equalsIgnoreCase("Critical temperature", measurement.getQuantifiedObject().getNormalizedName()))
            .collect(Collectors.toList());

        List<Pair<Quantity, Measurement>> criticalTemperaturesFlatten = MeasurementUtils.flattenMeasurements(criticalTemperatures);

        List<Measurement> assignedTemperature = new ArrayList<>();

        for (Superconductor superconductor : superconductors) {
            List<LayoutToken> layoutTokensSupercon = superconductor.getLayoutTokens();
            Pair<Integer, Integer> extremitiesSuperconductor = getExtremitiesAsIndex(tokens,
                layoutTokensSupercon.get(0).getOffset(), layoutTokensSupercon.get(layoutTokensSupercon.size() - 1).getOffset());

            extremitiesSuperconductor = adjustExtremities(extremitiesSuperconductor, superconductor.getLayoutTokens(), tokens);

            int superconductorLayoutTokenLowerOffset = superconductor.getLayoutTokens().get(0).getOffset();
            int superconductorLayoutTokenHigherOffset = superconductor.getLayoutTokens().get(superconductor.getLayoutTokens().size() - 1).getOffset()
                + superconductor.getLayoutTokens().get(superconductor.getLayoutTokens().size() - 1).getText().length();

            double superconductorCentroidOffset = ((double) (superconductorLayoutTokenHigherOffset + superconductorLayoutTokenLowerOffset)) / 2.0;

            List<Pair<Quantity, Measurement>> criticalTemperaturesFlattenSorted = criticalTemperaturesFlatten.stream().sorted((o1, o2) -> {
                Quantity quantityT1 = o1.getLeft();
                int t1LowerLayoutTokenOffset = quantityT1.getLayoutTokens().get(0).getOffset();
                int t1HigherLayoutTokenOffset = quantityT1.getLayoutTokens().get(quantityT1.getLayoutTokens().size() - 1).getOffset()
                    + quantityT1.getLayoutTokens().get(quantityT1.getLayoutTokens().size() - 1).getText().length();

                double t1CentroidOffset = ((double) (t1HigherLayoutTokenOffset + t1LowerLayoutTokenOffset)) / 2.0;

                Quantity quantityT2 = o2.getLeft();
                int t2LowerLayoutTokenOffset = quantityT2.getLayoutTokens().get(0).getOffset();
                int t2HigherLayoutTokenOffset = quantityT2.getLayoutTokens().get(quantityT2.getLayoutTokens().size() - 1).getOffset()
                    + quantityT2.getLayoutTokens().get(quantityT2.getLayoutTokens().size() - 1).getText().length();

                double t2CentroidOffset = ((double) (t2HigherLayoutTokenOffset + t2LowerLayoutTokenOffset)) / 2.0;

                double distanceT1Supercon = Math.abs(t1CentroidOffset - superconductorCentroidOffset);
                double distanceT2Supercon = Math.abs(t2CentroidOffset - superconductorCentroidOffset);

                if (distanceT1Supercon > distanceT2Supercon) {
                    return 1;
                } else if (distanceT2Supercon > distanceT1Supercon) {
                    return -1;
                } else {
                    return 0;
                }
            }).collect(Collectors.toList());

            List<LayoutToken> superconductorLayoutTokenWindow = tokens.subList(extremitiesSuperconductor.getLeft(), extremitiesSuperconductor.getRight());
            int offsetWindowStart = superconductorLayoutTokenWindow.get(0).getOffset();
            LayoutToken lastToken = superconductorLayoutTokenWindow.get(superconductorLayoutTokenWindow.size() - 1);
            int offsetWindowEnd = lastToken.getOffset();

            for (Pair<Quantity, Measurement> criticalTemperature : criticalTemperaturesFlattenSorted) {
                if (assignedTemperature.contains(criticalTemperature.getRight())) {
                    continue;
                }

                List<LayoutToken> criticalTemperatureLayoutTokens = criticalTemperature.getLeft().getLayoutTokens();
                int temperatureOffsetStart = criticalTemperatureLayoutTokens.get(0).getOffset();
                int temperatureOffsetEnd = criticalTemperatureLayoutTokens.get(criticalTemperatureLayoutTokens.size() - 1).getOffset();
                if ((temperatureOffsetStart < offsetWindowStart && temperatureOffsetEnd >= offsetWindowStart)
                    || (temperatureOffsetEnd > offsetWindowEnd && temperatureOffsetStart <= offsetWindowEnd)
                    || (temperatureOffsetStart >= offsetWindowStart && temperatureOffsetEnd < offsetWindowEnd)
                    || (temperatureOffsetStart > offsetWindowStart && temperatureOffsetEnd <= offsetWindowEnd)) {
                    superconductor.setCriticalTemperatureMeasurement(criticalTemperature.getRight());
                    assignedTemperature.add(criticalTemperature.getRight());
                    break;
                }
            }
        }

        return superconductors;
    }

    private Pair<Integer, Integer> getContainedSentenceAsIndex(List<LayoutToken> entityLayoutTokens, List<LayoutToken> tokens) {

        List<List<LayoutToken>> sentences = this.sentenceSegmenter.getSentencesAsLayoutToken(tokens);

        int entityOffsetStart = entityLayoutTokens.get(0).getOffset();
        int entityOffsetEnd = Iterables.getLast(entityLayoutTokens).getOffset();

        //In which sentence is the entity?
        Optional<List<LayoutToken>> entitySentenceOptional = sentences
            .stream()
            .filter(CollectionUtils::isNotEmpty)
            .filter(sentence -> {
                int sentenceStartOffset = Iterables.getFirst(sentence, null).getOffset();
                int sentenceEndOffset = Iterables.getLast(sentence).getOffset();

                return entityOffsetStart > sentenceStartOffset && entityOffsetEnd < sentenceEndOffset;
            })
            .findFirst();

        if (!entitySentenceOptional.isPresent()) {
            return Pair.of(0, tokens.size() - 1);
        }

        List<LayoutToken> sentence = entitySentenceOptional.get();

        return Pair.of(tokens.indexOf(sentence.get(0)), tokens.indexOf(Iterables.getLast(sentence)));
    }


    /**
     * we reduce the extremities window if going on a separate sentence
     **/
    private Pair<Integer, Integer> adjustExtremities(Pair<Integer, Integer> originalExtremities, List<LayoutToken> entityLayoutTokens, List<LayoutToken> tokens) {

        List<List<LayoutToken>> sentences = this.sentenceSegmenter.getSentencesAsLayoutToken(tokens);

        int entityOffsetStart = entityLayoutTokens.get(0).getOffset();
        int entityOffsetEnd = entityLayoutTokens.get(entityLayoutTokens.size() - 1).getOffset();

        //In which sentence is the entity?
        Optional<List<LayoutToken>> entitySentenceOptional = sentences
            .stream()
            .filter(CollectionUtils::isNotEmpty)
            .filter(sentence -> {
                int sentenceStartOffset = Iterables.getFirst(sentence, null).getOffset();
                int sentenceEndOffset = Iterables.getLast(sentence).getOffset();

                return entityOffsetEnd < sentenceEndOffset && entityOffsetStart > sentenceStartOffset;
            })
            .findFirst();


        if (!entitySentenceOptional.isPresent()) {
            return originalExtremities;
        }

        List<LayoutToken> superconductorSentence = entitySentenceOptional.get();


        int startSentenceOffset = superconductorSentence.get(0).getOffset();
        int endSentenceOffset = superconductorSentence.get(superconductorSentence.size() - 1).getOffset();

        //Get the layout tokens they correspond
        Optional<LayoutToken> first = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == startSentenceOffset).findFirst();
        Optional<LayoutToken> last = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == endSentenceOffset).findFirst();

        if (!first.isPresent() || !last.isPresent()) {
            return originalExtremities;
        }
        int newStart = originalExtremities.getLeft();
        int newEnd = originalExtremities.getRight();

        int adjustedStart = tokens.indexOf(first.get());
        int adjustedEnd = tokens.indexOf(last.get());

        if (originalExtremities.getLeft() < adjustedStart) {
            newStart = adjustedStart;
        }
        if (originalExtremities.getRight() > adjustedEnd) {
            newEnd = adjustedEnd;
        }

        return new ImmutablePair<>(newStart, newEnd);
    }

    public static int WINDOW_TC = Integer.MAX_VALUE;

    /* We work with offsets (so no need to increase by size of the text) and we return indexes in the token list */
    protected Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower,
                                                           int centroidOffsetHigher) {
        return getExtremitiesAsIndex(tokens, centroidOffsetLower, centroidOffsetHigher, WINDOW_TC);
    }


    protected Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower,
                                                           int centroidOffsetHigher, int windowlayoutTokensSize) {
        int start = 0;
        int end = tokens.size() - 1;

        List<LayoutToken> centralTokens = tokens.stream()
            .filter(layoutToken -> layoutToken.getOffset() == centroidOffsetLower
                || (layoutToken.getOffset() > centroidOffsetLower && layoutToken.getOffset() < centroidOffsetHigher))
            .collect(Collectors.toList());

        if (isNotEmpty(centralTokens)) {
            int centroidLayoutTokenIndexStart = tokens.indexOf(centralTokens.get(0));
            int centroidLayoutTokenIndexEnd = tokens.indexOf(centralTokens.get(centralTokens.size() - 1));

            if (centroidLayoutTokenIndexStart > windowlayoutTokensSize) {
                start = centroidLayoutTokenIndexStart - windowlayoutTokensSize;
            }
            if (end - centroidLayoutTokenIndexEnd > windowlayoutTokensSize) {
                end = centroidLayoutTokenIndexEnd + windowlayoutTokensSize + 1;
            }
        }

        return new ImmutablePair<>(start, end);
    }


    public PDFAnnotationResponse process(String text) {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        return annotate(tokens);
    }


    public PDFAnnotationResponse annotate(InputStream inputStream) {
        PDFAnnotationResponse PDFAnnotationResponse = new PDFAnnotationResponse();
        List<Superconductor> superconductorNamesList = new ArrayList<>();
        PDFAnnotationResponse.setEntities(superconductorNamesList);
        List<Measurement> temperaturesList = new ArrayList<>();
        PDFAnnotationResponse.setMeasurements(temperaturesList);

        Document doc = null;
        File file = null;

        try {
            file = IOUtilities.writeInputFile(inputStream);
            GrobidAnalysisConfig config =
                new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                    .build();
            DocumentSource documentSource =
                DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            GrobidPDFEngine.processDocument(doc, l -> PDFAnnotationResponse.extendEntities(annotate(l)));

            List<Page> pages = new ArrayList<>();
            for (org.grobid.core.layout.Page page : doc.getPages()) {
                pages.add(new Page(page.getHeight(), page.getWidth()));
            }

            PDFAnnotationResponse.setPages(pages);

        } catch (Exception e) {
            throw new GrobidException("Cannot process pdf file: " + file.getPath(), e);
        } finally {
            IOUtilities.removeTempFile(file);
        }

        return PDFAnnotationResponse;
    }

    protected List<Measurement> markCriticalTemperatures(List<Measurement> temperatures, List<LayoutToken> tokens, List<Pair<String, List<LayoutToken>>> tcExpressionList) {
        if (isEmpty(tokens)) {
            return temperatures;
        }

        if (CollectionUtils.isEmpty(tcExpressionList)) {
            tcExpressionList.add(new ImmutablePair<>("Tc", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Tc")));
            tcExpressionList.add(new ImmutablePair<>("tc", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("tc")));
            tcExpressionList.add(new ImmutablePair<>("T c", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("T c")));
            tcExpressionList.add(new ImmutablePair<>("t c", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("t c")));
        }

        List<Pair<String, List<LayoutToken>>> closedTcExpressionList = new ArrayList<>();
//        closedTcExpressionList.add(new ImmutablePair<>("at", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("at")));
//        closedTcExpressionList.add(new ImmutablePair<>("around", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("around")));
        closedTcExpressionList.add(new ImmutablePair<>("superconducts at", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("superconducts at")));
        closedTcExpressionList.add(new ImmutablePair<>("superconducts around", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("superconducts around")));
        closedTcExpressionList.add(new ImmutablePair<>("superconductivity at", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("superconductivity at")));
        closedTcExpressionList.add(new ImmutablePair<>("superconductivity around", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("superconductivity around")));

        // Expressions that mark a temperature as NON-Tc
        List<Pair<String, List<LayoutToken>>> closedNotTcExpressionList = new ArrayList<>();
        closedNotTcExpressionList.add(new ImmutablePair<>("higher", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("higher")));
        closedNotTcExpressionList.add(new ImmutablePair<>("lower", DeepAnalyzer.getInstance().tokenizeWithLayoutToken("lower")));

        List<Pair<Integer, Integer>> sentencesAsIndex = sentenceSegmenter.getSentencesAsIndex(tokens);
        List<Pair<Integer, Integer>> sentencesAsOffsetsPairs = sentenceSegmenter.getSentencesAsOffsetsPairs(tokens);

        outer:
        for (Measurement temperature : temperatures) {
            // We get the extremities without considering the unit
            Pair<Integer, Integer> extremities = MeasurementUtils.calculateExtremitiesAsIndex(temperature, tokens);

            int sentenceNumber = 0;
            if (sentencesAsOffsetsPairs.size() > 1) {
                for (int i = 0; i < sentencesAsOffsetsPairs.size(); i++) {
                    int sentenceStartOffset = sentencesAsOffsetsPairs.get(i).getLeft();
                    int sentenceEndOffset = sentencesAsOffsetsPairs.get(i).getRight();

                    if (tokens.get(extremities.getLeft()).getOffset() >= sentenceStartOffset && tokens.get(extremities.getRight() - 1).getOffset() < sentenceEndOffset) {
                        sentenceNumber = i;
                        break;
                    }
                }
            }

            Pair<Integer, Integer> sentenceBoundaryIndexes = sentencesAsIndex.get(sentenceNumber);

            // check if there is any non-TC expressions after the temperature:
            for (Pair<String, List<LayoutToken>> notTcExpression : closedNotTcExpressionList) {
                String nonTcString = notTcExpression.getLeft();

                List<LayoutToken> nonTcLayoutTokens = notTcExpression.getRight();
                String nonTcExpressionAsString = LayoutTokensUtil.toText(nonTcLayoutTokens);
                int nonTcExpressionSize = nonTcLayoutTokens.size();
                int indexTokenAfterToCompare = extremities.getRight() + 1;

                if (indexTokenAfterToCompare + nonTcExpressionSize < sentenceBoundaryIndexes.getRight()) {
                    String subString = LayoutTokensUtil.toText(tokens.subList(indexTokenAfterToCompare, indexTokenAfterToCompare + nonTcExpressionSize + 1));
                    if (StringUtils.equals(nonTcExpressionAsString, StringUtils.trim(subString))) {
                        temperature.setQuantifiedObject(null);
                        continue outer;
                    }
                }
            }

            //Searching for more constrained expressions just before the temperature
            for (Pair<String, List<LayoutToken>> tcExpression : closedTcExpressionList) {
                String tcString = tcExpression.getLeft();

                List<LayoutToken> tcLayoutTokens = tcExpression.getRight();
                String tcExpressionAsString = LayoutTokensUtil.toText(tcLayoutTokens);
                int tcExpressionSize = tcLayoutTokens.size();

                int indexTokenToCompareWith = extremities.getLeft() - 1;
                if (indexTokenToCompareWith - tcExpressionSize >= 0) {
                    String subString = LayoutTokensUtil.toText(tokens.subList(indexTokenToCompareWith - tcExpressionSize, indexTokenToCompareWith));
                    if (StringUtils.equals(tcExpressionAsString, subString)) {
                        QuantifiedObject quantifiedObject = new QuantifiedObject(subString, "Critical Temperature");
                        temperature.setQuantifiedObject(quantifiedObject);
                        continue outer;
                    }
                }
            }

            //Searching for more general expression extracted from the paper sliding back until I reach a sentence or another temperature.
            for (Pair<String, List<LayoutToken>> tcExpression : tcExpressionList) {
                String tcString = tcExpression.getLeft();
                List<LayoutToken> tcLayoutTokens = tcExpression.getRight();
                String tcExpressionAsString = LayoutTokensUtil.toText(tcLayoutTokens);

                int tcLayoutTokenSize = tcLayoutTokens.size();

                // from the temperature to the back
                for (int i = extremities.getLeft() - 1; i >= tcLayoutTokenSize + sentenceBoundaryIndexes.getLeft(); i--) {
                    LayoutToken current = tokens.get(i);

                    // Make sure I don't go out of bound

                    String subString = LayoutTokensUtil.toText(tokens.subList(i - tcLayoutTokenSize, i));
                    if (StringUtils.equals(tcExpressionAsString, subString)) {
                        QuantifiedObject quantifiedObject = new QuantifiedObject(subString, "Critical Temperature");
                        temperature.setQuantifiedObject(quantifiedObject);
                        continue outer;
                    }
                }
            }
        }

        return temperatures;
    }


    public PDFAnnotationResponse annotate(List<LayoutToken> tokens) {
        List<Superconductor> superconductorsNames = superconductorsParser.process(tokens);

//        List<Superconductor> namedEntitiesList = superconductorsNames.stream()
//            .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL))
//                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_CLASS_LABEL))
//                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL))
//                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL))
//            )
//            .collect(Collectors.toList());

        List<Measurement> temperatureList = getMeasurements(tokens, superconductorsNames);

//        List<Superconductor> linkedSuperconductors = linkSuperconductorsWithTc(namedEntitiesList, temperatureList, tokens);

        return new PDFAnnotationResponse(superconductorsNames, temperatureList, new ArrayList<>());
    }

    private List<Measurement> getTemperatures(List<LayoutToken> tokens) {
        List<Measurement> measurements = quantityParser.process(tokens);
        List<Measurement> temperatures = MeasurementUtils.filterMeasurements(measurements,
            Collections.singletonList(UnitUtilities.Unit_Type.TEMPERATURE));

        return temperatures;
    }

    private List<Measurement> getMeasurements(List<LayoutToken> tokens, List<Superconductor> superconductorsNames) {
        List<Pair<String, List<LayoutToken>>> tcExpressionList = superconductorsNames.stream()
            .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL)))
            .map(tc -> new ImmutablePair<>(tc.getName(), tc.getLayoutTokens()))
            .collect(Collectors.toList());

        List<Measurement> temperatures = getTemperatures(tokens);
//        return markCriticalTemperatures(temperatures, tokens, tcExpressionList);
        return temperatures;
    }

    public PDFProcessingResponse process(InputStream uploadedInputStream) {
        PDFProcessingResponse pdfProcessingResponse = new PDFProcessingResponse();

        Document doc = null;
        File file = null;

        try {
            file = IOUtilities.writeInputFile(uploadedInputStream);
            GrobidAnalysisConfig config =
                new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                    .build();
            DocumentSource documentSource =
                DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            GrobidPDFEngine.processDocument(doc, l -> pdfProcessingResponse.addParagraph(process(l)));

            List<Page> pages = doc.getPages().stream().map(p -> new Page(p.getHeight(), p.getWidth())).collect(Collectors.toList());

            pdfProcessingResponse.setPages(pages);
        } catch (Exception e) {
            throw new GrobidException("Cannot process pdf file: " + file.getPath(), e);
        } finally {
            IOUtilities.removeTempFile(file);
        }

        return pdfProcessingResponse;
    }

    public ProcessedParagraph process(List<LayoutToken> tokens) {
        List<Superconductor> superconductorsNames = superconductorsParser.process(tokens);

        List<Superconductor> namedEntitiesList = superconductorsNames.stream()
            .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL))
                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_CLASS_LABEL))
                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL))
                || s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL))
            )
            .collect(Collectors.toList());

        List<Measurement> temperatureList = getTemperatures(tokens);

        ProcessedParagraph processedParagraph = new ProcessedParagraph();

        processedParagraph.setTokens(tokens.stream().map(l -> {

            boolean subscript = l.isSubscript();
            boolean superscript = l.isSuperscript();

            String style = "baseline";
            if (superscript) {
                style = "superscript";
            } else if (subscript) {
                style = "subscript";
            }

            return new Token(l.getText(), l.getFont(), l.getFontSize(), style, l.getOffset());
        }).collect(Collectors.toList()));

        processedParagraph.setText(LayoutTokensUtil.toText(tokens));

        List<Span> spansFromSuperconductors = namedEntitiesList.stream()
            .map(s -> {
                List<LayoutToken> layoutTokens = s.getLayoutTokens();
                Pair<Integer, Integer> extremitiesSuperconductor = MeasurementUtils.getExtremitiesAsIndex(tokens,
                    layoutTokens.get(0).getOffset(), Iterables.getLast(layoutTokens).getOffset() + 1);

                List<BoundingBox> boundingBoxes = BoundingBoxCalculator.calculate(layoutTokens);

                return new Span(s.getName(), lowerCase(substring(s.getType(), 1, length(s.getType()) - 1)),
                    s.getOffsetStart() - tokens.get(0).getOffset(), s.getOffsetEnd() - tokens.get(0).getOffset(),
                    extremitiesSuperconductor.getLeft(), extremitiesSuperconductor.getRight(), boundingBoxes);
            })
            .collect(Collectors.toList());

        List<Span> spansForQuantities = temperatureList.stream()
            .flatMap(t -> {
                List<Quantity> quantityList = QuantityOperations.toQuantityList(t);
                List<LayoutToken> layoutTokens = QuantityOperations.getLayoutTokens(quantityList);

                int start = layoutTokens.get(0).getOffset();
                int end = Iterables.getLast(layoutTokens).getOffset();

                // Token start and end
                Pair<Integer, Integer> extremitiesQuantityAsIndex = MeasurementUtils
                    .getExtremitiesAsIndex(tokens,
                        Math.min(start, end), Math.max(start, end) + 1);

                //Offset start and end
                List<OffsetPosition> offsets = QuantityOperations.getOffsets(quantityList);
                List<OffsetPosition> sortedOffsets = offsets.stream()
                    .sorted(Comparator.comparingInt(o -> o.start))
                    .collect(Collectors.toList());

                List<BoundingBox> boundingBoxes = BoundingBoxCalculator.calculate(layoutTokens);

                int lowerOffset = sortedOffsets.get(0).start;
                int higherOffset = Iterables.getLast(sortedOffsets).end;

                String type = "temperature";

                return Stream.of(
                    new Span(LayoutTokensUtil.toText(tokens.subList(extremitiesQuantityAsIndex.getLeft(), extremitiesQuantityAsIndex.getRight())),
                        type,
                        lowerOffset - tokens.get(0).getOffset(),
                        higherOffset - tokens.get(0).getOffset(),
                        extremitiesQuantityAsIndex.getLeft(), extremitiesQuantityAsIndex.getRight(),
                        boundingBoxes)
                );

            }).collect(Collectors.toList());

        processedParagraph.getSpans().addAll(spansFromSuperconductors);
        processedParagraph.getSpans().addAll(spansForQuantities);

        List<Span> sortedSpans = processedParagraph.getSpans().stream()
            .sorted(Comparator.comparingInt(Span::getOffsetStart))
            .collect(Collectors.toList());

        processedParagraph.setSpans(sortedSpans);

        return processedParagraph;
    }
}
