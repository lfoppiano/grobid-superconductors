package org.grobid.trainer.stax;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class AnnotationExtractionStaxHandlerTest {

    private AnnotationExtractionStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new AnnotationExtractionStaxHandler();
    }

    @Test
    public void testHandler_simpleCase() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("superconductor.text.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Triple<String, Integer, Integer>> data = target.getData();

        data.stream().map(Triple::toString).forEach(System.out::println);

    }
}