package org.grobid.core.engines;

import org.easymock.EasyMock;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.chemDataExtractor.Span;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemDataExtractionClient;
import org.grobid.core.utilities.OffsetPosition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.SuperconductorsParser.NONE_CHEMSPOT_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

public class SuperconductorsParserTest {

    private SuperconductorsParser target;

    private ChemDataExtractionClient mockChemspotClient;

    @Before
    public void setUp() throws Exception {
        mockChemspotClient = EasyMock.createMock(ChemDataExtractionClient.class);

        LibraryLoader.load();
//        target = new SuperconductorsParser(GrobidModels.DUMMY, mockChemspotClient);

        target = new SuperconductorsParser(SuperconductorsModels.SUPERCONDUCTORS, mockChemspotClient);
    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions() {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
                "compounds, where 14 group elements Si, Ge, and Sn forming the\n" +
                "framework with polyhedral cages are partially substituted with\n" +
                "lower valent elements such as ");

        tokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Span> mentions = Arrays.asList(new Span(70, 72, "Si"), new Span(74, 76, "Ge"));
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

        List<Span> mentions = Arrays.asList(new Span(29, 44, "Zintl compounds"), new Span(70, 72, "Si"), new Span(74, 76, "Ge"));
        List<Boolean> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        List<Boolean> collect = booleans.stream().filter(b -> !b.equals(Boolean.FALSE)).collect(Collectors.toList());

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

        List<Span> mentions = Arrays.asList(new Span(70, 72, "Si"), new Span(73, 75, "Ge"));
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


    @Test
    @Ignore("Not sure what this tests are doing - method is not used")
    public void calculateOffsets() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic 2 effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space. ");
        List<LayoutToken> theTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The electronic specific heat of as-grown and annealed single-crystals of ");

        theTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        OffsetPosition offsetPosition = target.calculateOffsets(tokens, theTokens, 0);

        assertThat(offsetPosition.start, is(0));
        assertThat(offsetPosition.end, is(72));
    }

    @Test
    @Ignore("Not sure what this tests are doing - method is not used")
    public void calculateOffsets2() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic 2 effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space. ");
        List<LayoutToken> theTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("FeSe 1-x Te x ");

        theTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 447);
        });

        OffsetPosition offsetPosition = target.calculateOffsets(tokens, theTokens, 447);

        assertThat(offsetPosition.start, is(447));
        assertThat(offsetPosition.end, is(460));
    }


    @Test
    @Ignore("Not sure what this tests are doing - method is not used")
    public void calculateOffsets3() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("(Color online) (a) Field-swept 31 P-NMR spectra in BaFe 2 (As 0:75 P 0:25 ) 2 . The solid black and dashed red arrows indicate the magnetic fields where the 1=T 1 of the paramagnetic and AFM states is measured. (b) Temperature dependence of the averaged internal field hH int i estimated from the second moment of the observed NMR signals. hH int i increases below T N but decreases at T Ã c . The broken line indicates the fitting of the data ranging from T Ã c to T N to the phenomenological formula of cðT N À T Þ with c ¼ 0:02, T N ¼ 56:2 K, and ¼ 0:42. The red solid curve is the fit with the GL model in the case of homogeneous coexistence with M 0 ¼ 0:35 B , A ¼ 2:03 Â 10 À2 K À1 , and B ¼ 8:33 Â 10 À2 K À1 ]. (c) Temperature dependence of ðT 1 T Þ À1 measured at the sharp paramagnetic NMR signal (closed squares) and the broad magnetic NMR signal (triangles and circles). (d) and (e) ðT 1 T Þ À1 measured across the 31 P spectra at The ðT 1 T Þ À1 over the entire spectrum at 5 K is smaller than that at 20 K, indicative of the occurrence of superconductivity over the entire region of the sample. ");
        List<LayoutToken> theTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("56:2 K");

        theTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 8529);
        });

        OffsetPosition offsetPosition = target.calculateOffsets(tokens, theTokens, 0);

        assertThat(offsetPosition.start, is(447));
        assertThat(offsetPosition.end, is(460));
    }
}