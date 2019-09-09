package org.grobid.core.engines.training;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Superconductor;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wipo.analyzers.wipokr.utils.StringUtil.*;

public class SuperconductorsTrainingTSVFormatter implements SuperconductorsOutputFormattter {

    @Override
    public String format(List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList, int id) {
        StringBuilder accumulator = new StringBuilder();

        accumulator.append("#FORMAT=WebAnno TSV 3.2").append("\n");
        accumulator.append("#T_SP=webanno.custom.Supercon|supercon_tag").append("\n\n");

        AtomicInteger annotationId = new AtomicInteger(0);
        int paragraphId = 1;
        AtomicInteger tokenOffset = new AtomicInteger(0);

        for (Pair<List<Superconductor>,  List<LayoutToken>> labeledText : labeledTextList) {
            accumulator.append("\n");
            List<LayoutToken> layoutTokens = labeledText.getRight();
            accumulator.append("#Text=").append(LayoutTokensUtil.toText(layoutTokens)).append("\n");
            accumulator.append(trainingExtraction(labeledText.getLeft(), layoutTokens, paragraphId, annotationId, tokenOffset));
            paragraphId++;
        }


        return accumulator.toString();
    }

    protected String trainingExtraction(List<Superconductor> superconductorList, List<LayoutToken> layoutTokens, int paragraphId,
                                        AtomicInteger annotationId, AtomicInteger tokenOffset) {
        StringBuilder paragraphAccumulator = new StringBuilder();

        int superconductorIdx = 0;
        int tokenId = 1;//tokenOffset.incrementAndGet();
        int lastTokenLayout = tokenOffset.get();

        Superconductor superconductorElement = new Superconductor();
        superconductorElement.setOffsetStart(Integer.MAX_VALUE);
        superconductorElement.setName(" ");

        if (CollectionUtils.isNotEmpty(superconductorList)) {
            superconductorElement = superconductorList.get(superconductorIdx);
            annotationId.incrementAndGet();
        }

        for (LayoutToken layoutToken : layoutTokens) {
            if (isBlank(layoutToken.getText())) {
                tokenId++;
                continue;
            }
            paragraphAccumulator.append(paragraphId).append("-").append(tokenId).append("\t");

            int layoutTokenStart = layoutToken.getOffset();
            int layoutTokenEnd = layoutTokenStart + layoutToken.getText().length();

            int outputTokenStart = tokenOffset.get() + layoutTokenStart;
            int outputTokenEnd = outputTokenStart + layoutToken.getText().length();

            paragraphAccumulator.append(outputTokenStart).append("-")
                    .append(outputTokenEnd).append("\t");

            paragraphAccumulator.append(layoutToken.getText()).append("\t");

            if (layoutTokenEnd < superconductorElement.getOffsetStart()) {
                paragraphAccumulator.append("_").append("\t");
            } else {
                String type = substring(superconductorElement.getType(), 1, length(superconductorElement.getType()) - 1);
                if (layoutTokenStart >= superconductorElement.getOffsetStart()
                        && layoutTokenEnd <= superconductorElement.getOffsetEnd()) {
                    paragraphAccumulator.append(type).append("[").append(annotationId.get()).append("]").append("\t");
                } else if (layoutTokenStart > superconductorElement.getOffsetEnd()) {
                    if (superconductorIdx < superconductorList.size() - 1) {
                        annotationId.incrementAndGet();
                        superconductorIdx++;
                        superconductorElement = superconductorList.get(superconductorIdx);

                        if (layoutTokenEnd < superconductorElement.getOffsetStart()) {
                            paragraphAccumulator.append("_").append("\t");
                        } else if (layoutTokenStart >= superconductorElement.getOffsetStart()
                                && layoutTokenEnd <= superconductorElement.getOffsetEnd()) {
                            paragraphAccumulator.append(type).append("[").append(annotationId.get()).append("]").append("\t");
                        } else {
                            paragraphAccumulator.append("_").append("\t");
                        }
                    } else {
                        paragraphAccumulator.append("_").append("\t");
                    }
                } else {
                    paragraphAccumulator.append("_").append("\t");
                }
            }

            paragraphAccumulator.append("\n");
            lastTokenLayout = outputTokenEnd;
            tokenId++;
        }
        tokenOffset.set(lastTokenLayout + 1);

        return paragraphAccumulator.toString();
    }

}
