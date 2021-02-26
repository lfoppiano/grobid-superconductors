package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Class for features used for superconductor identification in raw texts such as scientific articles
 * and patent descriptions.
 */
public class FeaturesVectorNanostructure {
    public LayoutToken token = null;    // just the reference value, not a feature

    public String string = null; // lexical feature
    public String label = null;     // label if known

    public String digit;                // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE
    public String punctType = null;
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public String fontStyle = null;   // one of BASELINE (default), SUPERSCRIPT, SUBSCRIPT

    public boolean bold = false;
    public boolean italic = false;

    public String chemicalCompound = null;

    public String shadowNumber = null; // Convert digits to “0”

    public String wordShape = null;
    // Convert upper-case letters to "X", lowercase letters to "x", digits to "d" and other to "c"
    // there is also a trimmed variant where sequence of similar character shapes are reduced to one
    // converted character shape

    public String wordShapeTrimmed = null;

    private boolean isNumberToken = false;

    public String printVector() {
        if (isBlank(string)) {
            return null;
        }
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // prefix (4)
        res.append(" ").append(TextUtilities.prefix(string, 1));
        res.append(" ").append(TextUtilities.prefix(string, 2));
        res.append(" ").append(TextUtilities.prefix(string, 3));
        res.append(" ").append(TextUtilities.prefix(string, 4));

        // suffix (4)
        res.append(" ").append(TextUtilities.suffix(string, 1));
        res.append(" ").append(TextUtilities.suffix(string, 2));
        res.append(" ").append(TextUtilities.suffix(string, 3));
        res.append(" ").append(TextUtilities.suffix(string, 4));


        // digit information (1)
        res.append(" " + digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // token length
        //res.append(" " + string.length());

        // shadow number
        res.append(" " + shadowNumber);

        // word shape
        res.append(" " + wordShape);

        // word shape trimmed
        res.append(" " + wordShapeTrimmed);

        res.append(" " + bold);

        res.append(" " + italic);

        res.append(" " + fontStyle);

        // value returned by a chemical recognitor
        res.append(" " + chemicalCompound);

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "");
        /*else
            res.append(" 0");*/

        return res.toString();
    }

    /**
     * Add the features for the chemical entity extraction model.
     */
    public static FeaturesVectorNanostructure addFeatures(LayoutToken token,
                                                          String label,
                                                          String compoundType) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorNanostructure featuresVector = new FeaturesVectorNanostructure();
        featuresVector.token = token;
        String string = token.getText();
        featuresVector.string = string;
        featuresVector.label = label;


        if (string.length() == 1) {
            featuresVector.singleChar = true;
        }

        if (featureFactory.test_number(string))
            featuresVector.digit = "ALLDIGIT";
        else if (FeatureFactory.test_digit(string))
            featuresVector.digit = "CONTAINDIGIT";
        else
            featuresVector.digit = "NODIGIT";

        Matcher m0 = featureFactory.isPunct.matcher(string);
        if (m0.find()) {
            featuresVector.punctType = "PUNCT";
        }

        if (string.equals("(") || string.equals("[") || string.equals("「")) {
            featuresVector.punctType = "OPENBRACKET";
        } else if (string.equals(")") || string.equals("]") || string.equals("」")) {
            featuresVector.punctType = "ENDBRACKET";
        } else if (string.equals(".") || string.equals("⋅") || string.equals("•") || string.equals("·") || string.equals("．") || string.equals("。")) {
            featuresVector.punctType = "DOT";
        } else if (string.equals(",") || string.equals("、")) {
            featuresVector.punctType = "COMMA";
        } else if (string.equals("-") || string.equals("−") || string.equals("–") || string.equals("ー")) {
            featuresVector.punctType = "HYPHEN";
        } else if (string.equals("\"") || string.equals("'") || string.equals("`") || string.equals("”") || string.equals("’")) {
            featuresVector.punctType = "QUOTE";
        }

        //DEFAULTS
        if (featuresVector.digit == null)
            featuresVector.digit = "NODIGIT";

        if (featuresVector.punctType == null)
            featuresVector.punctType = "NOPUNCT";

        if (token.isBold())
            featuresVector.bold = true;

        if (token.isItalic())
            featuresVector.italic = true;

        if (token.isSuperscript()) {
            featuresVector.fontStyle = "SUPERSCRIPT";
        } else if (token.isSubscript()) {
            featuresVector.fontStyle = "SUBSCRIPT";
        } else {
            featuresVector.fontStyle = "BASELINE";
        }

        featuresVector.shadowNumber = TextUtilities.shadowNumbers(string);

        featuresVector.wordShape = FeaturesVectorNanostructure.wordShapeJapanese(string);

        featuresVector.wordShapeTrimmed = FeaturesVectorNanostructure.wordShapeTrimmedJapanese(string);

        // Chemical compound
        featuresVector.chemicalCompound = compoundType;

        return featuresVector;
    }

    /**
     * @param character a japanese character full or half with
     * @return k if it's kanji, y if it's katakana and h if it's hiragana
     */
    public static char getCharacterNature(char character) {
        if ((int) character >= 0x30a0 && (int) character <= 0x30ff) {
            return 'y';
        } else if ((int) character >= 0x3040 && (int) character <= 0x309f) {
            return 'h';
        } else if ((int) character >= 0x4e00 && (int) character <= 0x9faf) {
            return 'k';
        } else if ((int) character >= 0xff00 && (int) character <= 0xffef) {
            return 'r';
        } else {
            return character;
        }
    }

    public static String wordShapeJapanese(String word) {
        StringBuilder shape = new StringBuilder();
        for (char c : word.toCharArray()) {
            shape.append(getCharacterNature(c));
        }

        StringBuilder finalShape = new StringBuilder().append(shape.charAt(0));

        String suffix = "";
        if (word.length() > 2) {
            suffix = shape.substring(shape.length() - 2);
        } else if (word.length() > 1) {
            suffix = shape.substring(shape.length() - 1);
        }

        StringBuilder middle = new StringBuilder();
        if (shape.length() > 3) {
            char ch = shape.charAt(1);
            for (int i = 1; i < shape.length() - 2; i++) {
                middle.append(ch);
                while (ch == shape.charAt(i) && i < shape.length() - 2) {
                    i++;
                }
                ch = shape.charAt(i);
            }

            if (ch != middle.charAt(middle.length() - 1)) {
                middle.append(ch);
            }
        }
        return finalShape.append(middle).append(suffix).toString();

    }

    public static String wordShapeTrimmedJapanese(String word) {
        StringBuilder shape = new StringBuilder();
        for (char c : word.toCharArray()) {
            shape.append(getCharacterNature(c));
        }

        StringBuilder middle = new StringBuilder();

        char ch = shape.charAt(0);
        for (int i = 0; i < shape.length(); i++) {
            middle.append(ch);
            while (ch == shape.charAt(i) && i < shape.length() - 1) {
                i++;
            }
            ch = shape.charAt(i);
        }

        if (ch != middle.charAt(middle.length() - 1)) {
            middle.append(ch);
        }
        return middle.toString();
    }

}

	
