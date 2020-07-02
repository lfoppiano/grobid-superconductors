package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Token {
    private final String text;
    private final String font;
    private final String style;
    private final int offset;
    private final boolean isBold;
    private final boolean isItalic;
    private final double fontSize;

    public Token(String text, String font, double fontSize, String style, int offset, boolean isItalic, boolean isBold) {
        this.text = text;
        this.font = font;
        this.fontSize = fontSize;
        this.style = style;
        this.isItalic = isItalic;
        this.isBold = isBold;
        this.offset = offset;
    }

    public String getText() {
        return text;
    }

    public String getFont() {
        return font;
    }

    public String getStyle() {
        return style;
    }

    public int getOffset() {
        return offset;
    }

    public double getFontSize() {
        return fontSize;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public boolean isBold() {
        return isBold;
    }
}
