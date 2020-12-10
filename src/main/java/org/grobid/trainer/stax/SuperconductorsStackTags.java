package org.grobid.trainer.stax;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SuperconductorsStackTags {

    private final List<String> stackTags = new LinkedList<>();

    public void append(String tag) {
        stackTags.add(tag);
    }

    public String peek() {
        return stackTags.remove(stackTags.size() - 1);
    }

    public String toString() {
        return "/" + StringUtils.join(stackTags, "/");
    }

    public static SuperconductorsStackTags from(String path) {
        final SuperconductorsStackTags superconductorsStackTags = new SuperconductorsStackTags();
        Arrays.stream(StringUtils.split(path, "/"))
            .forEach(superconductorsStackTags::append);
        return superconductorsStackTags;
    }

    public static SuperconductorsStackTags from(SuperconductorsStackTags tags) {
        return SuperconductorsStackTags.from(tags.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuperconductorsStackTags superconductorsStackTags1 = (SuperconductorsStackTags) o;
        return Objects.equals(toString(), superconductorsStackTags1.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackTags.toString());
    }
}