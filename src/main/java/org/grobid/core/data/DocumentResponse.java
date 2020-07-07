package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class DocumentResponse {

    private long runtime;
    private List<ProcessedParagraph> paragraphs;
    private List<Page> pages;

    public DocumentResponse() {
        paragraphs = new ArrayList<>();
    }

    public DocumentResponse(List<ProcessedParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<ProcessedParagraph> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<ProcessedParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public void addParagraphs(List<ProcessedParagraph> paragraphs) {
        paragraphs.forEach(this::addParagraph);
    }

    public void addParagraph(ProcessedParagraph paragraph) {
        //TODO: remove tokens will break supercuration....
//        paragraph.setTokens(null);
        this.paragraphs.add(paragraph);
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages() {
        return pages;
    }
}
