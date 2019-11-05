package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

public class ProcessedParagraph {
    private String text;
    private List<Span> spans = new ArrayList<>();
    private List<Token> token = new ArrayList<>();


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
        return token;
    }

    public void setTokens(List<Token> token) {
        this.token = token;
    }
}
