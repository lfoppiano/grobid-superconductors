package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@JsonInclude(Include.NON_EMPTY)
public class PDFProcessingResponse {

    private long runtime;
    private List<ProcessedParagraph> paragraphs;

    public PDFProcessingResponse() {
        paragraphs = new ArrayList<>();
    }

    public PDFProcessingResponse(List<ProcessedParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<ProcessedParagraph> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<ProcessedParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public void addParagraph(ProcessedParagraph paragraphs) {
        this.paragraphs.add(paragraphs);
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }
}
