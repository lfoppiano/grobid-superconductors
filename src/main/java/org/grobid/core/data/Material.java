package org.grobid.core.data;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Material {

    private String name;
    private String structure;
    private String formula;
    private String doping;
    private Map<String, String> variables = new HashMap<>();

    private List<BoundingBox> boundingBoxes = new ArrayList<>();
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private OffsetPosition offsets = new OffsetPosition();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
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

    public Map<String, String> getVariables() {
        return variables;
    }

    public void addVariable(String variable, String value) {
        this.variables.putIfAbsent(variable, value);
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public OffsetPosition getOffsets() {
        return offsets;
    }

    public void setOffsets(OffsetPosition offsets) {
        this.offsets = offsets;
    }

    public void setOffsetStart(int startPos) {
        this.offsets.start = startPos;
    }

    public void setOffsetEnd(int endPos) {
        this.offsets.end = endPos;
    }
}
