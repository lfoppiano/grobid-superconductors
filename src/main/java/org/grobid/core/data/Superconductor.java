package org.grobid.core.data;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.List;

public class Superconductor {

    private String name;

    private List<BoundingBox> boundingBoxes = new ArrayList<>();
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private OffsetPosition offsets = null;

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.replaceAll("\n", "");
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setOffsetStart(int start) {
        if (!hasOffset()) {
            offsets = new OffsetPosition();
        }
        offsets.start = start;
    }

    public int getOffsetStart() {
        if (hasOffset()) {
            return offsets.start;
        } else {
            return -1;
        }
    }

    public void setOffsetEnd(int end) {
        if (!hasOffset()) {
            offsets = new OffsetPosition();
        }
        offsets.end = end;
    }

    public int getOffsetEnd() {
        if (hasOffset()) {
            return offsets.end;
        } else {
            return -1;
        }
    }

    private boolean hasOffset() {
        return offsets != null;
    }

    public String toJson() {
        JsonStringEncoder encoder = BufferRecyclers.getJsonStringEncoder();
        StringBuilder json = new StringBuilder();
        boolean started = false;
        json.append("{ ");
        byte[] encodedName = null;
        json.append("\"name\":" + "\"" + name + "\"");
        started = true;

        if (offsets != null) {
            if (getOffsetStart() != -1) {
                if (!started) {
                    started = true;
                } else
                    json.append(", ");
                json.append("\"offsetStart\" : " + getOffsetStart());
            }
            if (getOffsetEnd() != -1) {
                if (!started) {
                    started = true;
                } else
                    json.append(", ");
                json.append("\"offsetEnd\" : " + getOffsetEnd());
            }
        }

        if ((boundingBoxes != null) && (boundingBoxes.size() > 0)) {
            json.append(", \"boundingBoxes\" : [");
            boolean first = true;
            for (BoundingBox box : boundingBoxes) {
                if (first)
                    first = false;
                else
                    json.append(",");
                json.append("{").append(box.toJson()).append("}");
            }
            json.append("] ");
        }

        json.append(" }");
        return json.toString();
    }
}
