package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SentenceSegmenterTest {

    SentenceSegmenter target;

    @Before
    public void setUp() throws Exception {
        target = new SentenceSegmenter();
    }

    @Test
    public void testGetSentences() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is one sentence. This is another sentence. ");

        List<List<LayoutToken>> sentences = target.getSentencesAsLayoutToken(tokens);

        assertThat(sentences, hasSize(2));
    }


    @Test
    public void testGetSentencesAsIndex() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is one sentence. This is another sentence. ");

        List<Pair<Integer, Integer>> sentences = target.getSentencesAsIndex(tokens);

        assertThat(sentences, hasSize(2));

        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(0).getLeft(), sentences.get(0).getRight())), is("This is one sentence. "));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(1).getLeft(), sentences.get(1).getRight())), is("This is another sentence. "));
    }

    @Test
    public void testGetSentencesAsIndex_singleSentence() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is one sentence. ");

        List<Pair<Integer, Integer>> sentences = target.getSentencesAsIndex(tokens);

        assertThat(sentences, hasSize(1));

        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(0).getLeft(), sentences.get(0).getRight())), is("This is one sentence. "));
    }

}