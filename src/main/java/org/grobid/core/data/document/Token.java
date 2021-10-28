package org.grobid.core.data.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.grobid.core.layout.LayoutToken;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Token {
    private final String text;
    private final String font;
    private final String style;
    private final int offset;
    private final boolean isBold;
    private final boolean isItalic;
    private final double fontSize;

    @JsonCreator
    public Token(@JsonProperty("text") String text, @JsonProperty("font") String font, @JsonProperty("fontSize") double fontSize, @JsonProperty("style") String style, @JsonProperty("offset") int offset, @JsonProperty("italic") boolean isItalic, @JsonProperty("bold") boolean isBold) {
        this.text = text;
        this.font = font;
        this.fontSize = fontSize;
        this.style = style;
        this.isItalic = isItalic;
        this.isBold = isBold;
        this.offset = offset;
    }

    public static Token of(LayoutToken layoutToken) {
        String style = getStyle(layoutToken);

        return new Token(layoutToken.getText(), layoutToken.getFont(), layoutToken.getFontSize(), style, layoutToken.getOffset(), layoutToken.isItalic(), layoutToken.isBold());
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

    @JsonIgnore
    public static String getStyle(LayoutToken l) {
        boolean subscript = l.isSubscript();
        boolean superscript = l.isSuperscript();

        String style = "baseline";
        if (superscript) {
            style = "superscript";
        } else if (subscript) {
            style = "subscript";
        }
        return style;
    }
}
