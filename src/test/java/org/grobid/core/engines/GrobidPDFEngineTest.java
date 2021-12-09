package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.document.DocumentBlock;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class GrobidPDFEngineTest {

    @BeforeClass
    public static void before() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
    }

    @Test
    public void testIsNewParagraph_afterSection_shouldReturnTrue() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.SECTION);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(true));
    }

    @Test
    public void testIsNewParagraph_afterParagraph_shouldReturnTrue() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(true));
    }

    @Test
    public void testIsNewParagraph_afterFigure() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.FIGURE);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Ignore("Not needed anymore, as the tables are treated as paragraphs")
    public void testIsNewParagraph_afterTable() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.TABLE);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testIsNewParagraph_afterEquation_shouldReturnFalse() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.EQUATION);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testIsNewParagraph_afterMarker() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.CITATION_MARKER);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testIsNewParagraphFigureCaption_afterSection_shouldReturnTrue() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.SECTION);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(true));
    }

    @Test
    public void testIsNewParagraphFigureCaption_afterParagraph_shouldReturnTrue() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.PARAGRAPH);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(true));
    }

    @Test
    public void testIsNewParagraphFigureCaption_afterFigure() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.FIGURE);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Ignore("Not needed anymore")
    public void testIsNewParagraphFigureCaption_afterTable() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.TABLE);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testIsNewParagraphFigureCaption_afterEquation_shouldReturnFalse() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.EQUATION);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testIsNewParagraphFigureCaption_afterMarker() throws Exception {
        TaggingTokenCluster previousCluster = new TaggingTokenCluster(TaggingLabels.CITATION_MARKER);
        assertThat(GrobidPDFEngine.isNewParagraph(previousCluster, Arrays.asList(new LayoutToken(""))), is(false));
    }

    @Test
    public void testNormaliseAndCleanup_standardCase() throws Exception {
        String input = "This is a string with   many   double spaces \n\n and double \n which we remove ";

        List<LayoutToken> bodyLayouts = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> layoutTokens = GrobidPDFEngine.normaliseAndCleanup(bodyLayouts);

        assertThat(Iterables.getLast(layoutTokens).getText(), is(" "));

        assertThat(layoutTokens, hasSize(26));
        assertThat(LayoutTokensUtil.toText(layoutTokens), is("This is a string with many double spaces and double which we remove "));
    }

    @Test
    public void testNormaliseAndCleanup_withEndCommaAndSpace() throws Exception {
        String input = "This is a string with   many   double spaces \n\n and double \n which we remove, ";

        List<LayoutToken> bodyLayouts = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> layoutTokens = GrobidPDFEngine.normaliseAndCleanup(bodyLayouts);

        assertThat(Iterables.getLast(layoutTokens).getText(), is(" "));

        assertThat(layoutTokens, hasSize(27));
        assertThat(LayoutTokensUtil.toText(layoutTokens), is("This is a string with many double spaces and double which we remove, "));
    }

    @Test
    public void testNormaliseAndCleanup_withDuplicatedParenthesis() throws Exception {
        String input = "This is a string with   many   )) double spaces \n\n and double \n which we remove, ";

        List<LayoutToken> bodyLayouts = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> layoutTokens = GrobidPDFEngine.normaliseAndCleanup(bodyLayouts);

        assertThat(Iterables.getLast(layoutTokens).getText(), is(" "));

        assertThat(LayoutTokensUtil.toText(layoutTokens), is("This is a string with many )) double spaces and double which we remove, "));
        assertThat(layoutTokens, hasSize(30));
    }

    @Test
    public void testGetMarkersAsOffsets_realExample() throws Exception {
        String text = "The process of micellization is an important and interesting problem, whose mechanism is not yet understood. Its complexity does not allow to describe it completely. If the concentration n 1 of surfactant monomers (amphiphiles) ex- ceeds the critical micellization concentration, the monomers can form aggregates called micelles. There are different structures of micelles (spherical, cylindrical, disk-like, inverse, etc., see [1]), and they are classified by n 1 . For a relatively small concentration n 1 , the spherical structure is the most favorable 2 . There are two essential advantages in this case. First, the concentration of monomers is small and therefore one can apply some useful approximations. Second, the geometry of micelles is described by the only parameter -the radius of the sphere. Moreover, this radius can be expressed in terms of aggregation num- ber (the number of monomers which form an aggregate). Consequently, it is natural to use methods of the nucleation theory and, particularly, the notion of aggregation work. One can find its description and applications in [2]-[6] that investigate thermodynamic properties of micelles. ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Pair<Integer, Integer>> extremities = Arrays.asList(Pair.of(135, 138), Pair.of(380, 384));
//        extremities.stream().forEach(e -> System.out.println(tokens.subList(e.getLeft(), e.getRight()).stream().map(LayoutToken::getText).collect(Collectors.joining())));
        DocumentBlock block = new DocumentBlock(tokens, "a", "b");
        List<OffsetPosition> markersAsOffsets = GrobidPDFEngine.getMarkersAsOffsets(block, extremities);

        assertThat(markersAsOffsets, hasSize(2));
        assertThat(text.substring(markersAsOffsets.get(0).start, markersAsOffsets.get(0).end), is("[1]"));
        assertThat(text.substring(markersAsOffsets.get(1).start, markersAsOffsets.get(1).end), is("[6] "));
//        extremities.stream().forEach(m-> System.out.println(text.substring(m.getLeft(), m.getRight())));
    }


    @Test
    public void testGetMarkersAsOffsets_fakeCase() throws Exception {
        String text = "This is one sentence. [1] This another sentence, with another ref. [2] ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        
        List<Pair<Integer, Integer>> extremities = Arrays.asList(Pair.of(9, 12), Pair.of(27, 30));
//        extremities.stream().forEach(e -> System.out.println(tokens.subList(e.getLeft(), e.getRight()).stream().map(LayoutToken::getText).collect(Collectors.joining())));
        DocumentBlock block = new DocumentBlock(tokens, "a", "b");
        List<OffsetPosition> markersAsOffsets = GrobidPDFEngine.getMarkersAsOffsets(block, extremities);

        assertThat(markersAsOffsets, hasSize(2));
        assertThat(text.substring(markersAsOffsets.get(0).start, markersAsOffsets.get(0).end), is("[1]"));
        assertThat(text.substring(markersAsOffsets.get(1).start, markersAsOffsets.get(1).end), is("[2]"));
//        extremities.stream().forEach(m-> System.out.println(text.substring(m.getLeft(), m.getRight())));
    }
}