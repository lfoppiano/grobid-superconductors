package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.impl.OpenNLPSentenceDetector;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class SentenceSegmenter {

    private SentenceDetector tokenizer;

    public SentenceSegmenter() {
        this.tokenizer = new OpenNLPSentenceDetector();
    }

    public SentenceSegmenter(SentenceDetector sentenceDetector) {
        this.tokenizer = sentenceDetector;
    }

    public List<OffsetPosition> getSentencesAsOffsets(List<String> tokens) {
        String paragraph = String.join("", tokens);

        return tokenizer.detect(paragraph);
    }
    
    public List<OffsetPosition> detect(String text) {
        return tokenizer.detect(text);
    }

/*    public List<OffsetPosition> getSentencesAsIndexes(List<String> tokens) {
        List<OffsetPosition> sentencesAsOffsets = getSentencesAsOffsets(tokens);

        if (sentencesAsOffsets.size() == 1) {
            return Collections.singletonList(new OffsetPosition(0, tokens.size() - 1));
        }
        
        StringBuilder sb = new StringBuilder();

        for (String token :tokens) {
            
            sb.append(token);
        }

    }*/

    public static List<Pair<Integer, Integer>> fromOffsetsToIndexes(List<OffsetPosition> offsets, List<String> tokensWithoutSpaces) {
        if (isEmpty(offsets) || offsets.size() == 1) {
            return Arrays.asList(Pair.of(0, tokensWithoutSpaces.size()));
        }

//        List<ChemicalSpan> mentions = new ArrayList<>();
//        mentions = mentions.stream()
//            .sorted(Comparator.comparingInt(ChemicalSpan::getStart))
//            .collect(Collectors.toList());

        int offsetId = 0;
        OffsetPosition offset = offsets.get(offsetId);

        List<Pair<Integer, Integer>> resultIndexes = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        int idxStart = 0;
        int idxEnd = 0;
        for (int idx = 0; idx < tokensWithoutSpaces.size(); idx++) {
            String token = tokensWithoutSpaces.get(idx);

            int offsetStart = offset.start;
            int offsetEnd = offset.end;

            int tokenCumulatedStart = sb.toString().length();
            sb.append(token).append(" ");
            int tokenCumulatedEnd = sb.toString().length() + 1;

            if (tokenCumulatedEnd <= offsetEnd) {
                idxEnd = idx;
            } else {
                if (offsetId == offsets.size() - 1) {
                    resultIndexes.add(Pair.of(idxStart, idx + 1));
                } else {
                    resultIndexes.add(Pair.of(idxStart, idx + 1));
                    offsetId++;
                    idxStart = idx + 1;
                    offset = offsets.get(offsetId);
                }
            }
        }

//        if (tokensWithoutSpaces.size() > isChemicalEntity.size()) {
//            for (int counter = isChemicalEntity.size(); counter < tokens.size(); counter++) {
//                isChemicalEntity.add(Boolean.FALSE);
//            }
//        }

        return resultIndexes;
    }

    protected List<OffsetPosition> getSentencesFromLayoutTokensAsOffsets(List<LayoutToken> tokens) {
        List<String> stringList = tokens
            .stream()
            .map(LayoutToken::getText)
            .collect(Collectors.toList());

        return getSentencesAsOffsets(stringList);
    }

    /**
     * Returns the sentence boundaries as index based on the input layout token.
     * the Right() element will be exclusive, as any java stuff.
     */
    public List<Pair<Integer, Integer>> getSentencesAsIndex(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return new ArrayList<>();
        }

        List<OffsetPosition> sentencesAsOffsets = getSentencesFromLayoutTokensAsOffsets(tokens);

        int pairIndex = 0;

        int sentenceOffsetStart = sentencesAsOffsets.get(pairIndex).start;
        int sentenceOffsetEnd = sentencesAsOffsets.get(pairIndex).end;

        List<Pair<Integer, Integer>> results = new ArrayList<>();

        if (sentencesAsOffsets.size() == 1) {
            return Collections.singletonList(Pair.of(0, tokens.size()));
        }

        boolean beginning = true;
        int indexStart = -1;
        int indexEnd = -1;
        for (int i = 0; i < tokens.size(); i++) {
            LayoutToken layoutToken = tokens.get(i);
            if (layoutToken.getOffset() == sentenceOffsetStart) {
                if (StringUtils.equals(layoutToken.getText(), " ") && !beginning) {
                    // If the first element is a space I move it back to the previous sentence.
                    Pair<Integer, Integer> previousSentence = Iterables.getLast(results);
                    results.remove(results.size() - 1);
                    results.add(Pair.of(previousSentence.getLeft(), previousSentence.getRight() + 1));
                    indexStart = i + 1;
                    continue;
                }
                beginning = false;
                indexStart = i;
            }

            if (layoutToken.getOffset() == sentenceOffsetEnd) {
                indexEnd = i + 1;
                results.add(Pair.of(indexStart, indexEnd));

                if (pairIndex < sentencesAsOffsets.size() - 1) {
                    pairIndex++;
                    sentenceOffsetStart = sentencesAsOffsets.get(pairIndex).start;
                    sentenceOffsetEnd = sentencesAsOffsets.get(pairIndex).end;
                }
            }
        }

        return results;
    }

    public List<List<LayoutToken>> getSentencesAsLayoutToken(List<LayoutToken> tokens) {

        List<OffsetPosition> offsetPairs = getSentencesFromLayoutTokensAsOffsets(tokens);

        List<List<LayoutToken>> result = new ArrayList<>();

        int pairIndex = 0;

        int sentenceOffsetStart = offsetPairs.get(pairIndex).start;
        int sentenceOffsetEnd = offsetPairs.get(pairIndex).end;

        List<LayoutToken> sentence = new ArrayList<>();
        for (LayoutToken layoutToken : tokens) {

            if (layoutToken.getOffset() == sentenceOffsetStart) {
                if (StringUtils.equals(layoutToken.getText(), " ") && isNotEmpty(sentence)) {
                    sentence.add(layoutToken);
                    sentence = new ArrayList<>();
                    continue;
                }
                sentence = new ArrayList<>();
            }

            sentence.add(layoutToken);

            if (layoutToken.getOffset() == sentenceOffsetEnd) {
                result.add(sentence);
                if (pairIndex < offsetPairs.size() - 1) {
                    pairIndex++;
                    sentenceOffsetStart = offsetPairs.get(pairIndex).start;
                    sentenceOffsetEnd = offsetPairs.get(pairIndex).end;
                }
            }
        }

        return result;
    }
}
