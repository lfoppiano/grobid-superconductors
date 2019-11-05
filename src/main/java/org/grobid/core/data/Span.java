package org.grobid.core.data;

public class Span {

    private String label;
    private String type;
    private int offsetStart;
    private int offsetEnd;
    private int tokenStart;
    private int tokenEnd;

    public Span(String label, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd) {
        this.label = label;
        this.type = type;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOffsetStart() {
        return offsetStart;
    }

    public void setOffsetStart(int offsetStart) {
        this.offsetStart = offsetStart;
    }

    public int getOffsetEnd() {
        return offsetEnd;
    }

    public void setOffsetEnd(int offsetEnd) {
        this.offsetEnd = offsetEnd;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public void setTokenStart(int tokenStart) {
        this.tokenStart = tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public void setTokenEnd(int tokenEnd) {
        this.tokenEnd = tokenEnd;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
