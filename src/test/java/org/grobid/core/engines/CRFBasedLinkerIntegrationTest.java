package org.grobid.core.engines;

import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.TextPassage;
import org.grobid.core.data.Span;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRFBasedLinkerIntegrationTest {

    private CRFBasedLinker target;
    private ModuleEngine entityParser;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = new CRFBasedLinker(SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC, Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL));
        SuperconductorsParser superParser = new SuperconductorsParser(new ChemDataExtractorClient("http://falcon.nims.go.jp"), new MaterialParser(null));
        this.entityParser = new ModuleEngine(superParser, QuantityParser.getInstance(true), null, null);
    }

    @Test
    @Ignore("The test fails because the model does not recognise correctly the link")
    public void test() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
//        List<Span> annotations = entityParser.process(layoutTokens, true);
//        target.process(layoutTokens, annotations);
//
//        assertThat(annotations, hasSize(annotations.size()));
//        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());


//        assertThat(linkedEntities, hasSize(2));
//
//        assertThat(linkedEntities.get(0).getName(), is("MgB 2"));
//        assertThat(linkedEntities.get(0).getLinkedEntity().getName(), is("40 K"));
    }


    @Test
    @Ignore("failing test ")
    public void testRealCase_shouldRecogniseOneLink() throws Exception {
        String input = "The crystal structure of (Sr, Na)Fe 2 As 2 has been refined for polycrystalline samples in the range of 0 ⩽ x ⩽ 0.42 with a maximum T c of 26 K .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<TextPassage> paragraphs = entityParser.process(layoutTokens, true);
        List<Span> annotations = paragraphs.get(0).getSpans();
        target.process(layoutTokens, annotations);

        assertThat(annotations, hasSize(annotations.size()));
        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(1));

        assertThat(linkedEntities.get(0).getText(), is("(Sr, Na)Fe 2 As 2"));
        String linkId = linkedEntities.get(0).getLinks().get(0).getTargetId();
        Optional<Span> linkedSpan = linkedEntities.stream().filter(le -> String.valueOf(le.getId()).equals(linkId)).findFirst();
        assertThat(linkedSpan.isPresent(), is(true));
        assertThat(linkedSpan.get().getText(), is("26 K"));
    }

    @Test
    public void testRealCase_shouldNotLink() throws Exception {
        String input = "Previous studies have shown that pressure of 1 GPa can reduce T c , but only by less than 2 K in MgB 2 .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<TextPassage> paragraphs = entityParser.process(layoutTokens, true);
        List<Span> annotations = paragraphs.get(0).getSpans();
        target.process(layoutTokens, annotations);

        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(0));
    }

    @Test
    public void testRealCase_shouldExtract2Links() throws Exception {
        String input = "Theory-oriented experiments show that the compressed hydride of Group VI (hydrogen sulfide, H 3 S) exhibits a superconducting state at 203 K. Moreover, a Group V hydride (phosphorus hydride, PH 3 ) has also been studied and its T c reached a maximum of 103 K. The experimental realisation of the superconductivity in H 3 S and PH 3 inspired us to search for other hydride superconductors.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<TextPassage> paragraphs = entityParser.process(layoutTokens, true);
        List<Span> annotations = paragraphs.get(0).getSpans();

        // Set the materials to be linkable
        paragraphs.get(0).getSpans().stream()
            .filter(s -> Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL).contains(s.getType()))
            .forEach(s -> s.setLinkable(true));

        target.process(layoutTokens, annotations);

        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks()) && l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL)).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(2));
    }


}