package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a processed paragraph
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProcessedParagraph {
    private String text;

    private List<Span> spans = new ArrayList<>();

    private List<Token> tokens = new ArrayList<>();

    private List<Relationship> relationships = new ArrayList();

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
}
