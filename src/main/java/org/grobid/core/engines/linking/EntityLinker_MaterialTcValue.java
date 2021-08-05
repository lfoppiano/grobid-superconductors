package org.grobid.core.engines.linking;

import org.grobid.core.GrobidModel;
import org.grobid.core.data.Span;
import org.grobid.core.engines.AbstractParser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.layout.LayoutToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class EntityLinker_MaterialTcValue extends AbstractParser implements EntityLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinker_MaterialTcValue.class);

    private static volatile EntityLinker_MaterialTcValue instance;

    private List<String> annotationsToBeLinked = new ArrayList<>();

    public static EntityLinker_MaterialTcValue getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new EntityLinker_MaterialTcValue();
    }

    @Inject
    public EntityLinker_MaterialTcValue() {
        super(SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC);
        instance = this;
        this.annotationsToBeLinked = Arrays.asList(SUPERCONDUCTORS_MATERIAL_LABEL, SUPERCONDUCTORS_TC_VALUE_LABEL);
    }

    public EntityLinker_MaterialTcValue(GrobidModel model, List<String> validLinkAnnotations) {
        super(model);
        instance = this;
        this.annotationsToBeLinked = validLinkAnnotations;
    }

    @SuppressWarnings({"UnusedParameters"})
    public String addFeatures(List<LayoutToken> tokens, List<String> annotations) {
        StringBuilder result = new StringBuilder();
        try {
            ListIterator<LayoutToken> it = tokens.listIterator();
            while (it.hasNext()) {
                int index = it.nextIndex();
                LayoutToken token = it.next();

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

                FeaturesVectorEntityLinker featuresVector =
                    FeaturesVectorEntityLinker.addFeatures(token.getText(), null, annotations.get(index));
                result.append(featuresVector.printVector());
                result.append("\n");
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return result.toString();
    }

    public List<String> getAnnotationsToBeLinked() {
        return annotationsToBeLinked;
    }

    /**
     * Extract identified quantities from a labeled text.
     */
    public List<Span> extractResults(List<LayoutToken> tokens, String result, List<Span> annotations) {
        return CRFBasedLinker.extractResults(tokens, result, annotations,
                SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC, ENTITY_LINKER_MATERIAL_TC_LEFT_ATTACHMENT,
                ENTITY_LINKER_MATERIAL_TC_RIGHT_ATTACHMENT, ENTITY_LINKER_MATERIAL_TC_OTHER);
    }

    @Override
    public List<Span> markLinkableEntities(List<Span> rawAnnotations) {
        List<Span> markedAnnotations = rawAnnotations
            .stream()
            .map(s -> {
                Span n = new Span(s);

                if (n.getType().equals(SUPERCONDUCTORS_MATERIAL_LABEL)) {
                    n.setLinkable(true);
                }
                
                return n;
            })
            .collect(Collectors.toList());

        return markedAnnotations;
    }
}
