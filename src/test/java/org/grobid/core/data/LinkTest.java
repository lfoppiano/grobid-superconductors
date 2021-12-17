package org.grobid.core.data;

import org.grobid.core.data.document.Link;
import org.grobid.core.engines.label.SuperconductorsTaggingLabels;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


public class LinkTest {

    @Test
    public void testGetLinkType_material_tcValue() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL), is(Link.MATERIAL_TCVALUE_TYPE));
    }

    @Test
    public void testGetLinkType_pressure_tcValue() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_PRESSURE_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL), is(Link.TCVALUE_PRESSURE_TYPE));
    }

    @Test
    public void testGetLinkType_tcValue_pressure() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_PRESSURE_LABEL), is(Link.TCVALUE_PRESSURE_TYPE));
    }

    @Test
    public void testGetLinkType_tcValue_material() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL), is(Link.MATERIAL_TCVALUE_TYPE));
    }

    @Test
    public void testGetLinkType_me_method_tcValue() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL), is(Link.TCVALUE_ME_METHOD_TYPE));
    }

    @Test
    public void testGetLinkType_tcValue_me_method() throws Exception {
        assertThat(Link.getLinkType(SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL), is(Link.TCVALUE_ME_METHOD_TYPE));
    }

    @Test(expected = RuntimeException.class)
    public void testGetLinkType_wrongType_shouldThrowException() throws Exception {
        assertThat(Link.getLinkType(null, SuperconductorsTaggingLabels.SUPERCONDUCTORS_MEASUREMENT_METHOD_LABEL), is(Link.TCVALUE_ME_METHOD_TYPE));
    }

}