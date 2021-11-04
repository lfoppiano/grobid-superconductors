package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.document.TextPassage;
import org.grobid.core.data.material.Material;
import org.grobid.core.utilities.LabelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

public class TabularDataEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabularDataEngine.class);
    
    /**
     * Extract entities from a paragraph / sentence
     */
    public static List<SuperconEntry> extractEntities(List<TextPassage> paragraphs) {
        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();
        for (TextPassage paragraph : paragraphs) {
            for (Span span : paragraph.getSpans()) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }
        // Materials
        List<Span> materials = spansById.values().stream()
            .filter(span -> span.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());

        List<SuperconEntry> outputCSV = new ArrayList<>();

        for (Span m : materials) {
            SuperconEntry dbEntry = new SuperconEntry();
            dbEntry.setRawMaterial(m.getText());
            dbEntry.setSection(sectionsById.get(m.getId()).getLeft());
            dbEntry.setSubsection(sectionsById.get(m.getId()).getRight());
            dbEntry.setSentence(sentenceById.get(m.getId()));

            List<SuperconEntry> entriesWithAttributes = Material.processAttributes(m, dbEntry);

            Map<String, String> linkedToMaterial = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));

            if (isNotEmpty(linkedToMaterial.entrySet())) {
                for (Map.Entry<String, String> entry : linkedToMaterial.entrySet()) {
                    Span tcValue = spansById.get(entry.getKey());
                    dbEntry.setCriticalTemperature(tcValue.getText());
                    List<Span> pressures = tcValue.getLinks().stream()
                        .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                        .map(l -> spansById.get(l.getTargetId()))
                        .collect(Collectors.toList());

                    if (isNotEmpty(pressures)) {
                        boolean first = true;
                        for (Span pressure : pressures) {
                            if (first) {
                                dbEntry.setAppliedPressure(pressure.getText());
                                outputCSV.add(dbEntry);
                                first = false;
                            } else {
                                try {
                                    SuperconEntry newEntry = dbEntry.clone();
                                    newEntry.setAppliedPressure(pressure.getText());
                                    newEntry.setLinkType(entry.getValue());
                                } catch (CloneNotSupportedException e) {
                                    LOGGER.error("Cannot create a duplicate of the supercon entry: " + dbEntry.getRawMaterial() + ". ", e);
                                    break;
                                }
                            }
                        }
                    } else {
                        outputCSV.add(dbEntry);
                    }

                }
            } else {
                outputCSV.add(dbEntry);
            }
        }
        return outputCSV;
    }

    /**
     * Compute passage to convert into CSV file
     */
    //TODO: compute document information here and not in the workflow processor 
    public static List<SuperconEntry> computeTabularData(List<TextPassage> paragraphs) {
        Map<String, Span> spansById = new HashMap<>();
        Map<String, String> sentenceById = new HashMap<>();
        Map<String, Pair<String, String>> sectionsById = new HashMap<>();

        for (TextPassage paragraph : paragraphs) {
            List<Span> linkedSpans = paragraph.getSpans().stream()
                .filter(s -> s.getLinks().size() > 0)
                .collect(Collectors.toList());

            for (Span span : linkedSpans) {
                spansById.put(span.getId(), span);
                sentenceById.put(span.getId(), paragraph.getText());
                sectionsById.put(span.getId(), Pair.of(paragraph.getSection(), paragraph.getSubSection()));
            }
        }

        // Process materials (with links)
        List<Span> materials = spansById.values().stream()
            .filter(span -> span.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL))
            .collect(Collectors.toList());

        List<SuperconEntry> outputCSV = new ArrayList<>();
        for (Span m : materials) {
            SuperconEntry dbEntry = new SuperconEntry();
            dbEntry.setRawMaterial(m.getText());
            dbEntry.setSection(LabelUtils.getPlainLabelName(sectionsById.get(m.getId()).getLeft()));
            dbEntry.setSubsection(LabelUtils.getPlainLabelName(sectionsById.get(m.getId()).getRight()));
            dbEntry.setSentence(sentenceById.get(m.getId()));

            List<SuperconEntry> entriesWithAttachedAttributes = Material.processAttributes(m, dbEntry);

            // outputs target material id with linking methods (crf, vicinity, etc..) aggregated as list of strings 
            Map<String, String> linkTypesGroupedByMaterialId = m.getLinks().stream()
                .map(l -> Pair.of(l.getTargetId(), l.getType()))
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, joining(", "))));
            
            // Process TC
            boolean firstTemp = true;
            for (Map.Entry<String, String> entry : linkTypesGroupedByMaterialId.entrySet()) {
                Span linkedSpan = spansById.get(entry.getKey());
                if (linkedSpan.getType().equals(SUPERCONDUCTORS_TC_VALUE_LABEL)) {
                    List<SuperconEntry> entriesRelatedToThisTc = new ArrayList<>();
                    if (firstTemp) {
                        entriesWithAttachedAttributes.stream()
                            .forEach(dbE -> {
                                dbE.setCriticalTemperature(linkedSpan.getText());
                                entriesRelatedToThisTc.add(dbE);
                            });
                        firstTemp = false;
                    } else {
                        List<SuperconEntry> newClones = new ArrayList<>();
                        for (SuperconEntry ewa : entriesWithAttachedAttributes) {
                            try {
                                SuperconEntry anotherNewDbEntry = ewa.clone();
                                anotherNewDbEntry.setCriticalTemperature(linkedSpan.getText());
                                anotherNewDbEntry.setLinkType(entry.getValue());
                                newClones.add(anotherNewDbEntry);
                            } catch (CloneNotSupportedException e) {
                                LOGGER.error("Cannot create a duplicate of the supercon entry: " + ewa.getRawMaterial() + ". ", e);
                            }
                        }
                        entriesRelatedToThisTc.addAll(newClones);
                    }


                    //Process pressures - only linked to what's linked to material

                    List<Span> pressureLinkedToTheCurrentTc = linkedSpan.getLinks().stream()
                        .filter(l -> l.getTargetType().equals(SUPERCONDUCTORS_PRESSURE_LABEL))
                        .map(l -> spansById.get(l.getTargetId()))
                        .collect(Collectors.toList());

                    if (isNotEmpty(pressureLinkedToTheCurrentTc)) {
                        boolean first = true;
                        for (Span pressure : pressureLinkedToTheCurrentTc) {
                            if (first) {
                                entriesRelatedToThisTc.stream()
                                    .forEach(dbE -> dbE.setAppliedPressure(pressure.getText()));
                                first = false;
                            } else {
                                List<SuperconEntry> newClones = new ArrayList<>();
                                for (SuperconEntry ewa : entriesRelatedToThisTc) {
                                    try {
                                        SuperconEntry anotherNewDbEntry = ewa.clone();
                                        anotherNewDbEntry.setAppliedPressure(pressure.getText());
                                        newClones.add(anotherNewDbEntry);
                                    } catch (CloneNotSupportedException e) {
                                        LOGGER.error("Cannot create a duplicate of the supercon entry: " + ewa.getRawMaterial() + ". ", e);
                                    }
                                }
                                entriesRelatedToThisTc.addAll(newClones);
                            }
                        }
                    }
                    outputCSV.addAll(entriesRelatedToThisTc);
                }
            }
            
        }
        return outputCSV;
    }
}
