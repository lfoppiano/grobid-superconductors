package org.grobid.trainer.sax;


import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * SAX handler for TEI-style annotations. should work for patent PDM and our usual scientific paper encoding.
 * Measures are inline quantities annotations.
 * The training data for the CRF models are generated during the XML parsing.
 *
 * @author Patrice Lopez
 */
public class SuperconductorsAnnotationSaxHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperconductorsAnnotationSaxHandler.class);

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private boolean ignore = false;
    private boolean openSuperconductor = false;
    private String currentTag = null;

    private List<Pair<String, String>> labeled = null; // store line by line the labeled data

    public SuperconductorsAnnotationSaxHandler() {
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        if (accumulator != null) {
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<Pair<String, String>> getLabeledResult() {
        return labeled;
    }

    public void endElement(String uri,
                           String localName,
                           String qName) throws SAXException {
        try {
            if ((!qName.equals("lb")) && (!qName.equals("pb"))) {
                /*if (!qName.equals("num")) && (!qName.equals("measure"))
                    currentTag = "<other>";*/
                writeData(qName);
                currentTag = null;
            }
            if (qName.equals("supercon")) {
                openSuperconductor = false;
            } else if (qName.equals("figure")) {
                // figures (which include tables) were ignored !
                ignore = false;
            } else if (qName.equals("p") || qName.equals("paragraph")) {
                // let's consider a new CRF input per paragraph too
                labeled.add(new Pair<>("\n", null));
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts) throws SAXException {
        try {
            if (qName.equals("lb")) {
                accumulator.append(" +L+ ");
            } else if (qName.equals("pb")) {
                accumulator.append(" +PAGE+ ");
            } else if (qName.equals("space")) {
                accumulator.append(" ");
            } else {
                // we have to write first what has been accumulated yet with the upper-level tag
                String text = getText();
                if (text != null) {
                    if (text.length() > 0) {
                        currentTag = "<other>";
                        writeData(qName);
                    }
                }
                accumulator.setLength(0);

                // we output the remaining text
                if (qName.equals("supercon") && !ignore) {
                    openSuperconductor = true;
                    currentTag = "<supercon>";
                } else if (qName.equals("figure")) {
                    // figures are ignored ! this includes tables
                    ignore = true;
                } else if (qName.equals("TEI") || qName.equals("tei") || qName.equals("teiCorpus")) {
                    labeled = new ArrayList<>();
                    accumulator = new StringBuffer();
                    currentTag = null;
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    private void writeData(String qName) {
        if (currentTag == null)
            currentTag = "<other>";
        if ((qName.equals("supercon")) ||
                (qName.equals("measure")) || (qName.equals("num")) || (qName.equals("date")) ||
                (qName.equals("paragraph")) || (qName.equals("p")) ||
                (qName.equals("div"))
        ) {

            String text = getText();
            // we segment the text
            //StringTokenizer st = new StringTokenizer(text, " \n\t" + TextUtilities.fullPunctuations, true);
            List<String> tokenizations = null;
            try {
                tokenizations = DeepAnalyzer.getInstance().tokenize(text);
            } catch (Exception e) {
                //logger.error("fail to tokenize:, " + text, e);
                throw new GrobidException("fail to tokenize:, " + text, e);
            }
            boolean begin = true;
            for (String tok : tokenizations) {
                tok = tok.trim();
                if (tok.length() == 0)
                    continue;

                if (tok.equals("+L+")) {
                    labeled.add(new Pair<>("@newline", null));
                } else if (tok.equals("+PAGE+")) {
                    // page break should be a distinct feature
                    labeled.add(new Pair<>("@newpage", null));
                } else {
                    String content = tok;
                    int i = 0;
                    if (content.length() > 0) {
                        if (begin && (!currentTag.equals("<other>"))) {
                            labeled.add(new Pair<>(content, "I-" + currentTag));
                            begin = false;
                        } else {
                            labeled.add(new Pair<>(content, currentTag));
                        }
                    }
                }
                begin = false;
            }
            accumulator.setLength(0);
        }
    }

}
