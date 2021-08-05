package org.grobid.core.engines.linking;

import org.grobid.core.data.Span;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

public interface EntityLinker {
    String addFeatures(List<LayoutToken> tokens, List<String> annotations);

    List<String> getAnnotationsToBeLinked();

    String label(Iterable<String> data);

    String label(String data);

    List<Span> extractResults(List<LayoutToken> tokens, String result, List<Span> annotations);

    List<Span> markLinkableEntities(List<Span> rawAnnotations);
}
