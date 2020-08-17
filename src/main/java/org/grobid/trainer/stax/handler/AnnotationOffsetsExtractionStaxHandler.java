package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StaxParserContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * This class takes in account the top level tag and the list of the annotation tags.
 * <p>
 * For example the tag of the paragraph and the list of annotation to be extracted
 */
public class AnnotationOffsetsExtractionStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationOffsetsExtractionStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();

    private boolean insideParagraph = false;
    private int offset = 0;
    private Integer currentStartingPosition = -1;
    private Integer currentLength = 0;

    private List<String> annotationsTags;
    private List<String> topLevelTags;

    private StringBuilder continuum = new StringBuilder();

    private List<Triple<String, Integer, Integer>> data = new ArrayList<>();

    public AnnotationOffsetsExtractionStaxHandler(List<String> topLevelTags, List<String> annotationsTags) {
        this.topLevelTags = topLevelTags;
        this.annotationsTags = annotationsTags;
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
            insideParagraph = true;

        } else if (annotationsTags.contains(localName)) {
            this.currentStartingPosition = offset;
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (topLevelTags.contains(localName)) {
            insideParagraph = false;

        } else if (annotationsTags.contains(localName)) {
            currentLength = offset - this.currentStartingPosition;
            data.add(Triple.of(localName, currentStartingPosition, currentLength));

            currentLength = 0;
            currentStartingPosition = -1;
        }
    }

    @Override
    public void onCharacter(XMLStreamReader2 reader) {
        String text = reader.getText();
        accumulator.append(text);
        if (insideParagraph) {
            continuum.append(text);
            offset += text.length();
        }
    }

    private String getText(XMLStreamReader2 reader) {
        String text = reader.getText();
        text = trim(text);
        return text;
    }

    public String getContinuum() {
        return continuum.toString();
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

    public List<Triple<String, Integer, Integer>> getData() {
        return data;
    }

    public void setData(List<Triple<String, Integer, Integer>> data) {
        this.data = data;
    }
}
