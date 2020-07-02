package org.grobid.core.engines.training;

import com.google.common.collect.Iterables;
import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Span;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TeiUtils;

import java.util.List;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.utilities.TextUtilities.restrictedPunctuations;
import static org.wipo.analyzers.wipokr.utils.StringUtil.length;
import static org.wipo.analyzers.wipokr.utils.StringUtil.substring;

public class SuperconductorsTrainingXMLFormatter implements SuperconductorsOutputFormattter {

    @Override
    public String format(List<Pair<List<Span>, List<LayoutToken>>> labeledTextList, int id) {
        Element textNode = teiElement("text");
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        for (Pair<List<Span>, List<LayoutToken>> labeledText : labeledTextList) {
            textNode.appendChild(trainingExtraction(labeledText.getLeft(), labeledText.getRight()));
        }

        Element quantityDocumentRoot = TeiUtils.getTeiHeader(id);
        quantityDocumentRoot.appendChild(textNode);

        return XmlBuilderUtils.toXml(quantityDocumentRoot);
    }

    protected Element trainingExtraction(List<Span> superconductorList, List<LayoutToken> tokens) {
        Element p = teiElement("p");

        int startPosition = Iterables.getFirst(tokens, new LayoutToken()).getOffset();
        for (Span superconductor : superconductorList) {

            int start = superconductor.getOffsetStart();
            int end = superconductor.getOffsetEnd();

            String name = superconductor.getText();
            // remove < and > from the material name \o/
            Element supercon = teiElement(prepareType(superconductor.getType()));
            supercon.appendChild(name);

            String contentBefore = LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition, start));
            p.appendChild(contentBefore);
            p.appendChild(supercon);

            // We stop the process if something doesn't match
            int accumulatedOffset = startPosition + StringUtils.length(contentBefore) + StringUtils.length(name);
            if (end != accumulatedOffset) {
                throw new RuntimeException("Wrong synchronisation between entities and layout tokens. End entity offset: " + end
                    + " different from the expected offset: " + accumulatedOffset);
            }
            startPosition = end;
        }
        String textStripEnd = StringUtils.stripEnd(LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition)), null);

        //If the last chunk starts with punctuation, I trim the space
        if (StringUtils.containsAny(StringUtils.substring(StringUtils.trim(textStripEnd), 0, 1), restrictedPunctuations)) {
            textStripEnd = StringUtils.trim(textStripEnd);
        }
        p.appendChild(textStripEnd);

        return p;
    }

    private String prepareType(String type) {
        String typeReturn = type;
        if (StringUtils.startsWithIgnoreCase(type, "<")) {
            typeReturn = substring(type, 1, length(type) - 1);
        }
        return typeReturn;
    }
}
