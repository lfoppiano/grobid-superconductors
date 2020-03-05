package org.grobid.core.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.layout.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class Span {
    private String text;
    private String formattedText;
    private String type;
    private int offsetStart;
    private int offsetEnd;
    private int tokenStart;
    private int tokenEnd;

    //We use the hashcode to generate an unique id
    private int id;
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd) {
        this.text = text;
        this.type = type;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.id = hashCode();
    }

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes) {
        this(text, type, offsetStart, offsetEnd, tokenStart, tokenEnd);
        this.boundingBoxes = boundingBoxes;
    }

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes, String formattedText) {
        this(text, type, offsetStart, offsetEnd, tokenStart, tokenEnd, boundingBoxes);
        this.formattedText = formattedText;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Span span = (Span) o;

        return new EqualsBuilder()
            .append(offsetStart, span.offsetStart)
            .append(offsetEnd, span.offsetEnd)
            .append(tokenStart, span.tokenStart)
            .append(tokenEnd, span.tokenEnd)
            .append(text, span.text)
            .append(type, span.type)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(text)
            .append(type)
            .append(offsetStart)
            .append(offsetEnd)
            .append(tokenStart)
            .append(tokenEnd)
            .toHashCode();
    }

    public int getId() {
        return id;
    }

    public String getFormattedText() {
        return formattedText;
    }

    public void setFormattedText(String formattedText) {
        this.formattedText = formattedText;
    }
}
