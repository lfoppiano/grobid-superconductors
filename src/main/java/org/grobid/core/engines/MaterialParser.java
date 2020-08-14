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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

        // Usually the shape, doping and fabrication are shared between the instantiated material objects
        List<String> shapes = new ArrayList<>();
        List<String> dopings = new ArrayList<>();
        List<String> fabrications = new ArrayList<>();
        List<String> substrates = new ArrayList<>();

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
                dopings.add(clusterContent);

            } else if (clusterLabel.equals(MATERIAL_FORMULA)) {
                if (StringUtils.isNotEmpty(currentMaterial.getFormula())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                currentMaterial.setFormula(postProcessFormula(clusterContent));
                currentMaterial.addOffset(new OffsetPosition(startPos, endPos));
                currentMaterial.addBoundingBoxes(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_SHAPE)) {
                shapes.add(clusterContent);

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

            } else if (clusterLabel.equals(MATERIAL_FABRICATION)) {
                fabrications.add(clusterContent);
            } else if (clusterLabel.equals(MATERIAL_SUBSTRATE)) {
                substrates.add(clusterContent);
            } else if (clusterLabel.equals(MATERIAL_OTHER)) {

            } else {
                LOGGER.error("Warning: unexpected label in the material parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }

        extracted.add(currentMaterial);

        /**
         * if the doping has size > 1  and there is just one material, then it means
         * the experiment is about different doping ratio, therefore we need to create
         * more objects with different doping, with the same material name/formula
         **/

        String singleDoping = "";
        String singleSubstrate = "";
        if (dopings.size() > 1) {
            if (extracted.size() == 1) {
                Material singleExtractedMaterial = extracted.get(0);
                extracted = new ArrayList<>();
                for (String doping : dopings) {
                    Material newMaterial = new Material();
                    newMaterial.setName(singleExtractedMaterial.getName());
                    newMaterial.setFormula(postProcessFormula(singleExtractedMaterial.getFormula()));
                    newMaterial.setDoping(doping);
                    singleExtractedMaterial.getVariables().entrySet().stream()
                        .forEach(entry -> newMaterial.getVariables().put(entry.getKey(), entry.getValue()));
                    extracted.add(newMaterial);
                }
            } else {
                singleDoping = String.join(", ", dopings);
            }
        } else {
            if (dopings.size() == 1) {
                singleDoping = String.join(", ", dopings);
            }

            if (substrates.size() == 1) {
                singleSubstrate = substrates.get(0);
            } else if (substrates.size() > 1) {
                if (extracted.size() == 1) {
                    Material singleExtractedMaterial = extracted.get(0);
                    extracted = new ArrayList<>();
                    for (String substrate : substrates) {
                        Material newMaterial = new Material();
                        newMaterial.setName(singleExtractedMaterial.getName());
                        newMaterial.setFormula(postProcessFormula(singleExtractedMaterial.getFormula()));
                        newMaterial.setSubstrate(substrate);
                        singleExtractedMaterial.getVariables().entrySet().stream()
                            .forEach(entry -> newMaterial.getVariables().put(entry.getKey(), entry.getValue()));
                        extracted.add(newMaterial);
                    }
                } else {
                    singleSubstrate = String.join(", ", substrates);
                }
            } else {
                singleSubstrate = String.join(", ", substrates);
            }
        }

        //TODO: validate this
        String singleShape = String.join(", ", shapes);
        String singleFabrication = String.join(", ", fabrications);

        /** Post_processing the variables-> values **/
        for (Material material : extracted) {
            List<String> resolvedFormulas = Material.resolveVariables(material);

            //If there are no resolved formulas (no variable) I could still have a (A, B)C1,D2 formula type that can
            // be expanded
            if (isEmpty(resolvedFormulas) && StringUtils.isNotEmpty(material.getFormula())) {
                resolvedFormulas.add(material.getFormula());
            }

            if (isNotEmpty(resolvedFormulas)) {
                List<String> resolvedAndExpandedFormulas = resolvedFormulas.stream()
                    .flatMap(f -> Material.expandFormula(f).stream())
                    .collect(Collectors.toList());
                material.setResolvedFormulas(resolvedAndExpandedFormulas);
            }

            /** Shape and fabrication are shared properties **/
            if (isNotBlank(singleShape) && isBlank(material.getShape())) {
                material.setShape(singleShape);
            }
            if (isNotBlank(singleFabrication) && isBlank(material.getFabrication())) {
                material.setFabrication(singleShape);
            }

            /** doping and substrate are merged if there are already multiple materials **/
            if (isNotBlank(singleDoping) && isBlank(material.getDoping())) {
                material.setDoping(singleDoping);
            }

            if (isNotBlank(singleSubstrate) && isBlank(material.getSubstrate())) {
                material.setSubstrate(singleSubstrate);
            }

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

    public String postProcessFormula(String formula) {
        Pattern regex = Pattern.compile("( {0,1})([-−]x) {0,1}(1)( {0,1})");
        String formulaWithFixedVariableOperations = regex.matcher(formula).replaceAll("$1$3$2$4");

        String formulaWithoutInvalidCharacters = formulaWithFixedVariableOperations.replaceAll("\\p{C}", " ");

        return formulaWithoutInvalidCharacters;
    }

}
