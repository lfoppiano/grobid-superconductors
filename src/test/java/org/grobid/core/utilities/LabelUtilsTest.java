package org.grobid.core.utilities;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LabelUtilsTest {
    
    @Test
    public void testGetPlainLabelName() throws Exception {
        assertThat(LabelUtils.getPlainLabelName("<header>"), is("header"));
    }

}