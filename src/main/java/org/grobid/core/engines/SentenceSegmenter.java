package org.grobid.core.engines;

import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SentenceSegmenter {

    private Tokenizer tokenizer;

    public SentenceSegmenter() {
        tokenizer = new EnglishTokenizer();
    }

    public List<List<LayoutToken>> getSentences(List<LayoutToken> tokens) {
        List<Token> tokensNlp4j = tokens
                .stream()
                .map(token -> {
                    Token token1 = new Token(token.getText());
                    token1.setStartOffset(token.getOffset());
                    token1.setEndOffset(token.getOffset() + token.getText().length());
                    return token1;
                })
                .collect(Collectors.toList());

        List<List<Token>> segmented = tokenizer.segmentize(tokensNlp4j);
        List<Pair<Integer, Integer>> offsetPairs = segmented.stream()
                .map(s -> Pair.of(s.get(0).getStartOffset(), s.get(s.size() - 1).getStartOffset()))
                .collect(Collectors.toList());

        List<List<LayoutToken>> result = new ArrayList<>();

        int pairIndex = 0;

        int sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
        int sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();

        List<LayoutToken> sentence = null;
        for (LayoutToken layoutToken : tokens) {

            if (layoutToken.getOffset() == sentenceOffsetStart) {
                if(StringUtils.equals(layoutToken.getText(), " ") && sentence != null) {
                    sentence.add(layoutToken);
                    sentence = new ArrayList<>();
                    continue;
                }
                sentence = new ArrayList<>();
            }

            sentence.add(layoutToken);

            if (layoutToken.getOffset() == sentenceOffsetEnd) {
                result.add(sentence);
                if(pairIndex < segmented.size() - 1) {
                    pairIndex++;
                    sentenceOffsetStart = offsetPairs.get(pairIndex).getLeft();
                    sentenceOffsetEnd = offsetPairs.get(pairIndex).getRight();
                }

            }

        }

        return result;
    }
}
