package org.grobid.core.engines.training;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Superconductor;

import java.util.List;

public interface SuperconductorsOutputFormattter {
    String format(List<Pair<List<Superconductor>, String>> labeledTextList, int id);

}
