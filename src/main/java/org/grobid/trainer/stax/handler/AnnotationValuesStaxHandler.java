package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;

/**
 * This class extract all annotations labelled with the list of labels provided in input
 */
public class AnnotationValuesStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValuesStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();

    private boolean insideParagraph = false;

    private List<String> annotationsTags;

    private String currentTag;

    private List<Pair<String, String>> labeled = new ArrayList<>();

    public AnnotationValuesStaxHandler(List<String> annotationsTags) {
        this.annotationsTags = annotationsTags;
    }

    public AnnotationValuesStaxHandler() {
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

        if (annotationsTags.contains(localName)) {
            accumulator.setLength(0);

            this.currentTag = localName;
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (annotationsTags.contains(localName)) {
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
        accumulator.append(text);
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
        labeled.add(ImmutablePair.of(text, label));
        accumulator.setLength(0);
    }


    public List<Pair<String, String>> getLabeled() {
        return labeled;
    }
}
