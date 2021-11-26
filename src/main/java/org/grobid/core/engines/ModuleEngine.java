package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.document.*;
import org.grobid.core.data.material.Formula;
import org.grobid.core.data.material.Material;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.data.document.Token.getStyle;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.grobid.core.engines.linking.CRFBasedLinker.*;

@Singleton
public class ModuleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleEngine.class);

    private EngineParsers parsers;

    private SuperconductorsParser superconductorsParser;
    private QuantityParser quantityParser;
    private RuleBasedLinker ruleBasedLinker;
    private CRFBasedLinker crfBasedLinker;
    private GrobidSuperconductorsConfiguration configuration;

    ModuleEngine(GrobidSuperconductorsConfiguration configuration, SuperconductorsParser superconductorsParser, QuantityParser quantityParser, RuleBasedLinker ruleBasedLinker, CRFBasedLinker CRFBasedLinker) {
        this.superconductorsParser = superconductorsParser;
        this.quantityParser = quantityParser;
        this.ruleBasedLinker = ruleBasedLinker;
        this.crfBasedLinker = CRFBasedLinker;
        this.configuration = configuration;
    }

    @Inject
    public ModuleEngine(GrobidSuperconductorsConfiguration configuration, SuperconductorsParser superconductorsParser, RuleBasedLinker ruleBasedLinker, CRFBasedLinker CRFBasedLinker) {
        this(configuration, superconductorsParser, QuantityParser.getInstance(true), ruleBasedLinker, CRFBasedLinker);

        parsers = new EngineParsers();
    }

    @Deprecated
    private Pair<Integer, Integer> getContainedSentenceAsIndex(List<LayoutToken> entityLayoutTokens, List<LayoutToken> tokens) {

        List<List<LayoutToken>> sentences = new SentenceSegmenter().detectSentencesAsLayoutToken(tokens);

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

        List<List<LayoutToken>> sentences = new SentenceSegmenter().detectSentencesAsLayoutToken(tokens);

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
     * Process a chunk of text, namely a sentence
     *
     * @param text           paragraph or sentence to be processed
     * @param disableLinking disable the linking
     * @return
     */
    public DocumentResponse process(String text, boolean disableLinking) {
        List<OffsetPosition> sentenceOffsets = SentenceUtilities.getInstance()
            .runSentenceDetection(text, new Language("en"));

        List<RawPassage> sentencesAsLayoutToken = sentenceOffsets.stream()
            .map(sentenceOffset -> text.substring(sentenceOffset.start, sentenceOffset.end))
            .map(sentence -> DeepAnalyzer.getInstance().tokenizeWithLayoutToken(sentence))
            .map(RawPassage::new)
            .collect(Collectors.toList());

        List<TextPassage> textPassages = process(sentencesAsLayoutToken, disableLinking);

        DocumentResponse documentResponse = new DocumentResponse(textPassages);

        Map<Formula, List<String>> aggregatedFormulaAtDocumentLevel = processFormulasAtDocumentLevel(documentResponse);
        documentResponse.setAggregatedMaterials(aggregatedFormulaAtDocumentLevel);

        return documentResponse;
    }

    private List<Span> getQuantities(List<LayoutToken> tokens) {
        List<Span> spans = new ArrayList<>();
        List<Measurement> measurements = new ArrayList<>();

        //TODO: remove this when quantities will be updated
        try {
            measurements = quantityParser.process(tokens);
        } catch (Exception e) {
            LOGGER.warn("Error when processing quantities", e);
            return spans;
        }

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
        int consolidateHeader = StringUtils.isNotEmpty(this.configuration.getConsolidation().service) ? 1 : 0;

        GrobidAnalysisConfig config =
            new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                .analyzer(DeepAnalyzer.getInstance())
                .consolidateHeader(consolidateHeader)
                .withSentenceSegmentation(true)
                .build();

        try {
            file = IOUtilities.writeInputFile(uploadedInputStream);
            DocumentSource documentSource =
                DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            final List<RawPassage> accumulatedSentences = new ArrayList<>();
            BiblioInfo biblioInfo = GrobidPDFEngine.processDocument(doc, config, (documentBlock) -> {
                List<LayoutToken> cleanedLayoutTokens = documentBlock.getLayoutTokens().stream()
                    .map(l -> {
                        LayoutToken newOne = new LayoutToken(l);
                        newOne.setText(UnicodeUtil.normaliseText(l.getText()).replaceAll("\\p{C}", " "));
                        return newOne;
                    }).collect(Collectors.toList());

                List<LayoutToken> cleanedLayoutTokensRetokenized = DeepAnalyzer.getInstance()
                    .retokenizeLayoutTokens(cleanedLayoutTokens);

                accumulatedSentences.add(new RawPassage(cleanedLayoutTokensRetokenized, documentBlock.getSection(), documentBlock.getSubSection()));
            });

            documentResponse.addParagraphs(process(accumulatedSentences, disableLinking));

            List<Page> pages = doc.getPages().stream().map(p -> new Page(p.getHeight(), p.getWidth())).collect(Collectors.toList());

            documentResponse.setPages(pages);
            documentResponse.setBiblio(biblioInfo);
        } catch (Exception e) {
            throw new GrobidException("Cannot process input file. ", e);
        } finally {
            IOUtilities.removeTempFile(file);
        }

        Map<Formula, List<String>> aggregatedFormulaAtDocumentLevel = processFormulasAtDocumentLevel(documentResponse);
        documentResponse.setAggregatedMaterials(aggregatedFormulaAtDocumentLevel);


        return documentResponse;
    }

    public List<TextPassage> process(List<RawPassage> inputPassage, boolean disableLinking) {

        List<List<LayoutToken>> accumulatedLayoutTokens = inputPassage.stream()
            .map(RawPassage::getLayoutTokens)
            .collect(Collectors.toList());

        List<List<Span>> superconductorsList = superconductorsParser.process(accumulatedLayoutTokens);

        List<TextPassage> intermediateList = new ArrayList<>();

        for (int index = 0; index < superconductorsList.size(); index++) {
            List<Span> rawSuperconductorsSpans = superconductorsList.get(index);
            List<LayoutToken> tokens = accumulatedLayoutTokens.get(index);

            // Re-calculate the offsets to be based on the current paragraph - TODO: investigate this mismatch
            List<Span> superconductorsSpans = rawSuperconductorsSpans.stream()
                .map(s -> {
                    int paragraphOffsetStart = tokens.get(0).getOffset();
                    s.setOffsetStart(s.getOffsetStart() - paragraphOffsetStart);
                    s.setOffsetEnd(s.getOffsetEnd() - paragraphOffsetStart);
                    return s;
                })
                .collect(Collectors.toList());

            List<Span> quantitiesSpans = getQuantities(tokens).stream()
                .map(s -> {
                    int paragraphOffsetStart = tokens.get(0).getOffset();
                    s.setOffsetStart(s.getOffsetStart() - paragraphOffsetStart);
                    s.setOffsetEnd(s.getOffsetEnd() - paragraphOffsetStart);
                    return s;
                })
                .collect(Collectors.toList());

            List<Span> aggregatedSpans = new ArrayList<>();
            aggregatedSpans.addAll(superconductorsSpans);
            aggregatedSpans.addAll(quantitiesSpans);

            TextPassage textPassage = new TextPassage();
            textPassage.setTokens(tokens.stream().map(Token::of).collect(Collectors.toList()));
            textPassage.setText(LayoutTokensUtil.toText(tokens));

            String section = inputPassage.get(index).getSection();
            String subSection = inputPassage.get(index).getSubSection();

            if (section != null) {
                textPassage.setSection(section);
                textPassage.setSubSection(subSection);
            }
            textPassage.setType("sentence");
            textPassage.setSpans(pruneOverlappingAnnotations(aggregatedSpans));
            intermediateList.add(textPassage);
        }

        if (disableLinking) {
            return intermediateList;
        }

        List<TextPassage> outputList = new ArrayList<>();

        List<TextPassage> textPassagesWithLinks = ruleBasedLinker.process(intermediateList);

        for (int i = 0; i < textPassagesWithLinks.size(); i++) {
            TextPassage textPassageWithLinks = textPassagesWithLinks.get(i);

            List<Span> spansCopy = intermediateList.get(i).getSpans().stream()
                .map(Span::new)
                .collect(Collectors.toList());

            Map<String, Span> mapById = textPassageWithLinks.getSpans().stream()
                .filter(s -> s.getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL))
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            spansCopy.stream()
                .filter(s -> s.getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL))
                .forEach(s -> {
                    if (mapById.containsKey(s.getId())) {
                        s.setLinkable(mapById.get(s.getId()).isLinkable());
                    }
                });

            List<LayoutToken> tokens = accumulatedLayoutTokens.get(i);

            Map<String, Span> resultMaterialTcValueLinkerCrf = crfBasedLinker.process(tokens, spansCopy, MATERIAL_TCVALUE_ID)
                .parallelStream()
                .filter(s -> crfBasedLinker.getLinkingEngines().get(MATERIAL_TCVALUE_ID).getAnnotationsToBeLinked().contains(s.getType()))
                .filter(s -> isNotEmpty(s.getLinks()))
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            Map<String, Span> resultTcValuePressureLinkerCrf = crfBasedLinker.process(tokens, spansCopy, TCVALUE_PRESSURE_ID)
                .parallelStream()
                .filter(s -> crfBasedLinker.getLinkingEngines().get(TCVALUE_PRESSURE_ID).getAnnotationsToBeLinked().contains(s.getType()))
                .filter(s -> isNotEmpty(s.getLinks()))
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            Map<String, Span> resultTcValueMeMethodLinkerCrf = crfBasedLinker.process(tokens, spansCopy, TCVALUE_ME_METHOD_ID)
                .parallelStream()
                .filter(s -> crfBasedLinker.getLinkingEngines().get(TCVALUE_ME_METHOD_ID).getAnnotationsToBeLinked().contains(s.getType()))
                .filter(s -> isNotEmpty(s.getLinks()))
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            // Merge
            textPassageWithLinks.getSpans()
                .stream()
                .forEach(s -> {
                    if (resultMaterialTcValueLinkerCrf.containsKey(s.getId())) {
                        s.getLinks().addAll(resultMaterialTcValueLinkerCrf.get(s.getId()).getLinks());
                    }

                    if (resultTcValuePressureLinkerCrf.containsKey(s.getId())) {
                        s.getLinks().addAll(resultTcValuePressureLinkerCrf.get(s.getId()).getLinks());
                    }

                    if (resultTcValueMeMethodLinkerCrf.containsKey(s.getId())) {
                        s.getLinks().addAll(resultTcValueMeMethodLinkerCrf.get(s.getId()).getLinks());
                    }
                });

            outputList.add(textPassageWithLinks);
        }

        return outputList;
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
                    sb.append("</").append(previousStyle, 0, 3).append(">");
                    opened = null;
                    sb.append(lt.getText());
                } else if (currentStyle.equals("superscript")) {
                    if (previousStyle.equals("baseline")) {
                        sb.append("<").append(currentStyle, 0, 3).append(">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    } else {
                        sb.append("</").append(previousStyle, 0, 3).append(">");
                        sb.append("<").append(currentStyle, 0, 3).append(">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    }
                } else if (currentStyle.equals("subscript")) {
                    if (previousStyle.equals("baseline")) {
                        sb.append("<").append(currentStyle, 0, 3).append(">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    } else {
                        sb.append("</").append(previousStyle, 0, 3).append(">");
                        sb.append("<").append(currentStyle, 0, 3).append(">");
                        opened = currentStyle.substring(0, 3);
                        sb.append(lt.getText());
                    }
                }
            }
            previousStyle = currentStyle;
        }
        if (opened != null) {
            sb.append("</").append(opened).append(">");
            opened = null;
        }
        return sb.toString();
    }

    public Map<Formula, List<String>> processFormulasAtDocumentLevel(DocumentResponse document) {

        Map<Formula, List<String>> aggregatedFormulas = document.getPassages().stream().parallel()
            .filter(p -> isNotEmpty(p.getSpans()))
            .flatMap(p -> p.getSpans().stream())
            .filter(s -> (s.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL) || s.getType().equals(SUPERCONDUCTORS_CLASS_LABEL))
                && s.getAttributes().keySet().stream()
                .anyMatch(ak -> (ak.contains("_resolvedFormulas") && ak.contains("_formulaComposition")) ||
                    (!ak.contains("_resolvedFormulas") && ak.contains("_formula_formulaComposition"))))
            .map(s -> {
//                Map<String, String> attributesWeCareAbout = s.getAttributes().entrySet().stream()
//                    .filter(map -> (map.getKey().contains("_resolvedFormulas") && map.getKey().contains("_formulaComposition")) ||
//                        (!map.getKey().contains("_resolvedFormulas") && map.getKey().contains("_formula_formulaComposition")))
//                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, Object> nestedAttributes = Material.toNestedAttributes(s.getAttributes());

                List<Pair<Span, Formula>> o = new ArrayList<>();
                for (String materialKey : nestedAttributes.keySet()) {
                    Map<String, Object> materialAttributes = (Map<String, Object>) nestedAttributes.get(materialKey);
                    if (materialAttributes == null || !(materialAttributes.containsKey("formula") || materialAttributes.containsKey("resolvedFormulas"))) {
                        continue;
                    }
                    if (materialAttributes.containsKey("resolvedFormulas")) {
                        Map<String, Object> resolvedFormulas = (Map<String, Object>) materialAttributes.get("resolvedFormulas");
                        for (String resolvedFormulaKey : resolvedFormulas.keySet()) {
                            Map<String, Object> resolvedFormula = (Map<String, Object>) resolvedFormulas.get(resolvedFormulaKey);
                            Formula f = createFormula(resolvedFormula);

                            o.add(Pair.of(s, f));
                        }
                    } else {
                        Map<String, Object> formula = (Map<String, Object>) materialAttributes.get("formula");
                        Formula f = createFormula(formula);

                        o.add(Pair.of(s, f));
                    }
                }
                return o;
            }).flatMap(Collection::stream)
            .collect(Collectors.groupingBy(Pair::getRight, Collectors.mapping(u -> u.getLeft().getId(), Collectors.toList())));

        Map<Formula, List<String>> sortedAggregatedFormulas = aggregatedFormulas.entrySet().stream()
            .sorted(comparingInt(e -> e.getValue().size()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> {
                    throw new AssertionError();
                },
                LinkedHashMap::new
            ));
        Map<Formula, List<String>> sortedAggregatedFormulasReversed = new LinkedHashMap<>();
        List<Formula> keys = new LinkedList<>(sortedAggregatedFormulas.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
            sortedAggregatedFormulasReversed.put(keys.get(i), sortedAggregatedFormulas.get(keys.get(i)));
        }
        
        return sortedAggregatedFormulasReversed;
    }

    private Formula createFormula(Map<String, Object> formula) {
        String rawFormula = (String) formula.get("rawValue");
        Map<String, String> composition = (Map<String, String>) formula.get("formulaComposition");
        return new Formula(rawFormula, composition);
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
            .sorted(comparingInt(Span::getOffsetStart))
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
                            toBeRemoved.add(current);
                        } else if (StringUtils.length(previous.getText()) < StringUtils.length(current.getText())) {
                            toBeRemoved.add(previous);
                        } else {
                            if (current.getSource().equals(SuperconductorsModels.SUPERCONDUCTORS.getModelName())) {
                                if (isEmpty(current.getBoundingBoxes()) && isNotEmpty(previous.getBoundingBoxes())) {
                                    current.setBoundingBoxes(previous.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.debug("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(previous);
                            } else if (previous.getSource().equals(SuperconductorsModels.SUPERCONDUCTORS.getModelName())) {
                                if (isEmpty(previous.getBoundingBoxes()) && isNotEmpty(current.getBoundingBoxes())) {
                                    previous.setBoundingBoxes(current.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.debug("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
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
                            if (current.getSource().equals(SuperconductorsModels.SUPERCONDUCTORS.getModelName())) {
                                if (isEmpty(current.getBoundingBoxes()) && isNotEmpty(previous.getBoundingBoxes())) {
                                    current.setBoundingBoxes(previous.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.debug("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
                                }
                                toBeRemoved.add(previous);
                            } else if (previous.getSource().equals(SuperconductorsModels.SUPERCONDUCTORS.getModelName())) {
                                if (isEmpty(previous.getBoundingBoxes()) && isNotEmpty(current.getBoundingBoxes())) {
                                    previous.setBoundingBoxes(current.getBoundingBoxes());
                                } else if (isEmpty(current.getBoundingBoxes()) && isEmpty(previous.getBoundingBoxes())) {
                                    LOGGER.debug("Missing bounding boxes for " + current.getText() + " and " + previous.getText());
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
}
