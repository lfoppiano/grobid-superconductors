package org.grobid.core.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class MaterialTest {

    @Test
    public void testResolveVariable_1() throws Exception {
        Material material = new Material();
        material.setFormula("Fe1-xCuxO2");
        material.getVariables().put("x", Arrays.asList("0.1", "0.2", "0.3"));

        List<Material> materials = Material.resolveVariables(material);

        assertThat(materials, hasSize(3));
        assertThat(materials.get(0).getFormula(), is("Fe0.9Cu0.1O2"));
        assertThat(materials.get(1).getFormula(), is("Fe0.8Cu0.2O2"));
        assertThat(materials.get(2).getFormula(), is("Fe0.7Cu0.3O2"));
    }

    @Test
    public void testResolveVariable_2() throws Exception {
        Material material = new Material();
        material.setFormula("Fe1-xCuyO2");
        material.getVariables().put("x", Arrays.asList("0.1", "0.2", "0.3"));
        material.getVariables().put("y", Arrays.asList("-1", "-0.2", "0.3", "0.5"));

        List<Material> materials = Material.resolveVariables(material);

        assertThat(materials, hasSize(12));
        assertThat(materials.get(0).getFormula(), is("Fe0.9Cu-1O2"));
        assertThat(materials.get(1).getFormula(), is("Fe0.9Cu-0.2O2"));
        assertThat(materials.get(2).getFormula(), is("Fe0.9Cu0.3O2"));
        assertThat(materials.get(3).getFormula(), is("Fe0.9Cu0.5O2"));
        assertThat(materials.get(4).getFormula(), is("Fe0.8Cu-1O2"));
        assertThat(materials.get(5).getFormula(), is("Fe0.8Cu-0.2O2"));
        assertThat(materials.get(6).getFormula(), is("Fe0.8Cu0.3O2"));
        assertThat(materials.get(7).getFormula(), is("Fe0.8Cu0.5O2"));
        assertThat(materials.get(8).getFormula(), is( "Fe0.7Cu-1O2"));
        assertThat(materials.get(9).getFormula(), is( "Fe0.7Cu-0.2O2"));
        assertThat(materials.get(10).getFormula(), is("Fe0.7Cu0.3O2"));
        assertThat(materials.get(11).getFormula(), is("Fe0.7Cu0.5O2"));

    }

    @Test
    public void testReplaceVariable(){
        String output = Material.replaceVariable("Fe1-xCuxO2", "x", "0.8");

        assertThat(output, is("Fe0.2Cu0.8O2"));
    }

    @Test
    public void testReplaceVariable2(){
        String output = Material.replaceVariable("Fe-xCu1-xO2", "x", "0.8");

        assertThat(output, is("Fe-0.8Cu0.2O2"));
    }

    @Test
    public void testReplaceVariable3(){
        String output = Material.replaceVariable("FexCuxO2", "x", "0.8");

        assertThat(output, is("Fe0.8Cu0.8O2"));
    }

}