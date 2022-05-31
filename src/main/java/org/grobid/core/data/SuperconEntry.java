package org.grobid.core.data;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Represent a row in supercon
 **/
public class SuperconEntry implements Cloneable {

    //"id", "Raw material","rawMaterialId", "Name", "Formula", "Doping", "Shape", "Class", "Fabrication",
    // "Substrate","variables", "unitCellType", "unitCellTypeId", "spaceGroup", "spaceGroupId", "crystalStructure", "crystalStructureId", 
    // "Critical temperature", "criticalTemperatureId", "criticalTemperatureMeasurementMethod", "criticalTemperatureMeasurementMethodId", 
    // "Applied pressure", "Applied pressure id", "Material-tc link type",
    // "Section", "Subsection","Sentence", "type", "path","filename"

    private String id; 
    private String rawMaterial;
    private String materialId; 
    private String name;
    private String formula;
    private String doping;
    private String shape;
    private String materialClass;
    private String fabrication;
    private String substrate;
    private String variables;
    private String unitCellType; 
    private String unitCellTypeId; 
    private String spaceGroup;
    private String spaceGroupId;
    private String crystalStructure;
    private String crystalStructureId;
    private String criticalTemperature;
    private String criticalTemperatureId; 
    private String measurementMethod;
    private String measurementMethodId;
    private String appliedPressure;
    private String appliedPressureId;
    private String linkType;
    private String section;
    private String subsection;
    private String sentence;
    private String path;
    private String filename;
    private String hash;
    private String type;
    private Date timestamp;

    public SuperconEntry() {
    }

    public String getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(String rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getDoping() {
        return doping;
    }

    public void setDoping(String doping) {
        this.doping = doping;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getMaterialClass() {
        return materialClass;
    }

    public void setMaterialClass(String materialClass) {
        this.materialClass = materialClass;
    }

    public String getFabrication() {
        return fabrication;
    }

    public void setFabrication(String fabrication) {
        this.fabrication = fabrication;
    }

    public String getSubstrate() {
        return substrate;
    }

    public void setSubstrate(String substrate) {
        this.substrate = substrate;
    }

    public String getCriticalTemperature() {
        return criticalTemperature;
    }

    public void setCriticalTemperature(String criticalTemperature) {
        this.criticalTemperature = criticalTemperature;
    }

    public String getAppliedPressure() {
        return appliedPressure;
    }

    public void setAppliedPressure(String appliedPressure) {
        this.appliedPressure = appliedPressure;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubsection() {
        return subsection;
    }

    public void setSubsection(String subsection) {
        this.subsection = subsection;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuperconEntry that = (SuperconEntry) o;
        return Objects.equals(name, that.name) && Objects.equals(variables, that.variables)&& Objects.equals(formula, that.formula) && Objects.equals(doping, that.doping) && Objects.equals(shape, that.shape) && Objects.equals(materialClass, that.materialClass) && Objects.equals(fabrication, that.fabrication) && Objects.equals(substrate, that.substrate) && Objects.equals(criticalTemperature, that.criticalTemperature) && Objects.equals(appliedPressure, that.appliedPressure) && Objects.equals(linkType, that.linkType) && Objects.equals(section, that.section) && Objects.equals(subsection, that.subsection) && Objects.equals(sentence, that.sentence) && Objects.equals(hash, that.hash) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, formula, doping, variables, shape, materialClass, fabrication, substrate, criticalTemperature, appliedPressure, linkType, section, subsection, sentence, hash, timestamp);
    }

    @Override
    public SuperconEntry clone() throws CloneNotSupportedException {
        return (SuperconEntry) super.clone();
    }

    public List<String> toCsv() {
        List<String> outList = new ArrayList<>();

        outList.add(getRawMaterial());
        outList.add(getMaterialId());
        outList.add(getName());
        outList.add(getFormula());
        outList.add(getDoping());
        outList.add(getShape());
        outList.add(getMaterialClass());
        outList.add(getFabrication());
        outList.add(getSubstrate());
        outList.add(getVariables());
        outList.add(getUnitCellType());
        outList.add(getUnitCellTypeId());
        outList.add(getSpaceGroup());
        outList.add(getSpaceGroupId());
        outList.add(getCrystalStructure());
        outList.add(getCrystalStructureId());
        outList.add(getCriticalTemperature());
        outList.add(getCriticalTemperatureId());
        outList.add(getMeasurementMethod());
        outList.add(getMeasurementMethodId());
        outList.add(getAppliedPressure());
        outList.add(getAppliedPressureId());
        outList.add(getLinkType());
        outList.add(getSection());
        outList.add(getSubsection());
        outList.add(getSentence());
        outList.add(getType());
        outList.add(getPath());
        outList.add(getFilename());

        return outList;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getVariables() {
        return variables;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(String measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getCriticalTemperatureId() {
        return criticalTemperatureId;
    }

    public void setCriticalTemperatureId(String criticalTemperatureId) {
        this.criticalTemperatureId = criticalTemperatureId;
    }

    public String getMeasurementMethodId() {
        return measurementMethodId;
    }

    public void setMeasurementMethodId(String measurementMethodId) {
        this.measurementMethodId = measurementMethodId;
    }

    public String getAppliedPressureId() {
        return appliedPressureId;
    }

    public void setAppliedPressureId(String appliedPressureId) {
        this.appliedPressureId = appliedPressureId;
    }

    public String getSpaceGroup() {
        return spaceGroup;
    }

    public void setSpaceGroup(String spaceGroup) {
        this.spaceGroup = spaceGroup;
    }

    public String getCrystalStructure() {
        return crystalStructure;
    }

    public void setCrystalStructure(String crystalStructure) {
        this.crystalStructure = crystalStructure;
    }

    public String getUnitCellType() {
        return unitCellType;
    }

    public void setUnitCellType(String unitCellType) {
        this.unitCellType = unitCellType;
    }

    public String getUnitCellTypeId() {
        return unitCellTypeId;
    }

    public void setUnitCellTypeId(String unitCellTypeId) {
        this.unitCellTypeId = unitCellTypeId;
    }

    public String getSpaceGroupId() {
        return spaceGroupId;
    }

    public void setSpaceGroupId(String spaceGroupId) {
        this.spaceGroupId = spaceGroupId;
    }

    public String getCrystalStructureId() {
        return crystalStructureId;
    }

    public void setCrystalStructureId(String crystalStructureId) {
        this.crystalStructureId = crystalStructureId;
    }
}
