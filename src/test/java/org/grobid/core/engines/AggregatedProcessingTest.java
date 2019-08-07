package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnitUtilities;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AggregatedProcessingTest {

    AggregatedProcessing target;

    SuperconductorsParser mockSuperconductorsParser;
    AbbreviationsParser mockAbbreviationsParser;
    QuantityParser mockQuantityParser;

    @Before
    public void setUp() throws Exception {

        mockSuperconductorsParser = EasyMock.createMock(SuperconductorsParser.class);
        mockAbbreviationsParser = EasyMock.createMock(AbbreviationsParser.class);
        mockQuantityParser = EasyMock.createMock(QuantityParser.class);

        target = new AggregatedProcessing(mockSuperconductorsParser, mockQuantityParser);
    }

    @Test
    public void testAggregation() throws Exception {
        String text = "The Tc of the BaClE2 is 30K";
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(8), tokens.get(9)));
        superconductor.setType("material");

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

        EasyMock.expect(mockSuperconductorsParser.process(tokens)).andReturn(Arrays.asList(superconductor));
        EasyMock.expect(mockQuantityParser.process(tokens)).andReturn(Arrays.asList(temperature));

        EasyMock.replay(mockSuperconductorsParser, mockQuantityParser);

        OutputResponse response = target.process(tokens);
//        System.out.println(response.toJson());

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
}