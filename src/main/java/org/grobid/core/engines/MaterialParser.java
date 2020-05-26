package org.grobid.core.engines;


import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorMaterial;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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

    public String extractResultsForTraining(List<LayoutToken> tokens, String result) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.MATERIAL, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        StringBuilder rawTaggedValue = new StringBuilder();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();

            String escapedContent = StringEscapeUtils.escapeXml11(clusterContent);

            if (!clusterLabel.equals(MATERIAL_OTHER)) {
                rawTaggedValue.append(clusterLabel.getLabel());
            }
            rawTaggedValue.append(escapedContent);
            if (!clusterLabel.equals(MATERIAL_OTHER)) {
                rawTaggedValue.append(clusterLabel.getLabel().replace("<", "</"));
            }
            LabeledTokensContainer last = Iterables.getLast(cluster.getLabeledTokensContainers());
            if (last.isTrailingSpace()) {
                rawTaggedValue.append(" ");
            }
        }

        return rawTaggedValue.toString();
    }

    /**
     * Extract identified material from a labeled text.
     */
    public List<Material> extractResults(List<LayoutToken> tokens, String result) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(SuperconductorsModels.MATERIAL, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        List<Material> extracted = new ArrayList<>();
        Material currentMaterial = new Material();

        // Usually the shape is shared
        String shape = null;

        //We assume the doping is shared too, although might not always be the case
        String doping = null;

        String processingVariable = null;
        StringBuilder rawTaggedValue = new StringBuilder();

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

            if (!clusterLabel.equals(MATERIAL_OTHER)) {
                rawTaggedValue.append(clusterLabel.getLabel());
            }
            rawTaggedValue.append(clusterContent);
            if (!clusterLabel.equals(MATERIAL_OTHER)) {
                rawTaggedValue.append(clusterLabel.getLabel().replace("<", "</"));
            }
            LabeledTokensContainer last = Iterables.getLast(cluster.getLabeledTokensContainers());
            if (last.isTrailingSpace()) {
                rawTaggedValue.append(" ");
            }

            int startPos = theTokens.get(0).getOffset();
            int endPos = startPos + clusterContent.length();

            if (clusterLabel.equals(MATERIAL_NAME)) {
                if (StringUtils.isNotEmpty(currentMaterial.getName())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                currentMaterial.setName(clusterContent);
                currentMaterial.addOffset(new OffsetPosition(startPos, endPos));
                currentMaterial.addBoundingBoxes(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_DOPING)) {
                doping = clusterContent;
//                dopingOffsets.addAll(new OffsetPosition(startPos, endPos));
//                dopingBoundingBoxes.addAll(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_FORMULA)) {
                if (StringUtils.isNotEmpty(currentMaterial.getFormula())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                currentMaterial.setFormula(clusterContent);
                currentMaterial.addOffset(new OffsetPosition(startPos, endPos));
                currentMaterial.addBoundingBoxes(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_SHAPE)) {
                shape = clusterContent;
//                shapeOffsets.addAll(new OffsetPosition(startPos, endPos));
//                shapeBoundingBoxes.addAll(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_VALUE)) {
                String value = clusterContent;

                if (StringUtils.isNotEmpty(processingVariable)) {
                    String[] split = value.split(",|;|or|and");
                    List<String> listValues = Arrays.stream(split).map(StringUtils::trim).collect(Collectors.toList());
                    currentMaterial.getVariables().put(processingVariable, listValues);
                } else {
                    LOGGER.error("Got a value but the processing variable is empty. Value: " + value);
                }
            } else if (clusterLabel.equals(MATERIAL_VARIABLE)) {
                String variable = clusterContent;
                if (StringUtils.isNotEmpty(processingVariable)) {
                    if (!processingVariable.equals(variable)) {
                        processingVariable = variable;
                    }
                } else {
                    processingVariable = variable;
                }

            } else if (clusterLabel.equals(MATERIAL_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in the material parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }

        extracted.add(currentMaterial);

        /** Post_processing the variables-> values **/
        List<Material> processedMaterials = new ArrayList<>();

//        Map<String, List<String>> processedVariables = new HashMap<>();
//
//        for (Material material : extracted) {
//            if (isNotEmpty(material.getVariables().keySet())) {
//
//                for (Map.Entry<String, String> substitutions : material.getVariables().entrySet()) {
//                    String variable = substitutions.getKey();
//                    String values = substitutions.getValue();
//
//                    // split by comma
//
//                }
//            } else {
//                processedMaterials.add(material);
//            }
//        }
//
//        Material newMaterial = new Material();
//        newMaterial.setName(material.getName());
//        newMaterial.setFormula(material.getFormula());
//        newMaterial.setOffsets(material.getOffsets());
//        newMaterial.setBoundingBoxes(material.getBoundingBoxes());
//        newMaterial.addVariable(variable, s);
//        processedMaterials.add(newMaterial);

        for (Material material : extracted) {
            /** Shape and doping are shared properties **/
            material.setShape(shape);
            material.setDoping(doping);
            material.setRawTaggedValue(rawTaggedValue.toString());
        }

        return extracted;
    }

    public String generateTrainingData(List<LayoutToken> layoutTokens) {
        if (isEmpty(layoutTokens))
            return "";

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = tokens.stream().map(layoutToken -> {
                layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                return layoutToken;
            }
        ).collect(Collectors.toList());

        String features = null;
        try {
            // string representation of the feature matrix for CRF lib
            features = addFeatures(layoutTokensNormalised);

            String labelledResults = null;
            try {
                labelledResults = label(features);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for superconductors parsing failed.", e);
            }
            return extractResultsForTraining(tokens, labelledResults);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }


    public String generateTrainingData(String text) {
        text = text.replace("\r\t", " ");
        text = text.replace("\n", " ");
        text = text.replace("\t", " ");

        List<LayoutToken> layoutTokens = null;
        try {
            layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        } catch (Exception e) {
            LOGGER.error("fail to tokenize:, " + text, e);
        }

        return generateTrainingData(layoutTokens);

    }

}
