package org.grobid.core.engines.training;

import nu.xom.Attribute;
import nu.xom.Element;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Superconductor;
import org.grobid.core.data.Unit;
import org.grobid.core.utilities.UnitUtilities;

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
            Element supercon = teiElement("supercon");
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

    private Element unitToElement(String text, Unit unit) {
        int startU = unit.getOffsetStart();
        int endU = unit.getOffsetEnd();

        Element unitNode = teiElement("measure");

        if ((unit.getUnitDefinition() != null) && (unit.getUnitDefinition().getType() != null)) {
            unitNode.addAttribute(new Attribute("type", unit.getUnitDefinition().getType().toString()));
        } else {
            unitNode.addAttribute(new Attribute("type", "?"));
        }

        if (unit.getRawName() != null) {
            unitNode.addAttribute(new Attribute("unit", unit.getRawName().trim()));
        } else {
            unitNode.addAttribute(new Attribute("unit", "?"));
        }

        unitNode.appendChild(text.substring(startU, endU));
        return unitNode;
    }
}
