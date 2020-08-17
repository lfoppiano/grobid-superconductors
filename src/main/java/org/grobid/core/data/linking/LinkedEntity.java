package org.grobid.core.data.linking;

public class LinkedEntity {
    private String id;
    private LinkedEntity otherLinkedEntity;
    private String rawName;

    public LinkedEntity() {
    }

    public LinkedEntity(String id) {
        this.id = id;
    }

    public LinkedEntity(String id, String otherId) {
        this(id);
        LinkedEntity linkDestination = new LinkedEntity(otherId);
        this.setOtherLinkedEntity(linkDestination);
        linkDestination.setOtherLinkedEntity(this);
    }

    public LinkedEntity(String id, LinkedEntity otherEntity) {
        this(id);
        this.setOtherLinkedEntity(otherEntity);
        otherEntity.setOtherLinkedEntity(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOtherEntityId() {
        return otherLinkedEntity.getId();
    }

    public LinkedEntity getOtherLinkedEntity() {
        return otherLinkedEntity;
    }

    public void setOtherLinkedEntity(LinkedEntity otherLinkedEntity) {
        this.otherLinkedEntity = otherLinkedEntity;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getRawName() {
        return rawName;
    }
}
