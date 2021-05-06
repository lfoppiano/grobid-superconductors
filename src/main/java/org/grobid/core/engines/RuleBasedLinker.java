package org.grobid.core.engines;

import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.data.Span;
import org.grobid.core.data.TextPassage;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.LinkingModuleClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

@Singleton
public class RuleBasedLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedLinker.class);

    private GrobidSuperconductorsConfiguration configuration;
    private final LinkingModuleClient client;
    private boolean disabled = false;

    @Inject
    public RuleBasedLinker(GrobidSuperconductorsConfiguration configuration, LinkingModuleClient client) {
        this.configuration = configuration;
        this.client = client;
    }

    public TextPassage markTemperatures(TextPassage paragraph) {
        List<Span> originalSpans = paragraph.getSpans();
        if (CollectionUtils.isEmpty(originalSpans) || disabled) {
            return paragraph;
        }

        //Take out the bounding boxes
        Map<String, List<BoundingBox>> backupBoundingBoxes = new HashMap<>();
        originalSpans.forEach(s -> {
            backupBoundingBoxes.put(String.valueOf(s.getId()), s.getBoundingBoxes());
            s.setBoundingBoxes(new ArrayList<>());
        });

        //Take out the attributes
        Map<String, Map<String, String>> backupAttributes = new HashMap<>();
        originalSpans.forEach(s -> backupAttributes.put(String.valueOf(s.getId()), s.getAttributes()));

        TextPassage textPassageWithMarkedTemperatures = this.client.markCriticalTemperature(paragraph);

        // put the bounding boxes back where they were
        List<Span> processedSpans = textPassageWithMarkedTemperatures.getSpans();
        originalSpans.stream()
            .forEach(s -> {
                s.setBoundingBoxes(backupBoundingBoxes.get(String.valueOf(s.getId())));
                s.setAttributes(backupAttributes.get(String.valueOf(s.getId())));
            });

        //Copy linkability
        if (originalSpans.size() != processedSpans.size()) {
            LOGGER.info("The size of the processed spans is different than the original spans. Skipping linking");
        } else {
            for (int i = 0; i < originalSpans.size(); i++) {
                Span processedSpan = processedSpans.get(i);
                Span originalSpan = originalSpans.get(i);
                if (processedSpan.getId().equals(originalSpan.getId())) {
                    originalSpan.setLinkable(processedSpan.isLinkable());
                }
            }
        }
        return paragraph;
    }


    public List<TextPassage> process(TextPassage paragraph) {
        if (CollectionUtils.isEmpty(paragraph.getSpans()) || disabled) {
            return Collections.singletonList(paragraph);
        }

        //Take out the bounding boxes
        Map<String, List<BoundingBox>> backupBoundingBoxes = new HashMap<>();
        paragraph.getSpans().forEach(s -> {
            backupBoundingBoxes.put(String.valueOf(s.getId()), s.getBoundingBoxes());
            s.setBoundingBoxes(new ArrayList<>());
        });

        //Take out the attributes
        Map<String, Map<String, String>> backupAttributes = new HashMap<>();
        paragraph.getSpans().forEach(s -> backupAttributes.put(String.valueOf(s.getId()), s.getAttributes()));

        List<TextPassage> textPassages = client.extractLinks(paragraph);


        // put the bounding boxes back where they were
        textPassages.stream()
            .forEach(p -> p.getSpans().stream()
                .forEach(s -> {
                    s.setBoundingBoxes(backupBoundingBoxes.get(String.valueOf(s.getId())));
                    s.setAttributes(backupAttributes.get(String.valueOf(s.getId())));
                }));


//            Map<String, Span> spans = new HashMap<>();
//
//            processedTcPressure.stream()
//                .forEach(p -> p.getSpans().stream()
//                    .forEach(s -> {
//                        s.setBoundingBoxes(backupBoundingBoxes.get(String.valueOf(s.getId())));
//                        s.setAttributes(backupAttributes.get(String.valueOf(s.getId())));
//                        spans.put(s.getId(), s);
//                    }));
//
//            processedMaterialTc.stream().forEach(p -> {
//                p.setSection(paragraph.getSection());
//                p.setSubSection(paragraph.getSubSection());
//
//                String type = processedMaterialTc.size() == 1 ? "paragraph" : "sentence";
//                p.setType(type);
//
//                p.getSpans().stream().forEach(s -> {
//                    Span correspondingSpan = spans.get(s.getId());
//                    if (correspondingSpan != null) {
//                        List<Link> collect = correspondingSpan.getLinks().stream()
//                            .filter(f -> !f.getType().equals("crf"))
//                            .collect(Collectors.toList());
//                        s.addLinks(collect);
//                    }
//                });
//            });

        return textPassages;

    }
}
