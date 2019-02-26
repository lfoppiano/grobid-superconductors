package org.grobid.core.analyzers;

import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class DeepAnalyzerTest {

    private DeepAnalyzer target;

    @Before
    public void setUp() throws Exception {
        target = DeepAnalyzer.getInstance();
    }

    @Test
    public void testTokenize_1() throws Exception {
        List<LayoutToken> tokens = target.tokenizeWithLayoutToken("This is a sample text, with 1.5m of intelligence.");

        List<LayoutToken> reTokens = target.retokenizeLayoutTokens(tokens);

        assertThat(tokens, hasSize(reTokens.size()));

        for (int i = 0; i < reTokens.size(); i++) {
            assertThat(tokens.get(i).getOffset(), is(reTokens.get(i).getOffset()));
        }

    }

}