package org.grobid.core.data;

import java.util.StringJoiner;

import static org.grobid.core.engines.label.TaggingLabels.OTHER_LABEL;

/**
 * Object models the chain of linkable entities
 */
public class LinkToken {

    //The id of the entity (from the corresp or xml:id attribute)
    private final String id;
    //The generated id that is used to identify tokens from the same entity
    private final String entityId;
    private final String text;
    private final String linkLabel;
    private final String entityLabel;

    public LinkToken(String id, String entityId, String text, String linkLabel, String entityLabel) {
        this.text = text;
        this.id = id;
        this.linkLabel = linkLabel;
        this.entityLabel = entityLabel;
        this.entityId = entityId;
    }

    public static LinkToken of(String id, String entityId, String text, String linkLabel, String entityLabel) {
        return new LinkToken(id, entityId, text, linkLabel, entityLabel);
    }

    public static LinkToken of(String id, String entityId, String text, String entityLabel) {
        return LinkToken.of(id, entityId, text, OTHER_LABEL, entityLabel);
    }

    public static LinkToken of(String id, String entityId, String text) {
        return LinkToken.of(id, entityId, text, OTHER_LABEL, OTHER_LABEL);
    }

    public String getText() {
        return text;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public String getId() {
        return id;
    }

    public String getLinkLabel() {
        return linkLabel;
    }


    public String getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LinkToken.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("entityId='" + entityId + "'")
            .add("text='" + text + "'")
            .add("linkLabel='" + linkLabel + "'")
            .add("entityLabel='" + entityLabel + "'")
            .toString();
    }
}
