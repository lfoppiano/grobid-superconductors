package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void testNormaliseAndCleanup() throws Exception {
        String input = "This is a string with   many   double spaces \n\n and double \n which we remove ";

        List<LayoutToken> bodyLayouts = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> layoutTokens = GrobidPDFEngine.normaliseAndCleanup(bodyLayouts);

        assertThat(Iterables.getLast(layoutTokens).getText(), is(" "));

        assertThat(layoutTokens, hasSize(26));
    }

    @Test
    public void testNormaliseAndCleanup2() throws Exception {
        String input = "This is a string with   many   double spaces \n\n and double \n which we remove, ";

        List<LayoutToken> bodyLayouts = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<LayoutToken> layoutTokens = GrobidPDFEngine.normaliseAndCleanup(bodyLayouts);

        assertThat(Iterables.getLast(layoutTokens).getText(), is(" "));

        assertThat(layoutTokens, hasSize(27));
    }
}