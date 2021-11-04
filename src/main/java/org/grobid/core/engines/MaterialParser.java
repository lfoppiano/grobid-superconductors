package org.grobid.core.engines;


import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.material.ChemicalComposition;
import org.grobid.core.data.material.Formula;
import org.grobid.core.data.material.Material;
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
import org.grobid.core.utilities.client.ChemicalMaterialParserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.grobid.core.engines.SuperconductorsModels.MATERIAL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.*;

public class MaterialParser extends AbstractParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialParser.class);

    private static MaterialParser instance;
    private MaterialClassResolver materialClassResolver;
    private ChemicalMaterialParserClient chemicalMaterialParserClient;

    public static MaterialParser getInstance(MaterialClassResolver materialClassResolver, ChemicalMaterialParserClient chemicalMaterialParserClient) {
        if (instance == null) {
            getNewInstance(materialClassResolver, chemicalMaterialParserClient);
        }
        return instance;
    }

    private static synchronized void getNewInstance(MaterialClassResolver materialClassResolver, ChemicalMaterialParserClient chemicalMaterialParserClient) {
        instance = new MaterialParser(materialClassResolver, chemicalMaterialParserClient);
    }

    @Inject
    public MaterialParser(MaterialClassResolver materialClassResolver, ChemicalMaterialParserClient chemicalMaterialParserClient) {
        this(MATERIAL, materialClassResolver, chemicalMaterialParserClient);
    }

    protected MaterialParser(GrobidModel model, MaterialClassResolver materialClassResolver, ChemicalMaterialParserClient chemicalMaterialParserClient) {
        super(model);
        if (materialClassResolver == null) {
            LOGGER.info("The material class resolver is missing or null. The Material class will not be resolved. ");
        }

        if (chemicalMaterialParserClient == null) {
            LOGGER.info("The chemical material parser has not specified. Advanced chemical parsing will not be performed. ");
        }
        this.materialClassResolver = materialClassResolver;
        this.chemicalMaterialParserClient = chemicalMaterialParserClient;
    }

    public List<Material> process(String text) {
        return process(SuperconductorsParser.textToLayoutTokens(text));
    }


    public List<Material> process(List<LayoutToken> tokens) {

        List<Material> entities = new ArrayList<>();

        //Normalisation
        List<LayoutToken> layoutTokensNormalised = SuperconductorsParser.normalizeLayoutTokens(tokens);

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
        List<String> prefixedValues = new ArrayList<>();

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
            if (last != null && last.isTrailingSpace()) {
                rawTaggedValue.append(" ");
            }

            int startPos = theTokens.get(0).getOffset();
            int endPos = startPos + clusterContent.length();

            if (clusterLabel.equals(MATERIAL_NAME)) {
                if (StringUtils.isNotEmpty(currentMaterial.getName())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
//                if (chemicalMaterialParserClient != null) {
//                    List<String> convertedFormula = chemicalMaterialParserClient.convertNameToFormula(clusterContent);
//                    if (convertedFormula.size() >= 3 && StringUtils.isNotBlank(convertedFormula.get(2))) {
//                        currentMaterial.setCalculatedFormulaFromName(convertedFormula.get(2));                        
//                    }
//                }

                currentMaterial.setName(clusterContent);
                currentMaterial.addOffset(new OffsetPosition(startPos, endPos));
                currentMaterial.addBoundingBoxes(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_DOPING)) {
                dopings.add(clusterContent);

            } else if (clusterLabel.equals(MATERIAL_FORMULA)) {
                if (currentMaterial.getFormula() != null && StringUtils.isNotBlank(currentMaterial.getFormula().getRawValue())) {
                    extracted.add(currentMaterial);
                    currentMaterial = new Material();
                }
                String finalFormula = postProcessFormula(clusterContent);
                Formula formula = new Formula(finalFormula);
                if (chemicalMaterialParserClient != null) {
                    ChemicalComposition chemicalComposition = chemicalMaterialParserClient.convertFormulaToComposition(finalFormula);
                    formula.setFormulaComposition(chemicalComposition.getComposition());
                }

                currentMaterial.setFormula(formula);
                currentMaterial.addOffset(new OffsetPosition(startPos, endPos));
                currentMaterial.addBoundingBoxes(boundingBoxes);

            } else if (clusterLabel.equals(MATERIAL_SHAPE)) {
                shapes.add(clusterContent);

            } else if (clusterLabel.equals(MATERIAL_VALUE)) {
                String value = clusterContent;

                if (StringUtils.isNotEmpty(processingVariable)) {
                    List<String> listValues = extractVariableValues(value);
                    currentMaterial.getVariables().put(processingVariable, listValues);
                    if (isNotEmpty(prefixedValues)) {
                        currentMaterial.getVariables().get(processingVariable).addAll(prefixedValues);
                        prefixedValues = new ArrayList<>();
                    }
                } else {
                    if (value.contains("<")) {
                        prefixedValues.add(value);
                    } else {
                        LOGGER.error("Got a value but the processing variable is empty. Value: " + value);
                    }
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

            // Multiple dopings AND single material -> create multiple materials 
            if (extracted.size() == 1) {
                Material singleExtractedMaterial = extracted.get(0);
                extracted = new ArrayList<>();
                for (String doping : dopings) {
                    Material newMaterial = new Material();
                    newMaterial.setName(singleExtractedMaterial.getName());
                    newMaterial.setFormula(singleExtractedMaterial.getFormula());
                    newMaterial.setDoping(doping);
                    singleExtractedMaterial.getVariables().entrySet().stream()
                        .forEach(entry -> newMaterial.getVariables().put(entry.getKey(), entry.getValue()));
                    // Class will be computed later, 
                    // Substrate and fabrication will be added later as single or joined information 
                    extracted.add(newMaterial);
                }
            } else {
                // Multiple dopings AND multiple materials -> merge dopings and assign to each material
                singleDoping = String.join(", ", dopings);
            }
        } else {
            // Single doping
            if (dopings.size() == 1) {
                singleDoping = dopings.get(0);
            }

            if (substrates.size() == 1) {
                singleSubstrate = substrates.get(0);
            } else if (substrates.size() > 1) {

                // Multiple substrate AND single material -> create multiple materials 
                if (extracted.size() == 1) {
                    Material singleExtractedMaterial = extracted.get(0);
                    extracted = new ArrayList<>();
                    for (String substrate : substrates) {
                        Material newMaterial = new Material();
                        newMaterial.setName(singleExtractedMaterial.getName());
                        newMaterial.setFormula(singleExtractedMaterial.getFormula());
                        newMaterial.setSubstrate(substrate);
                        // Class will be computed later, 
                        // Doping and Fabrication will be added later as single or joined information

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

        /** Post_processing the variables -> values **/
        for (Material material : extracted) {
            List<String> resolvedFormulas = Material.resolveVariables(material);

            //If there are no resolved formulas (no variable) I could still have a (A, B)C1,D2 formula type that can
            // be expanded
            if (isEmpty(resolvedFormulas) && (material.getFormula() != null && StringUtils.isNotBlank(material.getFormula().getRawValue()))) {
                resolvedFormulas.add(material.getFormula().getRawValue());
            }

            //Expand formulas of type (A, B)blabla
            if (isNotEmpty(resolvedFormulas)) {
                List<Formula> resolvedAndExpandedFormulas = resolvedFormulas.stream()
                    .flatMap(f -> Material.expandFormula(f).stream())
                    .map(f -> {
                        Formula createdFormula = new Formula(f);
                        if (chemicalMaterialParserClient != null) {
                            createdFormula.setFormulaComposition(chemicalMaterialParserClient.convertFormulaToComposition(f).getComposition());
                        }
                        return createdFormula;
                    })
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

            // If we don't have any formula but a name, let's try to calculate the formula from the name... 

            if ((material.getFormula() == null
                || StringUtils.isBlank(material.getFormula().getRawValue()))
                && StringUtils.isNotBlank(material.getName())) {
                if (chemicalMaterialParserClient != null) {
                    ChemicalComposition convertedFormula = chemicalMaterialParserClient.convertNameToFormula(material.getName());
                    Formula formula = null;
                    if (isNotBlank(convertedFormula.getFormula())) {
                        formula = new Formula(convertedFormula.getFormula());
                        material.setFormula(formula);
                    } 
                    
                    if (convertedFormula.getComposition().keySet().size() > 0) {
                        if (formula == null) {
                            formula = new Formula();
                        }
                        formula.setFormulaComposition(convertedFormula.getComposition());
                        material.setFormula(formula);
                    }                     
                }
            }

            //This modifies the material object!
            if (materialClassResolver != null) {
                materialClassResolver.process(material);
            }
        }

        return extracted;
    }

    protected List<String> extractVariableValues(String value) {
        String[] split = value.split(",|;|or|and");
        return Arrays.stream(split).map(StringUtils::trim).collect(Collectors.toList());
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

    private static final Pattern REGEX_INVERTED_X_MINUS_1_PATTERN1 = Pattern.compile("( {0,1})([-−][xyzδ]) {0,1}([1-2])( {0,1})");
    private static final Pattern REGEX_INVERTED_X_MINUS_1_PATTERN2 = Pattern.compile("( {0,1})([xyzδ]) {0,1}([1-2][-−])( {0,1})");
    private static final Pattern REGEX_COMMA_INSTEAD_DOT = Pattern.compile("([A-Za-z]+ {0,1})([0-9])[,]([0-9])");
    private static final Pattern REGEX_COLON_INSTEAD_DOT = Pattern.compile("([0-9]):([0-9])");

    private static final List<Pair<String, String>> REPLACEMENT_SYMBOLS = Arrays.asList(
        Pair.of("À", "-"),
        Pair.of("Ϸ", "≈"),
        Pair.of("¼", "-"),
        Pair.of(" ͑", "")
    );

    public String postProcessFormula(String formula) {
        if (formula == null) {
            return "";
        }

        String formulaWithFixedVariableOperations1 = REGEX_INVERTED_X_MINUS_1_PATTERN1
            .matcher(formula).replaceAll("$1$3$2$4");
        String formulaWithFixedVariableOperations = REGEX_INVERTED_X_MINUS_1_PATTERN2
            .matcher(formulaWithFixedVariableOperations1).replaceAll("$1$3$2$4");
        String formulaWithFixedDots = REGEX_COLON_INSTEAD_DOT
            .matcher(formulaWithFixedVariableOperations).replaceAll("$1.$2");
        String formulaWithFixedCommas = REGEX_COMMA_INSTEAD_DOT
            .matcher(formulaWithFixedDots).replaceAll("$1$2.$3");

        String formulaSequencialReplacement = formulaWithFixedCommas;
        for (Pair<String, String> replacementSymbol : REPLACEMENT_SYMBOLS) {
            formulaSequencialReplacement = formulaSequencialReplacement.replaceAll(replacementSymbol.getLeft(), replacementSymbol.getRight());
        }

        return formulaSequencialReplacement.replaceAll("\\p{C}", " ");
    }

    public ChemicalMaterialParserClient getChemicalMaterialParserClient() {
        return chemicalMaterialParserClient;
    }

    public void setChemicalMaterialParserClient(ChemicalMaterialParserClient chemicalMaterialParserClient) {
        this.chemicalMaterialParserClient = chemicalMaterialParserClient;
    }
}
