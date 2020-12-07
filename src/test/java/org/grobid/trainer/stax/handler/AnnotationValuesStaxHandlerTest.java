package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.length;
import static org.grobid.service.command.InterAnnotationAgreementCommand.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

public class AnnotationValuesStaxHandlerTest {
    private AnnotationValuesStaxHandler target;


    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHandler_material_labeledEntities_extraction() throws Exception {

        target = new AnnotationValuesStaxHandler(Arrays.asList("material"));

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> data = target.getLabeledEntities();
        assertThat(data, hasSize(12));
        assertThat(data.get(0).getLeft(), is("LaFeAsO1−xHx"));
        assertThat(data.get(0).getRight(), is("<material>"));

        String accumulatedText = target.getGlobalAccumulatedText();
        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        assertThat(offsetsAnnotationsTags, hasSize(12));
        assertThat(data.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + data.get(0).getLeft().length())));
        assertThat(data.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + data.get(1).getLeft().length())));
        assertThat(data.get(2).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(2), offsetsAnnotationsTags.get(2) + data.get(2).getLeft().length())));
    }

    @Test
    public void testHandler_material_and_tc_labeledEntities_extraction() throws Exception {

        target = new AnnotationValuesStaxHandler(Arrays.asList("material", "tc"));

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> data = target.getLabeledEntities();
        assertThat(data, hasSize(62));
        assertThat(data.get(0).getLeft(), is("high-transition-temperature"));
        assertThat(data.get(0).getRight(), is("<tc>"));

        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(length(accumulatedText), is(12990));
        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        assertThat(offsetsAnnotationsTags, hasSize(62));
        assertThat(data.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + data.get(0).getLeft().length())));
        assertThat(data.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + data.get(1).getLeft().length())));
        assertThat(data.get(2).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(2), offsetsAnnotationsTags.get(2) + data.get(2).getLeft().length())));
    }

    @Test
    public void testHandler_material_and_tc_labeledEntities_withinParagraphs_extraction() throws Exception {

        target = new AnnotationValuesStaxHandler(Arrays.asList("p"), Arrays.asList("material", "tc"));

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> data = target.getLabeledEntities();
        assertThat(data, hasSize(62));
        assertThat(data.get(0).getLeft(), is("high-transition-temperature"));
        assertThat(data.get(0).getRight(), is("<tc>"));

        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(length(accumulatedText), is(12859));
        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        assertThat(offsetsAnnotationsTags, hasSize(62));
        assertThat(data.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + data.get(0).getLeft().length())));
        assertThat(data.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + data.get(1).getLeft().length())));
        assertThat(data.get(2).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(2), offsetsAnnotationsTags.get(2) + data.get(2).getLeft().length())));
    }


    @Test
    public void testHandler_defaultTags_extractStream() throws Exception {
        target = new AnnotationValuesStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS, ANNOTATION_DEFAULT_TAG_TYPES);

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeled = target.getLabeledStream();
        assertThat(labeled, hasSize(2918));

        assertThat(labeled.get(0).getLeft(), is("The"));
        assertThat(labeled.get(0).getRight(), is("<other>"));
        assertThat(labeled.get(4).getLeft(), is("high"));
        assertThat(labeled.get(4).getRight(), is("I-<tc>"));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_withoutTopLevelTags() throws Exception {
        target = new AnnotationValuesStaxHandler(Arrays.asList("supercon", "propertyOther"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeled = target.getLabeledStream();
        assertThat(labeled, hasSize(18));

        assertThat(labeled.get(0).getLeft(), is("Some"));
        assertThat(labeled.get(0).getRight(), is("<other>"));
        assertThat(labeled.get(8).getLeft(), is("annotation"));
        assertThat(labeled.get(8).getRight(), is("I-<supercon>"));

        List<Pair<String, String>> labeledEntities = target.getLabeledEntities();
        assertThat(labeledEntities, hasSize(2));

        assertThat(labeledEntities.get(0).getLeft(), is("annotation"));
        assertThat(labeledEntities.get(0).getRight(), is("<supercon>"));
        assertThat(labeledEntities.get(1).getLeft(), is("another sentence"));
        assertThat(labeledEntities.get(1).getRight(), is("<propertyOther>"));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_withTopLevelTags() throws Exception {
        target = new AnnotationValuesStaxHandler(Arrays.asList("p"), Arrays.asList("supercon", "propertyOther"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeledStream = target.getLabeledStream();
        assertThat(labeledStream, hasSize(18));

        assertThat(labeledStream.get(0).getLeft(), is("This"));
        assertThat(labeledStream.get(0).getRight(), is("<other>"));
        assertThat(labeledStream.get(6).getLeft(), is("annotation"));
        assertThat(labeledStream.get(6).getRight(), is("I-<supercon>"));

        List<Pair<String, String>> labeledEntities = target.getLabeledEntities();
        assertThat(labeledEntities, hasSize(2));

        assertThat(labeledEntities.get(0).getLeft(), is("annotation"));
        assertThat(labeledEntities.get(0).getRight(), is("<supercon>"));
        assertThat(labeledEntities.get(1).getLeft(), is("another sentence"));
        assertThat(labeledEntities.get(1).getRight(), is("<propertyOther>"));

        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(offsetsAnnotationsTags, hasSize(2));
        assertThat(labeledEntities.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + labeledEntities.get(0).getLeft().length())));
        assertThat(labeledEntities.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + labeledEntities.get(1).getLeft().length())));
    }
}