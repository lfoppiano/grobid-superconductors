package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Triple;
import org.easymock.EasyMock;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.external.chemDataExtractor.ChemicalSpan;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.client.ChemDataExtractorClient;
import org.grobid.core.utilities.client.StructureIdentificationModuleClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.CRFBasedLinkerIntegrationTest.initEngineForTests;
import static org.grobid.core.engines.SuperconductorsParser.NONE_CHEMSPOT_TYPE;
import static org.grobid.core.utilities.GrobidTestUtils.getWapitiResult;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.number.OrderingComparison.greaterThan;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Lexicon.class)
@PowerMockIgnore({"kotlin.*"})
public class SuperconductorsParserTest {

    private SuperconductorsParser target;

    private ChemDataExtractorClient mockChemspotClient;
    private MaterialParser mockMaterialParser;
    private StructureIdentificationModuleClient mockSpaceGroupsClient;


    @BeforeClass
    public static void before() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
    }


    @Before
    public void setUp() throws Exception {
        mockChemspotClient = EasyMock.createMock(ChemDataExtractorClient.class);
        mockMaterialParser = EasyMock.createMock(MaterialParser.class);
        mockSpaceGroupsClient = EasyMock.createMock(StructureIdentificationModuleClient.class);
        PowerMock.mockStatic(Lexicon.class);
        target = new SuperconductorsParser(GrobidModels.DUMMY, mockChemspotClient, mockMaterialParser, mockSpaceGroupsClient);
    }

    @Test
    public void testGetResults() throws Exception {
        String input = "In just a few months, the superconducting transition temperature (Tc) was increased to 55 K " +
            "in the electron-doped system, as well as 25 K in hole-doped La1−x SrxOFeAs compound.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<tc>", 13, 18),
            Triple.of("<tc>", 20, 21),
            Triple.of("<tcValue>", 29, 32),
            Triple.of("<tcValue>", 50, 53),
            Triple.of("<material>", 56, 66)
        );

        List<String> features = getFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        EasyMock.expect(mockMaterialParser.process((List<LayoutToken>) EasyMock.anyObject())).andReturn(new ArrayList<>()).anyTimes();
        EasyMock.replay(mockMaterialParser);

        List<Span> spans = target.extractResults(layoutTokens, results);

        assertThat(spans, hasSize(5));
        assertThat(spans.get(0).getType(), is("<tc>"));
        assertThat(spans.get(4).getType(), is("<material>"));
        assertThat(spans.get(4).getText(), is("hole-doped La1−x SrxOFeAs"));
        EasyMock.verify(mockMaterialParser);

    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions() {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified " +
            "into Zintl\n" +
            "compounds, where 14 group elements Si, Ge, and Sn forming the\n" +
            "framework with polyhedral cages are partially substituted with\n" +
            "lower valent elements such as ");

        tokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<ChemicalSpan> mentions = Arrays.asList(new ChemicalSpan(70, 72, "Si"), new ChemicalSpan(74, 76, "Ge"));
        List<Boolean> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        assertThat(booleans.stream().filter(b -> !b.equals(NONE_CHEMSPOT_TYPE)).count(), greaterThan(0L));
    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions_longMention() {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
            "compounds, where 14 group elements Si, Ge, and Sn forming the\n" +
            "framework with polyhedral cages are partially substituted with\n" +
            "lower valent elements such as ");

        tokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<ChemicalSpan> mentions = Arrays.asList(new ChemicalSpan(29, 44, "Zintl compounds"), new ChemicalSpan(70, 72, "Si"), new ChemicalSpan(74, 76, "Ge"));
        List<Boolean> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        List<Boolean> collect = booleans.stream()
            .filter(b -> !b.equals(Boolean.FALSE))
            .collect(Collectors.toList());

        assertThat(collect, hasSize(5));

        List<LayoutToken> annotatedTokens = IntStream
            .range(0, tokens.size())
            .filter(i -> !booleans.get(i).equals(Boolean.FALSE))
            .mapToObj(tokens::get)
            .collect(Collectors.toList());

        assertThat(annotatedTokens, hasSize(5));
        assertThat(annotatedTokens.get(0).getText(), is("Zintl"));
        assertThat(annotatedTokens.get(1).getText(), is("\n"));
        assertThat(annotatedTokens.get(2).getText(), is("compounds"));
        assertThat(annotatedTokens.get(3).getText(), is("Si"));
        assertThat(annotatedTokens.get(4).getText(), is("Ge"));
    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions_consecutives() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
            "compounds, where 14 group elements Si Ge, and Sn forming the\n" +
            "framework with polyhedral cages are partially substituted with\n" +
            "lower valent elements such as ");

        tokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<ChemicalSpan> mentions = Arrays.asList(new ChemicalSpan(70, 72, "Si"), new ChemicalSpan(73, 75, "Ge"));
        List<Boolean> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        List<Boolean> collect = booleans.stream().filter(b -> !b.equals(Boolean.FALSE)).collect(Collectors.toList());

        assertThat(collect, hasSize(2));

        List<LayoutToken> annotatedTokens = IntStream
            .range(0, tokens.size())
            .filter(i -> !booleans.get(i).equals(Boolean.FALSE))
            .mapToObj(tokens::get)
            .collect(Collectors.toList());

        assertThat(annotatedTokens, hasSize(2));
        assertThat(annotatedTokens.get(0).getText(), is("Si"));
        assertThat(annotatedTokens.get(1).getText(), is("Ge"));
    }

    private static List<String> getFeatures(List<LayoutToken> layoutTokens) {

        return layoutTokens.stream()
            .map(token -> FeaturesVectorSuperconductors
                .addFeatures(token, null, new LayoutToken(), null)
                .printVector())
            .collect(Collectors.toList());
    }

    @Test
    public void testExtractSpans_realExample() throws Exception {
        String text = "As a final assumption, a one-head description of the individual motor enzymes with M = 2 chemical states is adopted . Exploiting the above mentioned fact that P1 (x, t) + P2 (x, t) is a constant and normalized on [0, L] according to , one can eliminate P2 (x, t) from the master equation , yielding in the steady state (su- perscript st) the ordinary first order equation ẋ supplemented by the periodic boundary condition 22 P st 1 (x + L) = P st 1 (x).";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        layoutTokens.stream().forEach(l -> l.setOffset(l.getOffset() + 470134));
        List<ChemicalSpan> mentions = Arrays.asList(
            new ChemicalSpan(159, 161, "<space-groups>", "P1")
        );

        List<Span> spans = target.extractSpans(layoutTokens, mentions);

        assertThat(spans, hasSize(1));
        assertThat(spans.get(0).getText(), is("P1"));
        assertThat(spans.get(0).getTokenStart(), is(57));
        assertThat(spans.get(0).getTokenEnd(), is(59));
        assertThat(spans.get(0).getOffsetStart(), is(159));
        assertThat(spans.get(0).getOffsetEnd(), is(161));
    }

    @Test
    public void testExtractSpans_realExample2() throws Exception {
        String text = "WB 4.2 crystallizes in the space group P6 3 /mmc (No. 194) and has a crystal structure that is derived from the simple diborides";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        List<ChemicalSpan> mentions = Arrays.asList(
            new ChemicalSpan(39, 48, "space-group", "P6 3 /mmc")
        );

        List<Span> spans = target.extractSpans(layoutTokens, mentions);

        assertThat(spans, hasSize(1));
        assertThat(spans.get(0).getText(), is("P6 3 /mmc"));
        assertThat(spans.get(0).getTokenStart(), is(16));
        assertThat(spans.get(0).getTokenEnd(), is(23));
        assertThat(spans.get(0).getOffsetStart(), is(39));
        assertThat(spans.get(0).getOffsetEnd(), is(48));
    }
}