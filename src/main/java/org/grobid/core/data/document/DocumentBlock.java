package org.grobid.core.data.document;

import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

import static org.grobid.core.engines.tagging.GenericTaggerUtils.getPlainLabel;

/**
 * Represent a block of a document
 */
public class DocumentBlock {

    public static final String SECTION_BODY = getPlainLabel(SegmentationLabels.BODY_LABEL);
    public static final String SECTION_HEADER = getPlainLabel(SegmentationLabels.HEADER_LABEL);
    public static final String SECTION_ANNEX = getPlainLabel(SegmentationLabels.ANNEX_LABEL);

    public static final String SUB_SECTION_TITLE = getPlainLabel(TaggingLabels.TITLE_LABEL);
    public static final String SUB_SECTION_ABSTRACT = getPlainLabel(TaggingLabels.ABSTRACT_LABEL);
    public static final String SUB_SECTION_KEYWORDS = getPlainLabel(TaggingLabels.KEYWORD_LABEL);
    public static final String SUB_SECTION_TITLE_SECTION = getPlainLabel(TaggingLabels.SECTION_LABEL);
    public static final String SUB_SECTION_PARAGRAPH = getPlainLabel(TaggingLabels.PARAGRAPH_LABEL);
    public static final String SUB_SECTION_FIGURE = getPlainLabel(TaggingLabels.FIGURE_LABEL);
    public static final String SUB_SECTION_TABLE = getPlainLabel(TaggingLabels.TABLE_LABEL);

    private String section;
    private String subSection;
    private String paragraphId;
    private List<LayoutToken> layoutTokens;
    private List<List<LayoutToken>> markers = new ArrayList<>();
    private List<Span> spans = new ArrayList<>();

    public DocumentBlock(List<LayoutToken> tokens, String section, String subSection) {
        this.section = section;
        this.subSection = subSection;
        this.layoutTokens = new ArrayList<>(tokens);
    }

//    public DocumentBlock(List<LayoutToken> tokens, String section, String subSection, List<List<LayoutToken>> markers) {
//        this(tokens, section, subSection);
//        this.markers = new ArrayList<>(markers);
//    }

    public DocumentBlock(DocumentBlock documentBlock) {
        this(documentBlock.getLayoutTokens(), documentBlock.getSection(), documentBlock.getSubSection(), documentBlock.getSpans(), documentBlock.getMarkers());
    }

    public DocumentBlock(List<LayoutToken> layoutTokens, String section, String subSection, List<Span> spanList, List<List<LayoutToken>> markers) {
        this(layoutTokens, section, subSection);
        this.spans = new ArrayList<>(spanList);
        this.markers = new ArrayList<>(markers);
    }

    public DocumentBlock(List<LayoutToken> layoutTokens, String section, String subSection, String id, List<Span> spanList, List<List<LayoutToken>> markers) {
        this(layoutTokens, section, subSection, spanList, markers);
        this.paragraphId = id;
    }


    public String getSubSection() {
        return subSection;
    }

    public void setSubSection(String subSection) {
        this.subSection = subSection;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void setSpans(List<Span> spans) {
        this.spans = spans;
    }

    public List<Span> getSpans() {
        return spans;
    }

    public List<List<LayoutToken>> getMarkers() {
        return markers;
    }

    public void setMarkers(List<List<LayoutToken>> markers) {
        this.markers = markers;
    }

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphIdentifier) {
        this.paragraphId = paragraphIdentifier;
    }
}
