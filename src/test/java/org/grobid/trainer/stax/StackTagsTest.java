package org.grobid.trainer.stax;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class StackTagsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testEquals() {
        StackTags a = StackTags.from("/bao/miao/ciao");
        StackTags b = StackTags.from("/bao/miao/ciao");

        assertThat(a.equals(b), is(Boolean.TRUE));
    }
}