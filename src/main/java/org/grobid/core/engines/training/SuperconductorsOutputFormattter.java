package org.grobid.core.engines.training;

import org.grobid.core.data.document.DocumentBlock;

import java.util.List;

public interface SuperconductorsOutputFormattter {

    String format(List<DocumentBlock> documentBlocks, int id);

}
