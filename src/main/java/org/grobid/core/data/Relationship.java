package org.grobid.core.data;

public class Relationship {

    private Span left;
    private Span right;
    private String type;


    public Span getLeft() {
        return left;
    }

    public void setLeft(Span left) {
        this.left = left;
    }

    public Span getRight() {
        return right;
    }

    public void setRight(Span right) {
        this.right = right;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
