package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contains the bibligraphic information to be included in the response
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BiblioInfo {
    private String title;
    private String authors;
    private String doi;
    private Integer year;
    private String publisher;
    private String journal;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getYear() {
        return year;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getJournal() {
        return journal;
    }
}
