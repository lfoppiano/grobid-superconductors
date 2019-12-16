package org.grobid.core.engines.training;

import nu.xom.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SuperconductorsTrainingXMLFormatterTest {

    SuperconductorsTrainingXMLFormatter target;

    @Before
    public void setUp() throws Exception {
        target = new SuperconductorsTrainingXMLFormatter();
    }

    @Test
    public void testTrainingData_value() throws Exception {
        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(19);
        superconductor.setOffsetEnd(30);
        superconductor.setName("(TMTSF)2PF6");

        String text = "The Bechgaard salt (TMTSF)2PF6 (TMTSF = tetra- methyltetraselenafulvalene) was";

        superconductorList.add(superconductor);

        Element out = target.trainingExtraction(superconductorList, DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text));
        assertThat(out.toXML(), is("<p xmlns=\"http://www.tei-c.org/ns/1.0\">The Bechgaard salt <material>(TMTSF)2PF6</material> (TMTSF = tetra- methyltetraselenafulvalene) was</p>"));
    }


    @Test
    public void testTrainingData2_value() throws Exception {
        String text = "Specific-Heat Study of Superconducting and Normal States in FeSe 1-x Te x (0.6 ≤ x ≤ 1) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 4);
        });

        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(64);
        superconductor.setOffsetEnd(77);
        superconductor.setName("FeSe 1-x Te x");

        Superconductor superconductor2 = new Superconductor();
        superconductor2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor2.setOffsetStart(79);
        superconductor2.setOffsetEnd(90);
        superconductor2.setName("0.6 ≤ x ≤ 1");

        superconductorList.add(superconductor);
        superconductorList.add(superconductor2);

        Element out = target.trainingExtraction(superconductorList, layoutTokens);
        assertThat(out.toXML(), is("<p xmlns=\"http://www.tei-c.org/ns/1.0\">Specific-Heat Study of Superconducting and Normal States in <material>FeSe 1-x Te x</material> (<material>0.6 ≤ x ≤ 1</material>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</p>"));
    }


    @Test
    public void testTrainingData3_value() throws Exception {
        String text = "Specific-Heat Study of Superconducting and Normal States in FeSe 1-x Te x (0.6 ≤ x ≤ 1) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 4);
        });

        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(64);
        superconductor.setOffsetEnd(77);
        superconductor.setName("FeSe 1-x Te x");

        Superconductor superconductor2 = new Superconductor();
        superconductor2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor2.setOffsetStart(79);
        superconductor2.setOffsetEnd(90);
        superconductor2.setName("0.6 ≤ x ≤ 1");

        superconductorList.add(superconductor);
        superconductorList.add(superconductor2);

        List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList = new ArrayList<>();
        labeledTextList.add(Pair.of(superconductorList, layoutTokens));


        String output = target.format(labeledTextList, 1);
        assertThat(output,
            endsWith("<text xml:lang=\"en\"><p>Specific-Heat Study of Superconducting and Normal States in <material>FeSe 1-x Te x</material> (<material>0.6 ≤ x ≤ 1</material>) Single Crystals: Strong-Coupling Superconductivity, Strong Electron-Correlation, and Inhomogeneity</p></text></tei>"));
    }

    @Test
    public void testTrainingData4_value() throws Exception {
        String text = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(445);
        superconductor.setOffsetEnd(458);
        superconductor.setName("FeSe 1-x Te x");
        superconductorList.add(superconductor);

        Superconductor superconductor2 = new Superconductor();
        superconductor2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor2.setOffsetStart(460);
        superconductor2.setOffsetEnd(471);
        superconductor2.setName("0.6 ≤ x ≤ 1");
        superconductorList.add(superconductor2);

        Superconductor superconductor3 = new Superconductor();
        superconductor3.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor3.setOffsetStart(549);
        superconductor3.setOffsetEnd(561);
        superconductor3.setName("x = 0.6 -0.9");
        superconductorList.add(superconductor3);

        Superconductor superconductor4 = new Superconductor();
        superconductor4.setType(SUPERCONDUCTORS_TC_VALUE_LABEL);
        superconductor4.setOffsetStart(562);
        superconductor4.setOffsetEnd(569);
        superconductor4.setName("exhibit");
        superconductorList.add(superconductor4);

        Superconductor superconductor5 = new Superconductor();
        superconductor5.setType(SUPERCONDUCTORS_TC_LABEL);
        superconductor5.setOffsetStart(570);
        superconductor5.setOffsetEnd(592);
        superconductor5.setName("bulk superconductivity");
        superconductorList.add(superconductor5);

        Superconductor superconductor6 = new Superconductor();
        superconductor6.setType(SUPERCONDUCTORS_TC_LABEL);
        superconductor6.setOffsetStart(632);
        superconductor6.setOffsetEnd(647);
        superconductor6.setName("superconducting");
        superconductorList.add(superconductor6);

        Superconductor superconductor7 = new Superconductor();
        superconductor7.setType(SUPERCONDUCTORS_TC_LABEL);
        superconductor7.setOffsetStart(653);
        superconductor7.setOffsetEnd(675);
        superconductor7.setName("transition temperature");
        superconductorList.add(superconductor7);

        List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList = new ArrayList<>();
        labeledTextList.add(Pair.of(superconductorList, layoutTokens));

        String output = target.format(labeledTextList, 1);

        assertThat(output.substring(output.indexOf("<text xml:lang=\"en\">")),
            is("<text xml:lang=\"en\"><p>The electronic specific heat of as-grown and annealed single-crystals of <material>FeSe 1-x Te x</material> (<material>0.6 ≤ x ≤ 1</material>) has been investigated. It has been found that annealed single-crystals with <material>x = 0.6 -0.9</material> <tcValue>exhibit</tcValue> <tc>bulk superconductivity</tc> with a clear specific-heat jump at the <tc>superconducting</tc> (SC) <tc>transition temperature</tc>, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.</p></text></tei>"));
    }

    @Test(expected = RuntimeException.class)
    public void testTrainingData5_wrongOffsets_shoudlThrowException() throws Exception {
        String text = "The electronic specific heat of as-grown and annealed single-crystals of FeSe 1-x Te x (0.6 ≤ x ≤ 1) has been investigated. It has been found that annealed single-crystals with x = 0.6 -0.9 exhibit bulk superconductivity with a clear specific-heat jump at the superconducting (SC) transition temperature, T c . Both 2Δ 0 /k B T c [Δ 0 : the SC gap at 0 K estimated using the single-band BCS s-wave model] and ⊿C/(γ n -γ 0 )T c [⊿C: the specific-heat jump at T c , γ n : the electronic specific-heat coefficient in the normal state, γ 0 : the residual electronic specific-heat coefficient at 0 K in the SC state] are largest in the well-annealed single-crystal with x = 0.7, i.e., 4.29 and 2.76, respectively, indicating that the superconductivity is of the strong coupling. The thermodynamic critical field has also been estimated. γ n has been found to be one order of magnitude larger than those estimated from the band calculations and increases with increasing x at x = 0.6 -0.9, which is surmised to be due to the increase in the electronic effective mass, namely, the enhancement of the electron correlation. It has been found that there remains a finite value of γ 0 in the SC state even in the well-annealed single-crystals with x = 0.8 -0.9, suggesting an inhomogeneous electronic state in real space and/or momentum space.";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(l -> {
            l.setOffset(l.getOffset() + 372);
        });

        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor.setOffsetStart(445);
        superconductor.setOffsetEnd(458);
        superconductor.setName("FeSe 1-x Te x");
        superconductorList.add(superconductor);

        Superconductor superconductor2 = new Superconductor();
        superconductor2.setType(SUPERCONDUCTORS_MATERIAL_LABEL);
        superconductor2.setOffsetStart(460);
        superconductor2.setOffsetEnd(472);
        superconductor2.setName("0.6 ≤ x ≤ 1");
        superconductorList.add(superconductor2);

        List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList = new ArrayList<>();
        labeledTextList.add(Pair.of(superconductorList, layoutTokens));

        target.format(labeledTextList, 1);
    }
}