package org.grobid.core.engines.training;

public enum TrainingOutputFormat {
    TSV("tsv"),
    XML("tei.xml");

    private String name;

    TrainingOutputFormat(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
