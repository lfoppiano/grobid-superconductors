package org.grobid.core.utilities;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class MeasurementUtils {
    public static int WINDOW_TC = 20;

    /* We work with offsets (so no need to increase by size of the text) and we return indexes in the token list */
    protected static org.apache.commons.lang3.tuple.Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower, int centroidOffsetHigher) {
        return getExtremitiesAsIndex(tokens, centroidOffsetLower, centroidOffsetHigher, WINDOW_TC);
    }


    protected static org.apache.commons.lang3.tuple.Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower, int centroidOffsetHigher, int windowlayoutTokensSize) {
        int start = 0;
        int end = tokens.size() - 1;

        List<LayoutToken> centralTokens = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidOffsetLower || (layoutToken.getOffset() > centroidOffsetLower && layoutToken.getOffset() < centroidOffsetHigher)).collect(Collectors.toList());

        if (isNotEmpty(centralTokens)) {
            int centroidLayoutTokenIndexStart = tokens.indexOf(centralTokens.get(0));
            int centroidLayoutTokenIndexEnd = tokens.indexOf(centralTokens.get(centralTokens.size() - 1));

            if (centroidLayoutTokenIndexStart > windowlayoutTokensSize) {
                start = centroidLayoutTokenIndexStart - windowlayoutTokensSize;
            }
            if (end - centroidLayoutTokenIndexEnd > windowlayoutTokensSize) {
                end = centroidLayoutTokenIndexEnd + windowlayoutTokensSize + 1;
            }
        }

        return new ImmutablePair<>(start, end);
    }

    public static org.apache.commons.lang3.tuple.Pair<Integer, Integer> calculateQuantityExtremities(List<LayoutToken> tokens, Measurement measurement) {
        return calculateQuantityExtremities(tokens, measurement);
    }

    /**
     * Return measurement extremities as the index of the layout token. It's useful if we want to combine
     * measurement and layoutToken content.
     */
    public static org.apache.commons.lang3.tuple.Pair<Integer, Integer> calculateQuantityExtremities(List<LayoutToken> tokens, Measurement measurement, int window) {
        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremities = null;
        switch (measurement.getType()) {
            case VALUE:
                Quantity quantity = measurement.getQuantityAtomic();
                List<LayoutToken> layoutTokens = quantity.getLayoutTokens();

                extremities = getExtremitiesAsIndex(tokens, layoutTokens.get(0).getOffset(), layoutTokens.get(layoutTokens.size() - 1).getOffset(), window);

                break;
            case INTERVAL_BASE_RANGE:
                if (measurement.getQuantityBase() != null && measurement.getQuantityRange() != null) {
                    Quantity quantityBase = measurement.getQuantityBase();
                    Quantity quantityRange = measurement.getQuantityRange();

                    extremities = getExtremitiesAsIndex(tokens, quantityBase.getLayoutTokens().get(0).getOffset(), quantityRange.getLayoutTokens().get(quantityRange.getLayoutTokens().size() - 1).getOffset(), window);
                } else {
                    Quantity quantityTmp;
                    if (measurement.getQuantityBase() == null) {
                        quantityTmp = measurement.getQuantityRange();
                    } else {
                        quantityTmp = measurement.getQuantityBase();
                    }

                    extremities = getExtremitiesAsIndex(tokens, quantityTmp.getLayoutTokens().get(0).getOffset(), quantityTmp.getLayoutTokens().get(0).getOffset(), window);
                }

                break;

            case INTERVAL_MIN_MAX:
                if (measurement.getQuantityLeast() != null && measurement.getQuantityMost() != null) {
                    Quantity quantityLeast = measurement.getQuantityLeast();
                    Quantity quantityMost = measurement.getQuantityMost();

                    extremities = getExtremitiesAsIndex(tokens, quantityLeast.getLayoutTokens().get(0).getOffset(), quantityMost.getLayoutTokens().get(quantityMost.getLayoutTokens().size() - 1).getOffset(), window);
                } else {
                    Quantity quantityTmp;
                    if (measurement.getQuantityLeast() == null) {
                        quantityTmp = measurement.getQuantityMost();
                    } else {
                        quantityTmp = measurement.getQuantityLeast();
                    }

                    extremities = getExtremitiesAsIndex(tokens, quantityTmp.getLayoutTokens().get(0).getOffset(), quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getOffset(), window);
                }
                break;

            case CONJUNCTION:
                List<Quantity> quantityList = measurement.getQuantityList();
                if (quantityList.size() > 1) {
                    extremities = getExtremitiesAsIndex(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset(), quantityList.get(quantityList.size() - 1).getLayoutTokens().get(0).getOffset(), window);
                } else {
                    extremities = getExtremitiesAsIndex(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset(), quantityList.get(0).getLayoutTokens().get(0).getOffset(), window);
                }

                break;
        }
        return extremities;
    }

    /**
     * Return the offset of the measurement - useful for matching into sentences or lexicons
     */
    public static org.apache.commons.lang3.tuple.Pair<Integer, Integer> calculateExtremitiesOffsets(Measurement measurement) {
        List<org.apache.commons.lang3.tuple.Pair<Integer, Integer>> offsets = new ArrayList<>();

        switch (measurement.getType()) {
            case VALUE:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityAtomic()));
                break;
            case INTERVAL_BASE_RANGE:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityBase()));
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityRange()));

                break;
            case INTERVAL_MIN_MAX:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityLeast()));
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityMost()));
                break;

            case CONJUNCTION:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityList()));

                break;
        }
        return QuantityOperations.getContainingOffset(offsets);
    }


    /**
     * Given a list of measurements, retain only the type requested in the second parameter
     **/
    public static List<Measurement> filterMeasurements(List<Measurement> process, List<UnitUtilities.Unit_Type> typesToBeKept) {
        List<Measurement> filteredMeasurements = process.stream().filter(measurement -> {
            switch (measurement.getType()) {
                case VALUE:
                    return typesToBeKept.contains(measurement.getQuantityAtomic().getType());
                case CONJUNCTION:
                    return measurement.getQuantityList()
                            .stream().anyMatch(quantity -> typesToBeKept.contains(quantity.getType()));
                case INTERVAL_BASE_RANGE:
                    return typesToBeKept.contains(measurement.getQuantityBase().getType()) ||
                            typesToBeKept.contains(measurement.getQuantityRange().getType());

                case INTERVAL_MIN_MAX:
                    return (measurement.getQuantityMost() != null && typesToBeKept.contains(measurement.getQuantityMost().getType())) ||
                            (measurement.getQuantityLeast() != null && typesToBeKept.contains(measurement.getQuantityLeast().getType()));

            }

            return false;
        }).collect(Collectors.toList());

        return filteredMeasurements;
    }

    /**
     * Transform the complex Measurement object in a list of Pair(Quantity, Measurement)
     */
    public static List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> flattenMeasurements(List<Measurement> measurements) {
        return measurements.stream().flatMap(m -> MeasurementUtils.flattenMeasurement(m).stream()).collect(Collectors.toList());
    }

    public static List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> flattenMeasurement(Measurement measurement) {
        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> results = new ArrayList<>();

        if (measurement.getType().equals(UnitUtilities.Measurement_Type.VALUE)) {
            results.add(org.apache.commons.lang3.tuple.Pair.of(measurement.getQuantityAtomic(), measurement));
        } else if (measurement.getType().equals(UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE)) {

            if (measurement.getQuantityBase() != null) {
                results.add(org.apache.commons.lang3.tuple.Pair.of(measurement.getQuantityBase(), measurement));
            }

            if (measurement.getQuantityRange() != null) {
                results.add(org.apache.commons.lang3.tuple.Pair.of(measurement.getQuantityRange(), measurement));
            }

        } else if (measurement.getType().equals(UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX)) {
            if (measurement.getQuantityLeast() != null) {
                results.add(org.apache.commons.lang3.tuple.Pair.of(measurement.getQuantityLeast(), measurement));
            }

            if (measurement.getQuantityMost() != null) {
                results.add(org.apache.commons.lang3.tuple.Pair.of(measurement.getQuantityMost(), measurement));
            }

        } else if (measurement.getType().equals(UnitUtilities.Measurement_Type.CONJUNCTION)) {
            List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> collect = measurement.getQuantityList()
                    .stream()
                    .map(q -> Pair.of(q, measurement))
                    .collect(Collectors.toList());

            results.addAll(collect);
        }

        return results;
    }
}
