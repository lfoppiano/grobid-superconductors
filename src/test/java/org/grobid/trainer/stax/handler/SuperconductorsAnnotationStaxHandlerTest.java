package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationExtractionStaxHandler;
import org.grobid.trainer.stax.handler.SuperconductorAnnotationStaxHandler;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class SuperconductorsAnnotationStaxHandlerTest {

    private SuperconductorAnnotationStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new SuperconductorAnnotationStaxHandler(
                TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                ANNOTATION_DEFAULT_TAGS
        );
    }

    @Test
    public void testHandler_simpleCase() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Pair<String, String>> labeled = target.getLabeled();

        assertThat(labeled.get(0).getLeft(), is("The"));
        assertThat(labeled.get(0).getRight(), is("<other>"));

        System.out.println(Arrays.toString(labeled.toArray()));

    }

    @Test
    public void testHandler_testMissingTags() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957.superconductors.2.training.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

    }
}