package org.grobid.core.analyzers;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Tokenizer adequate for all Indo-European languages and special characters.
 * <p>
 * The difference with the Standard Grobid tokenizer is that this tokenizer
 * is also tokenizing mixture of alphabetical and numerical characters.
 * <p>
 * 1m74 ->  tokens.add(new LayoutToken("1"));
 * tokens.add(new LayoutToken("m"));
 * tokens.add(new LayoutToken("74"));
 *
 * @author Patrice Lopez
 */

public class DeepAnalyzer implements Analyzer {

    private static volatile DeepAnalyzer instance;

    public static DeepAnalyzer getInstance() {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
            if (instance == null)
                getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        instance = new DeepAnalyzer();
    }

    /**
     * Hidden constructor
     */
    private DeepAnalyzer() {
    }

    public static final String DELIMITERS = " \n\r\t([^%‰°,:;?.!/)-–−=≈<>+\"“”‘’'`$]*\u2666\u2665\u2663\u2660\u00A0";
    private static final String REGEX = "(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=\\D)";

    public String getName() {
        return "DeepAnalyzer";
    }

    public List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                result.add(subtokens[i]);
            }
        }

        return result;
    }

    public List<String> tokenize(String text, Language lang) {
        return tokenize(text);
    }

    public List<LayoutToken> tokenizeWithLayoutToken(String text) {
        List<LayoutToken> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                LayoutToken layoutToken = new LayoutToken();
                layoutToken.setText(subtokens[i]);
                result.add(layoutToken);
            }
        }

        return result;
    }

    public List<String> retokenize(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            result.addAll(tokenize(chunk));
        }
        return result;
    }

    public List<LayoutToken> retokenizeLayoutTokens(List<LayoutToken> tokens) {
        List<LayoutToken> result = new ArrayList<>();
        int idx = 0;
        for (LayoutToken token : tokens) {
            result.addAll(tokenize(token, idx));
            idx += StringUtils.length(token.getText());
        }
        return result;
    }

    public List<LayoutToken> tokenize(LayoutToken chunk, int startingIndex) {
        List<LayoutToken> result = new ArrayList<>();
        String text = chunk.getText();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                LayoutToken theChunk = new LayoutToken(chunk); // deep copy
                theChunk.setText(subtokens[i]);
                result.add(theChunk);
                theChunk.setOffset(startingIndex);
                startingIndex+= StringUtils.length(theChunk.getText());
            }
        }
        return result;
    }
}