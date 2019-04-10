package org.grobid.trainer.stax;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AnnotationExtractionStaxHandlerTest {

    private AnnotationExtractionStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new AnnotationExtractionStaxHandler(
                Arrays.asList("p"),
                Arrays.asList("supercon", "tc", "substitution", "propertyValue")
        );
    }

    @Test
    public void testHandler_simpleCase() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        String continuum = target.getContinuum();
        assertThat(continuum, is("This is a sentence with one annotation.This is another sentence with another annotation."));

        List<Triple<String, Integer, Integer>> data = target.getData();
        assertThat(data, hasSize(2));
        String annotation1 = target.getData().get(0).getLeft();
        Integer offset1 = target.getData().get(0).getMiddle();
        Integer length1 = target.getData().get(0).getRight();

        assertThat(continuum.substring(offset1, offset1 + length1), is("annotation"));
        assertThat(annotation1, is("supercon"));

        String annotation2 = target.getData().get(1).getLeft();
        Integer offset2 = target.getData().get(1).getMiddle();
        Integer length2 = target.getData().get(1).getRight();

        assertThat(continuum.substring(offset2, offset2 + length2), is("annotation"));
        assertThat(annotation2, is("propertyValue"));
    }
}