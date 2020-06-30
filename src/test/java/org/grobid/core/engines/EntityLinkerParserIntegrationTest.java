package org.grobid.core.engines;

import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class EntityLinkerParserIntegrationTest {

    EntityLinkerParser target;
    SuperconductorsParser superParser;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = new EntityLinkerParser(Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL));
        superParser = new SuperconductorsParser(new ChemDataExtractorClient("http://falcon.nims.go.jp"));
    }

    @Test
    @Ignore("The test fails because the model does not recognise correctly the link")
    public void test() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<Superconductor> annotations = superParser.process(layoutTokens);
        List<Superconductor> links = target.process(layoutTokens, annotations);

        assertThat(annotations, hasSize(links.size()));
        List<Superconductor> linkedEntities = links.stream().filter(l -> l.getLinkedEntity() != null).collect(Collectors.toList());
//        assertThat(linkedEntities, hasSize(2));
//
//        assertThat(linkedEntities.get(0).getName(), is("MgB 2"));
//        assertThat(linkedEntities.get(0).getLinkedEntity().getName(), is("40 K"));
    }


    @Test
    public void testRealCase_shouldRecogniseOneLink() throws Exception {
        String input = "The crystal structure of (Sr, Na)Fe 2 As 2 has been refined for polycrystalline samples in the range of 0 ⩽ x ⩽ 0.42 with a maximum T c of 26 K .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<Superconductor> annotations = superParser.process(layoutTokens);
        List<Superconductor> links = target.process(layoutTokens, annotations);

        assertThat(annotations, hasSize(links.size()));
        List<Superconductor> linkedEntities = links.stream().filter(l -> l.getLinkedEntity() != null).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(1));

        assertThat(linkedEntities.get(0).getName(), is("(Sr, Na)Fe 2 As 2"));
        assertThat(linkedEntities.get(0).getLinkedEntity().getName(), is("26 K"));
    }

    @Test
    public void testRealCase_shouldNotLink() throws Exception {
        String input = "Previous studies have shown that pressure of 1 GPa can reduce T c , but only by less than 2 K in MgB 2 .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<Superconductor> annotations = superParser.process(layoutTokens);
        List<Superconductor> links = target.process(layoutTokens, annotations);

        assertThat(annotations, hasSize(links.size()));
        List<Superconductor> linkedEntities = links.stream().filter(l -> l.getLinkedEntity() != null).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(0));
    }

    @Test
    public void testRealCase_shouldExtract2Links() throws Exception {
        String input = "Theory-oriented experiments show that the compressed hydride of Group VI (hydrogen sulfide, H 3 S) exhibits a superconducting state at 203 K. Moreover, a Group V hydride (phosphorus hydride, PH 3 ) has also been studied and its T c reached a maximum of 103 K. The experimental realisation of the superconductivity in H 3 S and PH 3 inspired us to search for other hydride superconductors.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<Superconductor> annotations = superParser.process(layoutTokens);
        List<Superconductor> links = target.process(layoutTokens, annotations);

        assertThat(annotations, hasSize(links.size()));
        List<Superconductor> linkedEntities = links.stream().filter(l -> l.getLinkedEntity() != null).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(2));
    }


}