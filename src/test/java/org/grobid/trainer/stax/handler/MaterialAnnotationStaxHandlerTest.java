package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.grobid.trainer.MaterialTrainer.ANNOTATION_DEFAULT_TAGS;
import static org.grobid.trainer.MaterialTrainer.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

public class MaterialAnnotationStaxHandlerTest {

    private MaterialAnnotationStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new MaterialAnnotationStaxHandler(
            TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
            ANNOTATION_DEFAULT_TAGS
        );
    }

    @Test
    public void testHandler_simpleCase() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("materials.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<List<Pair<String, String>>> labeled = target.getLabeled();

        assertThat(labeled, hasSize(3));
        assertThat(labeled.get(0).get(0).getLeft(), is("Under"));
        assertThat(labeled.get(0).get(1).getLeft(), is("-"));
        assertThat(labeled.get(0).get(2).getLeft(), is("doped"));
//        assertThat(labeled.get(0).getRight(), is("<other>"));

//        System.out.println(Arrays.toString(labeled.toArray()));

    }
}