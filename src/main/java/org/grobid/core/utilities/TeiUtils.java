package org.grobid.core.utilities;

import com.google.common.collect.Iterables;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;

public class TeiUtils {

    public static Element getTeiHeader(int id) {
        Element tei = teiElement("tei");
        Element teiHeader = teiElement("teiHeader");

        if (id == -1) {
            id = 1;
        }
        Element fileDesc = teiElement("fileDesc");
        fileDesc.addAttribute(new Attribute("xml:id", "http://www.w3.org/XML/1998/namespace", "_" + id));
        teiHeader.appendChild(fileDesc);
        Element publicationStmt = generatePublicationStmt();
        fileDesc.appendChild(publicationStmt);

        // tei/teiHeader/encodingDesc
        Element encodingDesc = teiElement("encodingDesc");
        Element appInfo = createAppInfoElement();
        encodingDesc.appendChild(appInfo);

        // tei/teiHeader/profileDesc
        Element profileDesc = teiElement("profileDesc");

        teiHeader.appendChild(encodingDesc);
        teiHeader.appendChild(profileDesc);
        tei.appendChild(teiHeader);

        return tei;
    }

    private static Element generatePublicationStmt() {
        Element publicationStmt = teiElement("publicationStmt");
        Element publisher = teiElement("publisher", "National Institute for Materials Science (NIMS), Tsukuba, Japan");
        publicationStmt.appendChild(publisher);
        Element availability = teiElement("availability");
        publicationStmt.appendChild(availability);
        Element licence = teiElement("licence");
        licence.addAttribute(new Attribute("target", "http://creativecommons.org/licenses/by/3.0/"));
        availability.appendChild(licence);
        licence.appendChild(teiElement("p", "The Creative Commons Attribution 3.0 Unported (CC BY 3.0) " +
            "Licence applies to this document."));
        return publicationStmt;
    }

    private static Element createAppInfoElement() {
        Element appInfo = teiElement("appInfo");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String dateISOString = df.format(new java.util.Date());

        Element application = teiElement("application");
        application.addAttribute(new Attribute("version", GrobidSuperconductorsConfiguration.getVersion()));
        application.addAttribute(new Attribute("ident", "grobid-superconductors"));
        application.addAttribute(new Attribute("when", dateISOString));

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("target", "https://github.com/lfoppiano/grobid-superconductors"));
        ref.appendChild("A machine learning software for extracting materials and their properties from " +
            "scientific literature.");
        application.appendChild(ref);
        appInfo.appendChild(application);
        return appInfo;
    }

    public static Element getElement(Element root, String elementName) {
        Elements childElements = root.getChildElements();
        List<Element> foundElements = new ArrayList<>();
        childElements.forEach(e -> {
            if (e.getLocalName().equals(elementName)) {
                foundElements.add(e);
            }
        });
        Element first = Iterables.getFirst(foundElements, null);
        return first;

    }

}
