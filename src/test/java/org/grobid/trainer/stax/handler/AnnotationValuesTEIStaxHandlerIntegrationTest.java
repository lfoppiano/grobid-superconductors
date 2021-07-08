package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.main.LibraryLoader;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAG_TYPES;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class AnnotationValuesTEIStaxHandlerIntegrationTest {
    private AnnotationValuesTEIStaxHandler target;


    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
    }

   
    @Test
    public void testHandler_defaultTags_extractStreamAsSentences() throws Exception {
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
}