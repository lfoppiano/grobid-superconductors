package org.grobid.core.data.document;

import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents a TextPassage raw material
 */
public class RawPassage {

    private List<LayoutToken> layoutTokens = new ArrayList<>();

    private String text;

    private String section;

    private String subSection;
    
    private String groupId; 

    public RawPassage(List<LayoutToken> layoutTokens, String section, String subSection) {
        this.layoutTokens = layoutTokens;
        this.section = section;
        this.subSection = subSection;
    }
    public RawPassage(List<LayoutToken> layoutTokens, String section, String subSection, String groupId) {
        this(layoutTokens, section, subSection);
        this.groupId = groupId;
    }

    public RawPassage(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public RawPassage() {
    }


    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubSection() {
        return subSection;
    }

    public void setSubSection(String subSection) {
        this.subSection = subSection;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
