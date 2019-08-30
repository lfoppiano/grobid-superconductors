package org.grobid.core.engines.training;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.internal.bytebuddy.implementation.bind.annotation.Super;
import org.grobid.core.data.Superconductor;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.utilities.TeiUtils;

import java.util.List;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.wipo.analyzers.wipokr.utils.StringUtil.length;
import static org.wipo.analyzers.wipokr.utils.StringUtil.substring;

public class SuperconductorsTrainingXMLFormatter implements SuperconductorsOutputFormattter {

    @Override
    public String format(List<Pair<List<Superconductor>, String>> labeledTextList, int id) {
        Element textNode = teiElement("text");
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        for (Pair<List<Superconductor>, String> labeledText : labeledTextList) {
            textNode.appendChild(trainingExtraction(labeledText.getLeft(), labeledText.getRight()));
        }

        Element quantityDocumentRoot = TeiUtils.getTeiHeader(id);
        quantityDocumentRoot.appendChild(textNode);

        return XmlBuilderUtils.toXml(quantityDocumentRoot);
    }

    protected Element trainingExtraction(List<Superconductor> superconductorList, String text) {
        Element p = teiElement("p");

        int pos = 0;
        for (Superconductor superconductor : superconductorList) {

            int start = superconductor.getOffsetStart();
            int end = superconductor.getOffsetEnd();

            String name = superconductor.getName();
            Element supercon = teiElement(substring(superconductor.getType(), 1, length(superconductor.getType()) - 1));
            supercon.appendChild(name);

            int initPos = pos;
            int firstPos = pos;
            while (pos < text.length()) {
                if (pos == start) {
                    if (initPos == firstPos) {
                        p.appendChild(text.substring(firstPos, start));
                    } else {
                        supercon.appendChild(text.substring(initPos, start));
                    }

                    pos = end;
                    initPos = pos;
                }

                if (pos >= end)
                    break;
                pos++;
            }

            p.appendChild(supercon);
        }
        p.appendChild(text.substring(pos));

        return p;
    }
}
