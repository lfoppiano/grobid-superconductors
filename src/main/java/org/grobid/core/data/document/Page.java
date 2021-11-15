package org.grobid.core.data.document;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Page {

    @JsonProperty("page_height")
    private double pageHeight;
    @JsonProperty("page_width")
    private double pageWidth;

    public Page(double pageHeight, double pageWidth) {
        this.pageHeight = pageHeight;
        this.pageWidth = pageWidth;
    }

    public double getHeight() {
        return pageHeight;
    }

    public void setPageHeight(double pageHeight) {
        this.pageHeight = pageHeight;
    }

    public double getWidth() {
        return pageWidth;
    }

    public void setPageWidth(double pageWidth) {
        this.pageWidth = pageWidth;
    }
}
