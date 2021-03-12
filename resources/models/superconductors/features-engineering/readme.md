# Feature engineering experiments

## Summary 

| Name | Changes | Precision | Recall  | F1 |  
|------|---------|-----------|---------|----|
| [baseline_142](210114-baseline) | baseline with 142 papers | 78.12   |     73.82   |     75.91|
| [extended_context](210121-extended_context) |increasing the number of token that are considered for each feature + increased window | 76.08   |     71.73   |     73.84|
| [rprop](21023-rprop_instead_of_bfg-l+extend_context) | Use rprop isntead of bfg-l| 79.95    |    69.62   |     74.43|
| [removed_features](210215-revert_context_extension+remove_features) |  removed features of fonts, italic, bold | 77.19     |   73.18   |     75.12 |
| removed_irrelevant_papers | removed documents having less than 3 entities per paragraph | 77.1   |     72.9 |        74.93| 
| [extended context + remove irrelevant papers](210216-removed_irrelevant_papers+extended_context) | - | 75.84   |     71.79 |       73.75| 
| baseline_150 | baseline with X papers | 
