package org.grobid.core.engines.training;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Superconductor;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

public interface SuperconductorsOutputFormattter {

    String format(List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList, int id);

}
