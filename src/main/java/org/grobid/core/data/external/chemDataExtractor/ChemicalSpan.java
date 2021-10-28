package org.grobid.core.data.external.chemDataExtractor;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ChemicalSpan {
    private String text;
    private String label;
    private String type;
    private int start;
    private int end;

    public ChemicalSpan() {
    }
    
    public ChemicalSpan(int start, int end, String label, String name) {
        this(start, end, label);
        this.text = name;
    }

    public ChemicalSpan(int start, int end, String label) {
        this.label = label;
        this.start = start;
        this.end = end;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
