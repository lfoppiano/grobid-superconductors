package org.grobid.core.engines.training;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Span;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

public interface SuperconductorsOutputFormattter {

    String format(List<Pair<List<Span>, List<LayoutToken>>> labeledTextList, int id);

}
