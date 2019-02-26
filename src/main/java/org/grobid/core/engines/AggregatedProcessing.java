package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnitUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Singleton
public class AggregatedProcessing {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedProcessing.class);

    private EngineParsers parsers;

    private SuperconductorsParser superconductorsParser;
    private AbbreviationsParser abbreviationsParser;
    private QuantityParser quantityParser;


    public AggregatedProcessing(SuperconductorsParser superconductorsParser, AbbreviationsParser abbreviationsParser, QuantityParser quantityParser) {
        this.superconductorsParser = superconductorsParser;
        this.abbreviationsParser = abbreviationsParser;
        this.quantityParser = quantityParser;
    }

    @Inject
    public AggregatedProcessing(SuperconductorsParser superconductorsParser, AbbreviationsParser abbreviationsParser) {
        this.superconductorsParser = superconductorsParser;
        this.abbreviationsParser = abbreviationsParser;
        this.quantityParser = QuantityParser.getInstance(true);
    }

    private List<Measurement> processQuantity(String text) {
        List<Measurement> process = quantityParser.process(text);

        return filterTemperature(process);
    }

    private List<Superconductor> linkSuperconductorsWithTc(List<Superconductor> superconductors, List<Measurement> measurements, List<LayoutToken> tokens) {
        if (measurements.size() == 0)
            return superconductors;

        for (Superconductor superconductor : superconductors) {
            Pair<Integer, Integer> extremitiesSuperconductor = getExtremitiesSingle(tokens, superconductor.getLayoutTokens().get(0).getOffset());


//            List<LayoutToken> superconductorWindow = tokens.subList(extremitiesSuperconductor.getLeft(), extremitiesSuperconductor.getRight());

            for (Measurement temperature : measurements) {
                if (temperature.getType() == UnitUtilities.Measurement_Type.VALUE) {
                    int temperatureOffset = temperature.getQuantityAtomic().getLayoutTokens().get(0).getOffset();
                    if (temperatureOffset > extremitiesSuperconductor.getLeft() || temperatureOffset < extremitiesSuperconductor.getRight()) {
                        if (temperature.getQuantifiedObject() != null
                                && StringUtils.equalsIgnoreCase("Critical temperature", temperature.getQuantifiedObject().getNormalizedName())) {
                            //link!
                            superconductor.setCriticalTemperature(temperature);
                            break;
                        } else {
                            LOGGER.warn("Found a temperature but not a critical temperature. Skipping it. ");
                        }
                    }
                }
            }

        }

        return superconductors;
    }

    private List<Measurement> processQuantity(List<LayoutToken> tokens) {
        List<Measurement> process = quantityParser.process(tokens);

        List<Measurement> temperatures = filterTemperature(process);

        for (Measurement temperature : temperatures) {
            Quantity quantity = null;
            Pair<Integer, Integer> extremities = null;
            switch (temperature.getType()) {
                case VALUE:
                    quantity = temperature.getQuantityAtomic();
                    List<LayoutToken> layoutTokens = quantity.getLayoutTokens();
                    int centerPosition = layoutTokens.get(0).getOffset();

                    extremities = getExtremitiesSingle(tokens, centerPosition);


                    break;
                case INTERVAL_BASE_RANGE:
                    if (temperature.getQuantityBase() != null && temperature.getQuantityRange() != null) {
                        Quantity quantityBase = temperature.getQuantityBase();
                        Quantity quantityRange = temperature.getQuantityRange();

                        extremities = getExtremitiesDouble(tokens, quantityBase.getLayoutTokens().get(0).getOffset(), quantityRange.getLayoutTokens().get(quantityRange.getLayoutTokens().size() - 1).getOffset());
                    } else {
                        Quantity quantityTmp;
                        if (temperature.getQuantityBase() == null) {
                            quantityTmp = temperature.getQuantityRange();
                        } else {
                            quantityTmp = temperature.getQuantityBase();
                        }

                        extremities = getExtremitiesSingle(tokens, quantityTmp.getLayoutTokens().get(0).getOffset());
                    }

                    break;

                case INTERVAL_MIN_MAX:
                    if (temperature.getQuantityLeast() != null && temperature.getQuantityMost() != null) {
                        Quantity quantityLeast = temperature.getQuantityLeast();
                        Quantity quantityMost = temperature.getQuantityMost();

                        extremities = getExtremitiesDouble(tokens, quantityLeast.getLayoutTokens().get(0).getOffset(), quantityMost.getLayoutTokens().get(quantityMost.getLayoutTokens().size() - 1).getOffset());
                    } else {
                        Quantity quantityTmp;
                        if (temperature.getQuantityLeast() == null) {
                            quantityTmp = temperature.getQuantityMost();
                        } else {
                            quantityTmp = temperature.getQuantityLeast();
                        }

                        extremities = getExtremitiesSingle(tokens, quantityTmp.getLayoutTokens().get(0).getOffset());
                    }
                    break;

                case CONJUNCTION:
                    List<Quantity> quantityList = temperature.getQuantityList();
                    if (quantityList.size() > 1) {
                        extremities = getExtremitiesDouble(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset(), quantityList.get(quantityList.size() - 1).getLayoutTokens().get(0).getOffset());
                    } else {
                        extremities = getExtremitiesSingle(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset());
                    }

                    break;
            }


            if (extremities == null) {
                continue;
            }
            List<LayoutToken> temperatureWindow = tokens.subList(extremities.getLeft(), extremities.getRight());

            if (isNotEmpty(temperatureWindow)) {
                ListIterator<LayoutToken> it = temperatureWindow.listIterator();

                // searching for Tc
                while (it.hasNext()) {
                    LayoutToken token = it.next();
                    if (StringUtils.equalsAny(token.getText(), "temperature")) {
                        QuantifiedObject quantifiedObject = new QuantifiedObject(token.getText());
                        temperature.setQuantifiedObject(quantifiedObject);
                    } else if (token.getText().equalsIgnoreCase("tc") || ((token.getText().equalsIgnoreCase("t")) && it.nextIndex() + 1 < temperatureWindow.size() && (temperatureWindow.get(it.nextIndex() + 1).getText().equalsIgnoreCase("c")))) {
                        String rawName = LayoutTokensUtil.toText(Arrays.asList(token, temperatureWindow.get(it.nextIndex()), temperatureWindow.get(it.nextIndex() + 1)));
                        QuantifiedObject quantifiedObject = new QuantifiedObject(rawName, "Critical Temperature");
                        temperature.setQuantifiedObject(quantifiedObject);
                    }
                }
            }


        }

        return temperatures;
    }

    public static int WINDOW_TC = 20;

    protected Pair<Integer, Integer> getExtremitiesSingle(List<LayoutToken> tokens, int centroidTokenOffset) {
        return getExtremitiesSingle(tokens, centroidTokenOffset, WINDOW_TC);
    }


    protected Pair<Integer, Integer> getExtremitiesSingle(List<LayoutToken> tokens, int centroidTokenOffset, int windowlayoutTokensSize) {
        int start = 0;
        int end = tokens.size() - 1;

        Optional<LayoutToken> centralToken = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidTokenOffset).findFirst();
        LayoutToken layoutToken = centralToken.orElseThrow(RuntimeException::new);

        int centroidLayoutTokenIndex = tokens.indexOf(layoutToken);

        if (centroidLayoutTokenIndex > windowlayoutTokensSize) {
            start = centroidLayoutTokenIndex - windowlayoutTokensSize;
        }
        if (end - centroidLayoutTokenIndex > windowlayoutTokensSize) {
            end = centroidLayoutTokenIndex + windowlayoutTokensSize + 1;
        }

        return new ImmutablePair(start, end);
    }

    protected Pair<Integer, Integer> getExtremitiesDouble(List<LayoutToken> tokens, int centroidTokenOffsetLow, int centroidTokenOffsetHigh) {
        return getExtremitiesDouble(tokens, centroidTokenOffsetLow, centroidTokenOffsetHigh, WINDOW_TC);
    }

    protected Pair<Integer, Integer> getExtremitiesDouble(List<LayoutToken> tokens, int centroidTokenOffsetLow, int centroidTokenOffsetHigh, int windowSize) {

        int start = 0;
        int end = tokens.size() - 1;

        Optional<LayoutToken> centralTokenLower = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidTokenOffsetLow).findFirst();
        Optional<LayoutToken> centralTokenHigher = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidTokenOffsetHigh).findFirst();
        LayoutToken layoutTokenLow = centralTokenLower.orElseThrow(RuntimeException::new);
        LayoutToken layoutTokenHigh = centralTokenHigher.orElseThrow(RuntimeException::new);

        int lowIndex = tokens.indexOf(layoutTokenLow);
        int highIndex = tokens.indexOf(layoutTokenHigh);

        if (lowIndex > 10) {
            start = lowIndex - 10;
        }
        if (end - highIndex > 10) {
            end = highIndex + 10;
        }

        return new ImmutablePair(start, end);

    }

    private List<Measurement> filterTemperature(List<Measurement> process) {
        List<Measurement> temperatures = process.stream().filter(measurement -> {
            switch (measurement.getType()) {
                case VALUE:
                    return UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityAtomic().getType());
                case CONJUNCTION:
                    return measurement.getQuantityList()
                            .stream().anyMatch(quantity -> UnitUtilities.Unit_Type.TEMPERATURE.equals(quantity.getType()));
                case INTERVAL_BASE_RANGE:
                    return UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityBase().getType()) ||
                            UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityRange().getType());

                case INTERVAL_MIN_MAX:
                    return (measurement.getQuantityMost() != null && UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityMost().getType())) ||
                            (measurement.getQuantityLeast() != null && UnitUtilities.Unit_Type.TEMPERATURE.equals(measurement.getQuantityLeast().getType()));

            }

            return false;
        }).collect(Collectors.toList());

        return temperatures;
    }

    public OutputResponse process(String text) {
        OutputResponse output = new OutputResponse();
        output.setTemperatures(processQuantity(text));
        output.setSuperconductors(superconductorsParser.process(text));
        output.setAbbreviations(abbreviationsParser.process(text));

        return output;
    }


    public OutputResponse process(InputStream inputStream) {
        parsers = new EngineParsers();

        OutputResponse outputResponse = new OutputResponse();
        List<Superconductor> superconductorNamesList = new ArrayList<>();
        outputResponse.setSuperconductors(superconductorNamesList);
        List<Measurement> temperaturesList = new ArrayList<>();
        outputResponse.setTemperatures(temperaturesList);
        List<Abbreviation> abbreviationList = new ArrayList<>();
        outputResponse.setAbbreviations(abbreviationList);

        Document doc = null;
        File file = null;

        OutputResponse response = new OutputResponse();

        try {
            file = IOUtilities.writeInputFile(inputStream);
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .build();
            DocumentSource documentSource =
                    DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
            doc = parsers.getSegmentationParser().processing(documentSource, config);

            // In the following, we process the relevant textual content of the document

            // for refining the process based on structures, we need to filter
            // segment of interest (e.g. header, body, annex) and possibly apply
            // the corresponding model to further filter by structure types

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                org.apache.commons.lang3.tuple.Pair<String, List<LayoutToken>> headerStruct = parsers.getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
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
                    if (titleTokens != null) {
                        outputResponse.extendEntities(process(titleTokens));
                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        outputResponse.extendEntities(process(abstractTokens));
                    }

                    // keywords
                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                    if (keywordTokens != null) {
                        outputResponse.extendEntities(process(keywordTokens));
                    }
                }
            }

            // we can process all the body, in the future figure and table could be the
            // object of more refined processing
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                org.apache.commons.lang3.tuple.Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, documentParts);

                String fulltextTaggedRawResult = null;
                if (featSeg != null) {
                    String featureText = featSeg.getLeft();
                    LayoutTokenization layoutTokenization = featSeg.getRight();

                    if (StringUtils.isNotEmpty(featureText)) {
                        fulltextTaggedRawResult = parsers.getFullTextParser().label(featureText);
                    }

                    TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, fulltextTaggedRawResult,
                            layoutTokenization.getTokenization(), true);

                    //Iterate and exclude figures and tables
                    for (TaggingTokenCluster cluster : Iterables.filter(clusteror.cluster(),
                            new TaggingTokenClusteror
                                    .LabelTypeExcludePredicate(TaggingLabels.TABLE_MARKER, TaggingLabels.EQUATION, TaggingLabels.CITATION_MARKER,
                                    TaggingLabels.FIGURE_MARKER, TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION_LABEL))) {

                        if (cluster.getTaggingLabel().equals(TaggingLabels.FIGURE)) {
                            //apply the figure model to only get the caption
                            final Figure processedFigure = parsers.getFigureParser()
                                    .processing(cluster.concatTokens(), cluster.getFeatureBlock());

                            List<LayoutToken> tokens = processedFigure.getCaptionLayoutTokens();

                            outputResponse.extendEntities(process(tokens));

                        } else if (cluster.getTaggingLabel().equals(TaggingLabels.TABLE)) {
                            //apply the table model to only get the caption/description
                            final Table processedTable = parsers.getTableParser().processing(cluster.concatTokens(), cluster.getFeatureBlock());
                            List<LayoutToken> tokens = processedTable.getFullDescriptionTokens();

                            outputResponse.extendEntities(process(tokens));
                        } else {
                            final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();

                            // extract all the layout tokens from the cluster as a list
                            List<LayoutToken> tokens = labeledTokensContainers.stream()
                                    .map(LabeledTokensContainer::getLayoutTokens)
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList());

                            outputResponse.extendEntities(process(tokens));

                        }

                    }
                }
            }

            // we don't process references (although reference titles could be relevant)
            // acknowledgement?

            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {

                List<LayoutToken> tokens = doc.getTokenizationParts(documentParts, doc.getTokenizations());
                outputResponse.extendEntities(process(tokens));
            }

            List<Page> pages = new ArrayList<>();
            for (org.grobid.core.layout.Page page : doc.getPages()) {
                pages.add(new Page(page.getHeight(), page.getWidth()));
            }

            outputResponse.setPages(pages);

        } catch (Exception e) {
            throw new GrobidException("Cannot process pdf file: " + file.getPath(), e);
        } finally {
            IOUtilities.removeTempFile(file);
        }

        return outputResponse;
    }

    public OutputResponse process(List<LayoutToken> tokens) {
        List<Superconductor> superconductorsNames = superconductorsParser.process(tokens);
        List<Measurement> temperatureList = processQuantity(tokens);
        List<Abbreviation> abbreviationsList = abbreviationsParser.process(tokens);

        List<Superconductor> linkedSuperconductors = linkSuperconductorsWithTc(superconductorsNames, temperatureList, tokens);

        return new OutputResponse(linkedSuperconductors, temperatureList, abbreviationsList);
    }
}
