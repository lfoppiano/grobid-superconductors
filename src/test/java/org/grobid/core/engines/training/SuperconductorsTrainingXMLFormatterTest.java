package org.grobid.core.engines.training;

import nu.xom.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.document.DocumentBlock;
import org.grobid.core.data.document.Span;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.AdditionalLayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SuperconductorsTrainingXMLFormatterTest {

    SuperconductorsTrainingXMLFormatter target;

    @Before
    public void setUp() throws Exception {
        target = new SuperconductorsTrainingXMLFormatter();
    }

    @Test(expected = RuntimeException.class)
    public void testDocumentConstruction_doubleKeywords_shouldThrowException() {
        List<Span> spanListTitle = new ArrayList<>();
        Span span1 = new Span("(TMTSF)2PF6", SUPERCONDUCTORS_MATERIAL_LABEL);
        span1.setOffsetStart(19);
        span1.setOffsetEnd(30);
        spanListTitle.add(span1);

        String textTitle = "The Bechgaard salt (TMTSF)2PF6 (TMTSF = tetra- methyltetraselenafulvalene) was";
        List<LayoutToken> layoutTokensTitle = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(textTitle);

        List<DocumentBlock> blocks = new ArrayList<>();
        DocumentBlock documentBlockTitle = new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_TITLE, spanListTitle, new ArrayList<>());
        blocks.add(documentBlockTitle);

        //Abstract
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_ABSTRACT, spanListTitle, new ArrayList<>()));
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_ABSTRACT, spanListTitle, new ArrayList<>()));

        //keywords
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_KEYWORDS, spanListTitle, new ArrayList<>()));
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_KEYWORDS, spanListTitle, new ArrayList<>()));

        //Paragraph
        String textParagraph = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokensParagraph = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(textParagraph);

        //Simulating a stream of token that is in the middle of the document
        layoutTokensParagraph.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Span> spanListParagraph = new ArrayList<>();
        Span Span = new Span();
        Span.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        Span.setOffsetStart(445);
        Span.setOffsetEnd(458);
        Span.setText("FeSe 1-x Te x");
        spanListParagraph.add(Span);

        Span Span2 = new Span();
        Span2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        Span2.setOffsetStart(460);
        Span2.setOffsetEnd(471);
        Span2.setText("0.6 ≤ x ≤ 1");
        spanListParagraph.add(Span2);

        Span Span3 = new Span();
        Span3.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        Span3.setOffsetStart(549);
        Span3.setOffsetEnd(561);
        Span3.setText("x = 0.6 -0.9");
        spanListParagraph.add(Span3);

        Span Span4 = new Span();
        Span4.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
        Span4.setOffsetStart(562);
        Span4.setOffsetEnd(569);
        Span4.setText("exhibit");
        spanListParagraph.add(Span4);

        Span Span5 = new Span();
        Span5.setType(SUPERCONDUCTORS_TC_LABEL);
        Span5.setOffsetStart(570);
        Span5.setOffsetEnd(592);
        Span5.setText("bulk superconductivity");
        spanListParagraph.add(Span5);

        Span Span6 = new Span();
        Span6.setType(SUPERCONDUCTORS_TC_LABEL);
        Span6.setOffsetStart(632);
        Span6.setOffsetEnd(647);
        Span6.setText("superconducting");
        spanListParagraph.add(Span6);

        Span Span7 = new Span();
        Span7.setType(SUPERCONDUCTORS_TC_LABEL);
        Span7.setOffsetStart(653);
        Span7.setOffsetEnd(675);
        Span7.setText("transition temperature");
        spanListParagraph.add(Span7);

        DocumentBlock blockParagraph = new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_PARAGRAPH, spanListParagraph, new ArrayList<>());
        blocks.add(blockParagraph);

        // figure caption
        blocks.add(new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_FIGURE, spanListParagraph, new ArrayList<>()));
        blocks.add(new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_TABLE, spanListParagraph, new ArrayList<>()));

        target.format(blocks, 1234);
    }


    @Test
    public void testDocumentConstruction() {
        List<Span> spanListTitle = new ArrayList<>();
        Span anotherSpan = new Span("(TMTSF)2PF6", SUPERCONDUCTORS_MATERIAL_LABEL);
        anotherSpan.setOffsetStart(19);
        anotherSpan.setOffsetEnd(30);
        spanListTitle.add(anotherSpan);
        
        String textTitle = "The Bechgaard salt (TMTSF)2PF6 (TMTSF = tetra- methyltetraselenafulvalene) was";
        List<LayoutToken> layoutTokensTitle = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(textTitle);

        Pair<Integer, Integer> indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensTitle, 19, 30);
        anotherSpan.setLayoutTokens(layoutTokensTitle.subList(indexes.getLeft(), indexes.getRight()));


        List<DocumentBlock> blocks = new ArrayList<>();
        DocumentBlock documentBlockTitle = new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_TITLE, spanListTitle, new ArrayList<>());
        blocks.add(documentBlockTitle);

        //Abstract
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_ABSTRACT, spanListTitle, new ArrayList<>()));
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_ABSTRACT, spanListTitle, new ArrayList<>()));

        //keywords
        blocks.add(new DocumentBlock(layoutTokensTitle, DocumentBlock.SECTION_HEADER, DocumentBlock.SUB_SECTION_KEYWORDS, spanListTitle, new ArrayList<>()));

        //Paragraph
        String textParagraph = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokensParagraph = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(textParagraph);

        //Simulating a stream of token that is in the middle of the document
        layoutTokensParagraph.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Span> spanListParagraph = new ArrayList<>();
        Span span1 = new Span();
        span1.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span1.setOffsetStart(445);
        span1.setOffsetEnd(458);
        span1.setText("FeSe 1-x Te x");
        spanListParagraph.add(span1);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 445, 458);
        span1.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));


        Span span2 = new Span();
        span2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span2.setOffsetStart(460);
        span2.setOffsetEnd(471);
        span2.setText("0.6 ≤ x ≤ 1");
        spanListParagraph.add(span2);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 460, 471);
        span2.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        Span span3 = new Span();
        span3.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span3.setOffsetStart(549);
        span3.setOffsetEnd(561);
        span3.setText("x = 0.6 -0.9");
        spanListParagraph.add(span3);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 549, 561);
        span3.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        Span span4 = new Span();
        span4.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
        span4.setOffsetStart(562);
        span4.setOffsetEnd(569);
        span4.setText("exhibit");
        spanListParagraph.add(span4);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 562, 569);
        span4.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        Span span5 = new Span();
        span5.setType(SUPERCONDUCTORS_TC_LABEL);
        span5.setOffsetStart(570);
        span5.setOffsetEnd(592);
        span5.setText("bulk superconductivity");
        spanListParagraph.add(span5);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 570, 592);
        span5.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        Span span6 = new Span();
        span6.setType(SUPERCONDUCTORS_TC_LABEL);
        span6.setOffsetStart(632);
        span6.setOffsetEnd(647);
        span6.setText("superconducting");
        spanListParagraph.add(span6);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 632, 647);
        span6.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        Span span7 = new Span();
        span7.setType(SUPERCONDUCTORS_TC_LABEL);
        span7.setOffsetStart(653);
        span7.setOffsetEnd(675);
        span7.setText("transition temperature");
        spanListParagraph.add(span7);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokensParagraph, 653, 675);
        span7.setLayoutTokens(layoutTokensParagraph.subList(indexes.getLeft(), indexes.getRight()));

        DocumentBlock blockParagraph = new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_PARAGRAPH, spanListParagraph, new ArrayList<>());
        blocks.add(blockParagraph);

        // figure caption
        blocks.add(new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_FIGURE, "1", spanListParagraph, new ArrayList<>()));

        blocks.add(new DocumentBlock(layoutTokensParagraph, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_TABLE, "2", spanListParagraph, new ArrayList<>()));

        target.format(blocks, 1234);
    }

    @Test
    public void testGetParent_previousParentNotNull_sameParagraphId_shouldreturnPreviousParent() throws Exception {
        Element body = teiElement("body");
        Element previousParent = teiElement("p");
        Element parent = target.getParentElement(body, "1234", "1234", previousParent, "ab", null);

        assertThat(parent, is(previousParent));
    }

    @Test
    public void testGetParent_previousParentNotNull_differentParagraphId_shouldreturnNewElement() throws Exception {
        Element body = teiElement("body");
        Element previousParent = teiElement("p");
        Element parent = target.getParentElement(body, "1234", "12345", previousParent, "ab", null);

        assertThat(parent, is(not(previousParent)));
    }

    @Test
    public void testGetParent_previousParentNull_differentParagraphId_shouldreturnNewElement() throws Exception {
        Element body = teiElement("body");
        Element previousParent = null;
        Element parent = target.getParentElement(body, "1234", "12345", previousParent, "ab", null);

        assertThat(parent, is(not(previousParent)));
        assertThat(parent.getLocalName(), is("ab"));
        assertThat(parent.getAttributeCount(), is(0));
    }

    @Test
    public void testGetParent_previousParentNull_sameParagraphId_shouldreturnNewElement() throws Exception {
        Element body = teiElement("body");
        Element previousParent = null;
        Element parent = target.getParentElement(body, "1234", "1234", previousParent, "ab", null);

        assertThat(parent, is(not(previousParent)));
        assertThat(parent.getLocalName(), is("ab"));
        assertThat(parent.getAttributeCount(), is(0));
    }

    @Test
    public void testTrainingData_value() throws Exception {
        List<Span> spanList = new ArrayList<>();
        Span span1 = new Span();
        span1.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span1.setOffsetStart(19);
        span1.setOffsetEnd(30);
        span1.setText("(TMTSF)2PF6");

        String text = "The Bechgaard salt (TMTSF)2PF6 (TMTSF = tetra- methyltetraselenafulvalene) was";
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        span1.setLayoutTokens(tokens.subList(6, 13));


        spanList.add(span1);

        Element out = target.trainingExtraction(spanList, tokens);
        assertThat(out.toXML(), is("<p xmlns=\"http://www.tei-c.org/ns/1.0\">The Bechgaard salt <rs type=\"material\">(TMTSF)2PF6</rs> (TMTSF = tetra- methyltetraselenafulvalene) was</p>"));
    }


    @Test
    public void testTrainingDataExtraction_withDefaultAndCustomTags() throws Exception {
        String text = "Specific-Heat Study of Superconducting and Normal States in FeSe 1-x Te x (0.6 ≤ x ≤ 1) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 4);
        });

        List<Span> superconductorList = new ArrayList<>();
        Span superconductor = new Span();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(64);
        superconductor.setOffsetEnd(77);
        superconductor.setText("FeSe 1-x Te x");
        superconductor.setLayoutTokens(layoutTokens.subList(18, 27));

        Span superconductor2 = new Span();
        superconductor2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor2.setOffsetStart(79);
        superconductor2.setOffsetEnd(90);
        superconductor2.setText("0.6 ≤ x ≤ 1");
        superconductor2.setLayoutTokens(layoutTokens.subList(29, 40));

        superconductorList.add(superconductor);
        superconductorList.add(superconductor2);

        //This will ensure that next time I modify the principle on which the offsets are calculated, will fail
        int startingOffset = layoutTokens.get(0).getOffset();
        assertThat(text.substring(superconductor.getOffsetStart() - startingOffset, superconductor.getOffsetEnd() - startingOffset), is(superconductor.getText()));
        assertThat(text.substring(superconductor2.getOffsetStart() - startingOffset, superconductor2.getOffsetEnd() - startingOffset), is(superconductor2.getText()));

        Element outputWithinDefaultTags = target.trainingExtraction(superconductorList, layoutTokens);
        assertThat(outputWithinDefaultTags.toXML(), is("<p xmlns=\"http://www.tei-c.org/ns/1.0\">Specific-Heat Study of Superconducting and Normal States in <rs type=\"material\">FeSe 1-x Te x</rs> (<rs type=\"material\">0.6 ≤ x ≤ 1</rs>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</p>"));

        Element outputWithinCustomTags = target.trainingExtraction(superconductorList, layoutTokens, "custom");
        assertThat(outputWithinCustomTags.toXML(), is("<custom xmlns=\"http://www.tei-c.org/ns/1.0\">Specific-Heat Study of Superconducting and Normal States in <rs type=\"material\">FeSe 1-x Te x</rs> (<rs type=\"material\">0.6 ≤ x ≤ 1</rs>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</custom>"));

        Element outputWithinCustomTagsAndAttributes = target.trainingExtraction(superconductorList, layoutTokens, "custom", Pair.of("key", "value"));
        assertThat(outputWithinCustomTagsAndAttributes.toXML(), is("<custom xmlns=\"http://www.tei-c.org/ns/1.0\" key=\"value\">Specific-Heat Study of Superconducting and Normal States in <rs type=\"material\">FeSe 1-x Te x</rs> (<rs type=\"material\">0.6 ≤ x ≤ 1</rs>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</custom>"));
    }


    @Test
    public void testTrainingDataExtraction_textBody() throws Exception {
        String text = "Specific-Heat Study of Superconducting and Normal States in FeSe 1-x Te x (0.6 ≤ x ≤ 1) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream()
            .forEach(l -> {
                l.setOffset(l.getOffset() + 4);
            });

        List<Span> spanList = new ArrayList<>();
        Span span1 = new Span();
        span1.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span1.setOffsetStart(64);
        span1.setOffsetEnd(77);
        span1.setText("FeSe 1-x Te x");
        span1.setLayoutTokens(layoutTokens.subList(18, 27));

        Span span2 = new Span();
        span2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span2.setOffsetStart(79);
        span2.setOffsetEnd(90);
        span2.setText("0.6 ≤ x ≤ 1");
        span2.setLayoutTokens(layoutTokens.subList(29, 40));

        spanList.add(span1);
        spanList.add(span2);

        List<DocumentBlock> documentBlocks = new ArrayList<>();
        documentBlocks.add(new DocumentBlock(layoutTokens, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_PARAGRAPH, "1", spanList, new ArrayList<>()));

        //This will ensure that next time I modify the principle on which the offsets are calculated, will fail
        int startingOffset = layoutTokens.get(0).getOffset();
        for (Span span : spanList) {
            assertThat(text.substring(span.getOffsetStart() - startingOffset, span.getOffsetEnd() - startingOffset), is(span.getText()));
        }

        String output = target.format(documentBlocks, 1);
        assertThat(output,
            endsWith("<text xml:lang=\"en\"><body><p><s>Specific-Heat Study of Superconducting and Normal States in <rs type=\"material\">FeSe 1-x Te x</rs> (<rs type=\"material\">0.6 ≤ x ≤ 1</rs>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</s></p></body></text></tei>"));
    }

    @Test
    public void textTrainingDataExtraction_textBody_withOffsets() throws Exception {
        String text = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        //Simulating a stream of token that is in the middle of the document
        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Span> spanList = new ArrayList<>();
        Span span1 = new Span();
        span1.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span1.setOffsetStart(445);
        span1.setOffsetEnd(458);
        span1.setText("FeSe 1-x Te x");
        spanList.add(span1);
        Pair<Integer, Integer> indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 445, 458);
        span1.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));
        

        Span span2 = new Span();
        span2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span2.setOffsetStart(460);
        span2.setOffsetEnd(471);
        span2.setText("0.6 ≤ x ≤ 1");
        spanList.add(span2);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 460, 471);
        span2.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));

        Span span3 = new Span();
        span3.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        span3.setOffsetStart(549);
        span3.setOffsetEnd(561);
        span3.setText("x = 0.6 -0.9");
        spanList.add(span3);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 549, 561);
        span3.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));

        Span span4 = new Span();
        span4.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
        span4.setOffsetStart(562);
        span4.setOffsetEnd(569);
        span4.setText("exhibit");
        spanList.add(span4);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 562, 569);
        span4.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));

        Span span5 = new Span();
        span5.setType(SUPERCONDUCTORS_TC_LABEL);
        span5.setOffsetStart(570);
        span5.setOffsetEnd(592);
        span5.setText("bulk superconductivity");
        spanList.add(span5);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 570, 592);
        span5.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));

        Span span6 = new Span();
        span6.setType(SUPERCONDUCTORS_TC_LABEL);
        span6.setOffsetStart(632);
        span6.setOffsetEnd(647);
        span6.setText("superconducting");
        spanList.add(span6);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 632, 647);
        span6.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));

        Span span7 = new Span();
        span7.setType(SUPERCONDUCTORS_TC_LABEL);
        span7.setOffsetStart(653);
        span7.setOffsetEnd(675);
        span7.setText("transition temperature");
        spanList.add(span7);
        indexes = AdditionalLayoutTokensUtil.getExtremitiesAsIndex(layoutTokens, 653, 675);
        span7.setLayoutTokens(layoutTokens.subList(indexes.getLeft(), indexes.getRight()));
        

        List<DocumentBlock> documentBlocks = new ArrayList<>();
        documentBlocks.add(new DocumentBlock(layoutTokens, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_PARAGRAPH, "1", spanList, new ArrayList<>()));

        //This will ensure that next time I modify the principle on which the offsets are calculated, will fail
        int startingOffset = layoutTokens.get(0).getOffset();
        for (Span span : spanList) {
            assertThat(text.substring(span.getOffsetStart() - startingOffset, span.getOffsetEnd() - startingOffset), is(span.getText()));
        }

        String output = target.format(documentBlocks, 1);

        assertThat(output.substring(output.indexOf("<text xml:lang=\"en\">")),
            is("<text xml:lang=\"en\"><body><p><s>The electronic specific heat of as-grown and annealed single-crystals of <rs type=\"material\">FeSe 1-x Te x</rs> (<rs type=\"material\">0.6 ≤ x ≤ 1</rs>) has been investigated. " +
                "It has been found that annealed single-crystals with <rs type=\"material\">x = 0.6 -0.9</rs> <rs type=\"tcValue\">exhibit</rs> <rs type=\"tc\">bulk superconductivity</rs> with a clear specific-heat jump at the <rs type=\"tc\">superconducting</rs> (SC) <rs type=\"tc\">transition temperature</rs>, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. " +
                "The thermodynamic critical field has also been estimated. " +
                "γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. " +
                "It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.</s></p></body></text></tei>"));
    }

    @Test(expected = RuntimeException.class)
    public void testTrainingData5_wrongOffsets_shoudlThrowException() throws Exception {
        String text = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Span> spanList = new ArrayList<>();
        Span Span = new Span();
        Span.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        Span.setOffsetStart(445);
        Span.setOffsetEnd(458);
        Span.setText("FeSe 1-x Te x");
        spanList.add(Span);

        Span Span2 = new Span();
        Span2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        Span2.setOffsetStart(460);
        Span2.setOffsetEnd(472);
        Span2.setText("0.6 ≤ x ≤ 1");
        spanList.add(Span2);

        List<DocumentBlock> documentBlocks = new ArrayList<>();
        documentBlocks.add(new DocumentBlock(layoutTokens, DocumentBlock.SECTION_BODY, DocumentBlock.SUB_SECTION_PARAGRAPH, spanList, new ArrayList<>()));

        target.format(documentBlocks, 1);
    }
}