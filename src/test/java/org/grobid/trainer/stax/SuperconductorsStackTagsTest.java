package org.grobid.trainer.stax;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SuperconductorsStackTagsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testEquals() {
        SuperconductorsStackTags a = SuperconductorsStackTags.from("/bao/miao/ciao");
        SuperconductorsStackTags b = SuperconductorsStackTags.from("/bao/miao/ciao");

        assertThat(a.equals(b), is(Boolean.TRUE));
    }
}