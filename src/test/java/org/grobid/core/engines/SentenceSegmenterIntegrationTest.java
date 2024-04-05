package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.grobid.core.engines.CRFBasedLinkerIntegrationTest.initEngineForTests;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

public class SentenceSegmenterIntegrationTest {

    SentenceSegmenter target;

    @Before
    public void setUp() throws Exception {
        initEngineForTests();
        target = new SentenceSegmenter();
    }


    @Test
    public void testGetSentences_simpleText() throws Exception {
        String originalText = "This is one sentence. This is another sentence. And a third sentence. ";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance()
            .tokenizeWithLayoutToken(originalText);

        List<String> tokens = layoutTokens.stream().map(LayoutToken::getText).collect(Collectors.toList());

        List<OffsetPosition> sentences = target.detectSentencesAsOffsets(tokens);

        assertThat(sentences, hasSize(3));
        assertThat(originalText.substring(sentences.get(0).start, sentences.get(0).end), is("This is one sentence."));
        assertThat(originalText.substring(sentences.get(1).start, sentences.get(1).end), is("This is another sentence."));
        assertThat(originalText.substring(sentences.get(2).start, sentences.get(2).end), is("And a third sentence."));
    }
    

    @Test
    public void testGetSentencesAsLayoutToken() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance()
            .tokenizeWithLayoutToken("This is one sentence. This is another sentence. ");

        List<List<LayoutToken>> sentences = target.detectSentencesAsLayoutToken(tokens);

        assertThat(sentences, hasSize(2));
    }


    @Test
    public void testGetSentencesAsIndex() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is one sentence. This is another sentence. ");

        List<Pair<Integer, Integer>> sentences = target.detectSentencesAsIndex(tokens);

        assertThat(sentences, hasSize(2));

        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(0).getLeft(), sentences.get(0).getRight())), is("This is one sentence. "));
        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(1).getLeft(), sentences.get(1).getRight())), is("This is another sentence. "));
    }

    @Test
    public void testGetSentencesAsIndex_singleSentence() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("This is one sentence. ");

        List<Pair<Integer, Integer>> sentences = target.detectSentencesAsIndex(tokens);

        assertThat(sentences, hasSize(1));

        assertThat(LayoutTokensUtil.toText(tokens.subList(sentences.get(0).getLeft(), sentences.get(0).getRight())), is("This is one sentence. "));
    }

}