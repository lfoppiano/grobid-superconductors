package org.grobid.trainer.stax;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class StackTags {

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

    public static StackTags from(String path) {
        final StackTags stackTags = new StackTags();
        Arrays.stream(StringUtils.split(path, "/"))
            .forEach(stackTags::append);
        return stackTags;
    }

    public static StackTags from(StackTags tags) {
        return StackTags.from(tags.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackTags stackTags1 = (StackTags) o;
        return Objects.equals(toString(), stackTags1.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackTags.toString());
    }
}