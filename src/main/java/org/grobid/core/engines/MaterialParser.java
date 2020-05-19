package org.grobid.core.engines;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorMaterial;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.grobid.core.engines.SuperconductorsModels.MATERIAL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

public class MaterialParser extends AbstractParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialParser.class);

    private static MaterialParser instance;

    public static MaterialParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new MaterialParser();
    }

    @Inject
    public MaterialParser() {
        super(MATERIAL);
    }

    protected MaterialParser(GrobidModel model) {
        super(model);
    }

    public List<Material> process(String text) {
        if (isBlank(text)) {
            return new ArrayList<>();
        }

        text = text.replace("\r", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> tokens = new ArrayList<>();
        try {
            tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        if (isEmpty(tokens)) {
            return new ArrayList<>();
        }

        return process(tokens);

    }


    public List<Material> process(List<LayoutToken> tokens) {

        List<Material> entities = new ArrayList<>();

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream()
            .map(layoutToken -> {
                    layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                    return layoutToken;
                }
            ).collect(Collectors.toList());


        if (isEmpty(layoutTokensNormalised))
            return new ArrayList<>();

        try {
            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(layoutTokensNormalised);

            if (StringUtils.isEmpty(ress))
                return entities;

            // labeled result from CRF lib
            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }

            List<Material> localEntities = extractResults(layoutTokensNormalised, res);

            entities.addAll(localEntities);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }


    private String addFeatures(List<LayoutToken> tokens) {
        StringBuilder result = new StringBuilder();
        try {
            for (LayoutToken token : tokens) {
                if (token.getText().trim().equals("@newline")) {
                    result.append("\n");
                    continue;
                }

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

                FeaturesVectorMaterial featuresVector =
                    FeaturesVectorMaterial.addFeatures(token.getText(), null);
                result.append(featuresVector.printVector());
                result.append("\n");
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return result.toString();
    }

    /**
     * Extract identified quantities from a labeled text.
     */
    public List<Material> extractResults(List<LayoutToken> tokens, String result) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.MATERIAL, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        List<Material> extracted = new ArrayList<>();
        Material currentMaterial = new Material();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();
            List<BoundingBox> boundingBoxes = null;

            if (!clusterLabel.equals(SUPERCONDUCTORS_OTHER))
                boundingBoxes = BoundingBoxCalculator.calculate(cluster.concatTokens());

//            OffsetPosition calculatedOffset = calculateOffsets(tokens, theTokens, pos);
//            pos = calculatedOffset.start;
//            int endPos = calculatedOffset.end;

            int startPos = theTokens.get(0).getOffset();
            int endPos = startPos + clusterContent.length();

            String doping = null;
            String shape = null;

            if (clusterLabel.equals(MATERIAL_NAME)) {
                if (StringUtils.isNotEmpty(currentMaterial.getName())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                currentMaterial.setName(clusterContent);

            } else if (clusterLabel.equals(MATERIAL_DOPING)) {
                doping = clusterContent;
            } else if (clusterLabel.equals(MATERIAL_FORMULA)) {
                if (StringUtils.isNotEmpty(currentMaterial.getFormula())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                currentMaterial.setFormula(clusterContent);

            } else if (clusterLabel.equals(MATERIAL_SHAPE)) {
                shape = clusterContent;
            } else if (clusterLabel.equals(MATERIAL_VALUE)) {

            } else if (clusterLabel.equals(MATERIAL_VARIABLE)) {

            } else if (clusterLabel.equals(SUPERCONDUCTORS_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in the material parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }

        return extracted;
    }

    public Pair<String, List<Material>> generateTrainingData(List<LayoutToken> layoutTokens) {
        return null;
    }

}
