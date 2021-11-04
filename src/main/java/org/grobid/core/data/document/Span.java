package org.grobid.core.data.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

import java.util.*;

/**
 * This is a generic implementation of a class representing a span, namely an entity
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Span implements Cloneable {

    //We use the hashcode to generate a unique id
    private String id = null;

    private String text;
    private String formattedText;

    private String type;

    //offset in the text
    @JsonProperty("offset_start")
    private int offsetStart;

    @JsonProperty("offset_end")
    private int offsetEnd;
    
    //tokens index referred to the layout token list
    @JsonProperty("token_start")
    private int tokenStart;

    @JsonProperty("token_end")
    private int tokenEnd;
    private boolean linkable;

    //The source where this span was generated from, usually it indicates the model
    private String source;

    // Contains the references triple (destinationId, destinationType, linkingMethod)
    private List<Link> links = new ArrayList<>();

    // Attribute map, used for adding lower-models information
    private Map<String, String> attributes = new HashMap<>();
    
    /**
     * These are internal objects that should not be serialised to JSON
     **/
//    @JsonIgnore
//    private List<Material> originalMaterials = new ArrayList<>();
    
//    @JsonIgnore
//    private List<Measurement> originalMeasurements = new ArrayList<>();
    
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    @JsonIgnore
    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public Span() {
    }

    public Span(Span other) {
        this.id = other.getId();
        this.text = other.getText();
        this.formattedText = other.getFormattedText();
        this.type = other.getType();
        this.offsetStart = other.getOffsetStart();
        this.offsetEnd = other.getOffsetEnd();

        this.tokenStart = other.getTokenStart();
        this.tokenEnd = other.getTokenEnd();
        this.linkable = other.isLinkable();
        this.layoutTokens = new ArrayList<>(other.getLayoutTokens());
        this.source = other.getSource();
        this.links = new ArrayList<>(other.getLinks());

        this.attributes = new HashMap<>(other.getAttributes());
        this.boundingBoxes = new ArrayList<>(other.getBoundingBoxes());
        this.layoutTokens = new ArrayList<>(other.getLayoutTokens());

    }

    public Span(String id, String text, String type) {
        this(text, type);
        this.id = id;
    }

    public Span(String text, String type) {
        this.text = text;
        this.type = type;
    }

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd) {
        this(text, type);
        this.source = source;
        this.offsetStart = offsetStart;
        this.offsetEnd = offsetEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.id = "" + hashCode();
    }

    public Span(String id, String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd) {
        this(text, type, source, offsetStart, offsetEnd, tokenStart, tokenEnd);
        this.id = id;
    }

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd,
                List<LayoutToken> layoutTokens) {
        this(text, type, source, offsetStart, offsetEnd, tokenStart, tokenEnd);
        this.layoutTokens = layoutTokens;
    }

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd,
                List<LayoutToken> layoutTokens, List<BoundingBox> boundingBoxes) {
        this(text, type, source, offsetStart, offsetEnd, tokenStart, tokenEnd, layoutTokens);
        this.boundingBoxes = boundingBoxes;
    }

    public Span(String text, String type, String source, int offsetStart, int offsetEnd, int tokenStart, int tokenEnd,
                List<LayoutToken> layoutTokens, List<BoundingBox> boundingBoxes, String formattedText) {
        this(text, type, source, offsetStart, offsetEnd, tokenStart, tokenEnd, layoutTokens, boundingBoxes);
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

    public String getId() {
        if (id == null) {
            this.id = "" + hashCode();
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

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String attributeName, String attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }

    public void addLink(Link linkedSpans) {
        this.links.add(linkedSpans);
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public boolean isLinkable() {
        return linkable;
    }

    public void setLinkable(boolean linkable) {
        this.linkable = linkable;
    }

    public void addLinks(List<Link> links) {
        this.links.addAll(links);
    }

    @Override
    public Span clone() {
        try {
            Span clone = (Span) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

//    public List<Material> getOriginalMaterials() {
//        return originalMaterials;
//    }
//
//    public void addOriginalMaterials(Material originalMaterial) {
//        this.originalMaterials.add(originalMaterial);
//    }
//    
//
//    public void setOriginalMaterials(List<Material> originalMaterials) {
//        this.originalMaterials = originalMaterials;
//    }
//
//    public List<Measurement> getOriginalMeasurements() {
//        return originalMeasurements;
//    }
//
//    public void setOriginalMeasurements(List<Measurement> originalMeasurements) {
//        this.originalMeasurements = originalMeasurements;
//    }
//
//    public void addOriginalMeasurement(Measurement originalMeasurement) {
//        this.originalMeasurements.add(originalMeasurement);
//    }
}
