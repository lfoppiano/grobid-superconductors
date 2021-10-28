package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.data.document.DocumentResponse;
import org.grobid.core.data.document.RawPassage;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.document.TextPassage;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnitUtilities;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Lexicon.class)
public class ModuleEngineTest {

    ModuleEngine target;

    SuperconductorsParser mockSuperconductorsParser;
    CRFBasedLinker mockCRFBasedLinker;
    QuantityParser mockQuantityParser;

    @BeforeClass
    public static void before() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
    }


    @Before
    public void setUp() throws Exception {
        PowerMock.mockStatic(Lexicon.class);
        mockSuperconductorsParser = EasyMock.createMock(SuperconductorsParser.class);
        mockCRFBasedLinker = EasyMock.createMock(CRFBasedLinker.class);
        mockQuantityParser = EasyMock.createMock(QuantityParser.class);

        target = new ModuleEngine(new GrobidSuperconductorsConfiguration(), mockSuperconductorsParser, mockQuantityParser, null, mockCRFBasedLinker);
    }

    @Test
    public void testAggregation() throws Exception {
        String text = "The Tc of the BaClE2 is 30K";
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Span superconductor = new Span();
        superconductor.setText("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(8), tokens.get(9)));
        superconductor.setType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        temperature.setQuantifiedObject(new QuantifiedObject("Tc", "critical temperature"));
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);

        quantity.setLayoutTokens(Arrays.asList(tokens.get(13), tokens.get(14)));
        temperature.setAtomicQuantity(quantity);

        List<List<LayoutToken>> aggregatedTokens = Arrays.asList(tokens);

        EasyMock.expect(mockSuperconductorsParser.process(aggregatedTokens)).andReturn(Arrays.asList(Arrays.asList(superconductor)));
        EasyMock.expect(mockQuantityParser.process(tokens)).andReturn(Arrays.asList(temperature));
//        EasyMock.expect(mockEntityLinkerParser.process((List<LayoutToken>) EasyMock.anyObject(), EasyMock.anyObject())).andReturn(new ArrayList<>());

        EasyMock.replay(mockSuperconductorsParser, mockQuantityParser);

        List<TextPassage> response = target.process(Arrays.asList(new RawPassage(tokens)), true);

        EasyMock.verify(mockSuperconductorsParser, mockQuantityParser);

    }

    @Test
    public void testGetExtremitiesIndex_short_nearBeginning() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        Pair<Integer, Integer> extremitiesSingle = target.getExtremitiesAsIndex(tokens, 5, 5, 3);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(6));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is("This is a "));
    }

    @Test
    public void testGetExtremitiesSingle_short_middle() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        Pair<Integer, Integer> extremitiesSingle = target.getExtremitiesAsIndex(tokens, 8, 8, 3);

        assertThat(extremitiesSingle.getLeft(), is(1));
        assertThat(extremitiesSingle.getRight(), is(8));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" is a short "));
    }

    @Test
    public void testGetExtremitiesSingle_long_middle() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        Pair<Integer, Integer> extremitiesSingle = target.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
    }


    @Test
    public void testGetExtremitiesSingle_long_centroidWithMultipleLayoutToken_middle() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        Pair<Integer, Integer> extremitiesSingle = target.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
    }

    @Test
    public void testToFormattedString_1() throws Exception {
        String text = "La x Fe 1-x";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        layoutTokens.get(2).setSuperscript(false);
        layoutTokens.get(2).setSubscript(true);

        layoutTokens.get(6).setSuperscript(false);
        layoutTokens.get(6).setSubscript(true);
        layoutTokens.get(7).setSuperscript(false);
        layoutTokens.get(7).setSubscript(true);
        layoutTokens.get(8).setSuperscript(false);
        layoutTokens.get(8).setSubscript(true);

        String s = ModuleEngine.getFormattedString(layoutTokens);

        assertThat(s, is("La <sub>x</sub> Fe <sub>1-x</sub>"));
    }

    @Test
    public void testComputeTabularData() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("14fcc539b1.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = ModuleEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(20));

        assertThat(superconEntries.get(18).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(18).getCriticalTemperature(), is("55 K"));
        assertThat(superconEntries.get(18).getAppliedPressure(), is(nullValue()));
        assertThat(superconEntries.get(18).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(19).getCriticalTemperature(), is("up to 164 K"));
        assertThat(superconEntries.get(19).getAppliedPressure(), is("30 GPa"));
    }
}