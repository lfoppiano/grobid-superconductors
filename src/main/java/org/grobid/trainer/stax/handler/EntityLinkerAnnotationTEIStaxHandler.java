package org.grobid.trainer.stax.handler;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.codehaus.stax2.XMLStreamReader2;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.data.document.LinkToken;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.stax.StaxParserContentHandler;
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.core.engines.label.TaggingLabels.OTHER_LABEL;

public class EntityLinkerAnnotationTEIStaxHandler implements StaxParserContentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinkerAnnotationTEIStaxHandler.class);
    private final String sourceLabel;
    private final String destinationLabel;

    private StringBuilder accumulator;

    /**
     * Indicate the type of link that has been identified already, if any:
     * LINK_SOURCE = a source have been already identified
     * LINK_DESTINATION = a destination has been already identified
     * null = I'm outside any linking
     */
    private static final String LINK_SOURCE = "source";
    private static final String LINK_DESTINATION = "destination";
    private String insideLink = null;
    private List<String> link_id = null;
    private String currentId = null;

    private Set<String> nonRelevantLinkIds = new HashSet<>();
    private List<LinkToken> labeled = new ArrayList<>();
    private List<SuperconductorsStackTags> containerPaths = new ArrayList<>();

    private SuperconductorsStackTags currentPosition = new SuperconductorsStackTags();

    //When I find a relevant path, I store it here
    private SuperconductorsStackTags currentContainerPath = null;
    private boolean insideEntity = false;
    private String currentAnnotationType = null;


    // Examples:
    // tcValue-material link -> (source: tcValue, destination: material)
    // pressure-tcValue link -> (source: pressure, destination tcValue)
    public EntityLinkerAnnotationTEIStaxHandler(List<SuperconductorsStackTags> containerPaths, String sourceLabel, String destinationLabel) {
        this.containerPaths = containerPaths;
        if (sourceLabel.startsWith("<")) {
            sourceLabel = sourceLabel.replace("<", "").replace(">", "");
        }
        this.sourceLabel = sourceLabel;

        if (destinationLabel.startsWith("<")) {
            destinationLabel = destinationLabel.replace("<", "").replace(">", "");
        }
        this.destinationLabel = destinationLabel;
        this.accumulator = new StringBuilder();
    }

    @Override
    public void onStartDocument(XMLStreamReader2 reader) {
    }

    @Override
    public void onEndDocument(XMLStreamReader2 reader) {
        writeData();

        Map<String, Long> summaryEntitiesAndIds = labeled.stream()
            .filter(l -> l.getEntityLabel().startsWith("I-") && isNotBlank(l.getId()))
            .flatMap(e -> Stream.of(e.getId().split(",")))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<String> collectedIdsToIgnore = summaryEntitiesAndIds.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
//            .flatMap(e -> Stream.of(e.getKey().split(",")))
            .collect(Collectors.toList());

        nonRelevantLinkIds.addAll(collectedIdsToIgnore);

        // link_id  -> null -> no link
        // link_id -> not_nll --> link with id
        String linkId = null;
        String previousLinkId = null;
        String currentEntityId = null;
        String currentEntityLinkLabel = null;
        int idx = -1;
        for (LinkToken linkToken : labeled) {
            idx += 1;
            String id = linkToken.getId();

            if (id != null && id.contains(",")) {
                List<String> ids = Arrays.asList(id.split(","));
                ids = (List<String>) CollectionUtils.removeAll(ids, CollectionUtils.intersection(ids, nonRelevantLinkIds));

                if (ids.size() == 1) {
                    id = ids.get(0);
                } else {
                    continue;
                }

            }

            String entityLabel = linkToken.getEntityLabel().startsWith("I-") ? linkToken.getEntityLabel().substring(3).replaceAll("[<>]", "") : linkToken.getEntityLabel().replaceAll("[<>]", "");
            if (nonRelevantLinkIds.contains(id) || isBlank(id) || (!entityLabel.equals(sourceLabel) && !entityLabel.equals(destinationLabel))) {
                if (currentEntityLinkLabel != null) {
                    currentEntityLinkLabel = null;
                }
                continue;

            } else {
                if (linkToken.getEntityId().equals(currentEntityId)) {
                    // same entity - repeat what decided before
                    if (currentEntityLinkLabel != null) {
                        labeled.set(idx, LinkToken.of(id, currentEntityId, linkToken.getText(), currentEntityLinkLabel, linkToken.getEntityLabel()));
                    }
                } else {
                    if (isNotBlank(previousLinkId)) {
                        nonRelevantLinkIds.add(previousLinkId);
                        if (id.equals(previousLinkId)) {
                            previousLinkId = null;
                            continue;
                        }
                        previousLinkId = null;
                    }
                    currentEntityId = linkToken.getEntityId();
                    // new entity - make new decision

                    if (id.equals(linkId)) {
                        currentEntityLinkLabel = "<link_left>";
                        previousLinkId = linkId;
                        linkId = null;
                    } else {
                        if (linkId == null) {
                            currentEntityLinkLabel = "<link_right>";
                            linkId = id;
                        } else {
                            nonRelevantLinkIds.add(id);
                            continue;
                        }
                    }
                    String linkLabelLocal = linkToken.getEntityLabel().startsWith("I-") ? "I-" + currentEntityLinkLabel : currentEntityLinkLabel;
                    labeled.set(idx, LinkToken.of(id, currentEntityId, linkToken.getText(), linkLabelLocal, linkToken.getEntityLabel()));
                }
            }
        }
    }

    @Override
    public void onStartElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();
        currentPosition.append(localName);

        if (currentContainerPath == null) {
            if (containerPaths.contains(currentPosition)) {
                currentContainerPath = SuperconductorsStackTags.from(currentPosition);

                //At every sentence I start from scratch
                insideLink = null;
                accumulator.setLength(0);
            }
        } else {
            String attributeValue = getAttributeValue(reader, "type");
            if ("rs".equals(localName)) {
                if (sourceLabel.equals(attributeValue)) {
                    this.currentAnnotationType = attributeValue;
                    //source (e.g. tcValue)

                    // I write the remaining data in the accumulated text as "other" label
                    writeData();
                    insideEntity = true;

                    String pointer = getAttributeValue(reader, "corresp");
                    if (isBlank(pointer)) {
                        return;
                    } else {
                        List<String> destinationIds = Arrays.asList(pointer.substring(1));
                        if (pointer.contains(",")) {
                            destinationIds = Arrays.stream(pointer.split(",")).map(d -> d.substring(1)).collect(Collectors.toList());
                        }
                        destinationIds = (List<String>) CollectionUtils.removeAll(destinationIds, nonRelevantLinkIds);

                        if (CollectionUtils.isNotEmpty(destinationIds)) {
                            currentId = String.join(",", destinationIds);
                        }
                    }

                } else if (destinationLabel.equals(attributeValue)) {
                    this.currentAnnotationType = attributeValue;
                    //destination (e.g. material)
                    writeData();

                    insideEntity = true;
                    currentId = getAttributeValue(reader, "id");
                } else {
                    // if the entity is not relevant but their id or pointer are matching the link_id I cancel the link

                    String id = getAttributeValue(reader, "id");
                    if (isNotBlank(id)) {
                        nonRelevantLinkIds.add(id);
                    }

                    String pointer = getAttributeValue(reader, "corresp");
                    if (isNotBlank(pointer)) {
                        List<String> pointers = Arrays.asList(pointer.substring(1));
                        if (pointer.contains(",")) {
                            pointers = Arrays.stream(pointer.split(",")).map(d -> d.substring(1)).collect(Collectors.toList());
                        }
                        if (CollectionUtils.isNotEmpty(pointers)) {
                            nonRelevantLinkIds.addAll(pointers);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEndElement(XMLStreamReader2 reader) {
        final String localName = reader.getName().getLocalPart();

        if (currentPosition.equals(currentContainerPath)) {
            currentContainerPath = null;
            if (insideLink != null || link_id != null) {
                // Cleanup the mess
                link_id = null;
                insideLink = null;
            }

            writeData();
            labeled.add(LinkToken.of("", "", "\n", ""));
        } else if (currentContainerPath != null && insideEntity
            && ("rs".equals(localName) && this.sourceLabel.equals(currentAnnotationType))) {

            // the destination (e.g. material) is coming before - link to the left
            writeData(currentId, null, this.currentAnnotationType);
            insideLink = null;
            //As this link has been closed, I add them in the exclusion list
//                nonRelevantLinkIds.add(currentId);
        } else if (currentContainerPath != null && insideEntity
            && ("rs".equals(localName) && this.destinationLabel.equals(this.currentAnnotationType))) {
            // destination: e.g. material

            // the tcValue is coming before - link to the left
            writeData(currentId, null, this.currentAnnotationType);
            link_id = null;
            insideLink = null;

        }
//        } else if (containerPaths == null) {
//            String text = getAccumulatedText();
//            writeStreamData(text, getTag("other"));
//            accumulator.setLength(0);
//        }

        this.currentPosition.peek();
        this.currentAnnotationType = null;
        this.currentId = "";
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
        writeData("", currentTag, null);
    }

    private void writeData(String id, String linkLabel, String entityLabel) {
        if (linkLabel == null) {
            linkLabel = "<other>";
        }

        if (entityLabel == null) {
            entityLabel = "<other>";
        }

        String entityId = RandomStringUtils.random(10, true, true);
        String text = accumulator.toString();
        List<String> tokens = null;
        try {
            tokens = DeepAnalyzer.getInstance().tokenize(text);
        } catch (Exception e) {
            throw new GrobidException("fail to tokenize: " + text, e);
        }
        boolean begin = true;
        for (String token : tokens) {
            token = token.trim();
            if (token.length() == 0)
                continue;

            if (begin) {
                String linkLabelLocal = !linkLabel.equals("<other>") ? "I-<" + linkLabel + ">" : linkLabel;
                String entityLabelLocal = !entityLabel.equals("<other>") ? "I-<" + entityLabel + ">" : entityLabel;
                labeled.add(LinkToken.of(id, entityId, token, linkLabelLocal, entityLabelLocal));
            } else {
                String linkLabelLocal = !linkLabel.equals("<other>") ? "<" + linkLabel + ">" : linkLabel;
                String entityLabelLocal = !entityLabel.equals("<other>") ? "<" + entityLabel + ">" : entityLabel;
                labeled.add(LinkToken.of(id, entityId, token, linkLabelLocal, entityLabelLocal));
            }

            begin = false;
        }
        accumulator.setLength(0);

    }

    public List<LinkToken> getLabeled() {
        return labeled;
    }


    /**
     * This method modifies the labeled list
     */
    private void revertPreviousLabelPointingRight() {
        for (int i = labeled.size() - 1; i >= 0; i--) {
            LinkToken labels = labeled.get(i);
            String linkLabel = labels.getLinkLabel();
            if (linkLabel.endsWith("<link_right>")) {
                labeled.set(i, LinkToken.of(labels.getId(), labels.getEntityId(), labels.getText(), OTHER_LABEL, labels.getEntityLabel()));
                if (linkLabel.startsWith("I-")) {
                    break;
                }
            }
        }
    }
}
