package org.grobid.core.utilities;

import org.grobid.core.data.ChemicalComposition;
import org.junit.Test;
import shadedwipo.org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;


public class ChemicalMaterialParserClientTest {

    @Test
    public void testFromJsonToChemicalComposition() {
        
        InputStream is = IOUtils.toInputStream("{\"elements\": {\"La\": \"3\", \"Ir\": \"2\", \"Ge\": \"2\"}, \"amounts_vars\": {}, \"elements_vars\": {}, \"formula\": \"La3Ir2Ge2\",\n" +
            "\"oxygen_deficiency\": null}");
        ChemicalComposition chemicalComposition = ChemicalMaterialParserClient.fromJsonToChemicalComposition(is);
        
        assertThat(chemicalComposition, is(notNullValue()));
        assertThat(chemicalComposition.getFormula(), is("La3Ir2Ge2"));
        assertThat(chemicalComposition.getElements(), is(notNullValue()));
        assertThat(chemicalComposition.getElements().keySet(), hasSize(3));
        assertThat(chemicalComposition.getElements().get("La"), is("3"));
        assertThat(chemicalComposition.getAmountsVars().keySet(), hasSize(0));
        assertThat(chemicalComposition.getOxygenDeficency(), is(nullValue()));
    }
}