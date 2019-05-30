package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@JsonInclude(Include.NON_EMPTY)
public class OutputResponse {

    private long runtime;
    private List<Superconductor> superconductors;
    private List<Measurement> temperatures;
    private List<Abbreviation> abbreviations;

    private List<Superconductor> other;

    public OutputResponse() {
        superconductors = new ArrayList<>();
        temperatures = new ArrayList<>();
        abbreviations = new ArrayList<>();
        other = new ArrayList<>();
    }

    public OutputResponse(List<Superconductor> superconductorList, List<Measurement> temperatures,
                          List<Abbreviation> abbreviations, List<Superconductor> other) {
        this.superconductors = superconductorList;
        this.temperatures = temperatures;
        this.abbreviations = abbreviations;
        this.other = other;
    }

    public OutputResponse extendEntities(OutputResponse other) {
        this.superconductors.addAll(other.getSuperconductors());
        this.abbreviations.addAll(other.getAbbreviations());
        this.temperatures.addAll(other.getTemperatures());
        this.other.addAll(other.getOther());

        return this;
    }

    private List<Page> pages;

    public List<Superconductor> getSuperconductors() {
        return superconductors;
    }

    public void setSuperconductors(List<Superconductor> superconductors) {
        this.superconductors = superconductors;
    }

    public List<Measurement> getTemperatures() {
        return temperatures;
    }

    public void setTemperatures(List<Measurement> temperatures) {
        this.temperatures = temperatures;
    }

    public List<Abbreviation> getAbbreviations() {
        return abbreviations;
    }

    public void setAbbreviations(List<Abbreviation> abbreviations) {
        this.abbreviations = abbreviations;
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

        if (isNotEmpty(getSuperconductors())) {
            jsonBuilder.append(", \"superconductors\" : [ ");
            first = true;
            for (Superconductor superconductor : getSuperconductors()) {
                if (first)
                    first = false;
                else
                    jsonBuilder.append(", ");
                jsonBuilder.append(superconductor.toJson());
            }
            jsonBuilder.append("]");
        }

        if (isNotEmpty(getSuperconductors())) {
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

        if (isNotEmpty(getTemperatures())) {
            jsonBuilder.append(", \"temperatures\": [");
            first = true;
            for (Measurement temperature : getTemperatures()) {
                if (!first)
                    jsonBuilder.append(", ");
                else
                    first = false;
                jsonBuilder.append(temperature.toJson());
            }
            jsonBuilder.append("]");
        }

        if (isNotEmpty(getAbbreviations())) {
            jsonBuilder.append(", \"abbreviations\": [");
            first = true;
            for (Abbreviation abbreviation : getAbbreviations()) {
                if (!first)
                    jsonBuilder.append(", ");
                else
                    first = false;
                jsonBuilder.append(abbreviation.toJson());
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
