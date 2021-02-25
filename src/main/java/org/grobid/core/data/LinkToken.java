package org.grobid.core.data;

import java.util.StringJoiner;

import static org.grobid.core.engines.label.TaggingLabels.OTHER_LABEL;

public class LinkToken {

    private final String id;

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
