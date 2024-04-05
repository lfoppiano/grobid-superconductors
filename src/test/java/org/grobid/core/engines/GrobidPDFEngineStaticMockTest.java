package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.document.DocumentBlock;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.grobid.core.engines.CRFBasedLinkerIntegrationTest.initEngineForTests;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.powermock.api.easymock.PowerMock.mockStatic;

/**
 * Same as GrobidPDFEngineTest but we will use this to mock the SentenceSplitter static call
 **/

@RunWith(PowerMockRunner.class)
@PrepareForTest(SentenceUtilities.class)
public class GrobidPDFEngineStaticMockTest {

    @BeforeClass
    public static void before() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
    }

    @Test
    public void testGetSentenceAsIndex_realExample() throws Exception {

        SentenceUtilities mockedSentenceUtilities = EasyMock.createMock(SentenceUtilities.class);
        String text = "The process of micellization is an important and interesting problem, whose mechanism is not yet understood. Its complexity does not allow to describe it completely. If the concentration n 1 of surfactant monomers (amphiphiles) ex- ceeds the critical micellization concentration, the monomers can form aggregates called micelles. There are different structures of micelles (spherical, cylindrical, disk-like, inverse, etc., see [1]), and they are classified by n 1 . For a relatively small concentration n 1 , the spherical structure is the most favorable 2 . There are two essential advantages in this case. First, the concentration of monomers is small and therefore one can apply some useful approximations. Second, the geometry of micelles is described by the only parameter -the radius of the sphere. Moreover, this radius can be expressed in terms of aggregation num- ber (the number of monomers which form an aggregate). Consequently, it is natural to use methods of the nucleation theory and, particularly, the notion of aggregation work. One can find its description and applications in [2]-[6] that investigate thermodynamic properties of micelles. ";
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<OffsetPosition> markersAsOffsets = Arrays.asList(new OffsetPosition(428, 431), new OffsetPosition(556, 558), new OffsetPosition(1096, 1099), new OffsetPosition(1100, 1104));
        DocumentBlock block = new DocumentBlock(tokens, "A", "B");

        List<OffsetPosition> sentences = Arrays.asList(new OffsetPosition(0, 108), new OffsetPosition(109, 165), new OffsetPosition(166, 329), new OffsetPosition(330, 466), new OffsetPosition(467, 559), new OffsetPosition(560, 608), new OffsetPosition(609, 710), new OffsetPosition(711, 805), new OffsetPosition(806, 927), new OffsetPosition(928, 1046), new OffsetPosition(1047, 1158));

        mockStatic(SentenceUtilities.class);
        expect(SentenceUtilities.getInstance()).andReturn(mockedSentenceUtilities);
        expect(mockedSentenceUtilities.runSentenceDetection(anyString(), anyObject(), anyObject(), anyObject())).andReturn(sentences);

        PowerMock.replay(SentenceUtilities.class, mockedSentenceUtilities);
        List<Pair<Integer, Integer>> sentencesOffsetsAsIndexes = GrobidPDFEngine.getSentencesOffsetsAsIndexes(block, markersAsOffsets);

        PowerMock.verify(SentenceUtilities.class, mockedSentenceUtilities);
        assertThat(sentencesOffsetsAsIndexes, hasSize(11));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(0).getLeft(), sentencesOffsetsAsIndexes.get(0).getRight())), is("The process of micellization is an important and interesting problem, whose mechanism is not yet understood."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(1).getLeft(), sentencesOffsetsAsIndexes.get(1).getRight())), is("Its complexity does not allow to describe it completely."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(2).getLeft(), sentencesOffsetsAsIndexes.get(2).getRight())), is("If the concentration n 1 of surfactant monomers (amphiphiles) ex- ceeds the critical micellization concentration, the monomers can form aggregates called micelles."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(3).getLeft(), sentencesOffsetsAsIndexes.get(3).getRight())), is("There are different structures of micelles (spherical, cylindrical, disk-like, inverse, etc., see [1]), and they are classified by n 1 ."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(4).getLeft(), sentencesOffsetsAsIndexes.get(4).getRight())), is("For a relatively small concentration n 1 , the spherical structure is the most favorable 2 ."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(5).getLeft(), sentencesOffsetsAsIndexes.get(5).getRight())), is("There are two essential advantages in this case."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(6).getLeft(), sentencesOffsetsAsIndexes.get(6).getRight())), is("First, the concentration of monomers is small and therefore one can apply some useful approximations."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(7).getLeft(), sentencesOffsetsAsIndexes.get(7).getRight())), is("Second, the geometry of micelles is described by the only parameter -the radius of the sphere."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(8).getLeft(), sentencesOffsetsAsIndexes.get(8).getRight())), is("Moreover, this radius can be expressed in terms of aggregation num- ber (the number of monomers which form an aggregate)."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(9).getLeft(), sentencesOffsetsAsIndexes.get(9).getRight())), is("Consequently, it is natural to use methods of the nucleation theory and, particularly, the notion of aggregation work."));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentencesOffsetsAsIndexes.get(10).getLeft(), sentencesOffsetsAsIndexes.get(10).getRight())), is("One can find its description and applications in [2]-[6] that investigate thermodynamic properties of micelles."));
    }
}