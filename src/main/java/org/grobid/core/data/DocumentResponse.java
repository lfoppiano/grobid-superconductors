package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.grobid.core.engines.AggregatedProcessing;
import org.grobid.service.controller.AnnotationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class DocumentResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

    private long runtime;

    private List<TextPassage> paragraphs;

    private List<Page> pages;

    public DocumentResponse() {
        paragraphs = new ArrayList<>();
    }

    public DocumentResponse(List<TextPassage> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<TextPassage> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<TextPassage> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public void addParagraphs(List<TextPassage> paragraphs) {
        paragraphs.forEach(this::addParagraph);
    }

    public void addParagraph(TextPassage paragraph) {
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

    public String toCsv() {
        List<SuperconEntry> outputList = AggregatedProcessing.computeTabularData(getParagraphs());
        List<List<String>> outputCSV = outputList.stream().map(SuperconEntry::toCsv).collect(Collectors.toList());

        StringBuilder out = new StringBuilder();
        try {
            final CSVPrinter printer = CSVFormat.DEFAULT
                .withHeader("Raw material", "Name", "Formula", "Doping", "Shape", "Class", "Fabrication", "Substrate", "Critical temperature", "Applied pressure", "Link type", "Section", "Subsection", "Sentence")
                .withQuote('"')
                .withQuoteMode(QuoteMode.ALL)
                .print(out);

            printer.printRecords(outputCSV);
        } catch (IOException e) {
            LOGGER.error("Something wrong when pushing out the CSV", e);
        }

        return out.toString();
    }
}
