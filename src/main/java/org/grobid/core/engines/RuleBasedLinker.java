package org.grobid.core.engines;

import com.google.inject.Singleton;
import org.grobid.core.data.document.Link;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.document.TextPassage;
import org.grobid.core.utilities.client.LinkingModuleClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class RuleBasedLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedLinker.class);

    private GrobidSuperconductorsConfiguration configuration;
    private final LinkingModuleClient client;

    @Inject
    public RuleBasedLinker(GrobidSuperconductorsConfiguration configuration, LinkingModuleClient client) {
        this.configuration = configuration;
        this.client = client;
    }

    public List<TextPassage> markTemperatures(List<TextPassage> passages) {
        List<TextPassage> newObjectList = passages.stream()
            .map(this::createNewCleanObject)
            .collect(Collectors.toList());

        List<TextPassage> responsePassages = client.markCriticalTemperature(newObjectList);

        assert (responsePassages.size() == passages.size());

        List<TextPassage> outputPassages = new ArrayList<>();

        for (int i = 0; i < passages.size(); i++) {
            TextPassage outputPassage = TextPassage.of(passages.get(i));

            //Fetch only what is needed
            Map<String, Span> spansMap = responsePassages.get(i).getSpans()
                .stream()
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            //Copy linkability
            if (responsePassages.get(i).getSpans().size() != outputPassage.getSpans().size()) {
                LOGGER.warn("The size of the processed spans is different than the original spans. Skipping linking");
            } else {
                for (int j = 0; j < outputPassage.getSpans().size(); j++) {
                    Span processedSpan = responsePassages.get(i).getSpans().get(j);
                    Span originalSpan = outputPassage.getSpans().get(j);
                    if (processedSpan.getId().equals(originalSpan.getId())) {
                        originalSpan.setLinkable(processedSpan.isLinkable());
                    }
                }
            }

            outputPassages.add(outputPassage);
        }
        return outputPassages;
    }


    public List<TextPassage> process(List<TextPassage> passage) {
        return process(passage, Arrays.asList(Link.MATERIAL_TCVALUE_TYPE, Link.TCVALUE_PRESSURE_TYPE, Link.TCVALUE_ME_METHOD_TYPE, Link.MATERIAL_CRYSTAL_STRUCTURE, Link.MATERIAL_SPACE_GROUPS), false);
    }

    public List<TextPassage> process(List<TextPassage> passage, List<String> linkTypes) {
        return process(passage, linkTypes, false);
    }

    /**
     * Apply the linking to a text passage
     *
     * @param passages           the text passage to be processed
     * @param linkTypes          type of links to be processed
     * @param skipClassification indicate if to skip the classification of tc
     * @return
     */
    public List<TextPassage> process(List<TextPassage> passages, List<String> linkTypes, boolean skipClassification) {
        List<TextPassage> newObjectList = passages.stream()
            .map(this::createNewCleanObject)
            .collect(Collectors.toList());

        List<TextPassage> responsePassages = client.extractLinks(newObjectList, linkTypes, skipClassification);

        assert (responsePassages.size() == passages.size());

        List<TextPassage> outputPassages = new ArrayList<>();

        for (int i = 0; i < passages.size(); i++) {
            TextPassage outputPassage = TextPassage.of(passages.get(i));

            //Fetch only what is needed
            Map<String, Span> spansMap = responsePassages.get(i).getSpans()
                .stream()
                .collect(Collectors.toMap(Span::getId, Function.identity()));

            outputPassage.getSpans()
                .stream()
                .forEach(s -> {
                    if (spansMap.containsKey(s.getId())) {
                        Span span = spansMap.get(s.getId());
                        s.getLinks().addAll(span.getLinks());
                        s.setLinkable(span.isLinkable());
                    }
                });

            outputPassage.setRelationships(responsePassages.get(i).getRelationships());
            outputPassages.add(outputPassage);
        }
        return outputPassages;
    }

    private TextPassage createNewCleanObject(TextPassage passage) {
        TextPassage requestPassage = new TextPassage();
        List<Span> newSpans = passage.getSpans()
            .stream()
            .map(s -> {
                Span newSpan = new Span(s);
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
