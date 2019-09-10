package org.grobid.core.engines.training;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.internal.bytebuddy.implementation.bind.annotation.Super;
import org.grobid.core.data.Superconductor;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TeiUtils;

import java.util.List;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.wipo.analyzers.wipokr.utils.StringUtil.*;

public class SuperconductorsTrainingXMLFormatter implements SuperconductorsOutputFormattter {

    @Override
    public String format(List<Pair<List<Superconductor>, List<LayoutToken>>> labeledTextList, int id) {
        Element textNode = teiElement("text");
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        for (Pair<List<Superconductor>, List<LayoutToken>> labeledText : labeledTextList) {
            textNode.appendChild(trainingExtraction(labeledText.getLeft(), labeledText.getRight()));
        }

        Element quantityDocumentRoot = TeiUtils.getTeiHeader(id);
        quantityDocumentRoot.appendChild(textNode);

        return XmlBuilderUtils.toXml(quantityDocumentRoot);
    }

    protected Element trainingExtraction(List<Superconductor> superconductorList, List<LayoutToken> tokens) {
        Element p = teiElement("p");
//        int pos = 0;
//        String text = LayoutTokensUtil.toText(tokens);

        int startOffset = 0;
        if (CollectionUtils.isNotEmpty(tokens)) {
            startOffset = tokens.get(0).getOffset();
        }

        int startPosition = 0;
        for (Superconductor superconductor : superconductorList) {

            int start = superconductor.getOffsetStart();
            int end = superconductor.getOffsetEnd();

            String name = superconductor.getName();
            // remove < and > from the material name \o/
            Element supercon = teiElement(prepareType(superconductor.getType()));
            supercon.appendChild(name);

            String contentBefore = LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition, start));
            p.appendChild(contentBefore);
//            if(LayoutTokensUtil.toText(
//                LayoutTokensUtil.subListByOffset(tokens, start,  start+2)).equals(" ")) {
//                p.appendChild(" ");
//            }

//            int initPos = pos;
//            int firstPos = pos;
//            while (pos < text.length()) {
//                if (pos == start) {
//                    if (initPos == firstPos) {
//                        p.appendChild(text.substring(firstPos, start));
//                    } else {
//                        supercon.appendChild(text.substring(initPos, start));
//                    }
//
//                    pos = end;
//                    initPos = pos;
//                }

//                if (pos >= end)
//                    break;
//                pos++;
//            }

            p.appendChild(supercon);
            startPosition = end;
        }
//        p.appendChild(text.substring(pos));

        p.appendChild(StringUtils.trim(LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition))));

        return p;
    }

    private String prepareType(String type) {
        String typeReturn = type;
        if(StringUtils.startsWithIgnoreCase(type, "<")) {
            typeReturn = substring(type, 1, length(type) - 1);
        }
        return typeReturn;
    }
}
