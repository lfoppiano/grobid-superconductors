package org.grobid.core.data.document;

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
import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.material.Formula;
import org.grobid.core.engines.TabularDataEngine;
import org.grobid.service.controller.AnnotationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class DocumentResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationController.class);

    private long runtime;

    private BiblioInfo biblio;

    private Map<Formula, List<String>> aggregatedMaterials = new HashMap<>();

    private List<TextPassage> passages = new ArrayList<>();

    private List<Page> pages = new ArrayList<>();

    public DocumentResponse() {
        passages = new ArrayList<>();
    }

    public DocumentResponse(List<TextPassage> passages) {
        this.passages = passages;
    }

    public List<TextPassage> getPassages() {
        return passages;
    }

    public void setPassages(List<TextPassage> passages) {
        this.passages = passages;
    }

    public void addParagraphs(List<TextPassage> passages) {
        passages.forEach(this::addPassage);
    }

    public void addPassage(TextPassage passage) {
        this.passages.add(passage);
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
        List<SuperconEntry> outputList = TabularDataEngine.computeTabularData(getPassages());
        List<List<String>> outputCSV = outputList.stream().map(SuperconEntry::toCsv).collect(Collectors.toList());

        StringBuilder out = new StringBuilder();
        try {
            final CSVPrinter printer = CSVFormat.DEFAULT
                .withHeader("Raw material", "Raw material ID", "Name", "Formula",
                    "Doping", "Shape", "Class", "Fabrication", "Substrate", "Variables",
                    "Unit-cell-type", "Unit-cell-type ID",
                    "Space Group", "Space Group ID",
                    "Crystal structure", "Crystal structure ID",
                    "Critical temperature", "Critical temperature ID",
                    "Measurement method", "Measurement method ID",
                    "Applied pressure", "Applied pressure ID",
                    "Link type", "Section", "Subsection", "Sentence",
                    "type", "path", "filename")
                .withQuote('"')
                .withQuoteMode(QuoteMode.ALL)
                .print(out);

            printer.printRecords(outputCSV);
        } catch (IOException e) {
            LOGGER.error("Something wrong when pushing out the CSV", e);
        }

        return out.toString();
    }

    /**
     * Converts all the entities to CSV
     **/
    public String toCsvAll() {
        List<SuperconEntry> outputList = TabularDataEngine.extractEntities(getPassages());
        List<List<String>> outputCSV = outputList.stream().map(SuperconEntry::toCsv).collect(Collectors.toList());

        StringBuilder out = new StringBuilder();
        try {
            final CSVPrinter printer = CSVFormat.DEFAULT
                .withHeader("Raw material", "Raw material ID", "Name", "Formula",
                    "Doping", "Shape", "Class", "Fabrication", "Substrate", "variables",
                    "Unit-cell-type", "Unit-cell-type ID",
                    "Space Group", "Space Group ID",
                    "Crystal structure", "Crystal structure ID",
                    "Critical temperature", "Critical temperature ID",
                    "Measurement method", "Measurement method ID",
                    "Applied pressure", "Applied pressure ID",
                    "Link type", "Section", "Subsection", "Sentence",
                    "type", "path", "filename")
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

    public void setAggregatedMaterials(Map<Formula, List<String>> aggregatedMaterials) {
        this.aggregatedMaterials = aggregatedMaterials;
    }

    public Map<Formula, List<String>> getAggregatedMaterials() {
        return aggregatedMaterials;
    }
}
