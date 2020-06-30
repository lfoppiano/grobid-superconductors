package org.grobid.core.engines;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.internal.bytebuddy.implementation.bind.annotation.Super;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

public class EntityLinkerParserTest {
    EntityLinkerParser target;

    @Before
    public void setUp() throws Exception {
        target = new EntityLinkerParser(GrobidModels.DUMMY, Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL));
    }

    @Test
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
    public void testEntityLinker() throws Exception {
        String input = "MgB 2 was discovered to be a superconductor in 2001, and it has a remarkably high critical temperature (T c ) around 40 K with a simple hexagonal structure.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<link_right>", 0, 3),
            Triple.of("<link_left>", 46, 49)
        );

        String result = getWapitiResult(layoutTokens, labels);

        Superconductor sup1 = new Superconductor();
        sup1.setName("MgB 2");
        sup1.setType("superconductor");
        sup1.setOffsetStart(0);
        sup1.setOffsetEnd(6);
        sup1.setLayoutTokens(IntStream.rangeClosed(0, 3).mapToObj(i -> layoutTokens.get(i)).collect(Collectors.toList()));

        Superconductor sup2= new Superconductor();
        sup2.setName("40 K");
        sup2.setType("temperature");
        sup2.setOffsetStart(142);
        sup2.setOffsetEnd(146);
        sup2.setLayoutTokens(IntStream.rangeClosed(46, 49).mapToObj(i -> layoutTokens.get(i)).collect(Collectors.toList()));

        List<Superconductor> links = target.extractResults(layoutTokens, result, Arrays.asList(sup1, sup2));

        assertThat(links, hasSize(2));
        assertThat(links.get(0).getName(), is("MgB 2"));
        assertThat(links.get(0).getLinkedEntity().getName(), is("40 K"));

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