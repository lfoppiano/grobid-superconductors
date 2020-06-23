package org.grobid.core.engines;

import org.grobid.core.GrobidModels;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EntityLinkerParserTest {
    EntityLinkerParser target;

    @Before
    public void setUp() throws Exception {
        target = new EntityLinkerParser(GrobidModels.DUMMY);
    }

    @Test
    public void testEntityLinker() throws Exception {
        String result = "";

//        String LayoutToken =
//        target.extractResults()
    }
}