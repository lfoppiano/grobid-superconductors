package org.grobid.core.utilities;

import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import org.grobid.core.data.Value;
import org.grobid.core.layout.LayoutToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

public class MeasurementUtilsTest {
    @Test
    public void testGetExtremitiesIndex_short_nearBeginning() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = MeasurementUtils.getExtremitiesAsIndex(tokens, 5, 5, 3);

        assertThat(extremitiesSingle.getLeft(), is(0));
        assertThat(extremitiesSingle.getRight(), is(6));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is("This is a "));
    }

    @Test
    public void testGetExtremitiesSingle_short_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a short sentence");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = MeasurementUtils.getExtremitiesAsIndex(tokens, 8, 8, 3);

        assertThat(extremitiesSingle.getLeft(), is(1));
        assertThat(extremitiesSingle.getRight(), is(8));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" is a short "));
    }

    @Test
    public void testGetExtremitiesSingle_long_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = MeasurementUtils.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
    }


    @Test
    public void testGetExtremitiesSingle_long_centroidWithMultipleLayoutToken_middle() {

        List<LayoutToken> tokens = QuantityAnalyzer.getInstance().tokenizeWithLayoutToken("This is a very very very long sentence, and we keep writing.");

        org.apache.commons.lang3.tuple.Pair<Integer, Integer> extremitiesSingle = MeasurementUtils.getExtremitiesAsIndex(tokens, 25, 25, 5);

        assertThat(extremitiesSingle.getLeft(), is(7));
        assertThat(extremitiesSingle.getRight(), is(18));
        List<String> stringList = tokens.subList(extremitiesSingle.getLeft(), extremitiesSingle.getRight()).stream().map(LayoutToken::getText).collect(Collectors.toList());
        assertThat(String.join("", stringList), is(" very very long sentence, and"));
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

        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

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

        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

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

        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

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

        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

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

        List<org.apache.commons.lang3.tuple.Pair<Quantity, Measurement>> pairs = MeasurementUtils.flattenMeasurement(measurement);

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