package org.grobid.core.engines.training;

import nu.xom.Element;
import org.grobid.core.data.Superconductor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SuperconductorsTrainingTSVFormatterTest {

    SuperconductorsTrainingTSVFormatter target;

    @Before
    public void setUp() throws Exception {
        target = new SuperconductorsTrainingTSVFormatter();
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

//        String out = target.trainingExtraction(superconductorList, text, 1);
//
//        System.out.println(out);
    }
}