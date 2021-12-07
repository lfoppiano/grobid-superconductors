package org.grobid.core.utilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class AdditionalLayoutTokensUtilTest{
    
    @Test
    public void testGetLayoutTokenListStartEndOffset() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence, but I need more information. ");

        List<LayoutToken> randomSubList = tokens.subList(4, 8);
        int layoutTokenListStartOffset = AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(randomSubList);
        int layoutTokenListEndOffset = AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(randomSubList);

        assertThat(LayoutTokensUtil.toText(tokens).substring(layoutTokenListStartOffset, layoutTokenListEndOffset),
            is(LayoutTokensUtil.toText(randomSubList)));
    }

    @Test
    public void testGetLayoutTokenStartEndOffset() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence, but I need more information. ");

        LayoutToken randomLayoutToken = tokens.get(4);

        int layoutTokenStartOffset = AdditionalLayoutTokensUtil.getLayoutTokenStartOffset(randomLayoutToken);
        int layoutTokenEndOffset = AdditionalLayoutTokensUtil.getLayoutTokenEndOffset(randomLayoutToken);

        assertThat(LayoutTokensUtil.toText(tokens).substring(layoutTokenStartOffset, layoutTokenEndOffset),
            is(LayoutTokensUtil.toText(Arrays.asList(randomLayoutToken))));
    }

    @Test
    public void testGetExtremitiesIndex_sub_shouldReturnSubset() {
        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 2, 7);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(3));

        String outputText = LayoutTokensUtil.toText(tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()));
        //Correcting the startOffset to include the whole layoutToken manually!
        String expected = LayoutTokensUtil.toText(tokens).substring(0, 7);
        assertThat(outputText, is(expected));

    }

    @Test
    public void testGetExtremitiesIndex_shouldReturnSubset() {
        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("The car is weighting only 10.77 grams. ");

        List<LayoutToken> layoutTokens = Arrays.asList(tokens.get(10), tokens.get(11), tokens.get(12));

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil
            .getExtremitiesAsIndex(tokens, AdditionalLayoutTokensUtil.getLayoutTokenListStartOffset(layoutTokens), AdditionalLayoutTokensUtil.getLayoutTokenListEndOffset(layoutTokens));

        assertThat(extremitiesSingle.getLeft(), is(10));
        assertThat(extremitiesSingle.getRight(), is(13));

        assertThat(LayoutTokensUtil.toText(tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight())), is("10.77"));
    }

    @Test
    public void testGetExtremitiesIndex_sub2_shouldReturnSubset() {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 2, 9);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(5));

        String outputText = LayoutTokensUtil.toText(tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()));
        assertThat(outputText, is("This is a"));
    }


    @Test
    public void testGetExtremitiesIndex_outerSet_shouldReturnWholeString() {
        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 0, 190);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(9));
        String outputText = LayoutTokensUtil.toText(tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()));
        assertThat(outputText, is("This is a short sentence"));
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testGetExtremitiesIndex_offsetsOutside_shouldThrowException() throws Exception {
//        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");
//
//        AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 10000, 19000);
//    }

    @Test
    public void testGetExtremitiesIndex_short_nearBeginning() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 5, 5, 3);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(6));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is("This is a "));
    }

    @Test
    public void testGetExtremitiesSingle_short_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 8, 8, 3);

        assertThat(extremitiesSingle.getLeft(), is(1));
        assertThat(extremitiesSingle.getRight(), is(8));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" is a short "));
    }

    @Test
    public void testGetExtremitiesSingle_long_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
    }


    @Test
    public void testGetExtremitiesSingle_long_centroidWithMultipleLayoutToken_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        Pair<Integer, Integer> extremitiesSingle = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
    }

//    @Test
//    public void testFromOffsetsToIndexes_withoutSpaces_realCase() throws Exception {
//        String originalText = "This is one sentence. This is another sentence. And third sentence. ";
//
//        List<String> tokens = DeepAnalyzer.getInstance().tokenize(originalText).stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
//        List<OffsetPosition> offsets = Arrays.asList(
//            new OffsetPosition(0, 21),
//            new OffsetPosition(22, 47),
//            new OffsetPosition(48, 67)
//        );
//        
//        List<Pair<Integer, Integer>> pairs = AdditionalLayoutTokensUtil.fromOffsetsToIndexesOfTokensWithoutSpaces(offsets, tokens);
//
//        assertThat(pairs, hasSize(3));
//        assertThat(String.join("", tokens.subList(pairs.get(0).getLeft(), pairs.get(0).getRight())), is(originalText.substring(offsets.get(0).start, offsets.get(0).end).replace(" ", "")));
//        assertThat(pairs.get(0).getLeft(), Is.is(0));
//        assertThat(pairs.get(0).getRight(), Is.is(5));
//
//        assertThat(String.join("", tokens.subList(pairs.get(1).getLeft(), pairs.get(1).getRight())), is(originalText.substring(offsets.get(1).start, offsets.get(1).end).replace(" ", "")));
//        assertThat(pairs.get(1).getLeft(), Is.is(5));
//        assertThat(pairs.get(1).getRight(), Is.is(10));
//
//        assertThat(String.join("", tokens.subList(pairs.get(2).getLeft(), pairs.get(2).getRight())), is(originalText.substring(offsets.get(2).start, offsets.get(2).end).replace(" ", "")));
//        assertThat(pairs.get(2).getLeft(), Is.is(10));
//        assertThat(pairs.get(2).getRight(), Is.is(14));
//    }

    @Test 
    public void testFromOffsetsToIndexes_withSpaces_realCase() throws Exception {
        String originalText = "We have fabricated thin films of FeTe 1Àx Se x using a scotch-tape method. The superconductivities of the thin films are different from each other although these films were fabricated from the same bulk sample. The result clearly presents the inhomogeneous superconductivity in FeTe 1Àx Se x . The difference might arise from inhomogeneity due to the excess Fe concentration. The resistivity of a thin film with low excess Fe shows good superconductivity with the sharp superconducting-transition width and more isotropic superconductivity. ";

        List<String> tokens = new ArrayList<>(DeepAnalyzer.getInstance()
            .tokenize(originalText));
        
        List<OffsetPosition> sentencePositions = new ArrayList<>();
        sentencePositions.add(new OffsetPosition(0, 74));
        sentencePositions.add(new OffsetPosition(75, 210));
        sentencePositions.add(new OffsetPosition(211, 293));
        sentencePositions.add(new OffsetPosition(294, 375));
        sentencePositions.add(new OffsetPosition(376, 540));

        List<Pair<Integer, Integer>> pairs = AdditionalLayoutTokensUtil.fromOffsetsToIndexesOfTokensWithSpaces(sentencePositions, tokens);
        
        assertThat(pairs, hasSize(5));
    }
    
    
    @Test
    public void testFromOffsetsToIndexes_WithSpaces() throws Exception {
        String originalText = "This is one sentence. This is another sentence. And third sentence. ";

        List<String> tokens = new ArrayList<>(DeepAnalyzer.getInstance()
            .tokenize(originalText));
        
        List<OffsetPosition> offsets = Arrays.asList(
            new OffsetPosition(0, 21),
            new OffsetPosition(22, 47),
            new OffsetPosition(48, 68)
        );

        List<Pair<Integer, Integer>> pairs = AdditionalLayoutTokensUtil.fromOffsetsToIndexesOfTokensWithSpaces(offsets, tokens);

        assertThat(pairs, hasSize(3));
        assertThat(pairs.get(0).getLeft(), Is.is(0));
        assertThat(pairs.get(0).getRight(), Is.is(8));

        assertThat(pairs.get(1).getLeft(), Is.is(8));
        assertThat(pairs.get(1).getRight(), Is.is(17));

        assertThat(pairs.get(2).getLeft(), Is.is(17));
        assertThat(pairs.get(2).getRight(), Is.is(24));
    }

}