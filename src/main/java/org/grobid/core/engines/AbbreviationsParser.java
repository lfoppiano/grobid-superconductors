package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Abbreviation;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorSuperconductors;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.grobid.core.engines.label.AbbreviationsTaggingLabels.ABBREVIATION_OTHER;
import static org.grobid.core.engines.label.AbbreviationsTaggingLabels.ABBREVIATION_VALUE_NAME;

@Singleton
public class AbbreviationsParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationsParser.class);

    private static volatile AbbreviationsParser instance;

    public static AbbreviationsParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new AbbreviationsParser();
    }

    @Inject
    public AbbreviationsParser() {
        super(AbbreviationsModels.ABBREVIATIONS);
    }

    public Pair<String, List<Abbreviation>> generateTrainingData(List<LayoutToken> layoutTokens) {

        List<Abbreviation> measurements = new ArrayList<>();
        String ress = null;

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        try {
            // string representation of the feature matrix for CRF lib
            ress = addFeatures(tokens);

            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantity parsing failed.", e);
            }
            measurements.addAll(extractResults(tokens, res));
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return Pair.of(ress, measurements);
    }

    public Pair<String, List<Abbreviation>> generateTrainingData(String text) {
        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> layoutTokens = null;
        try {
            layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        return generateTrainingData(layoutTokens);

    }

    public List<Abbreviation> process(List<LayoutToken> layoutTokens) {

        List<Abbreviation> entities = new ArrayList<>();

        // List<LayoutToken> for the selected segment
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        // list of textual tokens of the selected segment
        //List<String> texts = getTexts(tokenizationParts);

//        List<Mention> mentions = chemspotClient.processText(LayoutTokensUtil.toText(layoutTokens));
//        List<Boolean> listChemspotEntities = synchroniseLayoutTokensWithMentions(tokens, mentions);


        if (isEmpty(tokens))
            return new ArrayList<>();

        try {
            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(tokens);

            if (StringUtils.isEmpty(ress))
                return new ArrayList<>();

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantity parsing failed.", e);
            }

            List<Abbreviation> localEntities = extractResults(tokens, res);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }

    /**
     * Extract all occurrences of measurement/quantities from a simple piece of text.
     */
    public List<Abbreviation> process(String text) {
        if (isBlank(text)) {
            return null;
        }

        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> tokens = null;
        try {
            tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        if ((tokens == null) || (tokens.size() == 0)) {
            return new ArrayList<>();
        }
        return process(tokens);
    }


    @SuppressWarnings({"UnusedParameters"})
    private String addFeatures(List<LayoutToken> tokens) {
        StringBuilder result = new StringBuilder();
        try {
            LayoutToken previous = new LayoutToken();
            ListIterator<LayoutToken> it = tokens.listIterator();
            while (it.hasNext()) {
                int index = it.nextIndex();
                LayoutToken token = it.next();

                if (token.getText().trim().equals("@newline")) {
                    result.append("\n");
                    continue;
                }

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

//                // parano normalisation
//                text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
//                if (text.trim().length() == 0) {
//                    continue;
//                }


                FeaturesVectorSuperconductors featuresVector =
                        FeaturesVectorSuperconductors.addFeatures(token, null, previous, false);
                result.append(featuresVector.printVector());
                result.append("\n");
                previous = token;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return result.toString();
    }

    /**
     * Extract identified quantities from a labeled text.
     */
    public List<Abbreviation> extractResults(List<LayoutToken> tokens, String result) {
        List<Abbreviation> resultList = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(AbbreviationsModels.ABBREVIATIONS, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int pos = 0; // position in term of characters for creating the offsets

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();
            List<BoundingBox> boundingBoxes = null;

            if (!clusterLabel.equals(ABBREVIATION_OTHER))
                boundingBoxes = BoundingBoxCalculator.calculate(cluster.concatTokens());

            String text = LayoutTokensUtil.toText(tokens);
            if ((pos < text.length() - 1) && (text.charAt(pos) == ' '))
                pos += 1;
            int endPos = pos;
            boolean start = true;
            for (LayoutToken token : theTokens) {
                if (token.getText() != null) {
                    if (start && token.getText().equals(" ")) {
                        pos++;
                        endPos++;
                        continue;
                    }
                    if (start)
                        start = false;
                    endPos += token.getText().length();
                }
            }

            if ((endPos > 0) && (endPos <= text.length()) && (text.charAt(endPos - 1) == ' '))
                endPos--;

            Abbreviation abbreviation = null;

            if (clusterLabel.equals(ABBREVIATION_VALUE_NAME)) {
                abbreviation = new Abbreviation();
                abbreviation.setName(clusterContent);
                abbreviation.setLayoutTokens(theTokens);
                abbreviation.setBoundingBoxes(boundingBoxes);
                abbreviation.setOffsetStart(pos);
                abbreviation.setOffsetEnd(endPos);
                resultList.add(abbreviation);
            } else if (clusterLabel.equals(ABBREVIATION_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in abbreviation parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }

            pos = endPos;
        }

        return resultList;
    }
}
