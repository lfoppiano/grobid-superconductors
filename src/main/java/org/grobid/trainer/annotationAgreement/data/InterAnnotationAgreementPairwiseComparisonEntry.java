package org.grobid.trainer.annotationAgreement.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent comparison data between two annotators/raters
 * with a list of categories and their respective annotation agreement indexes
 */
public class InterAnnotationAgreementPairwiseComparisonEntry {

    private int rater0;
    private int rater1;

    private Map<String, Double> agreementByCategory = new HashMap<>();
    private double agreementAverage = 0.0;

    public InterAnnotationAgreementPairwiseComparisonEntry(int rater0, int rater1) {

        this.rater0 = rater0;
        this.rater1 = rater1;
    }

    public int getRater0() {
        return rater0;
    }

    public void setRater0(int rater0) {
        this.rater0 = rater0;
    }

    public int getRater1() {
        return rater1;
    }

    public void setRater1(int rater1) {
        this.rater1 = rater1;
    }


    public void addAgreementFigures(String category, Double measurement) {
        this.agreementByCategory.put(category, measurement);
    }

    public void setAgreementAverage(double agreementAverage) {
        this.agreementAverage = agreementAverage;
    }

    public Map<String, Double> getAgreementByCategory() {
        return agreementByCategory;
    }

    public double getAgreementAverage() {
        return agreementAverage;
    }
}
