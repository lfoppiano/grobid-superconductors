package org.grobid.core.data.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.DocumentResponse;
import org.grobid.core.data.document.Span;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;

import java.io.InputStream;
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
        assertThat(outputFormulas.get(0), is("Sr 1-x Na x Fe 2 As 2"));
    }

    @Test
    public void testExpandFormula3() throws Exception {
        String formula = "(Sr,K)Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, IsCollectionWithSize.hasSize(1));
        assertThat(expandFormulas.get(0), is("Sr 1-x K x Fe2As2"));
    }

    @Test
    public void testExpandFormula4() throws Exception {
        String formula = "(Sr , K ) Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, IsCollectionWithSize.hasSize(1));
        assertThat(expandFormulas.get(0), is("Sr 1-x K x Fe2As2"));
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
        assertThat(expandedFormulas.get(0), is("Sr 1-x La x Fe 2 O 7"));

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
        material.setFormula(new Formula("Fe1-xCuxO2"));
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
        material.setFormula(new Formula("Fe1-xCuyO2"));
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
        material.setFormula(new Formula("Li x (NH 3 ) y Fe 2 (Te z Se 1−z ) 2"));
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
        material.setFormula(new Formula("Li x (NH 3 ) 1-x Fe 2 (Te x Se 1−x ) 2"));
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
        material.setFormula(new Formula("La Fe 2"));
        Map<String, String> attributeMap = Material.asAttributeMap(material, "test");

        assertThat(attributeMap.keySet(), hasSize(1));
        assertThat(attributeMap.get("test_formula_rawValue"), is("La Fe 2"));
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
        assertThat(attributeMap.get("test_variables_x_0"), is("0.5"));
    }

    @Test
    public void testAsAttributeMap_materialWithAdditionalInformation() {
        Material material = new Material();

        material.setName("name");
        material.setDoping("10%-Zn");
        material.setShape("shape");
        material.setFormula(new Formula("Cu x Fe y"));
        material.addVariable("x", Arrays.asList("1", "2", "3"));
        material.addVariable("y", Arrays.asList("1", "2", "3"));
        material.setResolvedFormulas(Arrays.asList(new Formula("res1"), new Formula("res2")));

        Map<String, String> attributeMap = Material.asAttributeMap(material);
        assertThat(attributeMap.keySet(), hasSize(12));
        assertThat(attributeMap.get("name"), is("name"));
        assertThat(attributeMap.get("shape"), is("shape"));
        assertThat(attributeMap.get("doping"), is("10%-Zn"));
        assertThat(attributeMap.get("formula_rawValue"), is("Cu x Fe y"));
        assertThat(attributeMap.get("variables_x_0"), is("1"));
        assertThat(attributeMap.get("variables_x_1"), is("2"));
        assertThat(attributeMap.get("variables_x_2"), is("3"));
        assertThat(attributeMap.get("variables_y_0"), is("1"));
        assertThat(attributeMap.get("variables_y_1"), is("2"));
        assertThat(attributeMap.get("variables_y_2"), is("3"));
        assertThat(attributeMap.get("resolvedFormulas_0_rawValue"), is("res1"));
        assertThat(attributeMap.get("resolvedFormulas_1_rawValue"), is("res2"));
    }

    @Test
    public void testAsAttributeMapWithPrefix_materialWithAdditionalInformation() {
        Material material = new Material();

        material.setName("name");
        material.setDoping("doping");
        material.setShape("shape");
        material.setFormula(new Formula("Cu x Fe y"));
        material.addVariable("x", Arrays.asList("1", "2", "3"));
        material.addVariable("y", Arrays.asList("1", "2", "3"));

        Map<String, String> attributeMap = Material.asAttributeMap(material, "bao123");
        assertThat(attributeMap.keySet(), hasSize(10));
        assertThat(attributeMap.get("bao123_name"), is("name"));
        assertThat(attributeMap.get("bao123_shape"), is("shape"));
        assertThat(attributeMap.get("bao123_doping"), is("doping"));
        assertThat(attributeMap.get("bao123_formula_rawValue"), is("Cu x Fe y"));
        assertThat(attributeMap.get("bao123_variables_x_0"), is("1"));
        assertThat(attributeMap.get("bao123_variables_x_1"), is("2"));
        assertThat(attributeMap.get("bao123_variables_x_2"), is("3"));
        assertThat(attributeMap.get("bao123_variables_y_0"), is("1"));
        assertThat(attributeMap.get("bao123_variables_y_1"), is("2"));
        assertThat(attributeMap.get("bao123_variables_y_2"), is("3"));

    }

    @Test
    public void testLinkedHashMapToString_onlyStrings() throws Exception {
        Material material = new Material();
        material.setName("name");
        material.setDoping("doping");
        material.setShape("shape");
        
        ObjectMapper m = new ObjectMapper();
        Map<String, Object> mappedObject = m.convertValue(material, new TypeReference<Map<String, Object>>() {
        });

        Map<String, String> stringStringMap = Material.linkedHashMapToString(mappedObject);
        
        assertThat(stringStringMap.keySet(), hasSize(3));
        assertThat(stringStringMap.get("name"), is("name"));
        assertThat(stringStringMap.get("doping"), is("doping"));
        assertThat(stringStringMap.get("shape"), is("shape"));
    }

    @Test
    public void testLinkedHashMapToString_stringsMixedWithLists() throws Exception {
        Material material = new Material();
        material.setFormula(new Formula("formula"));
        material.setShape("shape");

        ObjectMapper m = new ObjectMapper();
        Map<String, Object> mappedObject = m.convertValue(material, new TypeReference<Map<String, Object>>() {
        });

        Map<String, String> stringStringMap = Material.linkedHashMapToString(mappedObject);

        assertThat(stringStringMap.keySet(), hasSize(2));
        assertThat(stringStringMap.get("shape"), is("shape"));
        assertThat(stringStringMap.get("formula_rawValue"), is("formula"));
    }

    @Test
    public void testLinkedHashMapToString_stringsMixedWithLists_2() throws Exception {
        Material material = new Material();
        Formula formula = new Formula("formula");
        formula.setFormulaComposition(Map.of("La", "2", "Fe", "4"));
        material.setFormula(formula);
        material.setShape("shape");
        material.setName("name");
        material.setResolvedFormulas(Arrays.asList(new Formula("12345"), new Formula("abcde")));

        ObjectMapper m = new ObjectMapper();
        Map<String, Object> mappedObject = m.convertValue(material, new TypeReference<Map<String, Object>>() {
        });

        Map<String, String> stringStringMap = Material.linkedHashMapToString(mappedObject);

        assertThat(stringStringMap.keySet(), hasSize(7));
        assertThat(stringStringMap.get("shape"), is("shape"));
        assertThat(stringStringMap.get("formula_rawValue"), is("formula"));
        assertThat(stringStringMap.get("formula_formulaComposition_La"), is("2"));
        assertThat(stringStringMap.get("formula_formulaComposition_Fe"), is("4"));
        assertThat(stringStringMap.get("resolvedFormulas_0_rawValue"), is("12345"));
        assertThat(stringStringMap.get("resolvedFormulas_1_rawValue"), is("abcde"));
    }

    @Test
    public void testStringToLinkedHashMap_singleString() throws Exception {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> input = new HashMap<>();
        input.put("material", "Mg B2");

        Map<String, Object> output = Material.stringToLinkedHashMap(Arrays.asList("material"), "Mg B2", result);
        
        assertThat(output.keySet(), hasSize(1));
        assertThat(output.get("material"), is("Mg B2"));
    }

    @Test
    public void testStringToLinkedHashMap_2LevelsObject() throws Exception {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> input = new HashMap<>();
        input.put("material_formula_rawValue", "Mg B2");

        Map<String, Object> output = Material.stringToLinkedHashMap(Arrays.asList("material", "formula", "rawValue"), "Mg B2", result);

        assertThat(output.keySet(), hasSize(1));
        assertThat(((LinkedHashMap<String, Object>)((LinkedHashMap<String, Object>) output.get("material")).get("formula")).get("rawValue"), is("Mg B2"));
    }

    @Test
    public void testStringToLinkedHashMap_0LevelsObject() throws Exception {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> input = new HashMap<>();

        Map<String, Object> output = Material.stringToLinkedHashMap(new ArrayList<>(), "Mg B2", result);

        assertThat(output.keySet(), hasSize(0));
    }

    @Test
    public void testStringToLinkedHashMap_sample() throws Exception {
        Map<String, Object> base = new HashMap<>();

        List<String> names = Arrays.asList("a", "b", "c");
        String value = "ciao";

        Map<String, Object> output = Material.stringToLinkedHashMap(names, value, base);

        assertThat(output.keySet(), hasSize(1));
        assertThat(((Map<String, Object>) output.get("a")).keySet(), hasSize(1));
        assertThat(((Map<?, ?>)((Map<?, ?>) output.get("a")).get("b")).get("c"), is("ciao"));

    }

    @Test
    public void testProcessAttributes_singleMaterial() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("sample_response.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        Optional<Span> first = documentResponse.getPassages().get(0).getSpans().stream()
            .filter(s -> s.getId().equals("450101894"))
            .findFirst();

        Span span = first.get();

        SuperconEntry entry = new SuperconEntry();
        List<SuperconEntry> superconEntries = Material.processAttributes(span, entry);

        assertThat(superconEntries, hasSize(1));
        assertThat(superconEntries.get(0).getMaterialClass(), is("Oxides, Cuprates"));
        assertThat(superconEntries.get(0).getFormula(), is("HgBa 2 Ca 2 Cu 3 O 9"));
    }

    @Test
    public void testProcessAttributes_twoMaterials() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("sample_response_2materials.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        Optional<Span> first = documentResponse.getPassages().get(0).getSpans().stream()
            .filter(s -> s.getId().equals("450101894"))
            .findFirst();

        Span span = first.get();

        SuperconEntry entry = new SuperconEntry();
        List<SuperconEntry> superconEntries = Material.processAttributes(span, entry);

        assertThat(superconEntries, hasSize(2));
        assertThat(superconEntries.get(0).getMaterialClass(), is("Oxides, Cuprates"));
        assertThat(superconEntries.get(0).getFormula(), is("HgBa 2 Ca 2 Cu 3 O 9"));

        assertThat(superconEntries.get(1).getMaterialClass(), is("Iron Based"));
        assertThat(superconEntries.get(1).getFormula(), is("La 1 Fe 2"));
    }

    @Test
    public void testFillDbEntryFromAttributes_singleMaterial() throws Exception {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("class", "Alloys");
        LinkedHashMap<String, Object> formula = new LinkedHashMap<>();
        formula.put("rawValue", "HgBa 2 Ca 2 Cu 3 O 9");
        attributes.put("formula", formula);
        attributes.put("fabrication", "welding");
        attributes.put("substrate", "ham");
        attributes.put("shape", "magic powder");
        attributes.put("doping", "dopamine");
        
        SuperconEntry dbEntry = new SuperconEntry();
        Material.fillDbEntryFromAttributes(attributes, dbEntry);

        assertThat(dbEntry.getMaterialClass(), is("Alloys"));
        assertThat(dbEntry.getFormula(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(dbEntry.getFabrication(), is("welding"));
        assertThat(dbEntry.getSubstrate(), is("ham"));
        assertThat(dbEntry.getShape(), is("magic powder"));
        assertThat(dbEntry.getDoping(), is("dopamine"));

    }
    
    @Test
    public void testFillDbEntryFromAttributes_singleValue_variables() throws Exception {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        attributes.put("variables", variables);
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("0", "0.5");
        variables.put("x", value);

        SuperconEntry dbEntry = new SuperconEntry();
        Material.fillDbEntryFromAttributes(attributes, dbEntry);

        assertThat(dbEntry.getVariables(), is("x=0.5"));
        
    }

    @Test
    public void testFillDbEntryFromAttributes_doubleValues_variables() throws Exception {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        attributes.put("variables", variables);
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("0", "0.5");
        value.put("1", "1.5");
        variables.put("x", value);

        SuperconEntry dbEntry = new SuperconEntry();
        Material.fillDbEntryFromAttributes(attributes, dbEntry);

        assertThat(dbEntry.getVariables(), is("x=0.5, 1.5"));

    }
}