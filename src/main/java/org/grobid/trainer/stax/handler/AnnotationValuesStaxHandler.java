package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.exceptions.GrobidException;
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

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;

/**
 * This class extracts from XML a) the labelled stream b) the list of labelled entities
 *
 * The constructor accept the top level tags (e.g. paragraph tags), as optional, and the list of tags that are considered
 * important to identify each entity.
 */
public class AnnotationValuesStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValuesStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();
    private StringBuilder accumulatedText = new StringBuilder();

    private boolean insideParagraph = false;

    private List<String> annotationsTags;
    private List<String> topLevelTags;

    // Record the offsets of each annotation tag

    private final List<Integer> offsetsAnnotationsTags = new ArrayList<>();
    private int lastOffset;
    private String currentTag;

    private List<Pair<String, String>> labeledEntities = new ArrayList<>();
    private List<Pair<String, String>> labeledStream = new ArrayList<>();

    public AnnotationValuesStaxHandler(List<String> annotationsTags) {
        this(null, annotationsTags);
    }

    public AnnotationValuesStaxHandler(List<String> topLevelTags, List<String> annotationsTags) {
        this.topLevelTags = topLevelTags;
        this.annotationsTags = annotationsTags;
    }

    public AnnotationValuesStaxHandler() {
        this(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS, ANNOTATION_DEFAULT_TAGS);
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

        if (annotationsTags.contains(localName) && insideParagraph) {
            String text = getAccumulatedText();
            // I write the remaining data in the accumulated text as "other" label
            writeStreamData(text, getTag("other"));
//            lastOffset += accumulator.toString().length();
            accumulator.setLength(0);
            offsetsAnnotationsTags.add(lastOffset);

            this.currentTag = localName;
        } else if (topLevelTags == null || topLevelTags.contains(localName)) {
            insideParagraph = true;

        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (insideParagraph && annotationsTags.contains(localName)) {
            String text = getAccumulatedText();
            writeData(text, getTag(localName));
            writeStreamData(text, getTag(localName));
//            lastOffset += accumulator.toString().length();
            accumulator.setLength(0);
        } else if (topLevelTags != null && topLevelTags.contains(localName)) {
            String text = getAccumulatedText();
            writeStreamData(text, getTag("other"));
            accumulator.setLength(0);
            insideParagraph = false;
            labeledStream.add(ImmutablePair.of("\n", null));
        } else if (topLevelTags == null){
            String text = getAccumulatedText();
            writeStreamData(text, getTag("other"));
            accumulator.setLength(0);
        }
    }

    private String getTag(String localName) {
        return "<" + localName + ">";
    }

    @Override
    public void onCharacter(XMLStreamReader2 reader) {
        String text = reader.getText();
        if (insideParagraph) {
            accumulator.append(text);
            accumulatedText.append(text);
            lastOffset += length(text);
        }
    }

    private String getText(XMLStreamReader2 reader) {
        String text = reader.getText();
        text = trim(text);
        return text;
    }

    private String getAccumulatedText() {
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
        labeledEntities.add(ImmutablePair.of(text, label));
    }

    private void writeStreamData(String text, String label) {

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
                    labeledStream.add(ImmutablePair.of(content, "I-" + label));
                    begin = false;
                } else {
                    labeledStream.add(ImmutablePair.of(content, label));
                }
            }

            begin = false;
        }
    }


    public List<Pair<String, String>> getLabeledEntities() {
        return labeledEntities;
    }

    public List<Pair<String, String>> getLabeledStream() {
        return labeledStream;
    }

    public List<Integer> getOffsetsAnnotationsTags() {
        return offsetsAnnotationsTags;
    }

    public String getGlobalAccumulatedText() {
        return accumulatedText.toString();
    }
}
