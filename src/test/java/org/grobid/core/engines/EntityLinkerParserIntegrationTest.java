package org.grobid.core.engines;

import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class EntityLinkerParserIntegrationTest {

    EntityLinkerParser target;
    SuperconductorsParser superParser;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = new EntityLinkerParser();
        superParser = new SuperconductorsParser(new ChemDataExtractorClient("falcon.nims.go.jp"));
    }

    @Test
    public void test() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<Superconductor> annotations = superParser.process(layoutTokens);
        List<Superconductor> links = target.process(layoutTokens, annotations);
    }

}