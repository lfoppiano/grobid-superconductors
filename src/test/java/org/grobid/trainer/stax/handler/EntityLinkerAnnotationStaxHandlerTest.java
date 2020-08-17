package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.getLast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class EntityLinkerAnnotationStaxHandlerTest {
    private EntityLinkerAnnotationStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new EntityLinkerAnnotationStaxHandler("p", "tcValue", "material");
    }

    @Test
    public void testHandler_simpleCase_shouldCreateOneLink() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("linked.annotations.test.simple.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Triple<String, String, String>> labeled = target.getLabeled();

//        labeled.stream().map(Triple::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(62));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        Stream<String> linkRight = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.endsWith("<link_right>"));

        Stream<String> linkLeft = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.endsWith("<link_left>"));

        //number of tokens
        assertThat(linkRight.count(), is(5L));
        assertThat(linkLeft.count(), is(4L));

        Stream<String> entitiesLinkRight = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.startsWith("I-<link_right>"));

        Stream<String> entitiesLinkLeft = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.startsWith("I-<link_left>"));

        assertThat(entitiesLinkRight.count(), is(1L));
        assertThat(entitiesLinkLeft.count(), is(1L));
    }

    @Test
    public void testHandler_simpleCase_NoadijacentLinking_shouldReturnNoLinks() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("linked.annotations.test.simple.noAdjacentLinks.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(is);

        StaxUtils.traverse(reader, target);

        List<Triple<String, String, String>> labeled = target.getLabeled();
        assertThat(target.getLabeled(), hasSize(62));

        Stream<String> linkRight = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.contains("link_right"));

        Stream<String> linkLeft = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.contains("link_left"));

        //number of tokens
        assertThat(linkRight.count(), is(0L));
        assertThat(linkLeft.count(), is(0L));
    }

    @Test
    public void testHandler_realCase_tcValue_material() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("linked.annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Triple<String, String, String>> labeled = target.getLabeled();

//        labeled.stream().map(Pair::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(3576));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        Stream<String> entitiesLinkRight = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.startsWith("I-<link_right>"));

        Stream<String> entitiesLinkLeft = labeled
            .stream()
            .map(Triple::getMiddle)
            .filter(v -> v.startsWith("I-<link_left>"));

        assertThat(entitiesLinkRight.count(), is(4L));
        assertThat(entitiesLinkLeft.count(), is(4L));
    }


    @Test
    public void testHandler_realCase_tcValue_material_shouldReturnEntities() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("Drozdov_etal_2015.superconductors.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Triple<String, String, String>> labeled = target.getLabeled();

        List<Triple> rightAttachments = labeled
            .stream()
            .filter(v -> v.getMiddle().startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<Triple> leftAttachments = labeled
            .stream()
            .filter(v -> v.getMiddle().startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments, hasSize(16));
        assertThat(rightAttachments, hasSize(16));

        for (int i = 0; i < leftAttachments.size(); i++) {
            assertThat(rightAttachments.get(i).getRight(), is(not(leftAttachments.get(i).getRight())));
//            System.out.println(rightAttachments.get(i) + " > "+ leftAttachments.get(i));
        }

        assertThat(rightAttachments.get(0).getLeft(), is("sulfur"));
        assertThat(leftAttachments.get(0).getLeft(), is("80"));

        assertThat(rightAttachments.get(15).getLeft(), is("sulfur"));
        assertThat(leftAttachments.get(15).getLeft(), is("<"));

    }

    @Test
    public void testHandler_realCase_pressure_tcValue() throws Exception {
        target = new EntityLinkerAnnotationStaxHandler("p", "pressure", "tcValue");

        InputStream inputStream = this.getClass().getResourceAsStream("linked.annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<Triple<String, String, String>> labeled = target.getLabeled();

//        labeled.stream().map(Pair::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(3576));

        List<Triple> rightAttachments = labeled
            .stream()
            .filter(v -> v.getMiddle().startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<Triple> leftAttachments = labeled
            .stream()
            .filter(v -> v.getMiddle().startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(rightAttachments, hasSize(4));
        assertThat(leftAttachments, hasSize(4));

        for (int i = 0; i < leftAttachments.size(); i++) {
            assertThat(rightAttachments.get(i).getRight(), is(not(leftAttachments.get(i).getRight())));
//            System.out.println(rightAttachments.get(i) + " > "+ leftAttachments.get(i));
        }

        assertThat(rightAttachments.get(0).getLeft(), is("exceeds"));
        assertThat(leftAttachments.get(0).getLeft(), is("3"));

        assertThat(getLast(rightAttachments.iterator()).getLeft(), is("48"));
        assertThat(getLast(leftAttachments.iterator()).getLeft(), is("3"));
    }



//    @Test(expected = GrobidException.class)
//    public void testHandler_incompleteLinks2() throws Exception {
//        InputStream is = this.getClass().getResourceAsStream("trainingdata.sample.quantifiedObjects.5.xml");
//
//        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(is);
//
//        StaxUtils.traverse(reader, target);
//
//        target.getLabeled();
//    }
}