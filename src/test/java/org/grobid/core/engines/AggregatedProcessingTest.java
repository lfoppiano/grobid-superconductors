package org.grobid.core.engines;

import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnitUtilities;
import org.junit.Before;
import org.junit.Test;
import shadedwipo.org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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

        target = new AggregatedProcessing(mockSuperconductorsParser, mockQuantityParser, mockEntityLinkerParser);
    }

    @Test
    public void testAggregation() throws Exception {
        String text = "The Tc of the BaClE2 is 30K";
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
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
        EasyMock.expect(mockEntityLinkerParser.process((List<LayoutToken>) EasyMock.anyObject(), EasyMock.anyObject())).andReturn(new ArrayList<Superconductor>());

        EasyMock.replay(mockSuperconductorsParser, mockQuantityParser, mockEntityLinkerParser);

        PDFAnnotationResponse response = target.annotate(tokens);
//        System.out.println(response.toJson());

        EasyMock.verify(mockSuperconductorsParser, mockQuantityParser, mockEntityLinkerParser);

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
    public void testMarkTemperature_noExpressions_noMatch_shouldWork() {
        String text = "The Tc of the BaClE2 is 30K";

        List<Superconductor> extractedEntities = new ArrayList<>();

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(8), tokens.get(9)));
        superconductor.setType("material");
        extractedEntities.add(superconductor);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);

        quantity.setLayoutTokens(Arrays.asList(tokens.get(13), tokens.get(14)));
        temperature.setAtomicQuantity(quantity);

        List<Measurement> measurements = target.markCriticalTemperatures(Arrays.asList(temperature), tokens, new ArrayList<>());

        assertThat(measurements, hasSize(1));
    }

    @Test
    public void testMarkTemperature_noExpressions_match_shouldWork() {
        String text = "The material BaClE2 superconducts at 30K";

        List<Superconductor> extractedEntities = new ArrayList<>();

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(4), tokens.get(5), tokens.get(6)));
        superconductor.setType("material");
        extractedEntities.add(superconductor);

//        Superconductor tcExpression = new Superconductor();
//        tcExpression.setName("Tc");
//        tcExpression.setLayoutTokens(Arrays.asList(tokens.get(2)));
//        tcExpression.setType("tc");
//        extractedEntities.add(tcExpression);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);

        quantity.setLayoutTokens(Arrays.asList(tokens.get(11), tokens.get(12)));
        temperature.setAtomicQuantity(quantity);

//        List<Pair<String, List<LayoutToken>>> tcExpressionList = extractedEntities.stream()
//                .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL)))
//                .map(tc -> new ImmutablePair<>(tc.getName(), tc.getLayoutTokens()))
//                .collect(Collectors.toList());


        List<Measurement> measurements = target.markCriticalTemperatures(Arrays.asList(temperature), tokens, new ArrayList<>());

        assertThat(measurements, hasSize(1));
        assertThat(measurements.get(0).getQuantifiedObject(), is(not(nullValue())));
        assertThat(measurements.get(0).getQuantifiedObject().getNormalizedName(), is("Critical Temperature"));
    }

    @Test
    public void testMarkTemperature_noExpressions_MultiSentence_match_shouldWork() {
        String text = "We are explaining some important notions. The material BaClE2 superconducts at 30K. What about going for a beer? ";

        List<Superconductor> extractedEntities = new ArrayList<>();

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(tokens.subList(17, 20));
        superconductor.setType("material");
        extractedEntities.add(superconductor);

//        Superconductor tcExpression = new Superconductor();
//        tcExpression.setName("Tc");
//        tcExpression.setLayoutTokens(Arrays.asList(tokens.get(2)));
//        tcExpression.setType("tc");
//        extractedEntities.add(tcExpression);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);

        quantity.setLayoutTokens(tokens.subList(24, 26));
        temperature.setAtomicQuantity(quantity);

//        List<Pair<String, List<LayoutToken>>> tcExpressionList = extractedEntities.stream()
//                .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL)))
//                .map(tc -> new ImmutablePair<>(tc.getName(), tc.getLayoutTokens()))
//                .collect(Collectors.toList());


        List<Measurement> measurements = target.markCriticalTemperatures(Arrays.asList(temperature), tokens, new ArrayList<>());

        assertThat(measurements, hasSize(1));
        assertThat(measurements.get(0).getQuantifiedObject(), is(not(nullValue())));
        assertThat(measurements.get(0).getQuantifiedObject().getNormalizedName(), is("Critical Temperature"));
    }

    @Test
    public void testMarkTemperature_expression_match_shouldWork() {
        String text = "The material BaClE2 has Tc at 30K";

        List<Superconductor> extractedEntities = new ArrayList<>();

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(4), tokens.get(5), tokens.get(6)));
        superconductor.setType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL);
        extractedEntities.add(superconductor);

        Superconductor tcExpression = new Superconductor();
        tcExpression.setName("Tc");
        tcExpression.setLayoutTokens(Arrays.asList(tokens.get(9)));
        tcExpression.setType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL);
        extractedEntities.add(tcExpression);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);
        quantity.setLayoutTokens(Arrays.asList(tokens.get(13), tokens.get(14)));
        temperature.setAtomicQuantity(quantity);

        List<Pair<String, List<LayoutToken>>> tcExpressionList = extractedEntities.stream()
            .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL)))
            .map(tc -> new ImmutablePair<>(tc.getName(), tc.getLayoutTokens()))
            .collect(Collectors.toList());

        List<Measurement> measurements = target.markCriticalTemperatures(Arrays.asList(temperature), tokens, tcExpressionList);

        assertThat(measurements, hasSize(1));
        assertThat(measurements.get(0).getQuantifiedObject(), is(not(nullValue())));
        assertThat(measurements.get(0).getQuantifiedObject().getNormalizedName(), is("Critical Temperature"));

    }

    @Test
    public void testMarkTemperature_expression_nonRelativeNumber_Nomatch_shouldWork() {
        String text = "The material BaClE2 has Tc at 30K higher than";

        List<Superconductor> extractedEntities = new ArrayList<>();

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        Superconductor superconductor = new Superconductor();
        superconductor.setName("BaClE2");
        superconductor.setLayoutTokens(Arrays.asList(tokens.get(4), tokens.get(5), tokens.get(6)));
        superconductor.setType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL);
        extractedEntities.add(superconductor);

        Superconductor tcExpression = new Superconductor();
        tcExpression.setName("Tc");
        tcExpression.setLayoutTokens(Arrays.asList(tokens.get(9)));
        tcExpression.setType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL);
        extractedEntities.add(tcExpression);

        Measurement temperature = new Measurement();
        temperature.setType(UnitUtilities.Measurement_Type.VALUE);
        Quantity quantity = new Quantity("30", new Unit("K"));
        Unit parsedUnit = new Unit("K");
        UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnit.setUnitDefinition(parsedUnitDefinition);
        quantity.setParsedUnit(parsedUnit);
        quantity.setLayoutTokens(Arrays.asList(tokens.get(13), tokens.get(14)));
        temperature.setAtomicQuantity(quantity);

        List<Pair<String, List<LayoutToken>>> tcExpressionList = extractedEntities.stream()
            .filter(s -> s.getType().equals(GenericTaggerUtils.getPlainLabel(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_LABEL)))
            .map(tc -> new ImmutablePair<>(tc.getName(), tc.getLayoutTokens()))
            .collect(Collectors.toList());

        List<Measurement> measurements = target.markCriticalTemperatures(Arrays.asList(temperature), tokens, tcExpressionList);

        assertThat(measurements, hasSize(1));
        assertThat(measurements.get(0).getQuantifiedObject(), is(nullValue()));

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

        String s = target.toFormattedString(layoutTokens);

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