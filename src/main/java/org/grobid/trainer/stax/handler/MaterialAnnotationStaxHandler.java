package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;

/**
 * This class takes in account the top level tag and the list of the annotation tags.
 * <p>
 * labeled output is a list of list, having the first list for each element within the top level tag and the second one
 * for each tagged element within the top level task.
 */
public class MaterialAnnotationStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialAnnotationStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();

    private boolean insideTopLevel = false;

    private List<String> annotationsTags;
    private List<String> topLevelTags;

    private String currentTag;

    private List<Pair<String, String>> instanceLabeled = new ArrayList<>();
    private List<List<Pair<String, String>>> instancesLabeled = new ArrayList<>();

    public MaterialAnnotationStaxHandler(List<String> topLevelTags, List<String> annotationsTags) {
        this.topLevelTags = topLevelTags;
        this.annotationsTags = annotationsTags;
    }

    public MaterialAnnotationStaxHandler() {
        this.topLevelTags = TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;
        this.annotationsTags = ANNOTATION_DEFAULT_TAGS;
    }

    @Override
    public void onStartDocument(XMLStreamReader2 reader) {
    }

    @Override
    public void onEndDocument(XMLStreamReader2 reader) {
    }

    @Override
    public void onStartElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (topLevelTags.contains(localName)) {
            insideTopLevel = true;

        } else if (annotationsTags.contains(localName) && insideTopLevel) {
            String text = getAccumulatedText();
            // I write the remaining data in the accumulated text as "other" label
            writeData(text, getTag("other"));

            accumulator.setLength(0);

            this.currentTag = localName;
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (topLevelTags.contains(localName)) {
            String text = getAccumulatedText();
            writeData(text, getTag("other"));
            insideTopLevel = false;
            instanceLabeled.add(ImmutablePair.of("\n", null));
            instancesLabeled.add(instanceLabeled);
            instanceLabeled = new ArrayList<>();

        } else if (annotationsTags.contains(localName)) {
            String text = getAccumulatedText();
            writeData(text, getTag(localName));
        }
    }

    private String getTag(String localName) {
        return "<" + localName + ">";
    }

    @Override
    public void onCharacter(XMLStreamReader2 reader) {
        String text = reader.getText();
        if (insideTopLevel) {
            accumulator.append(text);
        }
    }

    private String getText(XMLStreamReader2 reader) {
        String text = reader.getText();
        text = trim(text);
        return text;
    }

    public String getAccumulatedText() {
        return trim(accumulator.toString());
    }

    private String getAttributeValue(XMLStreamReader reader, String attributeName) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (attributeName.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }

        return "";
    }

    private String extractTagContent(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException {
        XMLEvent event = reader.nextEvent();
        String data = event.asCharacters().getData();
        data = data != null ? data.trim() : "";
        writer.add(event);
        return data;
    }

    private void writeData(String text, String label) {

        List<String> tokens = null;
        try {
            tokens = DeepAnalyzer.getInstance().tokenize(text);
        } catch (Exception e) {
            throw new GrobidException("Fail to tokenize:, " + text, e);
        }
        boolean begin = true;
        for (String token : tokens) {
            String content = trim(token);
            if (isNotEmpty(content)) {
                if (begin && (!label.equals("<other>"))) {
                    instanceLabeled.add(ImmutablePair.of(content, "I-" + label));
                    begin = false;
                } else {
                    instanceLabeled.add(ImmutablePair.of(content, label));
                }
            }

            begin = false;
        }
        accumulator.setLength(0);
    }


    public List<List<Pair<String, String>>> getLabeled() {
        return instancesLabeled;
    }
}
