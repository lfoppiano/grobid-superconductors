package org.grobid.core.data;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;

import java.util.ArrayList;
import java.util.List;

public class Material {

    private String structure;
    private String formula;

    private List<BoundingBox> boundingBoxes = new ArrayList<>();
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private OffsetPosition offsets = null;


}
