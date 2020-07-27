package org.grobid.core.engines;

import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnitUtilities;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AggregatedProcessingTest {

    AggregatedProcessing target;

    SuperconductorsParser mockSuperconductorsParser;
    EntityLinkerParser mockEntityLinkerParser;
    QuantityParser mockQuantityParser;

    @Before
    public void setUp() throws Exception {

        mockSuperconductorsParser = EasyMock.createMock(SuperconductorsParser.class);
        mockEntityLinkerParser = EasyMock.createMock(EntityLinkerParser.class);
        mockQuantityParser = EasyMock.createMock(QuantityParser.class);

        target = new AggregatedProcessing(mockSuperconductorsParser, mockQuantityParser, null, mockEntityLinkerParser);
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

        EasyMock.expect(mockSuperconductorsParser.process(tokens)).andReturn(Arrays.asList(superconductor));
        EasyMock.expect(mockQuantityParser.process(tokens)).andReturn(Arrays.asList(temperature));
//        EasyMock.expect(mockEntityLinkerParser.process((List<LayoutToken>) EasyMock.anyObject(), EasyMock.anyObject())).andReturn(new ArrayList<>());

        EasyMock.replay(mockSuperconductorsParser, mockQuantityParser);

        List<ProcessedParagraph> response = target.process(tokens, true);

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

        String s = target.getFormattedString(layoutTokens);

        assertThat(s, is("La <sub>x</sub> Fe <sub>1-x</sub>"));
    }

    public void testNLP4j() throws Exception {

        Tokenizer tokenizer = new EnglishTokenizer();
        String inputFile = "/Users/lfoppiano/development/projects/sentence-segmentation/data/pdf2_raw_text.txt";
        String output = "/Users/lfoppiano/development/projects/sentence-segmentation/data/pdf2-nlp4j-output.txt";

        String[] lines = FileUtils.readFileToString(new File(inputFile)).split("\n");

        List<String> outputLines = new ArrayList<>();

        for (String line : lines) {
            List<Token> tokens = tokenizer.tokenize(line);
            List<List<Token>> sentences = tokenizer.segmentize(tokens);

            for (List<Token> sentence : sentences) {
                int firstOffset = sentence.get(0).getStartOffset();
                int endOffset = sentence.get(sentence.size() - 1).getEndOffset();
                outputLines.add(line.substring(firstOffset, endOffset));
            }
        }

        FileUtils.writeLines(new File(output), outputLines);
    }

}