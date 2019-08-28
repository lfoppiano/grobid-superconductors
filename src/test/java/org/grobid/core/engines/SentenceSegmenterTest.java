package org.grobid.core.engines;

import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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

        List<List<LayoutToken>> sentences = target.getSentences(tokens);

        assertThat(sentences, hasSize(2));
    }

}