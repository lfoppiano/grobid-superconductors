package org.grobid.core.data;

public class Token {
    private String text;
    private String font;
    private String style;
    private int offset;
    private double fontSize;

    public Token(String text, String font, double fontSize, String style, int offset) {
        this.text = text;
        this.font = font;
        this.fontSize = fontSize;
        this.style = style;
        this.offset = offset;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }
}
