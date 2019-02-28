package org.grobid.core.engines;

import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.chemspot.Mention;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemspotClient;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static jersey.repackaged.com.google.common.base.CharMatcher.is;
import static org.easymock.EasyMock.*;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.*;

public class SuperconductorsParserTest {

    private SuperconductorsParser target;

    private ChemspotClient mockChemspotClient;

    @Before
    public void setUp() throws Exception {
        mockChemspotClient = EasyMock.createMock(ChemspotClient.class);

        LibraryLoader.load();
        target = new SuperconductorsParser(mockChemspotClient);
    }


    @Test
    public void testSynchroniseLayoutTokenWithMentions() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
                "compounds, where 14 group elements Si, Ge, and Sn forming the\n" +
                "framework with polyhedral cages are partially substituted with\n" +
                "lower valent elements such as ");
//        String text = LayoutTokensUtil.toText(tokens);

        List<Mention> mentions = Arrays.asList(new Mention(70, 72, "Si"), new Mention(74, 76, "Ge"));
//        expect(mockChemspotClient.processText(text)).andReturn(mentions);

//        ChemspotClient chemspotClient = new ChemspotClient("http://falcon.nims.go.jp/chemspot");
//        List<Mention> mentions = chemspotClient.processText(text);

//        replay(mockChemspotClient);
        List<Boolean> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

//        verify(mockChemspotClient);

        assertThat(booleans.stream().filter(b -> b).count(), greaterThan(0L));


    }
}