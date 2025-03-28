package org.grobid.core.engines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.easymock.EasyMock;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.document.RawPassage;
import org.grobid.core.data.document.TextPassage;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.normalization.UnitNormalizer;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.client.ChemDataExtractorClient;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.client.StructureIdentificationModuleClient;
import org.grobid.service.configuration.GrobidQuantitiesConfiguration;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore("This has been decommissioned")
public class CRFBasedLinkerIntegrationTest {

    private CRFBasedLinker target;
    private ModuleEngine moduleEngine;
    private ChemDataExtractorClient mockChemdataExtractorClient;
    private StructureIdentificationModuleClient mockSpaceGroupsClient;

    public static void initEngineForTests() throws IOException, IllegalAccessException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // https://stackoverflow.com/questions/14853324/can-not-find-deserialize-for-non-concrete-collection-type
        mapper.registerModule(new GuavaModule());
        GrobidSuperconductorsConfiguration configuration = mapper.readValue(RuleBasedLinker.class.getResourceAsStream("config-test.yml"), GrobidSuperconductorsConfiguration.class);
        configuration.getModels().stream().forEach(GrobidProperties::addModel);
        GrobidProperties.getInstance();
        Field modelMap = Whitebox.getField(GrobidProperties.class, "modelMap");

        Map<String, GrobidConfig.ModelParameters> newModelMap = (Map<String, GrobidConfig.ModelParameters>) modelMap.get(new HashMap<>());
        newModelMap.entrySet().stream()
            .forEach(entry -> {
                entry.getValue().engine = "wapiti";
            });
        Whitebox.setInternalState(GrobidProperties.class, "modelMap", newModelMap);
//        GrobidProperties.getDistinctModels().stream().forEach(model -> model);
        LibraryLoader.load();
    }

    @Before
    public void setUp() throws Exception {
        initEngineForTests();

        target = new CRFBasedLinker();
        mockChemdataExtractorClient = EasyMock.createMock(ChemDataExtractorClient.class);
        mockSpaceGroupsClient = EasyMock.createMock(StructureIdentificationModuleClient.class);
        SuperconductorsParser superParser = new SuperconductorsParser(mockChemdataExtractorClient, new MaterialParser(null, null), mockSpaceGroupsClient);
        this.moduleEngine = new ModuleEngine(new GrobidSuperconductorsConfiguration(), superParser, QuantityParser.getInstance(true), null, null);
    }

    @Test
    public void testRealCase_shouldExtract1Link() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> passages = moduleEngine.process(Arrays.asList(new RawPassage(layoutTokens)), true);

        assertThat(passages, hasSize(1));

        TextPassage passage = passages.get(0);
        List<Span> annotations = passage.getSpans();
        assertThat(annotations, hasSize(4));

        //set the annotations linkable 
        annotations
            .stream()
            .filter(l -> l.getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL) || l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .forEach(l -> l.setLinkable(true));

        target.process(layoutTokens, annotations, CRFBasedLinker.getInstance().MATERIAL_TCVALUE_ID);

        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(2));

        assertThat(linkedEntities.get(0).getText(), is("MgB 2"));
        assertThat(linkedEntities.get(0).getLinks().get(0).getTargetText(), is("40 K"));

        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }


    @Test
    @Ignore("The model does not extract any link. This test is quite useless.")
    public void testRealCase_shouldRecogniseOneLink() throws Exception {
        String input = "The crystal structure of (Sr, Na)Fe 2 As 2 has been refined for polycrystalline samples in the range of 0 ⩽ x ⩽ 0.42 with a maximum T c of 26 K .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> paragraphs = moduleEngine.process(Arrays.asList(new RawPassage(layoutTokens)), true);
        assertThat(paragraphs, hasSize(1));

        TextPassage paragraph = paragraphs.get(0);
        List<Span> annotations = paragraph.getSpans();
        assertThat(annotations, hasSize(annotations.size()));

        //set the annotations linkable 
        annotations
            .stream()
            .filter(l -> l.getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL) || l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .forEach(l -> l.setLinkable(true));

        List<Span> process = target.process(layoutTokens, annotations, CRFBasedLinker.MATERIAL_TCVALUE_ID);
        List<Span> linkedEntities = process.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(2));

        assertThat(linkedEntities.get(0).getText(), is("x ⩽ 0.42"));
        String linkId = linkedEntities.get(0).getLinks().get(0).getTargetId();
        Optional<Span> linkedSpan = linkedEntities.stream().filter(le -> String.valueOf(le.getId()).equals(linkId)).findFirst();
        assertThat(linkedSpan.isPresent(), is(true));
        assertThat(linkedSpan.get().getText(), is("26 K"));
        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }

    @Test
    public void testRealCase_shouldNotLink() throws Exception {
        String input = "Previous studies have shown that pressure of 1 GPa can reduce T c , but only by less than 2 K in MgB 2 .";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> paragraphs = moduleEngine.process(Arrays.asList(new RawPassage(layoutTokens)), true);
        assertThat(paragraphs, hasSize(1));

        TextPassage paragraph = paragraphs.get(0);
        List<Span> annotations = paragraph.getSpans();


        target.process(layoutTokens, annotations, CRFBasedLinker.MATERIAL_TCVALUE_ID);

        List<Span> linkedEntities = annotations.stream().filter(l -> isNotEmpty(l.getLinks())).collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(0));
        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }

    @Test
    @Ignore
    public void testRealCase_shouldExtract1Links_1() throws Exception {
        String input = "Theory-oriented experiments show that the compressed hydride of Group VI (hydrogen sulfide, H 3 S) exhibits a superconducting state at 203 K. ";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> paragraphs = moduleEngine.process(Arrays.asList(new RawPassage(layoutTokens)), true);
        assertThat(paragraphs, hasSize(1));

        TextPassage paragraph = paragraphs.get(0);
        List<Span> annotations = paragraph.getSpans();

        // Set the materials to be linkable
        paragraph.getSpans().stream()
            .filter(s -> Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL).contains(s.getType()))
            .forEach(s -> s.setLinkable(true));

        List<Span> processedSpans = target.process(layoutTokens, annotations, CRFBasedLinker.getInstance().MATERIAL_TCVALUE_ID);

        List<Span> linkedEntities = processedSpans.stream()
            .filter(l -> isNotEmpty(l.getLinks()) && l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(1));

        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }

    @Test
    public void testRealCase_shouldExtract1Links_2() throws Exception {
        String input = "Moreover, a Group V hydride (phosphorus hydride, PH 3 ) has also been studied and its T c reached a maximum of 103 K.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> paragraphs = moduleEngine.process(Arrays.asList(new RawPassage(layoutTokens)), true);
        assertThat(paragraphs, hasSize(1));

        TextPassage paragraph = paragraphs.get(0);
        List<Span> annotations = paragraph.getSpans();

        // Set the materials to be linkable
        paragraph.getSpans().stream()
            .filter(s -> Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL).contains(s.getType()))
            .forEach(s -> s.setLinkable(true));

        List<Span> processedSpans = target.process(layoutTokens, annotations, CRFBasedLinker.getInstance().MATERIAL_TCVALUE_ID);

        List<Span> linkedEntities = processedSpans.stream()
            .filter(l -> isNotEmpty(l.getLinks()) && l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());

        assertThat(linkedEntities, hasSize(1));

        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }

    @Test
    public void testRealCase_shouldExtract0Links_3() throws Exception {
        String input = "The experimental realisation of the superconductivity in H 3 S and PH 3 inspired us to search for other hydride superconductors.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        EasyMock.expect(mockChemdataExtractorClient.processBulk(EasyMock.anyObject())).andReturn(Arrays.asList(new ArrayList<>()));
        EasyMock.expect(mockSpaceGroupsClient.extractStructuresMulti(EasyMock.anyObject())).andReturn(new ArrayList<>());
        EasyMock.replay(mockChemdataExtractorClient, mockSpaceGroupsClient);

        List<TextPassage> paragraphs = moduleEngine.process(Collections.singletonList(new RawPassage(layoutTokens)), true);
        assertThat(paragraphs, hasSize(1));

        TextPassage paragraph = paragraphs.get(0);
        List<Span> annotations = paragraph.getSpans();

        // Set the materials to be linkable
        paragraph.getSpans().stream()
            .filter(s -> Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL).contains(s.getType()))
            .forEach(s -> s.setLinkable(true));

        target.process(layoutTokens, annotations, CRFBasedLinker.getInstance().MATERIAL_TCVALUE_ID);

        List<Span> linkedEntities = annotations.stream()
            .filter(l -> isNotEmpty(l.getLinks()) && l.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());
        assertThat(linkedEntities, hasSize(0));

        EasyMock.verify(mockChemdataExtractorClient, mockSpaceGroupsClient);
    }

}