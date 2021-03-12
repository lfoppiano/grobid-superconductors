package org.grobid.core.data;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Represent a row in supercon
 **/
public class SuperconEntry implements Cloneable {

    //"Raw material","Name","Formula","Doping","Shape","Class","Fabrication",
    // "Substrate","Critical temperature","Applied pressure","Link type",
    // "Section","Subsection","Sentence","path","filename"

    private String rawMaterial;
    private String name;
    private String formula;
    private String doping;
    private String shape;
    private String classification;
    private String fabrication;
    private String substrate;
    private String criticalTemperature;
    private String appliedPressure;
    private String linkType;
    private String section;
    private String subsection;
    private String sentence;
    private String path;
    private String filename;
    private String hash;
    private Date timestamp;

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

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
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
        return Objects.equals(name, that.name) && Objects.equals(formula, that.formula) && Objects.equals(doping, that.doping) && Objects.equals(shape, that.shape) && Objects.equals(classification, that.classification) && Objects.equals(fabrication, that.fabrication) && Objects.equals(substrate, that.substrate) && Objects.equals(criticalTemperature, that.criticalTemperature) && Objects.equals(appliedPressure, that.appliedPressure) && Objects.equals(linkType, that.linkType) && Objects.equals(section, that.section) && Objects.equals(subsection, that.subsection) && Objects.equals(sentence, that.sentence) && Objects.equals(hash, that.hash) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, formula, doping, shape, classification, fabrication, substrate, criticalTemperature, appliedPressure, linkType, section, subsection, sentence, hash, timestamp);
    }

    @Override
    public SuperconEntry clone() throws CloneNotSupportedException {
        return (SuperconEntry) super.clone();
    }

    public List<String> toCsv() {
        List<String> outList = new ArrayList<>();

        outList.add(getRawMaterial());
        outList.add(getName());
        outList.add(getFormula());
        outList.add(getDoping());
        outList.add(getShape());
        outList.add(getClassification());
        outList.add(getFabrication());
        outList.add(getSubstrate());
        outList.add(getCriticalTemperature());
        outList.add(getAppliedPressure());
        outList.add(getLinkType());
        outList.add(getSection());
        outList.add(getSubsection());
        outList.add(getSentence());

        return outList;
    }
}
