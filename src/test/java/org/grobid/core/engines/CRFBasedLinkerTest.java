package org.grobid.core.engines;

import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.document.Span;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.engines.linking.EntityLinker;
import org.grobid.core.engines.linking.EntityLinker_MaterialTcValue;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore("Decommissioned")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Lexicon.class)
public class CRFBasedLinkerTest {
    CRFBasedLinker target;

    @BeforeClass
    public static void before() throws Exception {
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);
    }

    @Before
    public void setUp() throws Exception {
        HashMap<String, EntityLinker> linkerImplementations = new HashMap<>();
        linkerImplementations.put(CRFBasedLinker.MATERIAL_TCVALUE_ID, 
            new EntityLinker_MaterialTcValue(GrobidModels.DUMMY, Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL)));
        target = new CRFBasedLinker(linkerImplementations);
    }

    @Test
    @Ignore("Still work in progress")
    public void testExtractEntities_missingEntities_shouldReconstructWhatPossible() throws Exception {
        String input = "In just a few months, the superconducting transition temperature (Tc) was increased to 55 K " +
            "in the electron-doped system, as well as 25 K in hole-doped La1−x SrxOFeAs compound. " +
            "Soon after, single crystals of LnFeAs(O1−x Fx) (Ln = Pr, Nd, Sm) were grown successfully by the NaCl/KCl " +
            "flux method, though the sub-millimeter sizes limit the experimental studies on them. " +
            "Therefore, FeAs-based single crystals with high crystalline quality, homogeneity and large sizes are " +
            "highly desired for precise measurements of the properties. " +
            "Very recently, the BaFe2As2 compound in a tetragonal ThCr2Si2-type structure with infinite Fe–As layers was reported. " +
            "By replacing the alkaline earth elements (Ba and Sr) with alkali elements (Na, K, and Cs), " +
            "superconductivity up to 38 K was discovered both in hole-doped and electron-doped samples. " +
            "Tc varies from 2.7 K in CsFe2As2 to 38 K in A1−xKxFe2As2 (A = Ba, Sr). ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

//        String result = "In\tin\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "just\tjust\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "a\ta\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
//            "few\tfew\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "months\tmonths\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "superconducting\tsuperconducting\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "transition\ttransition\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "temperature\ttemperature\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
//            "Tc\ttc\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
//            "was\twas\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "increased\tincreased\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "55\t55\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "electron\telectron\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "doped\tdoped\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "system\tsystem\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "well\twell\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "25\t25\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<tcValue>\tI-<link_right>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<tcValue>\t<link_right>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "hole\thole\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<material>\tI-<link_left>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<material>\t<link_left>\n" +
//            "doped\tdoped\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<material>\t<link_left>\n" +
//            "La\tla\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_left>\n" +
//            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<material>\t<link_left>\n" +
//            "x\tx\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<material>\t<link_left>\n" +
//            "SrxOFeAs\tsrxofeas\tINITCAP\tNODIGIT\tNOPUNCT\tXxXxXx\t<material>\t<link_left>\n" +
//            "compound\tcompound\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
//            "Soon\tsoon\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
//            "after\tafter\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "single\tsingle\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "crystals\tcrystals\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "LnFeAs\tlnfeas\tINITCAP\tNODIGIT\tNOPUNCT\tXxXxXx\t<material>\tI-<link_left>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<material>\t<link_left>\n" +
//            "O\to\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<material>\t<link_left>\n" +
//            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_left>\n" +
//            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<material>\t<link_left>\n" +
//            "x\tx\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<material>\t<link_left>\n" +
//            "Fx\tfx\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<material>\t<link_left>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<material>\t<link_left>\n" +
//            "Ln\tln\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<material>\t<link_left>\n" +
//            "Pr\tpr\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<material>\t<link_left>\n" +
//            "Nd\tnd\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<material>\t<link_left>\n" +
//            "Sm\tsm\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<material>\t<link_left>\n" +
//            "were\twere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "grown\tgrown\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "successfully\tsuccessfully\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "by\tby\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "NaCl\tnacl\tINITCAP\tNODIGIT\tNOPUNCT\tXxXXx\t<other>\t<other>\n" +
//            "/\t/\tALLCAPS\tNODIGIT\tNOPUNCT\t/\t<other>\t<other>\n" +
//            "KCl\tkcl\tINITCAP\tNODIGIT\tNOPUNCT\tXXx\t<other>\t<other>\n" +
//            "flux\tflux\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "method\tmethod\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "though\tthough\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "sub\tsub\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "millimeter\tmillimeter\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "sizes\tsizes\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "limit\tlimit\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "experimental\texperimental\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "studies\tstudies\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "on\ton\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "them\tthem\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
//            "Therefore\ttherefore\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "FeAs\tfeas\tINITCAP\tNODIGIT\tNOPUNCT\tXxXXx\t<material>\tI-<link_right>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<material>\t<link_right>\n" +
//            "based\tbased\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<material>\t<link_right>\n" +
//            "single\tsingle\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<material>\t<link_right>\n" +
//            "crystals\tcrystals\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<material>\t<link_right>\n" +
//            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "high\thigh\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "crystalline\tcrystalline\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "quality\tquality\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "homogeneity\thomogeneity\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "large\tlarge\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "sizes\tsizes\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "are\tare\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "highly\thighly\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "desired\tdesired\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "precise\tprecise\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "measurements\tmeasurements\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "properties\tproperties\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
//            "Very\tvery\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
//            "recently\trecently\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "BaFe\tbafe\tINITCAP\tNODIGIT\tNOPUNCT\tXxXXx\t<material>\tI-<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "As\tas\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "compound\tcompound\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "a\ta\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
//            "tetragonal\ttetragonal\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "ThCr\tthcr\tINITCAP\tNODIGIT\tNOPUNCT\tXxXXx\t<material>\tI-<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "Si\tsi\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "type\ttype\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "structure\tstructure\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "infinite\tinfinite\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "Fe\tfe\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "As\tas\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "layers\tlayers\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "was\twas\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "reported\treported\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
//            "By\tby\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "replacing\treplacing\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "alkaline\talkaline\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "earth\tearth\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "elements\telements\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
//            "Ba\tba\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "Sr\tsr\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
//            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "alkali\talkali\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "elements\telements\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
//            "Na\tna\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "Cs\tcs\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
//            "superconductivity\tsuperconductivity\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "up\tup\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "38\t38\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
//            "was\twas\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "discovered\tdiscovered\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "both\tboth\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "hole\thole\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "doped\tdoped\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
//            "electron\telectron\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
//            "doped\tdoped\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "samples\tsamples\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
//            "Tc\ttc\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
//            "varies\tvaries\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
//            "from\tfrom\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<tcValue>\tI-<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<tcValue>\t<link_right>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<tcValue>\t<link_right>\n" +
//            "7\t7\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<tcValue>\t<link_right>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<tcValue>\t<link_right>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "CsFe\tcsfe\tINITCAP\tNODIGIT\tNOPUNCT\tXxXXx\t<material>\tI-<link_left>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_left>\n" +
//            "As\tas\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_left>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_left>\n" +
//            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "38\t38\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
//            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
//            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
//            "A\ta\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<material>\tI-<link_right>\n" +
//            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<material>\t<link_right>\n" +
//            "xKxFe\txkxfe\tNOCAPS\tNODIGIT\tNOPUNCT\txXxXx\t<material>\t<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "As\tas\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_right>\n" +
//            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
//            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<material>\t<link_right>\n" +
//            "A\ta\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<material>\t<link_right>\n" +
//            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<material>\t<link_right>\n" +
//            "Ba\tba\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_right>\n" +
//            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<material>\t<link_right>\n" +
//            "Sr\tsr\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<material>\t<link_right>\n" +
//            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<material>\t<link_right>\n" +
//            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n";


//        List<Superconductor> links = target.extractResults(layoutTokens, result, Arrays.asList(sup1, sup2));

    }

    @Test
    @Ignore
    public void testErrorCase() throws Exception {
        String result = "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "W\tw\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "position\tposition\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "occupancy\toccupancy\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "freely\tfreely\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "refined\trefined\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "but\tbut\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "fractional\tfractional\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "occupancy\toccupancy\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "B\tb\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "site\tsite\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "constrained\tconstrained\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "such\tsuch\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "that\tthat\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "occupancy\toccupancy\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "B\tb\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<other>\t<other>\n" +
            "occupancy\toccupancy\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "W\tw\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "described\tdescribed\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "previous\tprevious\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "studies\tstudies\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "positional\tpositional\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "parameters\tparameters\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "B\tb\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "are\tare\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "freely\tfreely\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "refined\trefined\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "temperature\ttemperature\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "range\trange\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "from\tfrom\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "5\t5\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "under\tunder\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "applied\tapplied\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "magnetic\tmagnetic\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "fields\tfields\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "ranging\tranging\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "from\tfrom\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "600\t600\tNOCAPS\tALLDIGIT\tNOPUNCT\tddd\t<other>\t<other>\n" +
            "mT\tmt\tNOCAPS\tNODIGIT\tNOPUNCT\txX\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "steadily\tsteadily\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "decreases\tdecreases\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "increasing\tincreasing\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "applied\tapplied\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "field\tfield\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "expected\texpected\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "last\tlast\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "instance\tinstance\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "where\twhere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "resistivity\tresistivity\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "drops\tdrops\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "below\tbelow\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "50\t50\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "%\t%\tALLCAPS\tNODIGIT\tNOPUNCT\t%\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A00\t\uF0A00\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t\uF0A0d\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<tcValue>\tI-<link_right>\n" +
            "62\t62\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<tcValue>\t<link_right>\n" +
            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<tcValue>\t<link_right>\n" +
            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\uF0A0\th\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\tX\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A0500\t\uF0A0500\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t\uF0A0ddd\t<other>\t<other>\n" +
            "mT\tmt\tNOCAPS\tNODIGIT\tNOPUNCT\txX\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "thus\tthus\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
            "determined\tdetermined\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "upper\tupper\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "critical\tcritical\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "fields\tfields\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "plotted\tplotted\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "a\ta\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "function\tfunction\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "estimated\testimated\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "values\tvalues\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "were\twere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "plotted\tplotted\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "main\tmain\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "panel\tpanel\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "fitted\tfitted\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "a\ta\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "line\tline\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "slope\tslope\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "dμ\tdμ\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "/\t/\tALLCAPS\tNODIGIT\tNOPUNCT\t/\t<other>\t<other>\n" +
            "dT\uF0A0\tdt\uF0A0\tNOCAPS\tNODIGIT\tNOPUNCT\txX\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "33\t33\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<other>\t<other>\n" +
            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "For\tfor\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "many\tmany\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "superconductors\tsuperconductors\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "zero\tzero\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "temperature\ttemperature\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "upper\tupper\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "critical\tcritical\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "field\tfield\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "can\tcan\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "be\tbe\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "estimated\testimated\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "with\twith\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "Werthamer\twerthamer\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
            "Helfand\thelfand\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
            "Hohenberg\thohenberg\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "WHH\twhh\tALLCAPS\tNODIGIT\tNOPUNCT\tXXX\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "equation\tequation\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "given\tgiven\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "by\tby\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            ":\t:\tALLCAPS\tNODIGIT\tPUNCT\t:\t<other>\t<other>\n" +
            "where\twhere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "A\ta\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "−\t−\tALLCAPS\tNODIGIT\tHYPHEN\t−\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "693\t693\tNOCAPS\tALLDIGIT\tNOPUNCT\tddd\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "dirty\tdirty\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "limit\tlimit\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "taking\ttaking\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "∼2\t∼2\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t∼d\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<tcValue>\tI-<link_right>\n" +
            "05\t05\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<tcValue>\t<link_right>\n" +
            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<tcValue>\t<link_right>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "WB\twb\tALLCAPS\tNODIGIT\tNOPUNCT\tXX\t<material>\tI-<link_left>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "Based\tbased\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "on\ton\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "this\tthis\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "model\tmodel\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A00\t\uF0A00\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t\uF0A0d\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "47\t47\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "indicated\tindicated\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "by\tby\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "blue\tblue\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "closed\tclosed\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "circle\tcircle\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "however\thowever\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "last\tlast\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "measured\tmeasured\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "value\tvalue\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "50\t50\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "which\twhich\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "already\talready\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "above\tabove\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "WHH\twhh\tALLCAPS\tNODIGIT\tNOPUNCT\tXXX\t<other>\t<other>\n" +
            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
            "predicted\tpredicted\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "upper\tupper\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "critical\tcritical\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "field\tfield\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "of\tof\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "47\t47\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "nearly\tnearly\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "linear\tlinear\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "over\tover\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "a\ta\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "broad\tbroad\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "temperature\ttemperature\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "range\trange\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "that\tthat\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "we\twe\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "observe\tobserve\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "has\thas\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "been\tbeen\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "seen\tseen\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "previously\tpreviously\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "Fe\tfe\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
            "based\tbased\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "superconductors\tsuperconductors\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "also\talso\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "Nb\tnb\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "Pd\tpd\tINITCAP\tNODIGIT\tNOPUNCT\tXx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "81\t81\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "S\ts\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "5\t5\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "claimed\tclaimed\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "originate\toriginate\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "from\tfrom\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "multiband\tmultiband\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "superconductivity\tsuperconductivity\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "effect\teffect\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "The\tthe\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "resulting\tresulting\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "WB\twb\tALLCAPS\tNODIGIT\tNOPUNCT\tXX\t<other>\t<other>\n" +
            "4\t4\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "therefore\ttherefore\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "most\tmost\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "likely\tlikely\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "somewhere\tsomewhere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "between\tbetween\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "linear\tlinear\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "extrapolation\textrapolation\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "K\tk\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "71\t71\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "that\tthat\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "predicted\tpredicted\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "by\tby\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "WHH\twhh\tALLCAPS\tNODIGIT\tNOPUNCT\tXXX\t<other>\t<other>\n" +
            "model\tmodel\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "47\t47\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "Taking\ttaking\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "μ\tμ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "H\th\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A00\t\uF0A00\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t\uF0A0d\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "71\t71\tNOCAPS\tALLDIGIT\tNOPUNCT\tdd\t<other>\t<other>\n" +
            "T\tt\tALLCAPS\tNODIGIT\tNOPUNCT\tX\t<other>\t<other>\n" +
            "as\tas\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "upper\tupper\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "limit\tlimit\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "approximate\tapproximate\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "coherence\tcoherence\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "length\tlength\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "can\tcan\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "be\tbe\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "calculated\tcalculated\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "by\tby\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "using\tusing\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "the\tthe\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "Ginzburg\tginzburg\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "-\t-\tALLCAPS\tNODIGIT\tHYPHEN\t-\t<other>\t<other>\n" +
            "Landau\tlandau\tINITCAP\tNODIGIT\tNOPUNCT\tXxxx\t<other>\t<other>\n" +
            "formula\tformula\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "ξ\tξ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "GL\tgl\tALLCAPS\tNODIGIT\tNOPUNCT\tXX\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A0{f\t\uF0A0{f\tNOCAPS\tNODIGIT\tNOPUNCT\t\uF0A0{x\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "/\t/\tALLCAPS\tNODIGIT\tNOPUNCT\t/\t<other>\t<other>\n" +
            "[\t[\tALLCAPS\tNODIGIT\tOPENBRACKET\t[\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "πH\tπh\tNOCAPS\tNODIGIT\tNOPUNCT\txX\t<other>\t<other>\n" +
            "c\tc\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "]\t]\tALLCAPS\tNODIGIT\tENDBRACKET\t]\t<other>\t<other>\n" +
            "}\t}\tALLCAPS\tNODIGIT\tNOPUNCT\t}\t<other>\t<other>\n" +
            "1\t1\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "/\t/\tALLCAPS\tNODIGIT\tNOPUNCT\t/\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ",\t,\tALLCAPS\tNODIGIT\tCOMMA\t,\t<other>\t<other>\n" +
            "where\twhere\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "f\tf\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A0h\t\uF0A0h\tNOCAPS\tNODIGIT\tNOPUNCT\t\uF0A0x\t<other>\t<other>\n" +
            "/\t/\tALLCAPS\tNODIGIT\tNOPUNCT\t/\t<other>\t<other>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            "e\te\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "and\tand\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "is\tis\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "found\tfound\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "to\tto\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "be\tbe\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "ξ\tξ\tNOCAPS\tNODIGIT\tNOPUNCT\tx\t<other>\t<other>\n" +
            "GL\tgl\tALLCAPS\tNODIGIT\tNOPUNCT\tXX\t<other>\t<other>\n" +
            "(\t(\tALLCAPS\tNODIGIT\tOPENBRACKET\t(\t<other>\t<other>\n" +
            "0\t0\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ")\t)\tALLCAPS\tNODIGIT\tENDBRACKET\t)\t<other>\t<other>\n" +
            "\uF0A0\t\uF0A0\tALLCAPS\tNODIGIT\tNOPUNCT\t\uF0A0\t<other>\t<other>\n" +
            "=\t=\tALLCAPS\tNODIGIT\tNOPUNCT\t=\t<other>\t<other>\n" +
            "\uF0A026\t\uF0A026\tALLCAPS\tCONTAINDIGIT\tNOPUNCT\t\uF0A0dd\t<other>\t<other>\n" +
            "nm\tnm\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>\n" +
            "All\tall\tINITCAP\tNODIGIT\tNOPUNCT\tXxx\t<other>\t<other>\n" +
            "physical\tphysical\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "parameters\tparameters\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "for\tfor\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "WB\twb\tALLCAPS\tNODIGIT\tNOPUNCT\tXX\t<material>\tI-<link_right>\n" +
            "4\t4\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<material>\t<link_right>\n" +
            "2\t2\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<material>\t<link_right>\n" +
            "are\tare\tNOCAPS\tNODIGIT\tNOPUNCT\txxx\t<other>\t<other>\n" +
            "gathered\tgathered\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "in\tin\tNOCAPS\tNODIGIT\tNOPUNCT\txx\t<other>\t<other>\n" +
            "table\ttable\tNOCAPS\tNODIGIT\tNOPUNCT\txxxx\t<other>\t<other>\n" +
            "3\t3\tNOCAPS\tALLDIGIT\tNOPUNCT\td\t<other>\t<other>\n" +
            ".\t.\tALLCAPS\tNODIGIT\tDOT\t.\t<other>\t<other>";

        String input = "The W2 position occupancy is freely refined, but the fractional occupancy of the B2 site is constrained such that the occupancy of B2 = 1 − occupancy of W2, as described in previous studies . The positional parameters of B2 are freely refined. temperature range from 0.5 to 2.2 K under applied magnetic fields ranging from 0 to 600 mT. The T c steadily decreases with increasing applied field, as expected, and the last instance where the resistivity drops to below 50% is for T c \uF0A0=\uF0A00.62 K with μ 0 H\uF0A0=\uF0A0500 mT. The thus-determined upper critical fields (μ 0 H c2 ) plotted as a function of the estimated T c values were plotted in the main panel of and fitted to a line with slope dμ 0 H c2 /dT\uF0A0=\uF0A0−0.33 T K −1 . For many superconductors, the zero temperature upper critical field μ 0 H c2 (0) can be estimated with the Werthamer-Helfand-Hohenberg (WHH) equation given by: where A is −0.693 for the dirty limit and taking T c as ∼2.05 K for WB . Based on this model, the μ 0 H c2 (0)\uF0A0=\uF0A00.47 T (indicated by the blue closed circle in , however the last measured μ 0 H c2 value is 0.50 T which is already above the WHH-predicted upper critical field of 0.47 T. The nearly linear H c2 (T) over a broad temperature range that we observe has been seen previously for Fe based superconductors and also for Nb 2 Pd 0.81 S 5 and claimed to originate from the multiband superconductivity effect. The resulting μ 0 H c2 (0) for WB 4.2 is therefore most likely somewhere between the linear extrapolation to 0 K (0.71 T) and that predicted by the WHH model (0.47 T). Taking μ 0 H c2 (0)\uF0A0=\uF0A00.71 T as the upper limit, the approximate coherence length can be calculated by using the Ginzburg-Landau formula ξ GL (0)\uF0A0=\uF0A0{f 0 /[2πH c2 (0)]} 1/2 , where f 0 \uF0A0=\uF0A0h/2e and is found to be ξ GL (0)\uF0A0=\uF0A026 nm. All physical parameters for WB 4.2 are gathered in table 3.";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        Span span1 = new Span("5990861534589311109", "0.62 K", "<tcValue>", "bao", 484, 490, 190, 195);
        Span span2 = new Span("-5382679164980384918", "2.05 K", "<tcValue>", "bao", 930, 936, 396, 401);
        Span span3 = new Span("19-211", "WB", "<material>", "bao", 941, 943, 404, 405);
        Span span4 = new Span("-1500774536986790910", "WB 4.2", "<material>", "bao", 1812, 1818, 813, 818);

        List<Span> links = CRFBasedLinker.extractResults(layoutTokens, result, 
            Arrays.asList(span1, span2, span3, span4), 
            SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC, ENTITY_LINKER_MATERIAL_TC_LEFT_ATTACHMENT, 
            ENTITY_LINKER_MATERIAL_TC_RIGHT_ATTACHMENT, ENTITY_LINKER_MATERIAL_TC_OTHER);

        System.out.println(links);
    }

    @Test
    public void testEntityLinker() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<link_right>", 0, 3),
            Triple.of("<link_left>", 46, 49)
        );

        String result = getWapitiResult(layoutTokens, labels);

        Span sup1 = new Span();
        sup1.setText("MgB 2");
        sup1.setType("superconductor");
        sup1.setOffsetStart(0);
        sup1.setOffsetEnd(6);
        sup1.setLayoutTokens(IntStream.rangeClosed(0, 3).mapToObj(i -> layoutTokens.get(i)).collect(Collectors.toList()));

        Span sup2 = new Span();
        sup2.setText("40 K");
        sup2.setType("temperature");
        sup2.setOffsetStart(142);
        sup2.setOffsetEnd(146);
        sup2.setLayoutTokens(IntStream.rangeClosed(46, 49).mapToObj(i -> layoutTokens.get(i)).collect(Collectors.toList()));

        List<Span> links = target.extractResults(layoutTokens, result, Arrays.asList(sup1, sup2),
            SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC, ENTITY_LINKER_MATERIAL_TC_LEFT_ATTACHMENT,
            ENTITY_LINKER_MATERIAL_TC_RIGHT_ATTACHMENT, ENTITY_LINKER_MATERIAL_TC_OTHER);

        assertThat(links, hasSize(2));
        assertThat(links.get(0).getText(), is("MgB 2"));
        assertThat(links.get(0).getLinks().get(0).getTargetId(), is(String.valueOf(sup2.getId())));

    }

    /**
     * Utility method to generate a hypotetical result from wapiti.
     * Useful for testing the extraction of the sequence labeling.
     *
     * @param layoutTokens layout tokens of the initial text
     * @param labels       label maps. A list of Tripels, containing label (left), start_index (middle) and end_index exclusive (right)
     * @return a string containing the resulting features + labels returned by wapiti
     */
    public static String getWapitiResult(List<LayoutToken> layoutTokens, List<Triple<String, Integer, Integer>> labels) {
        PowerMock.mockStatic(Lexicon.class);
        List<String> features = layoutTokens.stream()
            .map(token -> FeaturesVectorEntityLinker.addFeatures(token.getText(), null, "other").printVector())
            .collect(Collectors.toList());

        List<String> labeled = new ArrayList<>();
        int idx = 0;

        for (Triple<String, Integer, Integer> label : labels) {

            if (idx < label.getMiddle()) {
                for (int i = idx; i < label.getMiddle(); i++) {
                    labeled.add("<other>");
                    idx++;
                }
            }

            for (int i = label.getMiddle(); i < label.getRight(); i++) {
                labeled.add(label.getLeft());
                idx++;
            }
        }

        if (idx < features.size()) {
            for (int i = idx; i < features.size(); i++) {
                labeled.add("<other>");
                idx++;
            }
        }

        assertThat(features, hasSize(labeled.size()));

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < features.size(); i++) {
            if (features.get(i) == null || features.get(i).startsWith(" ")) {
                continue;
            }
            sb.append(features.get(i)).append(" ").append(labeled.get(i)).append("\n");
        }

        return sb.toString();
    }
}