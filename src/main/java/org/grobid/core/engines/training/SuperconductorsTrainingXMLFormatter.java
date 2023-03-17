package org.grobid.core.engines.training;

import com.google.common.collect.Iterables;
import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.document.DocumentBlock;
import org.grobid.core.data.document.Span;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.SuperconductorsTeiUtils;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.utilities.TextUtilities.restrictedPunctuations;

public class SuperconductorsTrainingXMLFormatter implements SuperconductorsOutputFormattter {

    @Override
    public String format(List<DocumentBlock> documentBlocks, int id) {
        Element outputDocumentRoot = SuperconductorsTeiUtils.getTeiHeader(id);

        Element teiHeader = SuperconductorsTeiUtils.getElement(outputDocumentRoot, "teiHeader");
        Element fileDesc = SuperconductorsTeiUtils.getElement(teiHeader, "fileDesc");

        Element profileDesc = SuperconductorsTeiUtils.getElement(teiHeader, "profileDesc");

        Element textNode = teiElement("text");
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        Element body = teiElement("body");

//        Map<String, List<DocumentBlock>> byParagraphs = documentBlocks.stream()
//            .collect(Collectors.groupingBy(DocumentBlock::getParagraphId));

        String previousParagraphId = null;
        String previousSection = "NO_SECTION";
        Element previousParent = null;
        Element parent = null;
        for (DocumentBlock block : documentBlocks) {
            String paragraphId = block.getGroupId();
            if (block.getSection().equals(DocumentBlock.SECTION_BODY)) {
                if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_FIGURE)) {
                    parent = getParentElement(body, previousParagraphId, paragraphId, previousParent, "ab", Pair.of("type", "figureCaption"));
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                } else if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_TABLE)) {
                    parent = getParentElement(body, previousParagraphId, paragraphId, previousParent, "ab", Pair.of("type", "tableCaption"));
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                } else if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_PARAGRAPH)) {
                    parent = getParentElement(body, previousParagraphId, paragraphId, previousParent, "p", null);
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                } else if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_TITLE_SECTION)) {
                    parent = getParentElement(body, previousParagraphId, paragraphId, previousParent, "head", null);
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                } else {
                    parent = getParentElement(body, previousParagraphId, paragraphId, previousParent, "p", null);
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                }
            } else if (block.getSection().equals(DocumentBlock.SECTION_HEADER)) {
                if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_TITLE)) {
                    Element titleStatement = teiElement("titleStmt");
                    Element title = trainingExtraction(block.getSpans(), block.getLayoutTokens(), "title");
                    titleStatement.appendChild(title);
                    fileDesc.insertChild(titleStatement, 0);
                } else if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_KEYWORDS)) {
                    Element abKeywords = SuperconductorsTeiUtils.getElement(profileDesc, "ab");
                    if (abKeywords == null) {
                        abKeywords = trainingExtraction(block.getSpans(), block.getLayoutTokens(), "ab", Pair.of("type", "keywords"));
                        profileDesc.appendChild(abKeywords);
                    } else {
                        throw new RuntimeException("new keywords, but no space for them... ");
                    }
                } else if (block.getSubSection().equals(DocumentBlock.SUB_SECTION_ABSTRACT)) {
                    Element abstractElement = SuperconductorsTeiUtils.getElement(profileDesc, "abstract");
                    if (abstractElement == null) {
                        abstractElement = teiElement("abstract");
                        profileDesc.appendChild(abstractElement);
                    }
                    parent = getParentElement(abstractElement, previousParagraphId, paragraphId, previousParent, "p", null);
                    parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens(), "s"));
                } else {
                    throw new RuntimeException("The section or subsection have the wrong name. " +
                        "This will cause loss of data in the output generated files. Section name: " + block.getSection() +
                        ", " + block.getSubSection());
                }
            } else if (block.getSection().equals(DocumentBlock.SECTION_ANNEX)) {
                if (!StringUtils.equals(paragraphId, previousParagraphId)) {
                    parent = teiElement("p");
                    body.appendChild(parent);
                } else {
                    parent = previousParent;
                }
                parent.appendChild(trainingExtraction(block.getSpans(), block.getLayoutTokens()));
            } else {
                throw new RuntimeException("The section or subsection have the wrong name. " +
                    "This will cause loss of data in the output generated files. Section name: " + block.getSection() +
                    ", " + block.getSubSection());
            }
            previousParent = parent;
            previousParagraphId = paragraphId;
        }

        textNode.appendChild(body);
        outputDocumentRoot.appendChild(textNode);
        return XmlBuilderUtils.toXml(outputDocumentRoot);
    }

    /**
     * Create the parent element or, if under certain conditions, recycle the previous one.
     */
    protected Element getParentElement(Element body, String previousParagraphId, String paragraphId, Element previousParent, String parentTagName, Pair<String, String> attributes) {
        Element parent = null;

        if (previousParent == null || !StringUtils.equals(paragraphId, previousParagraphId)) {
            parent = teiElement(parentTagName);
            if (attributes != null) {
                parent.addAttribute(new Attribute(attributes.getLeft(), attributes.getRight()));
            }
            body.appendChild(parent);
        } else {
            parent = previousParent;
        }
        

        return parent;
    }

    protected Element trainingExtraction(List<Span> spanList, List<LayoutToken> tokens) {
        return trainingExtraction(spanList, tokens, "p");
    }

    protected Element trainingExtraction(List<Span> spanList, List<LayoutToken> tokens, String parentTag) {
        return trainingExtraction(spanList, tokens, parentTag, null);
    }

    protected Element trainingExtraction(List<Span> spanList, List<LayoutToken> tokens, String
        parentTag, Pair<String, String> parentTagAttribute) {
        Element p = teiElement(parentTag);
        if (parentTagAttribute != null) {
            p.addAttribute(new Attribute(parentTagAttribute.getKey(), parentTagAttribute.getValue()));
        }

        LayoutToken first = Iterables.getFirst(tokens, new LayoutToken());
        int startPosition = first != null ? first.getOffset() : 0;
        for (Span superconductor : spanList) {

            int start = superconductor.getOffsetStart();
            int end = superconductor.getOffsetEnd();

            String name = superconductor.getText();
            Element entityElement = teiElement("rs");
            // remove < and > from the material name \o/
//            entityElement.addAttribute(new Attribute("type", prepareType(superconductor.getType())));
            entityElement.addAttribute(new Attribute("type", prepareType(superconductor.getType())));
            entityElement.appendChild(name);

            String contentBefore = LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition, start));
            p.appendChild(contentBefore);
            p.appendChild(entityElement);

            // We stop the process if something doesn't match
            int accumulatedOffset = startPosition + length(contentBefore) + length(name);
            if (end != accumulatedOffset) {
                throw new RuntimeException("Wrong synchronisation between entities and layout tokens. End entity offset: " + end
                    + " different from the expected offset: " + accumulatedOffset);
            }
            startPosition = end;
        }
        String textStripEnd = StringUtils.stripEnd(LayoutTokensUtil.toText(LayoutTokensUtil.subListByOffset(tokens, startPosition)), null);

        //If the last chunk starts with punctuation, I trim the space
        if (StringUtils.containsAny(substring(StringUtils.trim(textStripEnd), 0, 1), restrictedPunctuations)) {
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
