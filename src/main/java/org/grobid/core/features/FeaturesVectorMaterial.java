package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

import java.util.regex.Matcher;

/**
 * Class for features used for superconductor identification in raw texts such as scientific articles
 * and patent descriptions.
 */
public class FeaturesVectorMaterial {
    public LayoutToken token = null;    // just the reference value, not a feature

    public String string = null; // lexical feature
    public String label = null;     // label if known

    public String capitalisation = null;// one of INITCAP, ALLCAPS, NOCAPS
    public String digit;                // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE
    public String punctType = null;
//    public String fontStyle = null;   // one of BASELINE (default), SUPERSCRIPT, SUBSCRIPT

//    public boolean bold = false;
//    public boolean italic = false;

//    public String chemicalCompound = null;

    public String shadowNumber = null; // Convert digits to “0”

    public String wordShape = null;
    // Convert upper-case letters to "X", lowercase letters to "x", digits to "d" and other to "c"
    // there is also a trimmed variant where sequence of similar character shapes are reduced to one
    // converted character shape

    public String wordShapeTrimmed = null;

    private boolean isNumberToken = false;

    public String printVector() {
//        if (isBlank(string)) {
//            return null;
//        }
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // lowercase string
        res.append(" " + string.toLowerCase());

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

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

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

        //Font status
//        res.append(" " + fontStatus);

        //Font size
//        res.append(" " + fontSize);

//        res.append(" " + bold);

//        res.append(" " + italic);

//        res.append(" " + fontStyle);

        // value returned by a chemical recognitor
//        res.append(" " + chemicalCompound);

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
    public static FeaturesVectorMaterial addFeatures(String text,
                                                     String label) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorMaterial featuresVector = new FeaturesVectorMaterial();
//        featuresVector.token = token;

        featuresVector.string = text;
        featuresVector.label = label;


        if (text.length() == 1) {
            featuresVector.singleChar = true;
        }

        if (featureFactory.test_all_capital(text))
            featuresVector.capitalisation = "ALLCAPS";
        else if (featureFactory.test_first_capital(text))
            featuresVector.capitalisation = "INITCAP";
        else
            featuresVector.capitalisation = "NOCAPS";

        if (featureFactory.test_number(text))
            featuresVector.digit = "ALLDIGIT";
        else if (featureFactory.test_digit(text))
            featuresVector.digit = "CONTAINDIGIT";
        else
            featuresVector.digit = "NODIGIT";

        Matcher m0 = featureFactory.isPunct.matcher(text);
        if (m0.find()) {
            featuresVector.punctType = "PUNCT";
        }
        if ((text.equals("(")) || (text.equals("["))) {
            featuresVector.punctType = "OPENBRACKET";
        } else if ((text.equals(")")) || (text.equals("]"))) {
            featuresVector.punctType = "ENDBRACKET";
        } else if (text.equals(".")) {
            featuresVector.punctType = "DOT";
        } else if (text.equals(",")) {
            featuresVector.punctType = "COMMA";
        } else if (text.equals("-") || text.equals("−") || text.equals("–")) {
            featuresVector.punctType = "HYPHEN";
        } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
            featuresVector.punctType = "QUOTE";
        }

        //DEFAULTS
        if (featuresVector.capitalisation == null)
            featuresVector.capitalisation = "NOCAPS";

        if (featuresVector.digit == null)
            featuresVector.digit = "NODIGIT";

        if (featuresVector.punctType == null)
            featuresVector.punctType = "NOPUNCT";

//        if (token.getBold())
//            featuresVector.bold = true;

//        if (token.getItalic())
//            featuresVector.italic = true;

//        if (StringUtils.equals(previousToken.getFont(), token.getFont())) {
//            featuresVector.fontStatus = "SAMEFONT";
//        } else {
//            featuresVector.fontStatus = "DIFFERENTFONT";
//        }

//        if (previousToken.fontSize < token.fontSize) {
//            featuresVector.fontSize = "HIGHERFONT";
//        } else if (previousToken.fontSize == token.fontSize) {
//            featuresVector.fontSize = "SAMEFONTSIZE";
//        } else {
//            featuresVector.fontSize = "LOWERFONT";
//        }

//        if(token.isSuperscript()) {
//            featuresVector.fontStyle = "SUPERSCRIPT";
//        } else if (token.isSubscript()) {
//            featuresVector.fontStyle = "SUBSCRIPT";
//        } else {
//            featuresVector.fontStyle = "BASELINE";
//        }

        featuresVector.shadowNumber = TextUtilities.shadowNumbers(text);

        featuresVector.wordShape = TextUtilities.wordShape(text);

        featuresVector.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(text);

        // Chemical compound
//        featuresVector.chemicalCompound = compoundType;

        return featuresVector;
    }

}

	
