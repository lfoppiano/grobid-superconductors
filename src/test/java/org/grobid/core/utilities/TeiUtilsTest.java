package org.grobid.core.utilities;

import nu.xom.Element;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class TeiUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testGetElement() {
        Element root = TeiUtils.getTeiHeader(123);
        Element teiHeader = TeiUtils.getElement(root, "teiHeader");
        Element fileDesc = TeiUtils.getElement(teiHeader, "fileDesc");

        assertThat(fileDesc, is(notNullValue()));

    }
}