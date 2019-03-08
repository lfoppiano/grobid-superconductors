package org.grobid.core.data;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.UnitUtilities;

import java.util.ArrayList;
import java.util.List;

public class Superconductor {

    private String name;

    private List<BoundingBox> boundingBoxes = new ArrayList<>();
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private OffsetPosition offsets = null;
    private Measurement criticalTemperatureMeasurement = null;

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

        String encodedName = new String(encoder.quoteAsString(new String(encoder.encodeAsUTF8(name))));
        json.append("{ ");
        json.append("\"name\":" + "\"" + encodedName + "\"");
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

        if (criticalTemperatureMeasurement != null) {
            if (criticalTemperatureMeasurement.getType() == UnitUtilities.Measurement_Type.VALUE) {
                Quantity tc = criticalTemperatureMeasurement.getQuantityAtomic();
                Unit criticalUnit = criticalTemperatureMeasurement.getQuantityAtomic().getRawUnit();
                json.append(", \"tc\":\"" + tc.getRawValue() + " " + criticalUnit.getRawName() + "\"");
            } else if (criticalTemperatureMeasurement.getType().equals(UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE)) {

                Unit criticalUnit = null;
                StringBuilder temperature = new StringBuilder();
                if (criticalTemperatureMeasurement.getQuantityBase() != null) {
                    Quantity tc = criticalTemperatureMeasurement.getQuantityBase();

                    criticalUnit = tc.getRawUnit();
                    temperature.append(tc.getRawValue());
                }

                if (criticalTemperatureMeasurement.getQuantityRange() != null) {
                    if (criticalTemperatureMeasurement.getQuantityBase() != null) {
                        temperature.append("±");
                    }
                    Quantity tc = criticalTemperatureMeasurement.getQuantityRange();

                    criticalUnit = tc.getRawUnit();
                    temperature.append(tc.getRawValue());

                }
                temperature.append(" ").append(criticalUnit.getRawName());

                json.append(", \"tc\":\"" + temperature.toString() + "\"");

            } else if (criticalTemperatureMeasurement.getType().equals(UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX)) {
                Unit criticalUnit = null;
                StringBuilder temperature = new StringBuilder();
                if (criticalTemperatureMeasurement.getQuantityMost() != null) {
                    Quantity tc = criticalTemperatureMeasurement.getQuantityMost();

                    criticalUnit = tc.getRawUnit();
                    temperature.append(" < ").append(tc.getRawValue());
                }

                if (criticalTemperatureMeasurement.getQuantityLeast() != null) {
                    Quantity tc = criticalTemperatureMeasurement.getQuantityLeast();

                    criticalUnit = tc.getRawUnit();
                    temperature.append(" > ").append(tc.getRawValue());

                }
                temperature.append(" ").append(criticalUnit.getRawName());

                json.append(", \"tc\":\"" + temperature.toString() + "\"");

            } else if (criticalTemperatureMeasurement.getType().equals(UnitUtilities.Measurement_Type.CONJUNCTION)) {
            }
        }

        json.append(" }");
        return json.toString();
    }

    public Measurement getCriticalTemperatureMeasurement() {
        return criticalTemperatureMeasurement;
    }

    public void setCriticalTemperatureMeasurement(Measurement criticalTemperatureMeasurement) {
        this.criticalTemperatureMeasurement = criticalTemperatureMeasurement;
    }
}
