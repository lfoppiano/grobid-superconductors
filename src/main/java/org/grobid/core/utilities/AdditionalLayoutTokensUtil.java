package org.grobid.core.utilities;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Extension of LayoutTokenUtil from Grobid. Eventually we could push some generic methods upstream
 */
public class AdditionalLayoutTokensUtil {

    public static int getLayoutTokenListStartOffset(List<LayoutToken> tokens) {
        if (isEmpty(tokens)) {
            return 0;
        }

        LayoutToken firstToken = tokens.get(0);
        return firstToken.getOffset();
    }

    public static int getLayoutTokenListEndOffset(List<LayoutToken> tokens) {
        if (isEmpty(tokens)) {
            return 0;
        }

        LayoutToken lastToken = tokens.get(tokens.size() - 1);
        return lastToken.getOffset() + lastToken.getText().length();
    }

    public static int getLayoutTokenEndOffset(LayoutToken layoutToken) {
        return layoutToken.getOffset() + layoutToken.getText().length();
    }

    public static int getLayoutTokenStartOffset(LayoutToken layoutToken) {
        return layoutToken.getOffset();
    }

    /**
     * Get the index of the layout token referring to the to the startOffset, endOffset tokens in the
     * supplied token list.
     * <p>
     * The returned are (start, end) with end excluded (same as usual java stuff).
     */
    public static Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int startOffset, int endOffset) {

        if (isEmpty(tokens)) {
            return Pair.of(0, 0);
        }

        if (startOffset > getLayoutTokenListEndOffset(tokens) || endOffset < getLayoutTokenListStartOffset(tokens)) {
            throw new IllegalArgumentException("StartOffset and endOffset are outside the offset boundaries of the layoutTokens. ");
        }
        int start = 0;
        int end = tokens.size() - 1;

        List<LayoutToken> centralTokens = tokens.stream()
            .filter(layoutToken -> (layoutToken.getOffset() >= startOffset && getLayoutTokenEndOffset(layoutToken) <= endOffset)
                    || (layoutToken.getOffset() >= startOffset && layoutToken.getOffset() < endOffset
                    || (getLayoutTokenEndOffset(layoutToken) > startOffset && getLayoutTokenEndOffset(layoutToken) < endOffset)
                )
            )
            .collect(Collectors.toList());

        int layoutTokenIndexStart = start;
        int layoutTokenIndexEnd = end;

        if (isNotEmpty(centralTokens)) {
            layoutTokenIndexStart = tokens.indexOf(centralTokens.get(0));
            layoutTokenIndexEnd = tokens.indexOf(Iterables.getLast(centralTokens));
        }

        // Making it exclusive as any java stuff
        return Pair.of(layoutTokenIndexStart, layoutTokenIndexEnd + 1);
    }

    /**
     * use getExtremitiesAsIndex(List<LayoutToken> tokens, int startOffset, int endOffset)
     **/
    @Deprecated
    protected static Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower, int centroidOffsetHigher, int windowlayoutTokensSize) {
        int start = 0;
        int end = tokens.size() - 1;

        List<LayoutToken> centralTokens = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidOffsetLower || (layoutToken.getOffset() > centroidOffsetLower && layoutToken.getOffset() < centroidOffsetHigher)).collect(Collectors.toList());

        if (isNotEmpty(centralTokens)) {
            int centroidLayoutTokenIndexStart = tokens.indexOf(centralTokens.get(0));
            int centroidLayoutTokenIndexEnd = tokens.indexOf(centralTokens.get(centralTokens.size() - 1));

            if (centroidLayoutTokenIndexStart > windowlayoutTokensSize) {
                start = centroidLayoutTokenIndexStart - windowlayoutTokensSize;
            }
            if (end - centroidLayoutTokenIndexEnd > windowlayoutTokensSize) {
                end = centroidLayoutTokenIndexEnd + windowlayoutTokensSize + 1;
            }
        }

        return new ImmutablePair<>(start, end);
    }

    public static List<Pair<Integer, Integer>> fromOffsetsToIndexesOfTokensWithoutSpaces(List<OffsetPosition> offsets, List<String> tokensWithoutSpaces) {
        if (isEmpty(offsets) || offsets.size() == 1) {
            return Arrays.asList(Pair.of(0, tokensWithoutSpaces.size()));
        }

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

        return resultIndexes;
    }


    public static List<Pair<Integer, Integer>> fromOffsetsToIndexesOfTokensWithSpaces(List<OffsetPosition> offsets, List<String> tokensWithSpaces) {
        if (isEmpty(offsets) || offsets.size() == 1) {
            return Arrays.asList(Pair.of(0, tokensWithSpaces.size()));
        }

        int offsetId = 0;
        OffsetPosition offset = offsets.get(offsetId);

        List<Pair<Integer, Integer>> resultIndexes = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        int idxStart = 0;
        int idxEnd = 0;
        for (int idx = 0; idx < tokensWithSpaces.size(); idx++) {
            String token = tokensWithSpaces.get(idx);

            int offsetStart = offset.start;
            int offsetEnd = offset.end;

            int tokenCumulatedStart = sb.toString().length();
            sb.append(token);
            int tokenCumulatedEnd = sb.toString().length();

            if (tokenCumulatedEnd <= offsetEnd) {
                idxEnd = idx;
            } else {
                if (offsetId == offsets.size() - 1) {
                    resultIndexes.add(Pair.of(idxStart, idx));
                } else {
                    resultIndexes.add(Pair.of(idxStart, idx));
                    offsetId++;
                    idxStart = idx;
                    offset = offsets.get(offsetId);
                }
            }
        }

        if (offsetId == offsets.size() - 1) {
            Pair<Integer, Integer> lastSentence = resultIndexes.get(resultIndexes.size() - 1);
            if (!(lastSentence.getLeft() == idxStart || lastSentence.getRight() >= idxStart)) {
                resultIndexes.add(Pair.of(idxStart, idxEnd));
            }
        }

        return resultIndexes;
    }
}
