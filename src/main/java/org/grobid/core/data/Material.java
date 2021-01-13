package org.grobid.core.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Material {

    private static final Logger LOGGER = LoggerFactory.getLogger(Material.class);

    private String name;
    private String shape;
    private String formula;
    private String doping;
    private String fabrication;
    private String substrate;
    private String rawTaggedValue;
    private String clazz;

    //This is the formula with each variable, replaced with random numbers, and can be used as a second attempt to
    // calculate the class from the formula
    private String sampleFormula;

    private List<String> resolvedFormulas = new ArrayList<>();

    @JsonIgnore
    private final Map<String, List<String>> variables = new HashMap<>();
    @JsonIgnore
    private List<BoundingBox> boundingBoxes = new ArrayList<>();
    @JsonIgnore
    private List<LayoutToken> layoutTokens = new ArrayList<>();
    @JsonIgnore
    private List<OffsetPosition> offsets = new ArrayList<>();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getDoping() {
        return doping;
    }

    public void setDoping(String doping) {
        this.doping = doping;
    }

    public Map<String, List<String>> getVariables() {
        return variables;
    }

    public void addVariable(String variable, List<String> value) {
        this.variables.putIfAbsent(variable, value);
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void setRawTaggedValue(String rawTaggedValue) {
        this.rawTaggedValue = rawTaggedValue;
    }

    public String getRawTaggedValue() {
        return rawTaggedValue;
    }

    public List<OffsetPosition> getOffsets() {
        return offsets;
    }

    public void setOffsets(List<OffsetPosition> offsets) {
        this.offsets = offsets;
    }

    public void addOffset(OffsetPosition offsetPosition) {
        offsets.add(offsetPosition);
    }

    public void setResolvedFormulas(List<String> resolvedFormulas) {
        this.resolvedFormulas = resolvedFormulas;
    }

    public void addBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes.addAll(boundingBoxes);
    }

    public static List<String> resolveVariables(Material material) {
        if (CollectionUtils.isEmpty(material.getVariables().keySet()) || isEmpty(material.getFormula())) {
            return new ArrayList<>();
        }

        if (isEmpty(material.getFormula())) {
            LOGGER.warn("Cannot resolve variables, as the material representation doesn't have a formula " + material.getFormula());
        }

        Set<String> variables = material.getVariables().keySet();
        Set<String> containedVariables = variables.stream()
            .filter(var -> material.getFormula().contains(var))
            .collect(Collectors.toSet());

        List<String> output = new ArrayList<>();

        if (containedVariables.size() == 0) {
            return output;
        }

        if (containedVariables.size() != variables.size()) {
            LOGGER.warn("While processing the variables, some are not present in the material formula and " +
                "won't be substituted: " + SetUtils.disjunction(variables, containedVariables));
        }

        Map<String, List<String>> mapOfContainedVariables = containedVariables.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                s -> material.getVariables().get(s)));

//        Map<String, List<String>> output = new HashMap<>();
//        for (String variable : containedVariables) {
//            for (int i = 0; i < material.getVariables().get(variable).size(); i++) {
//                output.put(variable, material.getFormula()
//                    .replaceAll(variable, material.getVariables().get(variable).get(i));
//            }
//        }

        try {
            generatePermutations(mapOfContainedVariables, new ArrayList(containedVariables), output, Pair.of(0, 0), material.getFormula());
        } catch (NumberFormatException e) {
            LOGGER.warn("Cannot replace variables " + Arrays.toString(variables.toArray()));
        }

//        material.getResolvedFormulas().addAll(output);
//        return output.stream().map(s -> {
//            Material material1 = new Material();
//            material1.setFormula(s);
//            return material1;
//        }).collect(Collectors.toList());

        return output;
    }

    /**
     * generate permutations via a recursive function.
     * <p>
     * depth: right -> valueIndex, left -> variableIndex
     */
    public static void generatePermutations(Map<String, List<String>> input, List<String> keyList,
                                            List<String> result, Pair<Integer, Integer> depth, String formula) {
        Integer variableIndex = depth.getLeft();
        String variable = keyList.get(variableIndex);
        Integer valueIndex = depth.getRight();

        String value = input.get(variable).get(valueIndex);
        if (valueIndex == input.get(variable).size() - 1 && variableIndex == keyList.size() - 1) {
            result.add(replaceVariable(formula, variable, value));
            return;
        }

        if (variableIndex == keyList.size() - 1) {
            result.add(replaceVariable(formula, variable, value));
            generatePermutations(input, keyList, result, Pair.of(variableIndex, valueIndex + 1), formula);
            return;
        }

        for (int i = 0; i < input.get(variable).size(); i++) {
            generatePermutations(input, keyList, result, Pair.of(variableIndex + 1, 0), replaceVariable(formula, variable, input.get(variable).get(i)));
        }
    }

    protected static String replaceVariable(String formula, String variable, String value) {
        // check if there is 1-X
        String returnFormula = formula;
        int startSearching = 0;
        while (formula.indexOf(variable, startSearching) > -1) {
            Integer variableIndex = formula.indexOf(variable, startSearching);

            if (variableIndex > -1) {
                if (formula.startsWith("-", variableIndex - 1)
                    || formula.startsWith("\u2212", variableIndex - 1)) {
                    Integer endSearch = variableIndex - 1;
                    while (endSearch > 0 && Character.isDigit(formula.charAt(endSearch - 1))) {
                        endSearch = endSearch - 1;
                    }
                    if (endSearch < variableIndex - 1) {
                        String number = formula.substring(endSearch, variableIndex - 1);
                        BigDecimal operation = new BigDecimal(number).subtract(new BigDecimal(value));
                        operation.setScale(2, RoundingMode.HALF_UP);
                        returnFormula = returnFormula.replaceFirst(number + formula.charAt(variableIndex - 1) + variable, String.valueOf(operation.doubleValue()));
                    } else {
                        if (value.startsWith("-") || value.startsWith("\u2212")) {
                            returnFormula = returnFormula.replaceFirst(formula.charAt(variableIndex - 1) + variable, value.substring(1));
                        } else {
                            returnFormula = returnFormula.replaceFirst(variable, value);
                        }
                    }
                } else {
                    // if the variable is followed by a lowercase character, I skip the substitution
                    if (variableIndex == formula.length() - 1
                        || !Character.isLowerCase(formula.charAt(variableIndex + variable.length()))) {
                        returnFormula = returnFormula.replaceFirst(variable, value);
                    }
                }
            }
            startSearching = variableIndex + 1;
        }

        return returnFormula;
    }

    public List<String> getResolvedFormulas() {
        return resolvedFormulas;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        boolean started = false;
        JsonStringEncoder jsonUtf8Encoder = JsonStringEncoder.getInstance();
        json.append("{ ");
        started = false;

        if (name != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"name\" : \"" + new String(jsonUtf8Encoder.quoteAsUTF8(name)) + "\"");
        }


        if (shape != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"shape\" : \"" + new String(jsonUtf8Encoder.quoteAsUTF8(shape)) + "\"");
        }

        if (doping != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"doping\" : \"" + new String(jsonUtf8Encoder.quoteAsUTF8(doping)) + "\"");
        }

        if (formula != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"formula\" : \"" + new String(jsonUtf8Encoder.quoteAsUTF8(formula)) + "\"");
        }


        if (CollectionUtils.isNotEmpty(resolvedFormulas)) {
            if (!started) {
                started = true;
            } else
                json.append(", ");

            json.append("\"resolvedFormulas\" : [");
            boolean first = true;
            for (String formula : resolvedFormulas) {
                if (first)
                    first = false;
                else
                    json.append(",");
                json.append("\"" + formula + "\"");
            }
            json.append("] ");
        }

        if (CollectionUtils.isNotEmpty(variables.keySet())) {
            if (!started) {
                started = true;
            } else
                json.append(", ");

            json.append("\"variables\" : {");
            boolean first = true;
            for (Map.Entry<String, List<String>> variable : variables.entrySet()) {
                if (first)
                    first = false;
                else
                    json.append(",");

                json.append("\"" + new String(jsonUtf8Encoder.quoteAsUTF8(variable.getKey())) + "\" : [");
                boolean first2 = true;
                for (String value : variable.getValue()) {
                    if (first2)
                        first2 = false;
                    else
                        json.append(",");

                    json.append("\"" + new String(jsonUtf8Encoder.quoteAsUTF8(value)) + "\"");
                }

                json.append("]");
            }
            json.append("} ");
        }

        json.append(" }");
        return json.toString();
    }

    /**
     * Expand a formula in the form (A, B)Formula in A x B 1-x Formula...
     */
    public static List<String> expandFormula(String formula) {

        String regex = "^ ?\\(([A-Za-z, ]+)\\)(.*)";
        Pattern formulaDopantPattern = Pattern.compile(regex);
        Matcher formulaDopantMatcher = formulaDopantPattern.matcher(formula);

        Pattern nameMaterialPattern = Pattern.compile("-[0-9]+");
        List<String> expandedFormulas = new ArrayList<>();

        if (formulaDopantMatcher.find()) {
            String dopants = formulaDopantMatcher.group(1);
            String formulaWithoutDopants = formulaDopantMatcher.group(2);
            String[] splittedDopants = dopants.split(",");

            Matcher nameMaterialMatcher = nameMaterialPattern.matcher(formulaWithoutDopants);
            if (nameMaterialMatcher.find()) {
                for (String dopant : splittedDopants) {
                    expandedFormulas.add(trim(dopant) + trim(formulaWithoutDopants));
                }
            } else {
                if (splittedDopants.length > 2) {
                    throw new RuntimeException("The formula " + formula + "cannot be expanded. ");
                }

                expandedFormulas.add(trim(splittedDopants[0]) + " x " + trim(splittedDopants[1]) + " 1-x " + trim(formulaWithoutDopants));
            }
        } else {
            return Arrays.asList(formula);
        }

        return expandedFormulas;
    }

    public String getFabrication() {
        return fabrication;
    }

    public void setFabrication(String fabrication) {
        this.fabrication = fabrication;
    }

    public String getSubstrate() {
        return substrate;
    }

    public void setSubstrate(String substrate) {
        this.substrate = substrate;
    }

    public static Map<String, String> asAttributeMap(Material material, final String keyPrefix) {
        Map<String, String> stringStringMap = asAttributeMap(material);

        ArrayList<String> keys = new ArrayList(stringStringMap.keySet());
        for (String k : keys) {
            stringStringMap.put(keyPrefix + "_" + k, stringStringMap.get(k));
            stringStringMap.remove(k);
        }
        ;

        return stringStringMap;
    }

    public static Map<String, String> asAttributeMap(Material material) {
        List<String> resolvedFormulas = material.getResolvedFormulas();
        material.setResolvedFormulas(null);

        ObjectMapper m = new ObjectMapper();
        Map<String, String> mappedObject = m.convertValue(material, new TypeReference<Map<String, String>>() {
        });

        if (CollectionUtils.isNotEmpty(resolvedFormulas)) {
            IntStream.range(0, resolvedFormulas.size())
                .forEach(i -> mappedObject.put("resolvedFormula_" + i, resolvedFormulas.get(i)));
        }

        return mappedObject;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public String getSampleFormula() {
        return sampleFormula;
    }

    public void setSampleFormula(String sampleFormula) {
        this.sampleFormula = sampleFormula;
    }
}
