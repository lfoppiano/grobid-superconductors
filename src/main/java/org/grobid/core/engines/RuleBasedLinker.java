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
import java.util.function.Function;
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
        if (CollectionUtils.isEmpty(passage.getSpans()) || disabled) {
            return passage;
        }

        TextPassage requestPassage = createNewCleanObject(passage);
        TextPassage responsePassage = this.client.markCriticalTemperature(requestPassage);

        TextPassage outputPassage = TextPassage.of(passage);
        
        //Copy linkability
        if (responsePassage.getSpans().size() != outputPassage.getSpans().size()) {
            LOGGER.warn("The size of the processed spans is different than the original spans. Skipping linking");
        } else {
            for (int i = 0; i < outputPassage.getSpans().size(); i++) {
                Span processedSpan = responsePassage.getSpans().get(i);
                Span originalSpan = outputPassage.getSpans().get(i);
                if (processedSpan.getId().equals(originalSpan.getId())) {
                    originalSpan.setLinkable(processedSpan.isLinkable());
                }
            }
        }
        
        return outputPassage;
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
        TextPassage responsePassage = client.extractLinks(requestPassage, linkTypes, skipClassification);
        
        TextPassage outputPassage = TextPassage.of(passage);
        
        //Fetch only what is needed
        Map<String, Span> spansMap = responsePassage.getSpans().stream().collect(Collectors.toMap(Span::getId, Function.identity()));
        
        outputPassage.getSpans().stream().forEach(s -> {
            if(spansMap.containsKey(s.getId())) {
                Span span = spansMap.get(s.getId());
                s.getLinks().addAll(span.getLinks());
                s.setLinkable(span.isLinkable());
            }
        });
        outputPassage.setRelationships(responsePassage.getRelationships());

        return outputPassage;
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

        requestPassage.setText(passage.getText());
        requestPassage.setSpans(newSpans);
        requestPassage.setTokens(new ArrayList<>(passage.getTokens()));
        return requestPassage;
    }
}
