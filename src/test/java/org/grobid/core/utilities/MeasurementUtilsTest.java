package org.grobid.core.utilities;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import org.grobid.core.data.Value;
import org.grobid.core.layout.LayoutToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class MeasurementUtilsTest {

    @Test
    public void testCalculateQuantityExtremities_AtomicValue() throws Exception {
        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The car is weighting only 10.77 grams. ");
        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.VALUE);
        final Unit unit = new Unit("grams");
        unit.setLayoutTokens(Arrays.asList(tokens.get(14)));

        final Quantity quantity = new Quantity("10.77", unit);
        quantity.setLayoutTokens(Arrays.asList(tokens.get(10), tokens.get(11), tokens.get(12)));
        measurement.setAtomicQuantity(quantity);

        Pair<Integer, Integer> extremities = MeasurementUtils.calculateExtremitiesAsIndex(measurement, tokens);

        assertThat(extremities.getLeft(), is(10));
        assertThat(extremities.getRight(), is(13));

        assertThat(LayoutTokensUtil.toText(tokens.subList(extremities.getLeft(), extremities.getRight())), is("10.77"));

    }

    @Test
    public void testCalculateQuantityExtremities_baseRange() throws Exception {

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The weight was 10 +- 2.2 grams.");

        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(16);
        unit.setOffsetEnd(21);

        final Quantity quantityBase = new Quantity("10", unit);
        quantityBase.setOffsetStart(8);
        quantityBase.setOffsetEnd(10);
        quantityBase.setLayoutTokens(Collections.singletonList(layoutTokens.get(6)));
        measurement.setQuantityBase(quantityBase);

        final Quantity quantityRange = new Quantity("2.2", unit);
        quantityRange.setOffsetStart(14);
        quantityRange.setOffsetEnd(15);
        quantityRange.setLayoutTokens(Arrays.asList(layoutTokens.get(9), layoutTokens.get(10), layoutTokens.get(11)));
        measurement.setQuantityRange(quantityRange);

        Pair<Integer, Integer> extremities = MeasurementUtils.calculateExtremitiesAsIndex(measurement, layoutTokens);

        assertThat(extremities.getLeft(), is(6));
        assertThat(extremities.getRight(), is(12));
    }

    @Test
    public void testCalculateQuantityExtremities_MinMax() throws Exception {

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The weight was 10 to 22.2 grams.");

        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX);
        final Unit unit = new Unit("grams");

        final Quantity quantityLeast = new Quantity("10", unit);
        quantityLeast.setLayoutTokens(Collections.singletonList(layoutTokens.get(6)));
        measurement.setQuantityLeast(quantityLeast);

        final Quantity quantityMost = new Quantity("22.2", unit);
        quantityMost.setLayoutTokens(Arrays.asList(layoutTokens.get(9), layoutTokens.get(10), layoutTokens.get(11), layoutTokens.get(12)));
        measurement.setQuantityMost(quantityMost);

        Pair<Integer, Integer> extremities = MeasurementUtils.calculateExtremitiesAsIndex(measurement, layoutTokens);

        assertThat(extremities.getLeft(), is(6));
        assertThat(extremities.getRight(), is(13));
    }

    @Test
    public void testCalculateQuantityExtremitiesList() throws Exception {

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken("The weight was 10, 15 and 22.2 grams.");

        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.CONJUNCTION);
        final Unit unit = new Unit("grams");

        final Quantity quantity1 = new Quantity("10", unit);
        quantity1.setLayoutTokens(Arrays.asList(layoutTokens.get(6)));

        final Quantity quantity2 = new Quantity("15", unit);
        quantity2.setLayoutTokens(Arrays.asList(layoutTokens.get(9)));

        final Quantity quantity3 = new Quantity("22.2", unit);
        quantity3.setLayoutTokens(Arrays.asList(layoutTokens.get(13), layoutTokens.get(14), layoutTokens.get(15)));

        List<Quantity> quantityList = new ArrayList<>();
        quantityList.add(quantity1);
        quantityList.add(quantity2);
        quantityList.add(quantity3);

        measurement.setQuantityList(quantityList);

        Pair<Integer, Integer> extremities = MeasurementUtils.calculateExtremitiesAsIndex(measurement, layoutTokens);

        assertThat(extremities.getLeft(), is(6));
        assertThat(extremities.getRight(), is(16));


    }


    @Test
    public void testFlattenMeasurement_atomic() throws Exception {

        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.VALUE);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(11);
        unit.setOffsetEnd(16);

        final Quantity quantity = new Quantity("10", unit);
        quantity.setOffsetStart(8);
        quantity.setOffsetEnd(10);

        final Value parsedValue = new Value();
        parsedValue.setRawValue(quantity.getRawValue());
        parsedValue.setOffsetStart(0);
        parsedValue.setOffsetEnd(2);
        quantity.setParsedValue(parsedValue);
        measurement.setAtomicQuantity(quantity);

        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(1));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(0).getLeft(), is(quantity));
    }

    @Test
    public void testFlattenMeasurement_interval() throws Exception {
        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(16);
        unit.setOffsetEnd(21);

        final Quantity quantityLeast = new Quantity("1", unit);
        quantityLeast.setOffsetStart(8);
        quantityLeast.setOffsetEnd(9);
        measurement.setQuantityLeast(quantityLeast);

        final Quantity quantityMost = new Quantity("12", unit);
        quantityMost.setOffsetStart(13);
        quantityMost.setOffsetEnd(15);
        measurement.setQuantityMost(quantityMost);

        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(2));
        assertThat(pairs.get(0).getLeft(), is(quantityLeast));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(1).getLeft(), is(quantityMost));
        assertThat(pairs.get(1).getRight(), is(measurement));
    }

    @Test
    public void testFlattenMeasurement_interval_incomplete() throws Exception {
        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX);
        Quantity quantityLeast = new Quantity("12", new Unit("grams", 16, 21), 13, 15);
        measurement.setQuantityLeast(quantityLeast);

        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(1));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(0).getLeft(), is(quantityLeast));
    }

    @Test
    public void testFlattenMeasurement_range() throws Exception {
        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(16);
        unit.setOffsetEnd(21);

        final Quantity quantityBase = new Quantity("10", unit);
        quantityBase.setOffsetStart(8);
        quantityBase.setOffsetEnd(10);
        measurement.setQuantityBase(quantityBase);

        final Quantity quantityRange = new Quantity("2", unit);
        quantityRange.setOffsetStart(14);
        quantityRange.setOffsetEnd(15);
        measurement.setQuantityRange(quantityRange);

        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(2));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(0).getLeft(), is(quantityBase));
        assertThat(pairs.get(1).getRight(), is(measurement));
        assertThat(pairs.get(1).getLeft(), is(quantityRange));
    }

    @Test
    public void testFlattenMeasurement_range_incomplete() throws Exception {
        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(16);
        unit.setOffsetEnd(21);

        final Quantity quantityBase = new Quantity("10", unit);
        quantityBase.setOffsetStart(8);
        quantityBase.setOffsetEnd(10);
        measurement.setQuantityBase(quantityBase);

        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(1));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(0).getLeft(), is(quantityBase));
    }

    @Test
    public void testFlattenMeasurement_list() throws Exception {

        Measurement measurement = new Measurement();
        measurement.setType(UnitUtilities.Measurement_Type.CONJUNCTION);
        final Unit unit = new Unit("grams");
        unit.setOffsetStart(20);
        unit.setOffsetEnd(25);

        final Quantity quantity1 = new Quantity("1", unit);
        quantity1.setOffsetStart(8);
        quantity1.setOffsetEnd(9);

        final Quantity quantity2 = new Quantity("5", unit);
        quantity2.setOffsetStart(11);
        quantity2.setOffsetEnd(12);

        final Quantity quantity3 = new Quantity("12", unit);
        quantity3.setOffsetStart(17);
        quantity3.setOffsetEnd(19);

        List<Quantity> quantityList = new ArrayList<>();
        quantityList.add(quantity1);
        quantityList.add(quantity2);
        quantityList.add(quantity3);

        measurement.setQuantityList(quantityList);


        List<Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

        assertThat(pairs, hasSize(3));
        assertThat(pairs.get(0).getRight(), is(measurement));
        assertThat(pairs.get(0).getLeft(), is(quantity1));
        assertThat(pairs.get(1).getRight(), is(measurement));
        assertThat(pairs.get(1).getLeft(), is(quantity2));
        assertThat(pairs.get(2).getRight(), is(measurement));
        assertThat(pairs.get(2).getLeft(), is(quantity3));
    }

}