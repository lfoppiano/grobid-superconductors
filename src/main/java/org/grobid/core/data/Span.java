package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

import java.util.*;

/**
 * This is a generic implementation of a class representing a span, namely an entity
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Span {
    //We use the hashcode to generate an unique id
    private Integer id = null;

    private String text;
    private String formattedText;

    private String type;

    //offset in the text
    private int offsetStart;
    private int offsetEnd;

    //tokens index referred to the layout token list
    private int tokenStart;
    private int tokenEnd;

    //The source where this span was generated from, namely the model
    private String source;

    // Contains the references (id) to other spans objects
    private List<List<String>> links = new ArrayList<>();

    // Attribute map, used for adding lower-models information
    private Map<String, String> attributes = new HashMap<>();

    /** These are internal objects that should not be serialised to JSON **/
    //@JsonIgnore
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    @JsonIgnore
    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public Span() {
    }

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

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes) {
        this(text, type, offsetStart, offsetEnd, tokenStart, tokenEnd, boundingBoxes);
        this.source = source;
    }

    public Span(String text, String type, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes, String formattedText) {
        this(text, type, offsetStart, offsetEnd, tokenStart, tokenEnd, boundingBoxes);
        this.formattedText = formattedText;
    }

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd, List<BoundingBox> boundingBoxes, String formattedText) {
        this(text, type, source, offsetStart, offsetEnd, tokenStart, tokenEnd, boundingBoxes);
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
            .append(source, span.source)
            .isEquals();
    }

    public boolean equalsWithoutSource(Object o) {
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
            .append(source)
            .toHashCode();
    }

    public int getId() {
        if (id == null) {
            this.id = hashCode();
        }
        return id;
    }

    public String getFormattedText() {
        return formattedText;
    }

    public void setFormattedText(String formattedText) {
        this.formattedText = formattedText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Span.class.getSimpleName() + "[", "]")
            .add("id=" + getId())
            .add("text='" + text + "'")
            .add("type='" + type + "'")
            .add("offsetStart=" + offsetStart)
            .add("offsetEnd=" + offsetEnd)
            .add("tokenStart=" + tokenStart)
            .add("tokenEnd=" + tokenEnd)
            .add("source='" + source + "'")
            .add("boundingBoxes=" + boundingBoxes)
            .add("layoutTokens=" + layoutTokens)
            .toString();
    }

    public List<List<String>> getLinks() {
        return links;
    }

    public void setLinks(List<List<String>> links) {
        this.links = links;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String attributeName, String attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }
}
