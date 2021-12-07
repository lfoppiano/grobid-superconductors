package org.grobid.core.utilities;

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

public class AdditionalLayoutTokensUtilTest {

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
        String originalText = "We have fabricated thin films of FeTe 1Àx Se x using a scotch-tape method. " +
            "The superconductivities of the thin films are different from each other although these films were fabricated from the same bulk sample. " +
            "The result clearly presents the inhomogeneous superconductivity in FeTe 1Àx Se x . " +
            "The difference might arise from inhomogeneity due to the excess Fe concentration. " +
            "The resistivity of a thin film with low excess Fe shows good superconductivity with the sharp superconducting-transition width and more isotropic superconductivity. ";

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

        assertThat(String.join("", tokens.subList(pairs.get(0).getLeft(), pairs.get(0).getRight())), is("We have fabricated thin films of FeTe 1Àx Se x using a scotch-tape method."));
        assertThat(String.join("", tokens.subList(pairs.get(0).getLeft(), pairs.get(0).getRight())), is(originalText.substring(sentencePositions.get(0).start, sentencePositions.get(0).end)));
        assertThat(String.join("", tokens.subList(pairs.get(1).getLeft(), pairs.get(1).getRight())), is("The superconductivities of the thin films are different from each other although these films were fabricated from the same bulk sample."));
        assertThat(String.join("", tokens.subList(pairs.get(1).getLeft(), pairs.get(1).getRight())), is(originalText.substring(sentencePositions.get(1).start, sentencePositions.get(1).end)));
        assertThat(String.join("", tokens.subList(pairs.get(2).getLeft(), pairs.get(2).getRight())), is("The result clearly presents the inhomogeneous superconductivity in FeTe 1Àx Se x ."));
        assertThat(String.join("", tokens.subList(pairs.get(2).getLeft(), pairs.get(2).getRight())), is(originalText.substring(sentencePositions.get(2).start, sentencePositions.get(2).end)));
        assertThat(String.join("", tokens.subList(pairs.get(3).getLeft(), pairs.get(3).getRight())), is("The difference might arise from inhomogeneity due to the excess Fe concentration."));
        assertThat(String.join("", tokens.subList(pairs.get(3).getLeft(), pairs.get(3).getRight())), is(originalText.substring(sentencePositions.get(3).start, sentencePositions.get(3).end)));
        assertThat(String.join("", tokens.subList(pairs.get(4).getLeft(), pairs.get(4).getRight())), is("The resistivity of a thin film with low excess Fe shows good superconductivity with the sharp superconducting-transition width and more isotropic superconductivity."));
        assertThat(String.join("", tokens.subList(pairs.get(4).getLeft(), pairs.get(4).getRight())), is(originalText.substring(sentencePositions.get(4).start, sentencePositions.get(4).end)));
    }


    @Test
    public void testFromOffsetsToIndexes_WithSpaces() throws Exception {
        String originalText = "This is one sentence. This is another sentence. And third sentence.  ";

        List<String> tokens = new ArrayList<>(DeepAnalyzer.getInstance()
            .tokenize(originalText));

        List<OffsetPosition> offsets = Arrays.asList(
            new OffsetPosition(0, 21),
            new OffsetPosition(22, 47),
            new OffsetPosition(48, 68)
        );

        List<Pair<Integer, Integer>> pairs = AdditionalLayoutTokensUtil.fromOffsetsToIndexesOfTokensWithSpaces(offsets, tokens);

        assertThat(pairs, hasSize(3));
        assertThat(String.join("", tokens.subList(pairs.get(0).getLeft(), pairs.get(0).getRight())), is(originalText.substring(offsets.get(0).start, offsets.get(0).end)));
        assertThat(pairs.get(0).getLeft(), Is.is(0));
        assertThat(pairs.get(0).getRight(), Is.is(8));

        assertThat(String.join("", tokens.subList(pairs.get(1).getLeft(), pairs.get(1).getRight())), is(originalText.substring(offsets.get(1).start, offsets.get(1).end)));
        assertThat(pairs.get(1).getLeft(), Is.is(9));
        assertThat(pairs.get(1).getRight(), Is.is(17));

        assertThat(String.join("", tokens.subList(pairs.get(2).getLeft(), pairs.get(2).getRight())), is(originalText.substring(offsets.get(2).start, offsets.get(2).end)));
        assertThat(pairs.get(2).getLeft(), Is.is(18));
        assertThat(pairs.get(2).getRight(), Is.is(25));
    }


    @Test
    public void testFromOffsetsToIndexes_WithSpaces_errorCase() throws Exception {
        String originalText = "WHPSHUDWXUH ZKHUH WKHUH LV D PD[LPXP LQ WKH RXWRISKDVH $& PDJQHWLF VXVFHSWLELOLW\\ χ 0 E 7KLV LV WKH QDWXUDO ORJDULWKP RI WKH LQYHUVH RI WKH PDJQHWL]DWLRQ UHOD[DWLRQ FDOFXODWHG IURP WKH IUHTXHQF\\ RI WKH $& PDJQHWLF ILHOG )LJXUH &DSWLRQV )LJXUH 3ORW RI WKH SRWHQWLDO HQHUJ\\ YV PDJQHWL]DWLRQ GLUHFWLRQ IRU D VLQJOHPROHFXOH PDJQHW VKRZLQJ WKH HQHUJ\\ EDUULHU VHSDUDWLQJ WKH VSLQXS DQG VSLQGRZQ RULHQWDWLRQV RI WKH PDJQHWLF YHFWRU DORQJ WKH DQLVRWURS\\ D[LV )LJXUH 'LDJUDP RI WKH OLJDQGV XVHG LQ WKLV ZRUN )LJXUH 257(3 UHSUHVHQWDWLRQ RI WKH FDWLRQ RI FRPSOH[ >0Q KPS %U + 2 @%U + 2 )LJXUH 257(3 UHSUHVHQWDWLRQ RI FRPSOH[ >0Q PHKPS &O @ + 2 )LJXUH 9LHZ RI WKH FU\\VWDO SDFNLQJ RI WKH 0Q PROHFXOHV RI FRPSOH[ 7KH GDUN UHG VSKHUHV DUH WKH EURPLGH FRXQWHULRQV DQG WKH OLJKW UHG VSKHUHV DUH WKH VROYDWH ZDWHU PROHFXOHV )LJXUH 9LHZ RI WKH WZR 0Q PROHFXOHV RI FRPSOH[ LQ WKH XQLW FHOO UHODWHG E\\ D JOLGH SODQH 7KH HDV\\ PROHFXODU D[HV RI WKHVH PROHFXOHV DUH UHODWHG E\\ D FDQWLQJ DQJOH RI ° )LJXUH 9LHZ RI WKH FU\\VWDO SDFNLQJ RI FRPSOH[ 7KH UHG VSKHUHV DUH WKH VROYDWH ZDWHU PROHFXOHV ZLWKLQ WKH XQLW FHOO )LJXUH 3ORW RI 0 7 YV WHPSHUDWXUH IRU D PLFURFU\\VWDOOLQH VDPSOH RI FRPSOH[ UHVWUDLQHG LQ HLFRVDQH 7KH VXVFHSWLELOLW\\ χ 0 ZDV PHDVXUHG XQGHU D N* PDJQHWLF ILHOG ZLWK WKH VDPSOH UHVWUDLQHG LQ HLFRVDQH 7KH VROLG OLQH FRUUHVSRQGV WR WKH ILW RI WKH GDWD DV GHVFULEHG LQ WKH WH[W )LJXUH 'LDJUDP VKRZLQJ WKH WZR GRPLQDQW PDJQHWLF H[FKDQJH SDWKZD\\V EHWZHHQ PHWDO FHQWHUV LQ FRPSOH[ )LJXUH 3ORW RI χ 0 7 YV WHPSHUDWXUH IRU D PLFURFU\\VWDOOLQH VDPSOH RI FRPSOH[ UHVWUDLQHG LQ HLFRVDQH 7KH VXVFHSWLELOLW\\ 0 ZDV PHDVXUHG XQGHU D N* PDJQHWLF ILHOG ZLWK WKH VDPSOH UHVWUDLQHG LQ HLFRVDQH 7KH VROLG OLQH FRUUHVSRQGV WR WKH ILW RI WKH GDWD DV GHVFULEHG LQ WKH WH[W )LJXUH 'LDJUDP VKRZLQJ WKH GRPLQDQW PDJQHWLF H[FKDQJH SDWKZD\\V EHWZHHQ PHWDO FHQWHUV FRPSOH[ 3ORW RI WKH UHGXFHG PDJQHWL]DWRQ 01µ % YV WKH UDWLR RI H[WHUQDO ILHOG DQG WKH DEVROXWH WHPSHUDWXUH >N*.@ 'DWD IRU FRPSOH[ WRS DQG FRPSOH[ ERWWRP ZHUH PHDVXUHG DW s q v DQG w N* LQ WKH . UDQJH 7KH VROLG OLQHV FRUUHVSRQG WR WKH ILW VHH WH[W )LJXUH 3ORW RI WKH HQHUJ\\ YV WKH H[WHUQDO ILHOG + IRU WKH ]HURILHOG VSOLW FRPSRQHQWV RI WKH 6 JURXQG VWDWH DVVXPLQJ 'N % . DQG J 7KH HQHUJ\\ OHYHO VSOLWWLQJ LV IRU D PROHFXOH ZLWK LWV SULQFLSOH PDJQHWLF DOLJQHG DORQJ WKH GLUHFWLRQ RI DSSOLHG PDJQHWLF ILHOG )LJXUH +LJKIUHTXHQF\\ (35 VSHFWUD IRU D RULHQWHG PLFURFU\\VWDOOLQH VDPSOH RI FRPSOH[ FROOHFWHG DW *+] DQG WHPSHUDWXUHV RI DQG . 7KH DVWHULFNV WKH ILQH VWUXFWXUH UHVRQDQFHV GXH WR WKH WUDQVLWLRQV EHWZHHQ 0V OHYHOV RI WKH JURXQG VWDWH +LJKIUHTXHQF\\ (35 VSHFWUD IRU D PLFURFU\\VWDOOLQH VDPSOH RI FRPSOH[ FROOHFWHG DW DQG *+] DQG WHPSHUDWXUH RI . +LJKIUHTXHQF\\ (35 VSHFWUD IRU D PLFURFU\\VWDOOLQH VDPSOH RI FRPSOH[ FROOHFWHG DW DQG *+] DQG WHPSHUDWXUH RI . )LJXUH 3ORWV RI UHVRQDQFH ILHOG YV 0 V QXPEHU IRU WKH +)(35 WUDQVLWLRQV EHWZHHQ WKH 0 V DQG 0 V ]HURILHOG FRPSRQHQWV RI WKH 6 JURXQG VWDWH IRU FRPSOH[ +)(35 GDWD ZHUH PHDVXUHG DW ORZHVW OLQH PLGGOH OLQH DQG KLJKHVW OLQH *+] DW . 7KH VROLG OLQHV UHSUHVHQW D ILW RI WKH GDWD DV GHVFULEHG LQ WKH WH[W )LJXUH +)(35 VSHFWUXP DW *+] DW . WRS DQG VLPXODWLRQ RI WKH VSHFWUXP ERWWRP 6LPXODWLRQ UHVXOWV DUH GHVFULEHG IXOO\\ LQ WKH WH[W )LJXUH 6LPXODWLRQ RI WKH +)(35 VSHFWUD DW DQG *+] IUHTXHQFLHV DW . WH[W IRU GHWDLOV )LJXUH 3ORW RI χ DQG χ YV WHPSHUDWXUH RI WKH $& PDJQHWLF VXVFHSWLELOLW\\ DW WKH LQGLFDWHG LQ WKH . UDQJH )LJXUH 3ORWV RI χ 0 7 WRS DQG 0 ERWWRP YV WHPSHUDWXUH IRU D SRO\\FU\\VWDOOLQH VDPSOH RI FRPSOH[ LQ D * $& ILHOG RVFLOODWLQJ DW WKH LQGLFDWHG IUHTXHQFLHV ZKHUH χ 0 DQG χ 0 DUH WKH LQSKDVH DQG RXWRISKDVH FRPSRQHQWV UHVSHFWLYHO\\ RI WKH $& PDJQHWLF VXVFHSWLELOLW\\ )LJXUH 3ORW RI WKH LQSKDVH WRS DQG RXWRISKDVH ERWWRP FRPSRQHQWV RI WKH $& PDJQHWLF IRU FRPSOH[ LQ WKH . UDQJH LQ D * $& ILHOG RVFLOODWLQJ DW +] )LJXUH 3ORW RI QDWXUDO ORJRULWKP RI WKH PDJQHWL]DWLRQ UHOD[DWLRQ UDWH >OQτ@ YV WKH LQYHUVH RI WKH DEVROXWH WHPSHUDWXUH >. @ 7KH VROLG OLQH UHSUHVHQWV D OHDVWVTXDUHV ILW RI WKH GDWD WR WKH HTXDWLRQ RI WKH FU\\VWDO SDFNLQJ RI WKH 0Q PROHFXOHV RI FRPSOH[ 7KH GDUN UHG DUH WKH EURPLGH FRXQWHULRQV DQG WKH OLJKW UHG VSKHUHV DUH WKH VROYDWH ZDWHU PROHFXOHV )LJXUH 9LHZ RI WKH WZR 0Q PROHFXOHV RI FRPSOH[ LQ WKH XQLW FHOO UHODWHG E\\ D JOLGH SODQH HDV\\ PROHFXODU D[HV RI WKHVH PROHFXOHV DUH UHODWHG E\\ D FDQWLQJ DQJOH RI ° )LJXUH 9LHZ RI WKH FU\\VWDO SDFNLQJ RI FRPSOH[ 7KH UHG VSKHUHV DUH WKH VROYDWH ZDWHU PROHFXOHV ZLWKLQ WKH XQLW FHOO $WRPLF FRRUGLQDWHV [\\] IRU FRPSOH[ DW \" . 0";

        List<String> tokens = new ArrayList<>(DeepAnalyzer.getInstance()
            .tokenize(originalText));

        List<OffsetPosition> offsets = Arrays.asList(
            new OffsetPosition(0, 1934),
            new OffsetPosition(1935, 2015),
            new OffsetPosition(2016, 2191),
            new OffsetPosition(2192, 2450),
            new OffsetPosition(2451, 2664),
            new OffsetPosition(2665, 2773),
            new OffsetPosition(2774, 3002),
            new OffsetPosition(3003, 3105),
            new OffsetPosition(3106, 3265),
            new OffsetPosition(3266, 3380),
            new OffsetPosition(3381, 3748),
            new OffsetPosition(3749, 4459),
            new OffsetPosition(4460, 4461)
        );

        List<Pair<Integer, Integer>> pairs = AdditionalLayoutTokensUtil.fromOffsetsToIndexesOfTokensWithSpaces(offsets, tokens);

        assertThat(pairs, hasSize(offsets.size()));
        assertThat(String.join("", tokens.subList(pairs.get(0).getLeft(), pairs.get(0).getRight())), is(originalText.substring(offsets.get(0).start, offsets.get(0).end)));
        assertThat(String.join("", tokens.subList(pairs.get(1).getLeft(), pairs.get(1).getRight())), is(originalText.substring(offsets.get(1).start, offsets.get(1).end)));
        assertThat(String.join("", tokens.subList(pairs.get(2).getLeft(), pairs.get(2).getRight())), is(originalText.substring(offsets.get(2).start, offsets.get(2).end)));
        assertThat(String.join("", tokens.subList(pairs.get(3).getLeft(), pairs.get(3).getRight())), is(originalText.substring(offsets.get(3).start, offsets.get(3).end)));
        assertThat(String.join("", tokens.subList(pairs.get(4).getLeft(), pairs.get(4).getRight())), is(originalText.substring(offsets.get(4).start, offsets.get(4).end)));
        assertThat(String.join("", tokens.subList(pairs.get(5).getLeft(), pairs.get(5).getRight())), is(originalText.substring(offsets.get(5).start, offsets.get(5).end)));
        assertThat(String.join("", tokens.subList(pairs.get(6).getLeft(), pairs.get(6).getRight())), is(originalText.substring(offsets.get(6).start, offsets.get(6).end)));
        assertThat(String.join("", tokens.subList(pairs.get(7).getLeft(), pairs.get(7).getRight())), is(originalText.substring(offsets.get(7).start, offsets.get(7).end)));
        assertThat(String.join("", tokens.subList(pairs.get(8).getLeft(), pairs.get(8).getRight())), is(originalText.substring(offsets.get(8).start, offsets.get(8).end)));
        assertThat(String.join("", tokens.subList(pairs.get(9).getLeft(), pairs.get(9).getRight())), is(originalText.substring(offsets.get(9).start, offsets.get(9).end)));
        assertThat(String.join("", tokens.subList(pairs.get(10).getLeft(), pairs.get(10).getRight())), is(originalText.substring(offsets.get(10).start, offsets.get(10).end)));
        assertThat(String.join("", tokens.subList(pairs.get(11).getLeft(), pairs.get(11).getRight())), is(originalText.substring(offsets.get(11).start, offsets.get(11).end)));
        assertThat(String.join("", tokens.subList(pairs.get(12).getLeft(), pairs.get(12).getRight())), is(originalText.substring(offsets.get(12).start, offsets.get(12).end)));

    }


}