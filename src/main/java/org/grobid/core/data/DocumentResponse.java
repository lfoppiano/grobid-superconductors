package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.grobid.core.engines.ModuleEngine;
import org.grobid.service.controller.AnnotationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class DocumentResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

    private long runtime;

    private BiblioInfo biblio;

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
        List<SuperconEntry> outputList = ModuleEngine.computeTabularData(getParagraphs());
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

    /** Converts all the entities to CSV **/
    public String toCsvAll() {
        List<SuperconEntry> outputList = ModuleEngine.extractEntities(getParagraphs());
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

    public static DocumentResponse fromJson(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, new TypeReference<DocumentResponse>() {
            });
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }

    public BiblioInfo getBiblio() {
        return biblio;
    }

    public void setBiblio(BiblioInfo biblio) {
        this.biblio = biblio;
    }
}
