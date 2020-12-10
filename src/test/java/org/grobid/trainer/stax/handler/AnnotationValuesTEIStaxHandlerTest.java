package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAG_TYPES;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AnnotationValuesTEIStaxHandlerTest {
    private AnnotationValuesTEIStaxHandler target;


    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHandler_defaultTags_extractStream() throws Exception {
        target = new AnnotationValuesTEIStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS, ANNOTATION_DEFAULT_TAG_TYPES);

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeled = target.getLabeledStream();
        assertThat(labeled, hasSize(151));

        assertThat(labeled.get(0).getLeft(), is("Review"));
        assertThat(labeled.get(0).getRight(), is("<other>"));
        assertThat(labeled.get(10).getLeft(), is("compounds"));
        assertThat(labeled.get(10).getRight(), is("I-<class>"));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_withoutTopLevelTags() throws Exception {
        target = new AnnotationValuesTEIStaxHandler(Arrays.asList("class", "tcValue"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeled = target.getLabeledStream();
        assertThat(labeled, hasSize(45));

        assertThat(labeled.get(0).getLeft(), is("The"));
        assertThat(labeled.get(0).getRight(), is("<other>"));

        List<Pair<String, String>> labeledEntities = target.getLabeledEntities();
        assertThat(labeledEntities, hasSize(1));

        assertThat(labeledEntities.get(0).getLeft(), is("2 K"));
        assertThat(labeledEntities.get(0).getRight(), is("<tcValue>"));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_onlyFromTitle() throws Exception {
        target = new AnnotationValuesTEIStaxHandler(
            Arrays.asList(SuperconductorsStackTags.from("/tei/teiHeader/fileDesc/titleStmt/title")),
            Arrays.asList("class", "tcValue"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations.tei2.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeledStream = target.getLabeledStream();
        assertThat(labeledStream, hasSize(24));

        assertThat(labeledStream.get(0).getLeft(), is("Review"));
        assertThat(labeledStream.get(0).getRight(), is("<other>"));
        assertThat(labeledStream.get(10).getLeft(), is("compounds"));
        assertThat(labeledStream.get(10).getRight(), is("I-<class>"));

        List<Pair<String, String>> labeledEntities = target.getLabeledEntities();
        assertThat(labeledEntities, hasSize(1));

        assertThat(labeledEntities.get(0).getLeft(), is("compounds of rare earth"));
        assertThat(labeledEntities.get(0).getRight(), is("<class>"));

        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(offsetsAnnotationsTags, hasSize(1));
        assertThat(labeledEntities.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + labeledEntities.get(0).getLeft().length())));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_withoutTopTags() throws Exception {
        target = new AnnotationValuesTEIStaxHandler(Arrays.asList("supercon", "propertyValue"));

        InputStream inputStream = this.getClass().getResourceAsStream("annotations2.tei2.test.xml");

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
        assertThat(labeledEntities.get(1).getLeft(), is("annotation"));
        assertThat(labeledEntities.get(1).getRight(), is("<propertyValue>"));

        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(offsetsAnnotationsTags, hasSize(2));
        assertThat(labeledEntities.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + labeledEntities.get(0).getLeft().length())));
        assertThat(labeledEntities.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + labeledEntities.get(1).getLeft().length())));
    }

    @Test
    public void testHandler_simple_extractStreamAndEntities_withoutTopTags_compatibilityOldFormat() throws Exception {
        target = new AnnotationValuesTEIStaxHandler(Arrays.asList("supercon", "propertyValue"));

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
        assertThat(labeledEntities.get(1).getLeft(), is("annotation"));
        assertThat(labeledEntities.get(1).getRight(), is("<propertyValue>"));

        List<Integer> offsetsAnnotationsTags = target.getOffsetsAnnotationsTags();
        String accumulatedText = target.getGlobalAccumulatedText();
        assertThat(offsetsAnnotationsTags, hasSize(2));
        assertThat(labeledEntities.get(0).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(0), offsetsAnnotationsTags.get(0) + labeledEntities.get(0).getLeft().length())));
        assertThat(labeledEntities.get(1).getLeft(), is(accumulatedText.substring(offsetsAnnotationsTags.get(1), offsetsAnnotationsTags.get(1) + labeledEntities.get(1).getLeft().length())));
    }
}