package org.grobid.core.features;

import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

import java.util.regex.Matcher;

/**
 * Class for features used for superconductor identification in raw texts such as scientific articles
 * and patent descriptions.
 */
public class FeaturesVectorEntityLinker {
    public LayoutToken token = null;    // just the reference value, not a feature

    public String string = null; // lexical feature
    public String label = null;     // label if known

    public String capitalisation = null;// one of INITCAP, ALLCAPS, NOCAPS
    public String digit;                // one of ALLDIGIT, CONTAINDIGIT, NODIGIT

    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE
    public String punctType = null;

    public String shadowNumber = null; // Convert digits to “0”

    public String wordShape = null;
    // Convert upper-case letters to "X", lowercase letters to "x", digits to "d" and other to "c"
    // there is also a trimmed variant where sequence of similar character shapes are reduced to one
    // converted character shape

    public String wordShapeTrimmed = null;

    public String entityType = null;

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
//        res.append(" ").append(TextUtilities.prefix(string, 1));
//        res.append(" ").append(TextUtilities.prefix(string, 2));
//        res.append(" ").append(TextUtilities.prefix(string, 3));
//        res.append(" ").append(TextUtilities.prefix(string, 4));
//
//        // suffix (4)
//        res.append(" ").append(TextUtilities.suffix(string, 1));
//        res.append(" ").append(TextUtilities.suffix(string, 2));
//        res.append(" ").append(TextUtilities.suffix(string, 3));
//        res.append(" ").append(TextUtilities.suffix(string, 4));

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

        // digit information (1)
        res.append(" " + digit);

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // token length
        //res.append(" " + string.length());

        // shadow number
//        res.append(" " + shadowNumber);

        // word shape
        res.append(" " + wordShape);

        // word shape trimmed
//        res.append(" " + wordShapeTrimmed);

        // entity type
        res.append(" " + GenericTaggerUtils.getPlainLabel(entityType));

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
    public static FeaturesVectorEntityLinker addFeatures(String token, String label, String entityType) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorEntityLinker featuresVector = new FeaturesVectorEntityLinker();
        String string = token;
        featuresVector.string = string;
        featuresVector.label = label;


        if (featureFactory.test_all_capital(string))
            featuresVector.capitalisation = "ALLCAPS";
        else if (featureFactory.test_first_capital(string))
            featuresVector.capitalisation = "INITCAP";
        else
            featuresVector.capitalisation = "NOCAPS";

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
        if ((string.equals("(")) || (string.equals("["))) {
            featuresVector.punctType = "OPENBRACKET";
        } else if ((string.equals(")")) || (string.equals("]"))) {
            featuresVector.punctType = "ENDBRACKET";
        } else if (string.equals(".")) {
            featuresVector.punctType = "DOT";
        } else if (string.equals(",")) {
            featuresVector.punctType = "COMMA";
        } else if (string.equals("-") || string.equals("−") || string.equals("–")) {
            featuresVector.punctType = "HYPHEN";
        } else if (string.equals("\"") || string.equals("'") || string.equals("`")) {
            featuresVector.punctType = "QUOTE";
        }

        //DEFAULTS
        if (featuresVector.punctType == null)
            featuresVector.punctType = "NOPUNCT";

        featuresVector.shadowNumber = TextUtilities.shadowNumbers(string);

        featuresVector.wordShape = TextUtilities.wordShape(string);

        featuresVector.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(string);

//        Optional<String> first = token.getLabels().stream()
//            .filter(taggingLabel -> taggingLabel.getGrobidModel().equals(SuperconductorsModels.SUPERCONDUCTORS))
//            .map(TaggingLabel::getLabel)
//            .findFirst();
//
//        // entity type
//        featuresVector.entityType = first.orElse(TaggingLabels.OTHER_LABEL);
        featuresVector.entityType = entityType;

        return featuresVector;
    }

}

	
