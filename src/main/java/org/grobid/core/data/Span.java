package org.grobid.core.data;

import org.grobid.core.layout.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class Span {

    private String text;
    private String type;
    private int offsetStart;
    private int offsetEnd;
    private int tokenStart;
    private int tokenEnd;
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd) {
        this.text = text;
        this.type = type;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
    }

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes) {
        this.text = text;
        this.type = type;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.boundingBoxes = boundingBoxes;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }
}
