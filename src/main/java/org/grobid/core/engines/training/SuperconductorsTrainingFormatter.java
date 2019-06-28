package org.grobid.core.engines.training;

import nu.xom.Element;
import org.grobid.core.data.Superconductor;

import java.util.List;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;

public class SuperconductorsTrainingFormatter {

    protected Element trainingExtraction(List<Superconductor> superconductorList, String text) {
        Element p = teiElement("p");

        int pos = 0;
        for (Superconductor superconductor : superconductorList) {

            int start = superconductor.getOffsetStart();
            int end = superconductor.getOffsetEnd();

            String name = superconductor.getName();
            Element supercon = teiElement(superconductor.getType());
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
