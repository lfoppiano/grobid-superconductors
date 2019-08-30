package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SentenceSegmenter {

    private Tokenizer tokenizer;

    public SentenceSegmenter() {
        tokenizer = new EnglishTokenizer();
    }


    public List<List<Token>> getSentences(List<LayoutToken> tokens) {
        List<Token> tokensNlp4j = tokens
                .stream()
                .map(token -> {
                    Token token1 = new Token(token.getText());
                    token1.setStartOffset(token.getOffset());
                    token1.setEndOffset(token.getOffset() + token.getText().length());
                    return token1;
                })
                .collect(Collectors.toList());

        return tokenizer.segmentize(tokensNlp4j);
    }

    public List<Pair<Integer, Integer>> getSentencesAsOffsetsPairs(List<LayoutToken> tokens) {
        List<List<Token>> sentences = getSentences(tokens);

        return sentences.stream()
                .map(s -> Pair.of(s.get(0).getStartOffset(), Iterables.getLast(s).getStartOffset()))
                .collect(Collectors.toList());

    }

    /**
     * Returns the sentence boundaries as index based on the input layout token.
     * the Right() element will be exclusive, as any java stuff.
     */
    public List<Pair<Integer, Integer>> getSentencesAsIndex(List<LayoutToken> tokens) {
        if(CollectionUtils.isEmpty(tokens)) {
            return new ArrayList<>();
        }

        List<Pair<Integer, Integer>> offsetPairs = getSentencesAsOffsetsPairs(tokens);

        int pairIndex = 0;

        int sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
        int sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();

        List<Pair<Integer, Integer>> results = new ArrayList<>();

        if (offsetPairs.size() == 1) {
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

                if (pairIndex < offsetPairs.size() - 1) {
                    pairIndex++;
                    sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
                    sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();
                }
            }
        }

        return results;
    }

    public List<List<LayoutToken>> getSentencesAsLayoutToken(List<LayoutToken> tokens) {

        List<Pair<Integer, Integer>> offsetPairs = getSentencesAsOffsetsPairs(tokens);

        List<List<LayoutToken>> result = new ArrayList<>();

        int pairIndex = 0;

        int sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
        int sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();

        List<LayoutToken> sentence = null;
        for (LayoutToken layoutToken : tokens) {

            if (layoutToken.getOffset() == sentenceOffsetStart) {
                if (StringUtils.equals(layoutToken.getText(), " ") && sentence != null) {
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
                    sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
                    sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();
                }
            }
        }

        return result;
    }
}
