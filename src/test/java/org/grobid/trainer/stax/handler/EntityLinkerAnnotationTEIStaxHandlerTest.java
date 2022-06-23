package org.grobid.trainer.stax.handler;

import com.ctc.wstx.stax.WstxInputFactory;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.data.document.LinkToken;
import org.grobid.service.command.InterAnnotationAgreementCommand;
import org.grobid.trainer.stax.StaxUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterators.getLast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class EntityLinkerAnnotationTEIStaxHandlerTest {
    private EntityLinkerAnnotationTEIStaxHandler target;

    private WstxInputFactory inputFactory = new WstxInputFactory();

    @Before
    public void setUp() {
        target = new EntityLinkerAnnotationTEIStaxHandler(InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS,
            "tcValue", "material");
    }

    @Test
    public void testHandler_2links_source_before_destination() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("link.sample.source_before_destination.1link.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(LinkToken::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(30));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        List<String> linkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> linkLeft = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        //number of tokens
        assertThat(String.join(" ", linkRight), is("the source entity"));
        assertThat(linkRight, hasSize(3));
        assertThat(String.join(" ", linkLeft), is("destination entity , whatever long it is"));
        assertThat(linkLeft, hasSize(7));

        //number of links
        List<String> entitiesLinkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> entitiesLinkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(entitiesLinkRight, hasSize(1));
        assertThat(entitiesLinkLeft, hasSize(1));
    }

    @Test
    public void testHandler_2links_source_before_destination_entity_in_middle() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("link.sample.source_before_destination.entity_in_middle.1link.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Triple::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(30));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        List<String> linkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> linkLeft = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        //number of tokens
        assertThat(String.join(" ", linkRight), is("the source entity"));
        assertThat(linkRight, hasSize(3));
        assertThat(String.join(" ", linkLeft), is("destination entity , whatever long it is"));
        assertThat(linkLeft, hasSize(7));

        //number of links
        List<String> entitiesLinkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> entitiesLinkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(entitiesLinkRight, hasSize(1));
        assertThat(entitiesLinkLeft, hasSize(1));
    }


    @Test
    public void testHandler_2links_destination_before_source() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("link.sample.destination_before_source.1link.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Triple::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(30));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        List<String> linkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> linkLeft = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        //number of tokens
        assertThat(String.join(" ", linkRight), is("source entity , whatever long it is"));
        assertThat(linkRight, hasSize(7));

        assertThat(String.join(" ", linkLeft), is("the destination entity"));
        assertThat(linkLeft, hasSize(3));

        //number of links
        List<String> entitiesLinkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> entitiesLinkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        assertThat(entitiesLinkRight, hasSize(1));
        assertThat(entitiesLinkLeft, hasSize(1));
    }

    @Test
    public void testHandler_2links_destination_before_source_entity_in_middle() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("link.sample.destination_before_source.1link.entity_in_middle.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Triple::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(30));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        List<String> linkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> linkLeft = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        //number of tokens
        assertThat(String.join(" ", linkRight), is("source entity , whatever long it is"));
        assertThat(linkRight, hasSize(7));

        assertThat(String.join(" ", linkLeft), is("the destination entity"));
        assertThat(linkLeft, hasSize(3));

        //number of links
        List<String> entitiesLinkRight = labeled.stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_left>"))
            .map(LinkToken::getText)
            .collect(Collectors.toList());

        List<String> entitiesLinkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        assertThat(entitiesLinkRight, hasSize(1));
        assertThat(entitiesLinkLeft, hasSize(1));
    }


    @Test
    public void testHandler_simpleCase_shouldCreateOneLink() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("link.sample.real_case.1.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(LinkToken::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(166));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        Map<String, List<LinkToken>> rightAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .collect(Collectors.groupingBy(LinkToken::getEntityId));

        Map<String, List<LinkToken>> leftAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .collect(Collectors.groupingBy(LinkToken::getEntityId));

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments.keySet(), hasSize(1));
        assertThat(rightAttachments.keySet(), hasSize(1));

        //number of tokens
        assertThat(rightAttachments.get(rightAttachments.keySet().toArray()[0]), hasSize(4));
        assertThat(leftAttachments.get(leftAttachments.keySet().toArray()[0]), hasSize(3));
    }

    @Test
    public void testHandler_simpleCase_NoadijacentLinking_shouldReturnNoLinks() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("linked.annotations.test.simple.noAdjacentLinks.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(is);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();
        assertThat(target.getLabeled(), hasSize(62));

        List<String> linkRight = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.contains("link_right"))
            .collect(Collectors.toList());

        List<String> linkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.contains("link_left"))
            .collect(Collectors.toList());

        //number of tokens
        assertThat(linkRight, hasSize(0));
        assertThat(linkLeft, hasSize(0));
    }

    @Test
    public void testHandler_realCase_tcValue_material() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("linked.annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Pair::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(3578));

//        assertThat(target.getLabeled().get(1).getKey(), is("car"));
//        assertThat(target.getLabeled().get(1).getValue(), is("I-<quantifiedObject_right>"));

        List<String> entitiesLinkRight = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<String> entitiesLinkLeft = labeled
            .stream()
            .map(LinkToken::getLinkLabel)
            .filter(v -> v.startsWith("I-<link_left>"))
                .collect(Collectors.toList());

        assertThat(entitiesLinkRight, hasSize(4));
        assertThat(entitiesLinkLeft, hasSize(4));
    }


    @Test
    public void testHandler_realCase_tcValue_material_shouldReturnEntities() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("Drozdov_etal_2015.superconductors.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

        List<LinkToken> rightAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<LinkToken> leftAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments, hasSize(13));
        assertThat(rightAttachments, hasSize(13));

        for (int i = 0; i < leftAttachments.size(); i++) {
            assertThat(rightAttachments.get(i).getEntityLabel(), is(not(leftAttachments.get(i).getEntityLabel())));
//            System.out.println(rightAttachments.get(i) + " > "+ leftAttachments.get(i));
        }

        assertThat(rightAttachments.get(0).getText(), is("sulfur"));
        assertThat(leftAttachments.get(0).getText(), is("80"));

        assertThat(rightAttachments.get(12).getText(), is("sulfur"));
        assertThat(leftAttachments.get(12).getText(), is("<"));

    }

    @Test
    public void testHandler_sample2_tcValue_material_shouldReturnEntities() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("Drozdov_etal_2015.superconductors.new.sample.2.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

        List<LinkToken> rightAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<LinkToken> leftAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments, hasSize(3));
        assertThat(rightAttachments, hasSize(3));

        for (int i = 0; i < leftAttachments.size(); i++) {
            assertThat(rightAttachments.get(i).getEntityLabel(), is(not(leftAttachments.get(i).getEntityLabel())));
//            System.out.println(rightAttachments.get(i) + " > "+ leftAttachments.get(i));
        }

        assertThat(rightAttachments.get(0).getText(), is("H"));
        assertThat(leftAttachments.get(0).getText(), is("<"));

        assertThat(rightAttachments.get(1).getText(), is("<"));
        assertThat(leftAttachments.get(1).getText(), is("H"));

        assertThat(rightAttachments.get(2).getText(), is("H"));
        assertThat(leftAttachments.get(2).getText(), is("200"));
    }

    @Test
    public void testHandler_sample_tcValue_material_shouldReturnEntities() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("Drozdov_etal_2015.superconductors.new.sample.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

        Map<String, List<LinkToken>> rightAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .collect(Collectors.groupingBy(LinkToken::getId));

        Map<String, List<LinkToken>> leftAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .collect(Collectors.groupingBy(LinkToken::getId));

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments.keySet(), hasSize(3));
        assertThat(rightAttachments.keySet(), hasSize(3));

        for (String key : leftAttachments.keySet()) {
            assertThat(rightAttachments.get(key).stream()
                .map(lt -> lt.getEntityLabel().replace("I-", ""))
                .distinct()
                .collect(Collectors.toList()), hasSize(1));
            assertThat(rightAttachments.get(key).get(0).getEntityLabel(), is(not(leftAttachments.get(key).get(0).getEntityLabel())));
        }

        assertThat(rightAttachments.get("x124").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("sulfur hydride"));
        assertThat(leftAttachments.get("x124").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("204 ( 1 ) K"));

        assertThat(rightAttachments.get("x133").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("sulfur hydride"));
        assertThat(leftAttachments.get("x133").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("205 K"));

        assertThat(rightAttachments.get("x139").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("sulfur hydride"));
        assertThat(leftAttachments.get("x139").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("203 . 5 K"));
    }

    @Test
    public void testHandler_realCase_pressure_tcValue() throws Exception {
//        Arrays.asList(SuperconductorsStackTags.from("/tei/text/body/p"), SuperconductorsStackTags.from("/tei/text/p"))
        target = new EntityLinkerAnnotationTEIStaxHandler(InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS, 
            "pressure", "tcValue");

        InputStream inputStream = this.getClass().getResourceAsStream("linked.annotations.test.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Pair::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(3578);

        List<LinkToken> rightAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_right>"))
            .collect(Collectors.toList());

        List<LinkToken> leftAttachments = labeled
            .stream()
            .filter(v -> v.getLinkLabel().startsWith("I-<link_left>"))
            .collect(Collectors.toList());

        assertThat(rightAttachments, hasSize(4));
        assertThat(leftAttachments, hasSize(4));

        for (int i = 0; i < leftAttachments.size(); i++) {
            assertThat(rightAttachments.get(i).getEntityLabel(), is(not(leftAttachments.get(i).getEntityLabel())));
//            System.out.println(rightAttachments.get(i) + " > "+ leftAttachments.get(i));
        }

        assertThat(rightAttachments.get(0).getText(), is("exceeds"));
        assertThat(leftAttachments.get(0).getText(), is("3"));

        assertThat(getLast(rightAttachments.iterator()).getText(), is("48"));
        assertThat(getLast(leftAttachments.iterator()).getText(), is("3"));
    }

    @Test
    public void testHandler_simpleCase_pressure_tcValue() throws Exception {
//        Arrays.asList(SuperconductorsStackTags.from("/tei/text/body/p"), SuperconductorsStackTags.from("/tei/text/p"))
        target = new EntityLinkerAnnotationTEIStaxHandler(InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS, 
            "pressure", "tcValue");

        InputStream inputStream = this.getClass().getResourceAsStream("1609.04957-CC.superconductors.simple.tei.xml");

        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);

        StaxUtils.traverse(reader, target);

        List<LinkToken> labeled = target.getLabeled();

//        labeled.stream().map(Pair::toString).forEach(System.out::println);

        assertThat(target.getLabeled(), hasSize(64));

        Map<String, List<LinkToken>> rightAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_right>"))
            .collect(Collectors.groupingBy(LinkToken::getId));

        Map<String, List<LinkToken>> leftAttachments = labeled.stream()
            .filter(v -> v.getLinkLabel().endsWith("<link_left>"))
            .collect(Collectors.groupingBy(LinkToken::getId));

        assertThat(leftAttachments.size(), is(rightAttachments.size()));

        assertThat(leftAttachments.keySet(), hasSize(1));
        assertThat(rightAttachments.keySet(), hasSize(1));

        for (String key : leftAttachments.keySet()) {
            assertThat(rightAttachments.get(key).stream()
                .map(lt -> lt.getEntityLabel().replace("I-", ""))
                .distinct()
                .collect(Collectors.toList()), hasSize(1));
            assertThat(rightAttachments.get(key).get(0).getEntityLabel(), is(not(leftAttachments.get(key).get(0).getEntityLabel())));
        }

        assertThat(rightAttachments.get("x47").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("up to 40 K"));
        assertThat(leftAttachments.get("x47").stream().map(LinkToken::getText).collect(Collectors.joining(" ")), is("3 . 0 GPa"));
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