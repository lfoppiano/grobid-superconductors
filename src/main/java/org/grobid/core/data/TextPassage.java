package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent a text passage, such as a sentence or a paragraph
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TextPassage {
    private String text;

    //Type can be "paragraph" or "sentence" for the time being...
    private String type;

    // Identify Header, Body, Annex (grobid segmentation model)
    private String section;

    // identify the subsection within the `section` models
    private String subSection;

    private List<Span> spans = new ArrayList<>();

    private List<Token> tokens = new ArrayList<>();

    private List<Relationship> relationships = new ArrayList<>();
    
    public static TextPassage of(TextPassage other) {
        TextPassage textPassage = new TextPassage();
        textPassage.setText(other.getText());
        textPassage.setType(other.getType());
        textPassage.setSection(other.getSection());
        textPassage.setSubSection(other.getSubSection());
        textPassage.setSpans(other.getSpans().stream().map(Span::new).collect(Collectors.toList()));
        textPassage.setTokens(new ArrayList<>(other.getTokens()));
        textPassage.setRelationships(new ArrayList<>(other.getRelationships()));
        
        return textPassage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Span> getSpans() {
        return spans;
    }

    public void setSpans(List<Span> spans) {
        this.spans = spans;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> token) {
        this.tokens = token;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
