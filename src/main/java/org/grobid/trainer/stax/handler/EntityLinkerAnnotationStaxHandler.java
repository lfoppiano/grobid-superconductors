package org.grobid.trainer.stax.handler;


import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.QuantityAnalyzer;
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

public class EntityLinkerAnnotationStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinkerAnnotationStaxHandler.class);
    private final String sourceLabel;
    private final String destinationLabel;
    private final String topTag;

    private StringBuilder accumulator;

    private boolean insideLink = false;
    private String link_id = null;

    private List<Triple<String, String, String>> labeled = new ArrayList<>();

    // Examples:
    // tcValue-material link -> (source: tcValue, destination: material)
    // pressure-tcValue link -> (source: pressure, destination tcValue)

    public EntityLinkerAnnotationStaxHandler(String topTag, String sourceLabel, String destinationLabel) {
        this.topTag = topTag;
        this.sourceLabel = sourceLabel;
        this.destinationLabel = destinationLabel;

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

        if (topTag.equals(localName)) {
            link_id = null;
            //At every paragraph I start from scratch

        } else if (sourceLabel.equals(localName)) {
            //e.g. tcValue

            writeData();

            String pointer = getAttributeValue(reader, "ptr");
            if (isBlank(pointer)) {
                return;
            } else {
                String destinationId = pointer.substring(1);
                if (pointer.contains(",")) {
                    destinationId = pointer.split(",")[0].substring(1);
                }
                if (link_id == null) {
                    link_id = destinationId;
//                    insideLink = true;
                } else {
                    // if I have a link_id not null and different from the current id, it means that this
                    // entity should be ignored
                    if (!link_id.equals(destinationId)) {
                        revertPreviousLabelPointingRight();
                        link_id = destinationId;
                        insideLink = false;
                    }
                }
            }

        } else if (destinationLabel.equals(localName)) {
            //e.g. material
            writeData();

            String id = getAttributeValue(reader, "id");
            if (isNotEmpty(id)) {
                if (link_id == null) {
                    link_id = id;
                } else {
                    if (!id.equals(link_id)) {
//                        link_id = id;

                        LOGGER.warn("Ignoring id = " + id);

//                        throw new GrobidException("Something wrong, link_id is indicating a different id, this means that " +
//                            "a) there is a different id in the destination, " +
//                            "b) there is a mistake" +
//                            "" +
//                            "In either cases I we both go down. ");
                    }
                }
            }
        }
    }

    /**
     * This method modifies the labeled list
     */
    private void revertPreviousLabelPointingRight() {
        for (int i = labeled.size() - 1; i >= 0; i--) {
            Triple<String, String, String> labels = labeled.get(i);
            if (labels.getMiddle().endsWith("<link_right>")) {
                labeled.set(i, Triple.of(labels.getLeft(), "<other>", labels.getRight()));
                if (labels.getMiddle().startsWith("I-")) {
                    break;
                }
            }
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();
        if (topTag.equals(localName)) {
            if (link_id != null) {
                // Cleanup the mess
                revertPreviousLabelPointingRight();
            }

            writeData();
            labeled.add(new ImmutableTriple<>("\n", "", ""));
            link_id = null;
        } else if (sourceLabel.equals(localName)) {
            if (link_id == null) {
                //there is not ptr, so I should exclude this
            } else {
                if (insideLink) {
                    // the material is coming before - link to the left
                    writeData("link_left", localName);
                    insideLink = false;
                    link_id = null;
                } else {
                    // this is a promise that I will obtain the other end of the link - if Not I will wipe it out
                    writeData("link_right_" + link_id, localName);
                    insideLink = true;
                }
            }

        } else if (destinationLabel.equals(localName)) {

            // material

            if (link_id == null) {
                //ignore
            } else {
                if (insideLink) {
                    // the tcValue is coming before - link to the left
                    writeData("link_left", localName);
                    link_id = null;
                    insideLink = false;
                } else {
                    // this is a promise that I will obtain the other end of the link - if Not I will wipe it out
                    writeData("link_right_" + link_id, localName);
                    insideLink = true;
                }
            }

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

    private void writeData() {
        writeData(null);
    }

    private void writeData(String currentTag) {
        writeData(currentTag, null);
    }

    private void writeData(String currentTag, String secondLayerTag) {
        if (currentTag == null)
            currentTag = "<other>";
        else if (!currentTag.startsWith("<")) {
            currentTag = "<" + currentTag + ">";
        }

        if (secondLayerTag == null) {
            secondLayerTag = "<other>";
        } else if (!secondLayerTag.startsWith("<")) {
            secondLayerTag = "<" + secondLayerTag + ">";
        }

        String text = accumulator.toString();
        List<String> tokens = null;
        try {
            tokens = QuantityAnalyzer.getInstance().tokenize(text);
        } catch (Exception e) {
            throw new GrobidException("fail to tokenize: " + text, e);
        }
        boolean begin = true;
        for (String token : tokens) {
            token = token.trim();
            if (token.length() == 0)
                continue;

            if (begin && (!currentTag.equals("<other>"))) {
                labeled.add(new ImmutableTriple<>(token, "I-" + currentTag, "I-" + secondLayerTag));
            } else {
                labeled.add(new ImmutableTriple<>(token, currentTag, secondLayerTag));
            }

            begin = false;
        }
        accumulator.setLength(0);

    }

    public List<Triple<String, String, String>> getLabeled() {
        return labeled;
    }
}
