package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.trainer.stax.StackTags;
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

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * This class takes in account the top level tag and the list of the annotation tags.
 * <p>
 * For example the tag of the paragraph and the list of annotation to be extracted
 */
public class AnnotationOffsetsTEIExtractionStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationOffsetsTEIExtractionStaxHandler.class);

    private StringBuilder accumulator = new StringBuilder();

    // PlacesS to look for data:
    // - /tei/teiHeader/fileDesc/titleStmt/title
    // - /tei/teiHeader/profileDesc/abstract/p
    // - /tei/teiHeader/profileDesc/ab[type=keywords]
    // - /tei/text/body/p
    // - /tei/text/body/ab[type=figureCaption]
    // - /tei/text/body/ab[type=tableCaption]

    private int offset = 0;
    private Integer currentStartingPosition = -1;
    private Integer currentLength = 0;
    private boolean insideEntity = false;

    // With paragraph I meant any chunk of text
    private Integer paragraphNumbers = 0;

    private List<String> annotationTypes;
    private List<StackTags> containerPaths;

    //When I find a relevant path, I store it here
    private StackTags currentContainerPath = null;

    private String currentAnnotationType = null;

    //Store the current position in the tree
    private StackTags currentPosition = new StackTags();

    private StringBuilder continuum = new StringBuilder();

    private List<Triple<String, Integer, Integer>> data = new ArrayList<>();

    public AnnotationOffsetsTEIExtractionStaxHandler(List<String> annotationTypes) {
        this(Arrays.asList(StackTags.from("/tei/text/body/p"),
            StackTags.from("/tei/text/body/p")), annotationTypes);
    }

    /**
     * @param containerPaths  specifies the path where the data should be extracted, e.g. /tei/teiHeader/titleStmt/title
     * @param annotationTypes specifies the types of the <rs type="type"></rs> annotation to be extracted
     */
    public AnnotationOffsetsTEIExtractionStaxHandler(List<StackTags> containerPaths, List<String> annotationTypes) {
        this.annotationTypes = annotationTypes;
        this.containerPaths = containerPaths;
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
                currentContainerPath = StackTags.from(currentPosition);
            }
        } else {
            String attributeValue = getAttributeValue(reader, "type");
            if ("rs".equals(localName) && annotationTypes.contains(attributeValue)) {
                this.currentStartingPosition = offset;
                this.insideEntity = true;
                currentAnnotationType = attributeValue;
            }
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (currentPosition.equals(currentContainerPath)) {
            currentContainerPath = null;
            paragraphNumbers++;

        } else if (currentContainerPath != null && insideEntity && "rs".equals(localName)) {
            currentLength = offset - this.currentStartingPosition;
            data.add(Triple.of(currentAnnotationType, currentStartingPosition, currentLength));

            currentLength = 0;
            currentStartingPosition = -1;
            currentAnnotationType = null;
            this.insideEntity = false;
        }
        currentPosition.peek();
    }

    @Override
    public void onCharacter(XMLStreamReader2 reader) {
        String text = reader.getText();
        accumulator.append(text);
        if (currentContainerPath != null) {
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

    public Integer getParagraphNumbers() {
        return paragraphNumbers;
    }
}
