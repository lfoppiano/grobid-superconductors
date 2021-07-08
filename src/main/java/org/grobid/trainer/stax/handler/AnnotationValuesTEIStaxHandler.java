package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.grobid.trainer.stax.StaxParserContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAG_TYPES;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_PATHS;

/**
 * This class extracts from XML a) the labelled stream b) the list of labelled entities
 * <p>
 * The constructor accept the top level tags (e.g. paragraph tags), as optional, and the list of tags that are considered
 * important to identify each entity.
 */
public class AnnotationValuesTEIStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValuesTEIStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();
    private StringBuilder accumulatedText = new StringBuilder();

    private List<String> annotationTypes = new ArrayList<>();
    private List<SuperconductorsStackTags> containerPaths = new ArrayList<>();

    // Record the offsets of each annotation tag
    private final List<Integer> offsetsAnnotationsTags = new ArrayList<>();
    private int lastOffset;
    private String currentId;

    private List<Pair<String, String>> labeledEntities = new ArrayList<>();
    private List<Pair<String, String>> labeledStream = new ArrayList<>();

    private final List<Pair<String, String>> identifiers = new ArrayList<>();
    private SuperconductorsStackTags currentPosition = new SuperconductorsStackTags();

    //When I find a relevant path, I store it here
    private SuperconductorsStackTags currentContainerPath = null;
    private boolean insideEntity = false;
    private String currentAnnotationType;

    /**
     * Process only from the body, trying to keep compatibility with the previous version
     */
    public AnnotationValuesTEIStaxHandler(List<String> annotationTypes) {
        this(Arrays.asList(SuperconductorsStackTags.from("/tei/text/body/p"),
            SuperconductorsStackTags.from("/tei/text/p")), annotationTypes);
    }

    /**
     * @param containerPaths  specifies the path where the data should be extracted, e.g. /tei/teiHeader/titleStmt/title
     * @param annotationTypes specifies the types of the <rs type="type"></rs> annotation to be extracted
     */
    public AnnotationValuesTEIStaxHandler(List<SuperconductorsStackTags> containerPaths, List<String> annotationTypes) {
        this.containerPaths = containerPaths;
        this.annotationTypes = annotationTypes;
    }

    public AnnotationValuesTEIStaxHandler() {
        this(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS, ANNOTATION_DEFAULT_TAG_TYPES);
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
        currentPosition.append(localName);

        if (currentContainerPath == null) {
            if (containerPaths.contains(currentPosition)) {
                currentContainerPath = SuperconductorsStackTags.from(currentPosition);
            }
        } else {
            String attributeValue = getAttributeValue(reader, "type");
            if (("rs".equals(localName) && annotationTypes.contains(attributeValue))
                || (annotationTypes.contains(localName))) {
                String text = getAccumulatedText();
                // I write the remaining data in the accumulated text as "other" label
                writeStreamData(text, getTag("other"));
//            lastOffset += accumulator.toString().length();
                accumulator.setLength(0);
                offsetsAnnotationsTags.add(lastOffset);

                this.currentId = getAttributeValue(reader, "id");
                this.insideEntity = true;
                this.currentAnnotationType = attributeValue;
                if (annotationTypes.contains(localName)) {
                    this.currentAnnotationType = localName;
                }
            }
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (currentPosition.equals(currentContainerPath)) {
            // we are closing the container
            currentContainerPath = null;
            String text = getAccumulatedText();
            writeStreamData(text, getTag("other"));
            accumulator.setLength(0);
            labeledStream.add(ImmutablePair.of("\n", null));
        } else if (currentContainerPath != null && insideEntity
            && ("rs".equals(localName) || this.currentAnnotationType.equals(localName))) {
            String text = getAccumulatedText();
            writeData(text, getTag(currentAnnotationType));
            writeId(currentId, getTag(currentAnnotationType));
            writeStreamData(text, getTag(currentAnnotationType));
            accumulator.setLength(0);
            this.insideEntity = false;
        }
//        } else if (containerPaths == null) {
//            String text = getAccumulatedText();
//            writeStreamData(text, getTag("other"));
//            accumulator.setLength(0);
//        }

        this.currentPosition.peek();
    }

    private String getTag(String localName) {
        return "<" + localName + ">";
    }

    @Override
    public void onCharacter(XMLStreamReader2 reader) {
        String text = reader.getText();
        if (currentContainerPath != null) {
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

    private void writeId(String id, String label) {
        identifiers.add(ImmutablePair.of(id, label));
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

    public List<Pair<String, String>> getIdentifiers() {
        return identifiers;
    }
}
