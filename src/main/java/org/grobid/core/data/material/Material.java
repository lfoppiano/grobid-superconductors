package org.grobid.core.data.material;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.Span;
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Material {

    private static final Logger LOGGER = LoggerFactory.getLogger(Material.class);
    private static final String ENGLISH_ALPHABETH = "xyzabcdefghijklmnopqrstuvw";
    private String name;
    private String shape;
    private Formula formula;
    private String doping;
    private String fabrication;
    private String substrate;
    private String rawTaggedValue;
    @JsonProperty("class")
    private String clazz;

    //This is the formula with each variable, replaced with random numbers, and can be used as a second attempt to
    // calculate the class from the formula
    private String sampleFormula;

    private List<Formula> resolvedFormulas = new ArrayList<>();

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

    public Formula getFormula() {
        return formula;
    }

    public void setFormula(Formula formula) {
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

    public void setResolvedFormulas(List<Formula> resolvedFormulas) {
        this.resolvedFormulas = resolvedFormulas;
    }

    public void addBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes.addAll(boundingBoxes);
    }

    public static List<String> resolveVariables(Material material) {
        if (CollectionUtils.isEmpty(material.getVariables().keySet()) || material.getFormula() == null || isBlank(material.getFormula().getRawValue())) {
            return new ArrayList<>();
        }

        if (material.getFormula() == null || StringUtils.isBlank(material.getFormula().getRawValue())) {
            LOGGER.debug("Cannot resolve variables, as the material representation doesn't have a formula " + material.getFormula());
        }

        Set<String> variables = material.getVariables().keySet();
        Set<String> containedVariables = variables.stream()
            .filter(var -> material.getFormula().getRawValue().contains(var))
            .collect(Collectors.toSet());

        List<String> outputFormulasString = new ArrayList<>();

        if (containedVariables.size() == 0) {
            return outputFormulasString;
        }

        if (containedVariables.size() != variables.size()) {
            LOGGER.debug("While processing the variables, some are not present in the material formula and " +
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
            generatePermutations(mapOfContainedVariables, new ArrayList(containedVariables), outputFormulasString, Pair.of(0, 0), material.getFormula().getRawValue());
        } catch (NumberFormatException e) {

            Map<String, List<String>> cleanedMapOfContainedVariables = new HashMap<>();
            mapOfContainedVariables.keySet().forEach(variable -> {
                List<String> cleanedList = mapOfContainedVariables.get(variable).stream()
                    .map(value -> value.replaceAll("[^\\-0-9\\.]+", ""))
                    .collect(Collectors.toList());
                cleanedMapOfContainedVariables.put(variable, cleanedList);
            });

            try {
                generatePermutations(cleanedMapOfContainedVariables, new ArrayList(containedVariables), outputFormulasString, Pair.of(0, 0), material.getFormula().getRawValue());
            } catch (NumberFormatException e2) {
                LOGGER.debug("Cannot replace variables " + Arrays.toString(variables.toArray()));
            }
        }

//        material.getResolvedFormulas().addAll(output);
//        return output.stream().map(s -> {
//            Material material1 = new Material();
//            material1.setFormula(s);
//            return material1;
//        }).collect(Collectors.toList());

        return outputFormulasString;
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
                // If the variable is prefixed by - indicate the case 1-x, 2-x which require a more thoughful rewrite
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
                    //The variable is appearing alone, we apply direct substitution
                    if (variableIndex + variable.length() < formula.length() - 1) {
                        if (!Character.isLowerCase(formula.charAt(variableIndex + variable.length()))) {
                            returnFormula = returnFormula.replaceFirst(variable, value);
                        }
                    } else if (variableIndex + variable.length() == formula.length()) {
                        returnFormula = returnFormula.replaceFirst(variable, value);
                    } else {
                        LOGGER.debug("The variable " + variable + " substitution with value " + value + " into " + formula);
                    }
                }
            }
            startSearching = variableIndex + 1;
        }
        return returnFormula;
    }

    public List<Formula> getResolvedFormulas() {
        return resolvedFormulas;
    }

    @Deprecated
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
            json.append("\"formula\" : \"" + new String(jsonUtf8Encoder.quoteAsUTF8(formula.toString())) + "\"");
        }


        if (CollectionUtils.isNotEmpty(resolvedFormulas)) {
            if (!started) {
                started = true;
            } else
                json.append(", ");

            json.append("\"resolvedFormulas\" : [");
            boolean first = true;
            for (Formula formula : resolvedFormulas) {
                if (first)
                    first = false;
                else
                    json.append(",");
                json.append("\"" + formula.getRawValue() + "\"");
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
            List<String> splittedDopants = Arrays.stream(dopants.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

            Matcher nameMaterialMatcher = nameMaterialPattern.matcher(formulaWithoutDopants);
            if (nameMaterialMatcher.find()) {
                for (String dopant : splittedDopants) {
                    expandedFormulas.add(trim(dopant) + trim(formulaWithoutDopants));
                }
            } else {
                if (splittedDopants.size() == 1) {
                    expandedFormulas.add(formula);
                } else if (splittedDopants.size() == 2) {
                    expandedFormulas.add(trim(splittedDopants.get(0)) + " x " + trim(splittedDopants.get(1)) + " 1-x " + trim(formulaWithoutDopants));
                } else if (splittedDopants.size() > 2 && splittedDopants.size() < ENGLISH_ALPHABETH.length()) {
                    char[] alphabet = ENGLISH_ALPHABETH.toCharArray();
                    StringBuilder sb = new StringBuilder();
                    StringBuilder sb2 = new StringBuilder();
                    for (int i = 0; i < splittedDopants.size() - 1; i++) {
                        sb2.append("-").append(alphabet[i]);
                    }
                    sb2.append(" ");
                    sb.append(splittedDopants.get(0)).append(" 1").append(sb2.toString());
                    for (int i = 1; i < splittedDopants.size(); i++) {
                        String split = splittedDopants.get(i);
                        sb.append(trim(split));
                        sb.append(" ");
                        sb.append(alphabet[i - 1]);
                        sb.append(" ");
                    }
                    sb.append(trim(formulaWithoutDopants));
                    expandedFormulas.add(sb.toString());
                } else {
                    String message = "The formula " + formula + " cannot be expanded. ";
                    throw new RuntimeException(message);
                }
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

        List<String> keys = new ArrayList<>(stringStringMap.keySet());
        for (String k : keys) {
            stringStringMap.put(keyPrefix + "_" + k, stringStringMap.get(k));
            stringStringMap.remove(k);
        }
        ;
        return stringStringMap;
    }

    public static Map<String, String> linkedHashMapToString(Map<String, Object> input) {
        return linkedHashMapToString(input, "");

    }

    public static Map<String, Object> stringToLinkedHashMap(List<String> keys, String value, Map<String, Object> result) {

        List<String> keysMutable = new ArrayList<>(keys);
        if (keysMutable.size() == 0) {
            //error condition, go back and forget 
            return result;
        } else if (keysMutable.size() == 1) {
            //stopping condition
            result.put(keysMutable.get(0), value);
            return result;
        } else {
            // first add, then remove first and pass over
            String firstKey = keysMutable.get(0);
            keysMutable.remove(0);
            if (!result.containsKey(firstKey)) {
                result.put(firstKey, new LinkedHashMap<String, Object>());
            }
            
            stringToLinkedHashMap(keysMutable, value, (Map<String, Object>) result.get(firstKey));
            return result;
        }
    }

    public static Map<String, String> linkedHashMapToString(Map<String, Object> input, String prefix) {
        Map<String, String> output = new HashMap<>();

        for (String key : input.keySet()) {
            if (input.get(key) instanceof String) {
                if (StringUtils.isNotBlank(prefix)) {
                    output.put(prefix + "_" + key, (String) input.get(key));
                } else {
                    output.put(key, (String) input.get(key));
                }
            } else if (input.get(key) instanceof LinkedHashMap) {
                if (StringUtils.isNotBlank(prefix)) {
                    output.putAll(linkedHashMapToString((LinkedHashMap) input.get(key), prefix + "_" + key));
                } else {
                    output.putAll(linkedHashMapToString((LinkedHashMap) input.get(key), key));
                }
            } else if (input.get(key) instanceof ArrayList) {
                ArrayList<?> item = (ArrayList<?>) input.get(key);
                for (int i = 0; i < item.size(); i++) {
                    if (item.get(i) instanceof String) {
                        if (StringUtils.isNotBlank(prefix)) {
                            output.put(prefix + "_" + key + "_" + i, (String) item.get(i));
                        } else {
                            output.put(key + "_" + i, (String) item.get(i));
                        }
                    } else if (item.get(i) instanceof LinkedHashMap) {
                        if (StringUtils.isNotBlank(prefix)) {
                            output.putAll(linkedHashMapToString((LinkedHashMap) item.get(i), prefix + "_" + key + "_" + i));
                        } else {
                            output.putAll(linkedHashMapToString((LinkedHashMap) item.get(i), key + "_" + i));
                        }
                    }
                }
            }
        }
        return output;
    }

    //This modifies the object
    public static void fillDbEntryFromAttributes(Map<String, Object> materialObject, SuperconEntry dbEntry) {
        for (String propertyName : materialObject.keySet()) {
            switch (propertyName) {
                case "formula":
                    Map<String, Object> formula = (Map<String, Object>) materialObject.get(propertyName);
                    dbEntry.setFormula((String) formula.get("rawValue"));
                    break;
                case "name":
                    dbEntry.setName((String) materialObject.get(propertyName));
                    break;
                case "clazz":
                    dbEntry.setClassification((String) materialObject.get(propertyName));
                    break;
                case "shape":
                    dbEntry.setShape((String) materialObject.get(propertyName));
                    break;
                case "doping":
                    dbEntry.setDoping((String) materialObject.get(propertyName));
                    break;
                case "fabrication":
                    dbEntry.setFabrication((String) materialObject.get(propertyName));
                    break;
                case "substrate":
                    dbEntry.setSubstrate((String) materialObject.get(propertyName));
                    break;
                case "variables":
                    dbEntry.setVariables((String) materialObject.get(propertyName));
                    break;
            }
        }
    }

    public static List<SuperconEntry> processAttributes(Span inputSpan, SuperconEntry dbEntry) {
        List<SuperconEntry> resultingEntries = new ArrayList<>();

        Map<String, Object> nestedAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, String> a : inputSpan.getAttributes().entrySet()) {
            List<String> splits = List.of(a.getKey().split("_"));
            String value = a.getValue();

            nestedAttributes = Material.stringToLinkedHashMap(splits, value, nestedAttributes);
        }
        // first level is material0, material1 -> duplicate the dbEntry
        // second level are the properties
        // third and further levels are structured information

        boolean firstMaterial = true;
        for (String materialKey : nestedAttributes.keySet()) {
            if (firstMaterial) {
                firstMaterial = false;
                // add to current
                Map<String, Object> materialObject = (Map<String, Object>) nestedAttributes.get(materialKey);
                Material.fillDbEntryFromAttributes(materialObject, dbEntry);
                resultingEntries.add(dbEntry);
            } else {
                // create new and copy from previous 
                SuperconEntry newEntry = new SuperconEntry();
                newEntry.setRawMaterial(inputSpan.getText());
                newEntry.setSection(dbEntry.getSection());
                newEntry.setSubsection(dbEntry.getSubsection());
                newEntry.setSentence(dbEntry.getSentence());
                resultingEntries.add(newEntry);

                Map<String, Object> materialObject = (Map<String, Object>) nestedAttributes.get(materialKey);
                Material.fillDbEntryFromAttributes(materialObject, newEntry);
            }
        }

        return resultingEntries;
    }

    public static Map<String, String> asAttributeMap(Material material) {

        Map<String, String> output = new HashMap<>();

        ObjectMapper m = new ObjectMapper();
        Map<String, Object> mappedObject = m.convertValue(material, new TypeReference<Map<String, Object>>() {
        });

        output = linkedHashMapToString(mappedObject, "");

        return output;
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
