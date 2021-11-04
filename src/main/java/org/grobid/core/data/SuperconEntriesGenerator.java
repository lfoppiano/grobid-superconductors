package org.grobid.core.data;

import org.grobid.core.data.document.Span;
import org.grobid.core.data.material.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class aggregates the supercon extracted data and output the list of records
 **/
public class SuperconEntriesGenerator {
    private String rawMaterial;
    private List<String> names = new ArrayList<>();
    private List<String> formulas = new ArrayList<>();
    private List<String> dopings = new ArrayList<>();
    private List<String> shapes = new ArrayList<>();
    private String classification;
    private List<String> fabrications = new ArrayList<>();
    private List<String> substrates = new ArrayList<>();
    private List<String> criticalTemperatures = new ArrayList<>();
    private List<String> appliedPressures = new ArrayList<>();
    private List<String> structures = new ArrayList<>();
    private List<String> variables = new ArrayList<>();

    private String section;
    private String subSection;
    private String sentence;

    public String getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(String rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public List<String> getFormulas() {
        return formulas;
    }

    public void setFormulas(List<String> formulas) {
        this.formulas = formulas;
    }

    public List<String> getDopings() {
        return dopings;
    }

    public void setDopings(List<String> dopings) {
        this.dopings = dopings;
    }

    public List<String> getShapes() {
        return shapes;
    }

    public void setShapes(List<String> shapes) {
        this.shapes = shapes;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classifications) {
        this.classification = classifications;
    }

    public List<String> getFabrications() {
        return fabrications;
    }

    public void setFabrications(List<String> fabrications) {
        this.fabrications = fabrications;
    }

    public List<String> getSubstrates() {
        return substrates;
    }

    public void setSubstrates(List<String> substrates) {
        this.substrates = substrates;
    }

    public List<String> getCriticalTemperatures() {
        return criticalTemperatures;
    }

    public void setCriticalTemperatures(List<String> criticalTemperatures) {
        this.criticalTemperatures = criticalTemperatures;
    }

    public List<String> getAppliedPressures() {
        return appliedPressures;
    }

    public void setAppliedPressures(List<String> appliedPressures) {
        this.appliedPressures = appliedPressures;
    }

    public List<String> getStructures() {
        return structures;
    }

    public void setStructures(List<String> structures) {
        this.structures = structures;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubSection() {
        return subSection;
    }

    public void setSubSection(String subSection) {
        this.subSection = subSection;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public void accumulateAttributes(Span inputSpan) {
        Map<String, Object> nestedAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, String> a : inputSpan.getAttributes().entrySet()) {
            List<String> splits = List.of(a.getKey().split("_"));
            String value = a.getValue();

            nestedAttributes = Material.stringToLinkedHashMap(splits, value, nestedAttributes);
        }
        // first level is material0, material1 -> duplicate the dbEntry
        // second level are the properties
        // third and further levels are structured information

        for (String materialKey : nestedAttributes.keySet()) {
            Map<String, Object> materialObject = (Map<String, Object>) nestedAttributes.get(materialKey);
            fillAttributes(materialObject);
                
        }
    }

    //This modifies the object
    public void fillAttributes(Map<String, Object> materialObject) {
        for (String propertyName : materialObject.keySet()) {
            switch (propertyName) {
                case "formula":
                    Map<String, Object> formula = (Map<String, Object>) materialObject.get(propertyName);
                    getFormulas().add((String) formula.get("rawValue"));
                    break;
                case "name":
                    getNames().add((String) materialObject.get(propertyName));
                    break;
                case "clazz":
                    setClassification((String) materialObject.get(propertyName));
                    break;
                case "shape":
                    getShapes().add((String) materialObject.get(propertyName));
                    break;
                case "doping":
                    getDopings().add((String) materialObject.get(propertyName));
                    break;
                case "fabrication":
                    getFabrications().add((String) materialObject.get(propertyName));
                    break;
                case "substrate":
                    getSubstrates().add((String) materialObject.get(propertyName));
                    break;
                case "variables":
                    getVariables().add((String) materialObject.get(propertyName));
                    break;
            }
        }
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }
}
