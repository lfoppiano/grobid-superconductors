package org.grobid.core.utilities;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SuperconductorsLabelUtilsTest {
    
    @Test
    public void testGetPlainLabelName() throws Exception {
        assertThat(SuperconductorsLabelUtils.getPlainLabelName("<header>"), is("header"));
    }

}