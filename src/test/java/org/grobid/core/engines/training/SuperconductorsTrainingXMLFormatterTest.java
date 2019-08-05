package org.grobid.core.engines.training;

import nu.xom.Element;
import org.grobid.core.data.Superconductor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SuperconductorsTrainingXMLFormatterTest {

    SuperconductorsTrainingXMLFormatter target;

    @Before
    public void setUp() throws Exception {
        target = new SuperconductorsTrainingXMLFormatter();
//        LibraryLoader.load();
    }

    @Test
    public void testTrainingData_value() throws Exception {
        List<Superconductor> superconductorList = new ArrayList<>();
        Superconductor superconductor = new Superconductor();
        superconductor.setType("material");
        superconductor.setOffsetStart(19);
        superconductor.setOffsetEnd(30);
        superconductor.setName("(TMTSF)2PF6");

        String text = "The Bechgaard salt (TMTSF)2PF6 (TMTSF = tetra- methyltetraselenafulvalene) was";

        superconductorList.add(superconductor);

        Element out = target.trainingExtraction(superconductorList, text);
        assertThat(out.toXML(), is("<p xmlns=\"http://www.tei-c.org/ns/1.0\">The Bechgaard salt <material>(TMTSF)2PF6</material> (TMTSF = tetra- methyltetraselenafulvalene) was</p>"));
    }
}