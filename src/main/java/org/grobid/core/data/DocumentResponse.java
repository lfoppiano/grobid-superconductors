package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.service.controller.AnnotationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_PRESSURE_LABEL;

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
        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();
        for (TextPassage paragraph : getParagraphs()) {
            List<Span> linkedSpans = paragraph.getSpans().stream()
                .filter(s -> s.getLinks().size() > 0)
                .collect(Collectors.toList());

            for (Span span : linkedSpans) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }

        // Materials
        List<Span> materials = spansById.entrySet().stream()
            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

//        List<Span> tcValues = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());
//
//        List<Span> pressures = spansById.entrySet().stream()
//            .filter(span -> span.getValue().getType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());

        List<List<String>> outputCSV = new ArrayList<>();
        for (Span m : materials) {
            String formula = "";
            String name = "";
            String cla = "";
            String doping = "";
            String shape = "";
            String fabrication = "";
            String substrate = "";

            for (Map.Entry<String, String> a : m.getAttributes().entrySet()) {
                String[] splits = a.getKey().split("_");
                String prefix = splits[0];
                String propertyName = splits[1];
                String value = a.getValue();

                if (propertyName.equals("formula")) {
                    formula = value;
                } else if (propertyName.equals("name")) {
                    name = value;
                } else if (propertyName.equals("clazz")) {
                    cla = value;
                } else if (propertyName.equals("shape")) {
                    shape = value;
                } else if (propertyName.equals("doping")) {
                    doping = value;
                } else if (propertyName.equals("fabrication")) {
                    fabrication = value;
                } else if (propertyName.equals("substrate")) {
                    substrate = value;
                }
            }

            Map<String, String> linkedToMaterial = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));

            for (Map.Entry<String, String> entry : linkedToMaterial.entrySet()) {
                Span tcValue = spansById.get(entry.getKey());
                List<Span> pressures = tcValue.getLinks().stream()
                    .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                    .map(l -> spansById.get(l.getTargetId()))
                    .collect(Collectors.toList());

                if (isNotEmpty(pressures)) {
                    for (Span pressure : pressures) {
                        outputCSV.add(Arrays.asList(m.getText(), name, formula, doping, shape, cla, fabrication, substrate,
                            tcValue.getText(), pressure.getText(), entry.getValue(), sectionsById.get(m.getId()).getLeft(),
                            sectionsById.get(m.getId()).getRight(), sentenceById.get(m.getId())));
                    }
                } else {
                    outputCSV.add(Arrays.asList(m.getText(), name, formula, doping, shape, cla, fabrication, substrate,
                        tcValue.getText(), "", entry.getValue(), sectionsById.get(m.getId()).getLeft(),
                        sectionsById.get(m.getId()).getRight(), sentenceById.get(m.getId())));
                }

            }
        }

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
