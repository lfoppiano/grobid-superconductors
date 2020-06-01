package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.data.QuantifiedObject;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.MeasureLabeled;
import org.grobid.trainer.stax.StaxParserContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Generic XML parser for linked annotations
 */
public class LinkedAnnotationStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedAnnotationStaxHandler.class);

    private StringBuilder accumulator;
    private final String xmlTagLinkFrom;
    private final String xmlTagLinkTo;

    private List<MeasureLabeled> entitiesWithoutId = new ArrayList<>();

    private boolean insideLinkFrom = false;
    private boolean insideLinkTo = false;
    private boolean checkForward = false;

    private List<Pair<String, String>> labeled = new ArrayList<>();

    private Map<String, Pair<QuantifiedObject, MeasureLabeled>> data;

    private QuantifiedObject currentLinkTo = null;

    private MeasureLabeled currentLinkFromLabel = null;

    public LinkedAnnotationStaxHandler(String xmlTagLinkFrom, String xmlTagLinkTo) {
        this.xmlTagLinkFrom = xmlTagLinkFrom;
        this.xmlTagLinkTo = xmlTagLinkTo;

        this.data = new HashMap<>();
        this.accumulator = new StringBuilder();
    }

    @Override
    public void onStartDocument(XMLStreamReader2 reader) {
    }

    @Override
    public void onEndDocument(XMLStreamReader2 reader) {
        writeData();
    }

    @Override
    public void onStartElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (xmlTagLinkFrom.equals(localName)) {
            writeData();

            String pointer = getAttributeValue(reader, "ptr");
            currentLinkFromLabel = new MeasureLabeled();
            //removing #
            if (isNotBlank(pointer)) {
                String id = pointer.substring(1);
                currentLinkFromLabel.setId(id);
            }

            if (checkForward) {
                if (currentLinkFromLabel.getId() != null) {
                    if (currentLinkTo != null
                        && !StringUtils.equals(currentLinkFromLabel.getId(), currentLinkTo.getId())) {
                        throw new GrobidException("The training data is inconsistent and should be corrected. " +
                            "\n\tfrom: " + currentLinkFromLabel.getId() +
                            "\n\tto: " + currentLinkTo.getId());
                    }
                }
                checkForward = false;
            }

            insideLinkFrom = true;

        } else if (xmlTagLinkTo.equals(localName)) {
            if (checkForward) {
                throw new GrobidException("The training data is inconsistent and should be corrected. " +
                    "\n\tquantifiedObjectId: " + currentLinkTo.getId());
            }
            String id = getAttributeValue(reader, "id");
            writeData();

            currentLinkTo = new QuantifiedObject();
            currentLinkTo.setId(id);

            insideLinkTo = true;
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (xmlTagLinkFrom.equals(localName)) {
            currentLinkFromLabel.setRawName(accumulator.toString());
            if (currentLinkTo != null) {
                if (currentLinkFromLabel != null
                    && StringUtils.equals(currentLinkFromLabel.getId(), currentLinkTo.getId())) {
                    // we find a match, the object was written already, so let's write the measurement
                    // and set them both to null

                    currentLinkFromLabel = null;
                    currentLinkTo = null;
                } else {
                    throw new GrobidException("The training data is inconsistent and should be corrected. " +
                        "\n\tfrom: " + currentLinkFromLabel.getId() +
                        "\n\tto: " + currentLinkTo.getId());
                }
            }

            if (currentLinkFromLabel != null && currentLinkFromLabel.getId() == null) {
                currentLinkFromLabel = null;
            }
            writeData(xmlTagLinkFrom);
            insideLinkFrom = false;

        } else if (xmlTagLinkTo.equals(localName)) {

            currentLinkTo.setRawName(accumulator.toString());

            if (currentLinkFromLabel != null) {
                if (StringUtils.equals(currentLinkTo.getId(), currentLinkFromLabel.getId())) {
                    writeData(xmlTagLinkTo + "_left");

                    // Reset
                    currentLinkFromLabel = null;
                    currentLinkTo = null;
                } else {
                    // The quantified object doesn't have any references before so I cannot write it as such.
                    currentLinkFromLabel = null;
                    checkForward = true;
                    writeData(xmlTagLinkTo + "_right");
                }
            } else {
                writeData(xmlTagLinkTo + "_right");
            }

            insideLinkTo = false;
        } else if ("p".equals(localName)) {
            writeData();
            labeled.add(new ImmutablePair<>("\n", ""));
        }
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

    public Map<String, Pair<QuantifiedObject, MeasureLabeled>> getData() {
        return data;
    }

    private void writeData() {
        writeData(null);
    }

    private void writeData(String currentTag) {
        if (currentTag == null)
            currentTag = "<other>";
        else if (!currentTag.startsWith("<")) {
            currentTag = "<" + currentTag + ">";
        }

        String text = accumulator.toString();
        List<String> tokens = null;
        try {
            tokens = DeepAnalyzer.getInstance().tokenize(text);
        } catch (Exception e) {
            throw new GrobidException("Fail to tokenize: " + text, e);
        }
        boolean begin = true;
        for (String token : tokens) {
            token = token.trim();
            if (token.length() == 0)
                continue;

            if (begin && (!currentTag.equals("<other>"))) {
                labeled.add(new ImmutablePair<>(token, "I-" + currentTag));
            } else {
                labeled.add(new ImmutablePair<>(token, currentTag));
            }

            begin = false;
        }
        accumulator.setLength(0);

    }

    public List<Pair<String, String>> getLabeled() {
        return labeled;
    }
}
