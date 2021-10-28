package org.grobid.core.utilities;

import org.grobid.core.data.material.ChemicalComposition;
import org.junit.Test;
import shadedwipo.org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;


public class ChemicalMaterialParserClientTest {

    @Test
    public void testFromJsonToChemicalComposition() {
        
        InputStream is = IOUtils.toInputStream("{\"composition\": {\"La\": \"3\", \"Ir\": \"2\", \"Ge\": \"2\"}, \"name\": \"\", \"formula\": \"La3Ir2Ge2\"}");
        ChemicalComposition chemicalComposition = ChemicalMaterialParserClient.fromJsonToChemicalComposition(is);
        
        assertThat(chemicalComposition, is(notNullValue()));
        assertThat(chemicalComposition.getFormula(), is("La3Ir2Ge2"));
        assertThat(chemicalComposition.getComposition(), is(notNullValue()));
        assertThat(chemicalComposition.getComposition().keySet(), hasSize(3));
        assertThat(chemicalComposition.getComposition().get("La"), is("3"));
    }
}