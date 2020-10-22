package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StackTags;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AnnotationOffsetsTEIExtractionStaxHandlerTest {

    private AnnotationOffsetsTEIExtractionStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        List<StackTags> stackTags = Arrays.asList(StackTags.from("/tei/teiHeader/fileDesc/titleStmt/title"),
            StackTags.from("/tei/teiHeader/profileDesc/abstract/p"),
            StackTags.from("/tei/teiHeader/profileDesc/ab"),
            StackTags.from("/tei/text/body/p"),
            StackTags.from("/tei/text/body/ab"));
        target = new AnnotationOffsetsTEIExtractionStaxHandler(stackTags, Arrays.asList("material", "tc", "tcValue"));
    }

    @Test
    public void testHandler_singleSection_noAnnotations() throws Exception {
        List<StackTags> stackTags = Arrays.asList(StackTags.from("/tei/text/body/ab"));

        target = new AnnotationOffsetsTEIExtractionStaxHandler(stackTags, Arrays.asList("material", "tc", "tcValue"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        String continuum = target.getContinuum();
        assertThat(continuum, is("The equiatomic binary compounds REZn (RE is a rare " +
            "earth) with the cubic CsCl-type structure (space group Pm3m)Table"));

        List<Triple<String, Integer, Integer>> data = target.getData();
        assertThat(data, hasSize(0));
    }

    @Test
    public void testHandler_singleSection_withAnnotations() throws Exception {
        List<StackTags> stackTags = Arrays.asList(StackTags.from("/tei/text/body/p"));

        target = new AnnotationOffsetsTEIExtractionStaxHandler(stackTags, Arrays.asList("material", "tc", "tcValue"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        String continuum = target.getContinuum();
        assertThat(continuum, is("The magnetocaloric effect (MCE) is a magneto-thermodynamic phenomenon which was " +
            "first discovered in pure Fe by Emil Warburg in 1881,The column with a mark \"N\" means that " +
            "superconductivity was not observed above 2 K."));

        List<Triple<String, Integer, Integer>> data = target.getData();
        assertThat(data, hasSize(1));
        String annotation1 = target.getData().get(0).getLeft();
        Integer offset1 = target.getData().get(0).getMiddle();
        Integer length1 = target.getData().get(0).getRight();

        assertThat(continuum.substring(offset1, offset1 + length1), is("2 K"));
        assertThat(annotation1, is("tcValue"));
    }

    @Test
    public void testHandler_multipleSections_annotations() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        String continuum = target.getContinuum();
        assertThat(continuum, is("Review of magnetic properties and magnetocaloric effect in the intermetallic " +
            "compounds of rare earth with low boiling point metal(s)*The magnetocaloric effect (MCE) in many rare " +
            "earth (RE) based intermetallic compounds has been extensively investigated during last two decadesMagnetocaloric " +
            "effect; Rare earth based intermetallic compounds; Low boiling point metal(s); RENi 2 B 2 C superconductors; " +
            "Magnetic phase transitionThe magnetocaloric effect (MCE) is a magneto-thermodynamic phenomenon which was " +
            "first discovered in pure Fe by Emil Warburg in 1881,The equiatomic binary compounds REZn (RE is a rare " +
            "earth) with the cubic CsCl-type structure (space group Pm3m)TableThe column with a mark \"N\" means that " +
            "superconductivity was not observed above 2 K."));

        List<Triple<String, Integer, Integer>> data = target.getData();
        assertThat(data, hasSize(1));
        String annotation1 = target.getData().get(0).getLeft();
        Integer offset1 = target.getData().get(0).getMiddle();
        Integer length1 = target.getData().get(0).getRight();

        assertThat(continuum.substring(offset1, offset1 + length1), is("2 K"));
        assertThat(annotation1, is("tcValue"));
    }

    @Test
    public void testHandler_multipleSections_noTypeExtracted() throws Exception {
        List<StackTags> stackTags = Arrays.asList(StackTags.from("/tei/teiHeader/fileDesc/titleStmt/title"),
            StackTags.from("/tei/teiHeader/profileDesc/abstract/p"),
            StackTags.from("/tei/teiHeader/profileDesc/ab"),
            StackTags.from("/tei/text/body/p"),
            StackTags.from("/tei/text/body/ab"));
        target = new AnnotationOffsetsTEIExtractionStaxHandler(stackTags, Arrays.asList("material", "tc"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        String continuum = target.getContinuum();
        assertThat(continuum, is("Review of magnetic properties and magnetocaloric effect in the intermetallic " +
            "compounds of rare earth with low boiling point metal(s)*The magnetocaloric effect (MCE) in many rare " +
            "earth (RE) based intermetallic compounds has been extensively investigated during last two decadesMagnetocaloric " +
            "effect; Rare earth based intermetallic compounds; Low boiling point metal(s); RENi 2 B 2 C superconductors; " +
            "Magnetic phase transitionThe magnetocaloric effect (MCE) is a magneto-thermodynamic phenomenon which was " +
            "first discovered in pure Fe by Emil Warburg in 1881,The equiatomic binary compounds REZn (RE is a rare " +
            "earth) with the cubic CsCl-type structure (space group Pm3m)TableThe column with a mark \"N\" means that " +
            "superconductivity was not observed above 2 K."));

        List<Triple<String, Integer, Integer>> data = target.getData();
        assertThat(data, hasSize(0));
    }
}