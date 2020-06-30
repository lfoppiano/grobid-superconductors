package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/** Use PdfProcessingResponse **/
@Deprecated
@JsonInclude(Include.NON_EMPTY)
public class PDFAnnotationResponse {

    private long runtime;
    private List<Superconductor> entities;
    private List<Measurement> measurements;

    private List<Superconductor> other;

    public PDFAnnotationResponse() {
        entities = new ArrayList<>();
        measurements = new ArrayList<>();
        other = new ArrayList<>();
    }

    public PDFAnnotationResponse(List<Superconductor> superconductorList, List<Measurement> measurements, List<Superconductor> other) {
        this.entities = superconductorList;
        this.measurements = measurements;
        this.other = other;
    }

    public PDFAnnotationResponse extendEntities(PDFAnnotationResponse other) {
        this.entities.addAll(other.getEntities());
        this.measurements.addAll(other.getMeasurements());
        this.other.addAll(other.getOther());

        return this;
    }

    private List<Page> pages;

    public List<Superconductor> getEntities() {
        return entities;
    }

    public void setEntities(List<Superconductor> entities) {
        this.entities = entities;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public String toJson() {
        StringBuilder jsonBuilder = new StringBuilder();

        jsonBuilder.append("{ ");
        jsonBuilder.append("\"runtime\" : " + runtime);
        boolean first = true;
        if (isNotEmpty(getPages())) {
            // page height and width
            jsonBuilder.append(", \"pages\":[");
            List<Page> pages = getPages();
            for (Page page : pages) {
                if (first)
                    first = false;
                else
                    jsonBuilder.append(", ");
                jsonBuilder.append("{\"page_height\":" + page.getHeight());
                jsonBuilder.append(", \"page_width\":" + page.getWidth() + "}");
            }
            jsonBuilder.append("]");
        }

        if (isNotEmpty(getEntities())) {
            jsonBuilder.append(", \"entities\" : [ ");
            first = true;
            for (Superconductor superconductor : getEntities()) {
                if (first)
                    first = false;
                else
                    jsonBuilder.append(", ");
                jsonBuilder.append(superconductor.toJson());
            }
            jsonBuilder.append("]");
        }

        if (isNotEmpty(getEntities())) {
            jsonBuilder.append(", \"other\" : [ ");
            first = true;
            for (Superconductor other : getOther()) {
                if (first)
                    first = false;
                else
                    jsonBuilder.append(", ");
                jsonBuilder.append(other.toJson());
            }
            jsonBuilder.append("]");
        }

        if (isNotEmpty(getMeasurements())) {
            jsonBuilder.append(", \"measurements\": [");
            first = true;
            for (Measurement temperature : getMeasurements()) {
                if (!first)
                    jsonBuilder.append(", ");
                else
                    first = false;
                jsonBuilder.append(temperature.toJson());
            }
            jsonBuilder.append("]");
        }

        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }

    public List<Superconductor> getOther() {
        return other;
    }

    public void setOther(List<Superconductor> other) {
        this.other = other;
    }
}
