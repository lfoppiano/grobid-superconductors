# Linking entities 

This page summarises the effort and evaluation of the entity linking approaches

## Summary results

In this table we show the results in comparison with the baseline. For all the results, see the [table below](#summary-results).

### Material-Tc

#### Baseline

(-) strong bias due to the fact that the model has been trained and evaluated on the same corpus 

| Name | Method | Task | Description | Precision | Recall  | F1 | St Dev |
|------|--------|---------|-----------|---------|---------|--------|------|
| rb-supermat-baseline      | Rule-based    | material-tcValue | eval against SuperMat       | 88    | 74    | 81    | |
| crf-10fold-baseline       | CRF           | material-tcValue | 10 fold cross-validation    | 68.52  | 70.11 | 69.16 | | 
| crf-supermat-baseline     | CRF           | material-tcValue | eval against SuperMat (-)   | 91    | 66    | 77    | |
| crf-10fold-baseline       | CRF           | tcValue-pressure | 10 fold cross-validation    | 72.92 | 67.67  | 69.76 | |
| crf-10fold-baseline       | CRF           | tcValue-me_method | 10 fold cross-validation    | 49.99 | 45.21 | 44.65 | |
