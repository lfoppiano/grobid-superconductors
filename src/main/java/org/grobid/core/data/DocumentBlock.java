package org.grobid.core.data;

import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a block of a document
 */
public class DocumentBlock {

    private String section;
    private String subSection;
    private List<Span> spans = new ArrayList<>();
    private List<LayoutToken> layoutTokens = new ArrayList<>();

    public DocumentBlock(String section, String subSection, List<LayoutToken> tokens) {

        this.section = section;
        this.subSection = subSection;
        this.layoutTokens = new ArrayList<>(tokens);
    }

    public DocumentBlock(DocumentBlock documentBlock) {
        this(documentBlock.getSection(), documentBlock.getSubSection(), documentBlock.getLayoutTokens());
    }

    public DocumentBlock(List<LayoutToken> layoutTokens, List<Span> spanList) {
        this.layoutTokens = new ArrayList<>(layoutTokens);
        this.spans = new ArrayList<>(spanList);
    }


    public String getSubSection() {
        return subSection;
    }

    public void setSubSection(String subSection) {
        this.subSection = subSection;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void setSpans(List<Span> spans) {
        this.spans = spans;
    }

    public List<Span> getSpans() {
        return spans;
    }
}
