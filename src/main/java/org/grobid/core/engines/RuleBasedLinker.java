package org.grobid.core.engines;

import com.google.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.data.Link;
import org.grobid.core.data.Span;
import org.grobid.core.data.TextPassage;
import org.grobid.core.data.Token;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.utilities.LinkingModuleClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

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

    public TextPassage markTemperatures(TextPassage passage) {
        List<Span> originalSpans = passage.getSpans();
        if (CollectionUtils.isEmpty(originalSpans) || disabled) {
            return passage;
        }

        TextPassage requestPassage = createNewCleanObject(passage);
        TextPassage textPassageWithMarkedTemperatures = this.client.markCriticalTemperature(requestPassage);
        List<Span> processedSpans = textPassageWithMarkedTemperatures.getSpans();
        
        //Copy linkability
        if (originalSpans.size() != processedSpans.size()) {
            LOGGER.warn("The size of the processed spans is different than the original spans. Skipping linking");
        } else {
            for (int i = 0; i < originalSpans.size(); i++) {
                Span processedSpan = processedSpans.get(i);
                Span originalSpan = originalSpans.get(i);
                if (processedSpan.getId().equals(originalSpan.getId())) {
                    originalSpan.setLinkable(processedSpan.isLinkable());
                }
            }
        }
        return passage;
    }


    public TextPassage process(TextPassage passage){
        return process(passage, Arrays.asList(Link.MATERIAL_TCVALUE_TYPE, Link.TCVALUE_PRESSURE_TYPE, Link.TCVALUE_ME_METHOD_TYPE), false);    
    }

    public TextPassage process(TextPassage passage, List<String> linkTypes){
        return process(passage, linkTypes, false);
    }
    
    /**
     * Apply the linking to a text passage
     * 
     * @param passage the text passage to be processed
     * @param linkTypes type of links to be processed
     * @param skipClassification indicate if to skip the classification of tc 
     * @return
     */
    public TextPassage process(TextPassage passage, List<String> linkTypes, boolean skipClassification) {
        if (CollectionUtils.isEmpty(passage.getSpans()) || disabled) {
            return passage;
        }

        TextPassage requestPassage = createNewCleanObject(passage);

        //TODO: process in bulk asynchronously 
        TextPassage responseTextPassage = client.extractLinks(requestPassage, linkTypes, skipClassification);
        
        //Fetch only what is needed
        if (responseTextPassage.getSpans().size() != passage.getSpans().size()) {
            LOGGER.warn("The size of the processed spans is different than the original spans. Skipping linking");
        } else {
            for (int i = 0; i < passage.getSpans().size(); i++) {
                Span responseSpan = responseTextPassage.getSpans().get(i);
                Span originalSpan = passage.getSpans().get(i);
                if (responseSpan.getId().equals(originalSpan.getId())) {
                    originalSpan.getLinks().addAll(responseSpan.getLinks());
                }
            }
        }
        passage.setRelationships(responseTextPassage.getRelationships());

        return passage;
    }

    private TextPassage createNewCleanObject(TextPassage passage) {
        TextPassage requestPassage = new TextPassage();
        List<Span> newSpans = passage.getSpans().stream()
            .map(s -> { Span newSpan = new Span(s);
                newSpan.setBoundingBoxes(new ArrayList<>());
                newSpan.setLinks(new ArrayList<>());
                return newSpan;
            })
            .collect(Collectors.toList());

        requestPassage.setSpans(newSpans);
        requestPassage.setTokens(new ArrayList<>(passage.getTokens()));
        return requestPassage;
    }
}
