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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

public class AnnotationValuesStaxHandlerTest {
    private AnnotationValuesStaxHandler target;


    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() throws Exception {
        target = new AnnotationValuesStaxHandler(Arrays.asList("material"));
    }

    @Test
    public void testHandler_material_extraction() throws Exception {

        target = new AnnotationValuesStaxHandler(
            Arrays.asList("material")
        );

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> data = target.getLabeled();
        assertThat(data, hasSize(12));
        assertThat(data.get(0).getLeft(), is("LaFeAsO1−xHx"));
        assertThat(data.get(0).getRight(), is("<material>"));
    }

    @Test
    public void testHandler_material_extraction_2() throws Exception {

        target = new AnnotationValuesStaxHandler(
            Arrays.asList("material", "tc")
        );

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> data = target.getLabeled();
        assertThat(data, hasSize(62));
        assertThat(data.get(0).getLeft(), is("high-transition-temperature"));
        assertThat(data.get(0).getRight(), is("<tc>"));
    }
}