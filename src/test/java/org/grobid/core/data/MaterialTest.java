package org.grobid.core.data;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class MaterialTest {

    @Test
    public void testResolveFormula_shouldNotExpandNorThrowException() throws Exception {
        List<String> outputFormulas = Material.expandFormula("(TMTTF) 2 PF 6");

        assertThat(outputFormulas, hasSize(1));
        assertThat(outputFormulas.get(0), is("(TMTTF) 2 PF 6"));
    }

    @Test
    public void testResolveFormula() throws Exception {
        List<String> outputFormulas = Material.expandFormula("(Sr, Na)Fe 2 As 2");

        assertThat(outputFormulas, hasSize(1));
        assertThat(outputFormulas.get(0), is("Sr x Na 1-x Fe 2 As 2"));
    }

    @Test
    public void testExpandFormula3() throws Exception {
        String formula = "(Sr,K)Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, IsCollectionWithSize.hasSize(1));
        assertThat(expandFormulas.get(0), is("Sr x K 1-x Fe2As2"));
    }

    @Test
    public void testExpandFormula4() throws Exception {
        String formula = "(Sr , K ) Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, IsCollectionWithSize.hasSize(1));
        assertThat(expandFormulas.get(0), is("Sr x K 1-x Fe2As2"));
    }

    @Test
    public void testExpandName() throws Exception {
        String formula = "(Sr,K)-2222";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, IsCollectionWithSize.hasSize(2));
        assertThat(expandFormulas.get(0), is("Sr-2222"));
        assertThat(expandFormulas.get(1), is("K-2222"));
    }

    @Test
    public void testExpandFormula_2variables() throws Exception {
        String inputFormula = "(Sr, La) Fe 2 O 7";

        List<String> expandedFormulas = Material.expandFormula(inputFormula);

        assertThat(expandedFormulas, hasSize(1));
        assertThat(expandedFormulas.get(0), is("Sr x La 1-x Fe 2 O 7"));

    }

    @Test
    public void testExpandFormula_4variables() throws Exception {
        String inputFormula = "(Sr, La, Cu, K) Fe 2 O 7";

        List<String> expandedFormulas = Material.expandFormula(inputFormula);

        assertThat(expandedFormulas, hasSize(1));
        assertThat(expandedFormulas.get(0), is("Sr 1-x-y-z La x Cu y K z Fe 2 O 7"));

    }

    @Test(expected = RuntimeException.class)
    public void testExpandFormulaWithTooManyVariables_shouldThrowsException() throws Exception {
        String inputFormula = "(Sr, Fe, La,Sr, Fe, La,Sr, Fe, La,Sr, Fe, La,Sr, Fe, La,Sr, Fe, La,Sr, Fe, La,Sr, Fe, La, Sr, Fe, La,Sr, Fe, Sr, Fe, La,Sr, Fe, Sr, Fe, La,Sr, Fe) Cu 2 O 13";

        System.out.println(Material.expandFormula(inputFormula));
    }

    @Test
    public void testResolveVariable_1() throws Exception {
        Material material = new Material();
        material.setFormula("Fe1-xCuxO2");
        material.getVariables().put("x", Arrays.asList("0.1", "0.2", "0.3"));
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
        assertThat(outputMaterials.get(8), is("Fe0.7Cu-1O2"));
        assertThat(outputMaterials.get(9), is("Fe0.7Cu-0.2O2"));
        assertThat(outputMaterials.get(10), is("Fe0.7Cu0.3O2"));
        assertThat(outputMaterials.get(11), is("Fe0.7Cu0.5O2"));

    }

    @Test
    public void testResolveVariable_3() throws Exception {
        Material material = new Material();
        material.setFormula("Li x (NH 3 ) y Fe 2 (Te z Se 1−z ) 2");
        material.getVariables().put("x", Arrays.asList("0.1"));
        material.getVariables().put("y", Arrays.asList("0.1"));
        material.getVariables().put("z", Arrays.asList("0.1"));
        List<String> outputMaterials = Material.resolveVariables(material);

        assertThat(outputMaterials, hasSize(1));
        assertThat(outputMaterials.get(0), is("Li 0.1 (NH 3 ) 0.1 Fe 2 (Te 0.1 Se 0.9 ) 2"));
    }

    @Test
    public void testResolveVariable_interval() throws Exception {
        Material material = new Material();
        material.setFormula("Li x (NH 3 ) 1-x Fe 2 (Te x Se 1−x ) 2");
        material.getVariables().put("x", Arrays.asList("< 0.1", "> 0.01"));
        List<String> outputMaterials = Material.resolveVariables(material);

        assertThat(outputMaterials, hasSize(2));
        assertThat(outputMaterials.get(0), is("Li 0.1 (NH 3 ) 0.9 Fe 2 (Te 0.1 Se 0.9 ) 2"));
        assertThat(outputMaterials.get(1), is("Li 0.01 (NH 3 ) 0.99 Fe 2 (Te 0.01 Se 0.99 ) 2"));
    }

    @Test
    public void testReplaceVariable() {
        String output = Material.replaceVariable("Fe1-xCuxO2", "x", "0.8");

        assertThat(output, is("Fe0.2Cu0.8O2"));
    }

    @Test
    public void testReplaceVariable2() {
        String output = Material.replaceVariable("Fe-xCu1-xO2", "x", "0.8");

        assertThat(output, is("Fe-0.8Cu0.2O2"));
    }

    @Test
    public void testReplaceVariable3() {
        String output = Material.replaceVariable("FexCuxO2", "x", "0.8");

        assertThat(output, is("Fe0.8Cu0.8O2"));
    }

    @Test
    public void testReplaceVariable4() {
        String output = Material.replaceVariable("LnFeAs(O1−x Fx)", "Ln", "Pr");

        assertThat(output, is("PrFeAs(O1−x Fx)"));
    }

    @Test
    public void testReplaceVariable5() {
        String output = Material.replaceVariable("1-x Ru x", "x", "0.2");

        assertThat(output, is("0.8 Ru 0.2"));
    }

    @Test
    public void testReplaceVariable_errorCase_1() {
        String output = Material.replaceVariable("RE", "RE", "Sc");

        assertThat(output, is("Sc"));
    }

    @Test
    public void testGeneratePermutations() {
        String formula = "Li x (NH 3 ) y Fe 2 (Te z Se 1−z ) 2";

        Map<String, List<String>> variables = new HashMap<>();
        variables.put("x", Arrays.asList("0.1"));
        variables.put("y", Arrays.asList("0.1"));
        variables.put("z", Arrays.asList("0.1"));
        List<String> result = new ArrayList<>();
        Material.generatePermutations(variables, new ArrayList<>(variables.keySet()), result,
            Pair.of(0, 0), formula);

        assertThat(result, hasSize(1));
        assertThat(result.get(0), is("Li 0.1 (NH 3 ) 0.1 Fe 2 (Te 0.1 Se 0.9 ) 2"));

    }

    @Test
    public void testGeneratePermutations_2() {
        String formula = "Li x (NH 3 ) y Fe 2 (Te z Se 1−z ) 2";

        Map<String, List<String>> variables = new HashMap<>();
        variables.put("x", Arrays.asList("0.1", "0.2"));
        variables.put("y", Arrays.asList("0.1", "0.2"));
        variables.put("z", Arrays.asList("0.1"));
        List<String> result = new ArrayList<>();
        Material.generatePermutations(variables, new ArrayList<>(variables.keySet()), result,
            Pair.of(0, 0), formula);

        assertThat(result, hasSize(4));
        assertThat(result.get(0), is("Li 0.1 (NH 3 ) 0.1 Fe 2 (Te 0.1 Se 0.9 ) 2"));
        assertThat(result.get(1), is("Li 0.1 (NH 3 ) 0.2 Fe 2 (Te 0.1 Se 0.9 ) 2"));
        assertThat(result.get(2), is("Li 0.2 (NH 3 ) 0.1 Fe 2 (Te 0.1 Se 0.9 ) 2"));
        assertThat(result.get(3), is("Li 0.2 (NH 3 ) 0.2 Fe 2 (Te 0.1 Se 0.9 ) 2"));
    }

    @Test
    public void testAsAttributeMap_materialWithFormula() {
        Material material = new Material();
        material.setFormula("La Fe 2");
        Map<String, String> attributeMap = Material.asAttributeMap(material, "test");

        assertThat(attributeMap.keySet(), hasSize(1));
        assertThat(attributeMap.get("test_formula"), is("La Fe 2"));
    }

    @Test
    public void testAsAttributeMap_materialWithName() {
        Material material = new Material();
        material.setName("Oxygen");
        Map<String, String> attributeMap = Material.asAttributeMap(material, "test");

        assertThat(attributeMap.keySet(), hasSize(1));
        assertThat(attributeMap.get("test_name"), is("Oxygen"));
    }

    @Test
    public void testAsAttributeMap_variableAndValue() {
        Material material = new Material();
        material.addVariable("x", Arrays.asList("0.5"));
        Map<String, String> attributeMap = Material.asAttributeMap(material, "test");

        assertThat(attributeMap.keySet(), hasSize(1));
        assertThat(attributeMap.get("test_variable_0"), is("x=0.5"));
    }

    @Test
    public void testAsAttributeMap_materialWithAdditionalInformation() {
        Material material = new Material();

        material.setName("name");
        material.setDoping("10%-Zn");
        material.setShape("shape");
        material.setFormula("Cu x Fe y");
        material.addVariable("x", Arrays.asList("1", "2", "3"));
        material.addVariable("y", Arrays.asList("1", "2", "3"));

        Map<String, String> attributeMap = Material.asAttributeMap(material);
        assertThat(attributeMap.keySet(), hasSize(6));
        assertThat(attributeMap.get("name"), is("name"));
        assertThat(attributeMap.get("shape"), is("shape"));
        assertThat(attributeMap.get("doping"), is("10%-Zn"));
        assertThat(attributeMap.get("formula"), is("Cu x Fe y"));
        assertThat(attributeMap.get("variable_0"), is("x=1,2,3"));
        assertThat(attributeMap.get("variable_1"), is("y=1,2,3"));
    }

    @Test
    public void testAsAttributeMapWithPrefix_materialWithAdditionalInformation() {
        Material material = new Material();

        material.setName("name");
        material.setDoping("doping");
        material.setShape("shape");
        material.setFormula("Cu x Fe y");
        material.addVariable("x", Arrays.asList("1", "2", "3"));
        material.addVariable("y", Arrays.asList("1", "2", "3"));

        Map<String, String> attributeMap = Material.asAttributeMap(material, "bao123");
        assertThat(attributeMap.keySet(), hasSize(6));
        assertThat(attributeMap.get("bao123_name"), is("name"));
        assertThat(attributeMap.get("bao123_shape"), is("shape"));
        assertThat(attributeMap.get("bao123_doping"), is("doping"));
        assertThat(attributeMap.get("bao123_formula"), is("Cu x Fe y"));
        assertThat(attributeMap.get("bao123_variable_0"), is("x=1,2,3"));
        assertThat(attributeMap.get("bao123_variable_1"), is("y=1,2,3"));

    }

}