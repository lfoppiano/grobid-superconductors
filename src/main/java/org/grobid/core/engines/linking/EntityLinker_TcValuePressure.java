package org.grobid.core.engines.linking;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Span;
import org.grobid.core.data.chemDataExtractor.ChemicalSpan;
import org.grobid.core.engines.AbstractParser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorEntityLinker;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.AdditionalLayoutTokensUtil;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

@Singleton
public class EntityLinker_TcValuePressure extends AbstractParser  implements EntityLinker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityLinker_TcValuePressure.class);

    private static volatile EntityLinker_TcValuePressure instance;

    private List<String> annotationLinks = new ArrayList<>();

    public static EntityLinker_TcValuePressure getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new EntityLinker_TcValuePressure();
    }

    @Inject
    public EntityLinker_TcValuePressure() {
        super(SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC);
        instance = this;
        annotationLinks = Arrays.asList(SUPERCONDUCTORS_TC_VALUE_LABEL, SUPERCONDUCTORS_PRESSURE_LABEL);
    }

    public EntityLinker_TcValuePressure(GrobidModel model, List<String> validLinkAnnotations) {
        super(model);
        instance = this;
        annotationLinks = validLinkAnnotations;
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

    @Override
    public List<String> getAnnotationsToBeLinked() {
        return this.annotationLinks;
    }

    @Override
    public List<Span> extractResults(List<LayoutToken> tokens, String result, List<Span> annotations) {
        return CRFBasedLinker.extractResults(tokens, result, annotations,
            SuperconductorsModels.ENTITY_LINKER_TC_PRESSURE, ENTITY_LINKER_TC_PRESSURE_LEFT_ATTACHMENT,
            ENTITY_LINKER_TC_PRESSURE_RIGHT_ATTACHMENT, ENTITY_LINKER_TC_PRESSURE_OTHER);
    }

    @Override
    public List<Span> markLinkableEntities(List<Span> rawAnnotations) {
        //Assuming that the Tc will be marked from a different process
        List<Span> markedAnnotations = rawAnnotations.stream()
            .map(s -> {
                Span n = new Span(s);
                if (n.getType().equals(SUPERCONDUCTORS_PRESSURE_LABEL)) {
                    n.setLinkable(true);
                }
                return n;
            })
            .collect(Collectors.toList());

        return markedAnnotations;
    }
}
