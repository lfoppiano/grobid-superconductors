package org.grobid.core.engines;

import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.chemspot.Mention;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemspotClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.SuperconductorsParser.NONE_CHEMSPOT_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

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
        List<Mention> mentions = Arrays.asList(new Mention(70, 72, "Si"), new Mention(74, 76, "Ge"));
        List<String> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        assertThat(booleans.stream().filter(b -> !b.equals(NONE_CHEMSPOT_TYPE)).count(), greaterThan(0L));
    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions_longMention() {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
                "compounds, where 14 group elements Si, Ge, and Sn forming the\n" +
                "framework with polyhedral cages are partially substituted with\n" +
                "lower valent elements such as ");
        List<Mention> mentions = Arrays.asList(new Mention(29, 44, "Zintl compounds"), new Mention(70, 72, "Si"), new Mention(74, 76, "Ge"));
        List<String> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        List<String> collect = booleans.stream().filter(b -> !b.equals(NONE_CHEMSPOT_TYPE)).collect(Collectors.toList());

        assertThat(collect, hasSize(5));

        List<LayoutToken> annotatedTokens = IntStream
                .range(0, tokens.size())
                .filter(i -> !booleans.get(i).equals(NONE_CHEMSPOT_TYPE))
                .mapToObj(tokens::get)
                .collect(Collectors.toList());

        assertThat(annotatedTokens, hasSize(5));
        assertThat(annotatedTokens.get(0).getText(), is("Zintl"));
        assertThat(annotatedTokens.get(1).getText(), is("\n"));
        assertThat(annotatedTokens.get(2).getText(), is("compounds"));
        assertThat(annotatedTokens.get(3).getText(), is("Si"));
        assertThat(annotatedTokens.get(4).getText(), is("Ge"));
    }

    @Test
    public void testSynchroniseLayoutTokenWithMentions_consecutives() {

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("Ge 30 can be classified into Zintl\n" +
                "compounds, where 14 group elements Si Ge, and Sn forming the\n" +
                "framework with polyhedral cages are partially substituted with\n" +
                "lower valent elements such as ");

        List<Mention> mentions = Arrays.asList(new Mention(70, 72, "Si"), new Mention(73, 75, "Ge"));
        List<String> booleans = target.synchroniseLayoutTokensWithMentions(tokens, mentions);

        List<String> collect = booleans.stream().filter(b -> !b.equals(NONE_CHEMSPOT_TYPE)).collect(Collectors.toList());

        assertThat(collect, hasSize(2));

        List<LayoutToken> annotatedTokens = IntStream
                .range(0, tokens.size())
                .filter(i -> !booleans.get(i).equals(NONE_CHEMSPOT_TYPE))
                .mapToObj(tokens::get)
                .collect(Collectors.toList());

        assertThat(annotatedTokens, hasSize(2));
        assertThat(annotatedTokens.get(0).getText(), is("Si"));
        assertThat(annotatedTokens.get(1).getText(), is("Ge"));
    }
}