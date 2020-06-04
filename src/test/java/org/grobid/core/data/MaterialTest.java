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

//        List<Material> materials = Material.resolveVariables(material);
//
//        assertThat(materials, hasSize(3));
//        assertThat(materials.get(0).getFormula(), is("Fe0.9Cu0.1O2"));
//        assertThat(materials.get(1).getFormula(), is("Fe0.8Cu0.2O2"));
//        assertThat(materials.get(2).getFormula(), is("Fe0.7Cu0.3O2"));
        List<String> outputMaterials = Material.resolveVariables(material);

        assertThat(outputMaterials, hasSize(3));
        assertThat(outputMaterials.get(0), is("Fe0.9Cu0.1O2"));
        assertThat(outputMaterials.get(1), is("Fe0.8Cu0.2O2"));
        assertThat(outputMaterials.get(2), is("Fe0.7Cu0.3O2"));
    }

    @Test
    public void testResolveVariable_2() throws Exception {
        Material material = new Material();
        material.setFormula("Fe1-xCuyO2");
        material.getVariables().put("x", Arrays.asList("0.1", "0.2", "0.3"));
        material.getVariables().put("y", Arrays.asList("-1", "-0.2", "0.3", "0.5"));

//        List<Material> materials = Material.resolveVariables(material);
//
//        assertThat(materials, hasSize(12));
//        assertThat(materials.get(0).getFormula(), is("Fe0.9Cu-1O2"));
//        assertThat(materials.get(1).getFormula(), is("Fe0.9Cu-0.2O2"));
//        assertThat(materials.get(2).getFormula(), is("Fe0.9Cu0.3O2"));
//        assertThat(materials.get(3).getFormula(), is("Fe0.9Cu0.5O2"));
//        assertThat(materials.get(4).getFormula(), is("Fe0.8Cu-1O2"));
//        assertThat(materials.get(5).getFormula(), is("Fe0.8Cu-0.2O2"));
//        assertThat(materials.get(6).getFormula(), is("Fe0.8Cu0.3O2"));
//        assertThat(materials.get(7).getFormula(), is("Fe0.8Cu0.5O2"));
//        assertThat(materials.get(8).getFormula(), is( "Fe0.7Cu-1O2"));
//        assertThat(materials.get(9).getFormula(), is( "Fe0.7Cu-0.2O2"));
//        assertThat(materials.get(10).getFormula(), is("Fe0.7Cu0.3O2"));
//        assertThat(materials.get(11).getFormula(), is("Fe0.7Cu0.5O2"));

        List<String> outputMaterials = Material.resolveVariables(material);

        assertThat(outputMaterials, hasSize(12));
        assertThat(outputMaterials.get(0), is("Fe0.9Cu-1O2"));
        assertThat(outputMaterials.get(1), is("Fe0.9Cu-0.2O2"));
        assertThat(outputMaterials.get(2), is("Fe0.9Cu0.3O2"));
        assertThat(outputMaterials.get(3), is("Fe0.9Cu0.5O2"));
        assertThat(outputMaterials.get(4), is("Fe0.8Cu-1O2"));
        assertThat(outputMaterials.get(5), is("Fe0.8Cu-0.2O2"));
        assertThat(outputMaterials.get(6), is("Fe0.8Cu0.3O2"));
        assertThat(outputMaterials.get(7), is("Fe0.8Cu0.5O2"));
        assertThat(outputMaterials.get(8), is( "Fe0.7Cu-1O2"));
        assertThat(outputMaterials.get(9), is( "Fe0.7Cu-0.2O2"));
        assertThat(outputMaterials.get(10), is("Fe0.7Cu0.3O2"));
        assertThat(outputMaterials.get(11), is("Fe0.7Cu0.5O2"));

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

    @Test
    public void testReplaceVariable4(){
        String output = Material.replaceVariable("LnFeAs(O1−x Fx)", "Ln", "Pr");

        assertThat(output, is("PrFeAs(O1−x Fx)"));
    }

//    @Test
//    public void testToJson() {
//        Material material = new Material();
//
//        material.setName("Material Name");
//        material.setDoping("Doping!");
//        material.setShape("shape!");
//        material.setFormula("Cu x Fe y");
//        material.addVariable("x", Arrays.asList("1","2","3"));
//        material.addVariable("y", Arrays.asList("1","2","3"));
//
//        List<String> outputMaterials = Material.resolveVariables(material);
//
//        System.out.println(material.toJson());
//    }

}