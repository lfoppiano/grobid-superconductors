package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Triple;
import org.easymock.EasyMock;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Span;
import org.grobid.core.data.chemDataExtractor.ChemicalSpan;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.SuperconductorsParser.NONE_CHEMSPOT_TYPE;
import static org.grobid.core.utilities.GrobidTestUtils.getWapitiResult;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

public class SuperconductorsParserTest {

    private SuperconductorsParser target;

    private ChemDataExtractorClient mockChemspotClient;
    private MaterialParser mockMaterialParser;

    @Before
    public void setUp() throws Exception {
        mockChemspotClient = EasyMock.createMock(ChemDataExtractorClient.class);
        mockMaterialParser = EasyMock.createMock(MaterialParser.class);
        target = new SuperconductorsParser(GrobidModels.DUMMY, mockChemspotClient, mockMaterialParser);
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
                .map(token -> FeaturesVectorSuperconductors.addFeatures(token, null, new LayoutToken(), null).printVector())
                .collect(Collectors.toList());
    }
}