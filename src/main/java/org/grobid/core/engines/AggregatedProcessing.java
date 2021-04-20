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
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.data.Token.getStyle;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class AggregatedProcessing {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedProcessing.class);

    private EngineParsers parsers;

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private SentenceSegmenter sentenceSegmenter;
    private RuleBasedLinker ruleBasedLinker;
    private CRFBasedLinker CRFBasedLinker;

    public AggregatedProcessing(SuperconductorsParser superconductorsParser, QuantityParser quantityParser, RuleBasedLinker ruleBasedLinker, CRFBasedLinker CRFBasedLinker) {
        this.superconductorsParser = superconductorsParser;
        this.quantityParser = quantityParser;
        this.sentenceSegmenter = new SentenceSegmenter();
        this.ruleBasedLinker = ruleBasedLinker;
        this.CRFBasedLinker = CRFBasedLinker;
        parsers = new EngineParsers();
    }

    @Inject
    public AggregatedProcessing(SuperconductorsParser superconductorsParser, RuleBasedLinker ruleBasedLinker, CRFBasedLinker CRFBasedLinker) {
        this(superconductorsParser, QuantityParser.getInstance(true), ruleBasedLinker, CRFBasedLinker);
    }

    @Deprecated
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

    /**
     * The text is a paragraph
     */
    public DocumentResponse process(String text, boolean disableLinking) {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<TextPassage> textPassage = process(tokens, disableLinking);

        return new DocumentResponse(textPassage);
    }

    private List<Span> getQuantities(List<LayoutToken> tokens) {
        List<Measurement> measurements = quantityParser.process(tokens);

        List<Span> spans = new ArrayList<>();

        spans.addAll(getTemperatures(measurements).stream()
            .flatMap(p -> Stream.of(MeasurementUtils.toSpan(p, tokens, SUPERCONDUCTORS_TC_VALUE_LABEL)))
            .collect(Collectors.toList()));

        spans.addAll(getPressures(measurements).stream()
            .flatMap(p -> Stream.of(MeasurementUtils.toSpan(p, tokens, SUPERCONDUCTORS_PRESSURE_LABEL)))
            .collect(Collectors.toList()));


        return spans;
    }

    private List<Measurement> getTemperatures(List<Measurement> measurements) {
        List<Measurement> temperatures = MeasurementUtils.filterMeasurementsByUnitType(measurements,
            Collections.singletonList(UnitUtilities.Unit_Type.TEMPERATURE));

        List<Measurement> kelvins = MeasurementUtils.filterMeasurementsByUnitValue(temperatures,
            Collections.singletonList("k"));

        return kelvins;
    }

    private List<Measurement> getPressures(List<Measurement> measurements) {
        List<Measurement> pressures = MeasurementUtils.filterMeasurementsByUnitType(measurements,
            Collections.singletonList(UnitUtilities.Unit_Type.PRESSURE));
        return pressures;
    }

    public DocumentResponse process(InputStream uploadedInputStream, boolean disableLinking) {
        DocumentResponse documentResponse = new DocumentResponse();

        Document doc = null;
        File file = null;

        try {
            file = IOUtilities.writeInputFile(uploadedInputStream);
            GrobidAnalysisConfig config =
                new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                    .analyzer(DeepAnalyzer.getInstance())
                    .build();
            DocumentSource documentSource =
                DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            GrobidPDFEngine.processDocument(doc, (documentBlock) -> {
                List<LayoutToken> cleanedLayoutTokens = documentBlock.getLayoutTokens().stream()
                    .map(l -> {
                        LayoutToken newOne = new LayoutToken(l);
                        newOne.setText(UnicodeUtil.normaliseText(l.getText()).replaceAll("\\p{C}", " "));
                        return newOne;
                    }).collect(Collectors.toList());

                List<LayoutToken> cleanedLayoutTokensRetokenized = DeepAnalyzer.getInstance()
                    .retokenizeLayoutTokens(cleanedLayoutTokens);

                documentResponse.addParagraphs(process(cleanedLayoutTokensRetokenized, disableLinking,
                    documentBlock.getSection(), documentBlock.getSubSection()));
            });

            List<Page> pages = doc.getPages().stream().map(p -> new Page(p.getHeight(), p.getWidth())).collect(Collectors.toList());

            documentResponse.setPages(pages);
        } catch (Exception e) {
            throw new GrobidException("Cannot process input file. ", e);
        } finally {
            IOUtilities.removeTempFile(file);
        }

        return documentResponse;
    }

    public List<TextPassage> process(List<LayoutToken> tokens, boolean disableLinking) {
        return process(tokens, disableLinking, null, null);
    }

    public List<TextPassage> process(List<LayoutToken> tokens, boolean disableLinking, String section, String subSection) {
        TextPassage textPassage = new TextPassage();

        textPassage.setTokens(tokens.stream().map(Token::of).collect(Collectors.toList()));
        textPassage.setText(LayoutTokensUtil.toText(tokens));
        if (section != null) {
            textPassage.setSection(section);
            textPassage.setSubSection(subSection);
        }
        textPassage.setType("paragraph");

        List<Span> spans = new ArrayList<>();
        List<Span> superconductorsList = superconductorsParser.process(tokens);
        // Re-calculate the offsets to be based on the current paragraph - TODO: investigate this mismatch
        List<Span> correctedSuperconductorsSpans = superconductorsList.stream()
            .map(s -> {
                int paragraphOffsetStart = tokens.get(0).getOffset();
                s.setOffsetStart(s.getOffsetStart() - paragraphOffsetStart);
                s.setOffsetEnd(s.getOffsetEnd() - paragraphOffsetStart);
                return s;
            })
            .collect(Collectors.toList());

        List<Span> quantitiesList = getQuantities(tokens).stream()
            .map(s -> {
                int paragraphOffsetStart = tokens.get(0).getOffset();
                s.setOffsetStart(s.getOffsetStart() - paragraphOffsetStart);
                s.setOffsetEnd(s.getOffsetEnd() - paragraphOffsetStart);
                return s;
            })
            .collect(Collectors.toList());

        spans.addAll(correctedSuperconductorsSpans);
        spans.addAll(quantitiesList);

        List<Span> sortedSpans = spans
            .stream()
            .sorted(Comparator.comparingInt(Span::getOffsetStart))
            .collect(Collectors.toList());

        textPassage.setSpans(pruneOverlappingAnnotations(sortedSpans));

        if (disableLinking) {
            return Collections.singletonList(textPassage);
        }

        //CRF-based

        // Set the materials to be linkable
        for (Span s : textPassage.getSpans()) {
            if (s.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL)) {
                s.setLinkable(true);
            }
        }

        /** Modify the objects **/
        TextPassage newTextPassage = ruleBasedLinker.markTemperatures(textPassage);
        CRFBasedLinker.process(tokens, newTextPassage.getSpans());

        //Rule-based: Because we split into sentences, we may obtain more information
        List<TextPassage> textPassages = ruleBasedLinker.process(textPassage);

        //TODO: Merge
        return textPassages;
    }


    public static String getFormattedString(List<LayoutToken> layoutTokens) {
        StringBuilder sb = new StringBuilder();
        String previousStyle = "baseline";
        String opened = null;
        for (LayoutToken lt : layoutTokens) {
            String currentStyle = getStyle(lt);
            if (currentStyle.equals(previousStyle)) {
                sb.append(lt.getText());
            } else {
                if (currentStyle.equals("baseline")) {
                    sb.append("</" + previousStyle.substring(0, 3) + ">");
                    opened = null;
                    sb.append(lt.getText());
                } else if (currentStyle.equals("superscript")) {
                    if (previousStyle.equals("baseline")) {
                        sb.append("<" + currentStyle.substring(0, 3) + ">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    } else {
                        sb.append("</" + previousStyle.substring(0, 3) + ">");
                        sb.append("<" + currentStyle.substring(0, 3) + ">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    }
                } else if (currentStyle.equals("subscript")) {
                    if (previousStyle.equals("baseline")) {
                        sb.append("<" + currentStyle.substring(0, 3) + ">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    } else {
                        sb.append("</" + previousStyle.substring(0, 3) + ">");
                        sb.append("<" + currentStyle.substring(0, 3) + ">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    }
                }
            }
            previousStyle = currentStyle;
        }
        if (opened != null) {
            sb.append("</" + opened + ">");
            opened = null;
        }
        return sb.toString();
    }


    /**
     * Remove overlapping annotations
     * - sort annotation by starting offset then pairwise check
     * - if they have the same type I take the one with the larger entity or the quantity model
     * - else if they have different type I take the one with the smaller entity size or the one from
     * the superconductors model
     **/
    public static List<Span> pruneOverlappingAnnotations(List<Span> spanList) {
        //Sorting by offsets
        List<Span> sortedEntities = spanList
            .stream()
            .sorted(Comparator.comparingInt(Span::getOffsetStart))
            .collect(Collectors.toList());

//                sortedEntities = sortedEntities.stream().distinct().collect(Collectors.toList());

        if (spanList.size() <= 1) {
            return sortedEntities;
        }

        List<Span> toBeRemoved = new ArrayList<>();

        Span previous = null;
        boolean first = true;
        for (Span current : sortedEntities) {

            if (first) {
                first = false;
                previous = current;
            } else {
                if (current.getOffsetEnd() < previous.getOffsetEnd() || previous.getOffsetEnd() > current.getOffsetStart()) {
                    LOGGER.debug("Overlapping. " + current.getText() + " <" + current.getType() + "> with " + previous.getText() + " <" + previous.getType() + ">");

                    if (current.getType().equals(previous.getType())) {
                        // Type is the same, I take the largest one
                        if (StringUtils.length(previous.getText()) > StringUtils.length(current.getText())) {
                            toBeRemoved.add(previous);
                        } else if (StringUtils.length(previous.getText()) < StringUtils.length(current.getText())) {
                            toBeRemoved.add(current);
                        } else {
                            if (current.getSource().equals("grobid-superconductors")) {
                                if (isEmpty(current.getBoundingBoxes()) && isNotEmpty(previous.getBoundingBoxes())) {
                                    current.setBoundingBoxes(previous.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.warn("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(previous);
                            } else if (previous.getSource().equals("grobid-superconductors")) {
                                if (isEmpty(previous.getBoundingBoxes()) && isNotEmpty(current.getBoundingBoxes())) {
                                    previous.setBoundingBoxes(current.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.warn("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(current);
                            } else {
                                toBeRemoved.add(previous);
                            }
                        }
                    } else if (!current.getType().equals(previous.getType())) {
                        // Type is different I take the shorter match

                        if (StringUtils.length(previous.getText()) < StringUtils.length(current.getText())) {
                            toBeRemoved.add(current);
                        } else if (StringUtils.length(previous.getText()) > StringUtils.length(current.getText())) {
                            toBeRemoved.add(previous);
                        } else {
                            if (current.getSource().equals("grobid-superconductors")) {
                                if (isEmpty(current.getBoundingBoxes()) && isNotEmpty(previous.getBoundingBoxes())) {
                                    current.setBoundingBoxes(previous.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.warn("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(previous);
                            } else if (previous.getSource().equals("grobid-superconductors")) {
                                if (isEmpty(previous.getBoundingBoxes()) && isNotEmpty(current.getBoundingBoxes())) {
                                    previous.setBoundingBoxes(current.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.warn("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(current);
                            } else {
                                toBeRemoved.add(previous);
                            }
                        }
                    }
                }
                previous = current;
            }
        }

        List<Span> newSortedEntitiers = (List<Span>) CollectionUtils.removeAll(sortedEntities, toBeRemoved);
        return newSortedEntitiers;
    }


    public static List<SuperconEntry> extractEntities(List<TextPassage> paragraphs) {
        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();
        for (TextPassage paragraph : paragraphs) {
            for (Span span : paragraph.getSpans()) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }
        // Materials
        List<Span> materials = spansById.values().stream()
            .filter(span -> span.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());

        List<SuperconEntry> outputCSV = new ArrayList<>();

        for (Span m : materials) {
            SuperconEntry dbEntry = new SuperconEntry();
            dbEntry.setRawMaterial(m.getText());
            dbEntry.setSection(sectionsById.get(m.getId()).getLeft());
            dbEntry.setSubsection(sectionsById.get(m.getId()).getRight());
            dbEntry.setSentence(sentenceById.get(m.getId()));

            for (Map.Entry<String, String> a : m.getAttributes().entrySet()) {
                String[] splits = a.getKey().split("_");
                String prefix = splits[0];
                String propertyName = splits[1];
                String value = a.getValue();

                if (propertyName.equals("formula")) {
                    dbEntry.setFormula(value);
                } else if (propertyName.equals("name")) {
                    dbEntry.setName(value);
                } else if (propertyName.equals("clazz")) {
                    dbEntry.setClassification(value);
                } else if (propertyName.equals("shape")) {
                    dbEntry.setShape(value);
                } else if (propertyName.equals("doping")) {
                    dbEntry.setDoping(value);
                } else if (propertyName.equals("fabrication")) {
                    dbEntry.setFabrication(value);
                } else if (propertyName.equals("substrate")) {
                    dbEntry.setSubstrate(value);
                }
            }

            Map<String, String> linkedToMaterial = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));

            if (isNotEmpty(linkedToMaterial.entrySet())) {
                for (Map.Entry<String, String> entry : linkedToMaterial.entrySet()) {
                    Span tcValue = spansById.get(entry.getKey());
                    dbEntry.setCriticalTemperature(tcValue.getText());
                    List<Span> pressures = tcValue.getLinks().stream()
                        .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                        .map(l -> spansById.get(l.getTargetId()))
                        .collect(Collectors.toList());

                    if (isNotEmpty(pressures)) {
                        boolean first = true;
                        for (Span pressure : pressures) {
                            if (first) {
                                dbEntry.setAppliedPressure(pressure.getText());
                                outputCSV.add(dbEntry);
                                first = false;
                            } else {
                                try {
                                    SuperconEntry newEntry = dbEntry.clone();
                                    newEntry.setAppliedPressure(pressure.getText());
                                    newEntry.setLinkType(entry.getValue());
                                } catch (CloneNotSupportedException e) {
                                    LOGGER.error("Cannot create a duplicate of the supercon entry: " + dbEntry.getRawMaterial() + ". ", e);
                                    break;
                                }
                            }
                        }
                    } else {
                        outputCSV.add(dbEntry);
                    }

                }
            } else {
                outputCSV.add(dbEntry);
            }
        }
        return outputCSV;
    }

    public static List<SuperconEntry> computeTabularData(List<TextPassage> paragraphs) {
        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();
        for (TextPassage paragraph : paragraphs) {
            List<Span> linkedSpans = paragraph.getSpans().stream()
                .filter(s -> s.getLinks().size() > 0)
                .collect(Collectors.toList());

            for (Span span : linkedSpans) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }

        // Materials
        List<Span> materials = spansById.values().stream()
            .filter(span -> span.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());

//        List<Span> tcValues = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());
//
//        List<Span> pressures = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());

        List<SuperconEntry> outputCSV = new ArrayList<>();
        for (Span m : materials) {
            SuperconEntry dbEntry = new SuperconEntry();
            dbEntry.setRawMaterial(m.getText());
            dbEntry.setSection(sectionsById.get(m.getId()).getLeft());
            dbEntry.setSubsection(sectionsById.get(m.getId()).getRight());
            dbEntry.setSentence(sentenceById.get(m.getId()));

            for (Map.Entry<String, String> a : m.getAttributes().entrySet()) {
                String[] splits = a.getKey().split("_");
                String prefix = splits[0];
                String propertyName = splits[1];
                String value = a.getValue();

                if (propertyName.equals("formula")) {
                    dbEntry.setFormula(value);
                } else if (propertyName.equals("name")) {
                    dbEntry.setName(value);
                } else if (propertyName.equals("clazz")) {
                    dbEntry.setClassification(value);
                } else if (propertyName.equals("shape")) {
                    dbEntry.setShape(value);
                } else if (propertyName.equals("doping")) {
                    dbEntry.setDoping(value);
                } else if (propertyName.equals("fabrication")) {
                    dbEntry.setFabrication(value);
                } else if (propertyName.equals("substrate")) {
                    dbEntry.setSubstrate(value);
                }
            }

            Map<String, String> linkedToMaterial = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));

            for (Map.Entry<String, String> entry : linkedToMaterial.entrySet()) {
                Span tcValue = spansById.get(entry.getKey());
                dbEntry.setCriticalTemperature(tcValue.getText());
                List<Span> pressures = tcValue.getLinks().stream()
                    .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                    .map(l -> spansById.get(l.getTargetId()))
                    .collect(Collectors.toList());

                if (isNotEmpty(pressures)) {
                    boolean first = true;
                    for (Span pressure : pressures) {
                        if (first) {
                            dbEntry.setAppliedPressure(pressure.getText());
                            outputCSV.add(dbEntry);
                            first = false;
                        } else {
                            try {
                                SuperconEntry newEntry = dbEntry.clone();
                                newEntry.setAppliedPressure(pressure.getText());
                                newEntry.setLinkType(entry.getValue());
                            } catch (CloneNotSupportedException e) {
                                LOGGER.error("Cannot create a duplicate of the supercon entry: " + dbEntry.getRawMaterial() + ". ", e);
                                break;
                            }
                        }
                    }
                } else {
                    outputCSV.add(dbEntry);
                }

            }
        }
        return outputCSV;
    }
}
