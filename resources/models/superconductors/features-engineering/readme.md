# Feature engineering experiments

## Summary 

| Name | Changes | Precision | Recall  | F1 |  
|------|---------|-----------|---------|----|
| [baseline_142](210114-baseline) | baseline with 142 papers | 78.12   |     73.82   |     75.91|
| [extended_context](210121-extended_context) |increasing the number of token that are considered for each feature + increased window | 76.08   |     71.73   |     73.84|
| [rprop](21023-rprop_instead_of_bfg-l+extend_context) | Use rprop isntead of bfg-l| 79.95    |    69.62   |     74.43|
| [baseline+correct_feature_U0D](21023-baseline+correct_feature) | correct feature U0D |  77.85   |     73.78    |    75.75|
| [removed_features](210215-revert_context_extension+remove_features) |  removed features of fonts, italic, bold | 77.19     |   73.18   |     75.12 |
| [removed_irrelevant_papers](210219-baseline+removed_irrelevant_documents) | removed documents having less than 3 entities per paragraph | 77.1   |     72.9 |        74.93| 
| [extended context + remove irrelevant papers](210216-removed_irrelevant_papers+extended_context) | - | 75.84   |     71.79 |       73.75| 
| [baseline+batch7](210323-baseline+batch7) | baseline + batch 7 (167 papers) | 77.83   |     73.95 |       75.83 |
| [baseline+batch7+batch8](210323-baseline+batch7+batch8) | baseline + batch 7 + batch 8 (172 papers) | 77.86   |    73.83   |     75.78 |
| [baseline+batch7+batch8 2](210409-baseline+batch7+batch8) | baseline + batch 7 + batch 8 (172 papers) | 78.14   |    74.13   |     76.07 |
| [baseline+batch7+batch8 with corrections](210409-baseline+correction+batch7+batch8) | baseline + batch 7 + batch 8 (172 papers) | 78.91    |    74.95    |    76.87 |
| [baseline+batch7+batch8 with corrections + removed CDE and fonts](210426-baseline+correction+batch7+batch8-cde_and_fonts_features) | baseline + batch 7 + batch 8 (172 papers) | 78.68    |    74.65   |     76.6 |

## Feature / no-features comparison

Uniqueness = nb of unique entities / total number of entities 

|       | Precision |             | Recall   |              | F1       |              |          |
|-------|-----------|-------------|----------|--------------|----------|--------------|----------|
| Batch | Features  | No-features | Features | No-features  | Features | No-features  | Corpus uniqueness|
| 1     | 79.67 | 79.3  | 74.37 | 71.5  | 76.9  | 75.16 | 40.55 |
| 2     | 76.08 | 76.8  | 69.2  | 66.6  | 72.4  | 71.24 | 47.28 |
| 3     | 78.55 | 78.81 | 71.83 | 69.23 | 75.02 | 73.67 | 48.93 |
| 4     | 73.25 | 73.29 | 67.85 | 65.23 | 70.41 | 68.99 | 45.21 |
| 5     | 71.62 | 71.02 | 67.02 | 63.84 | 69.12 | 67.43 | 49.12 |
| 6     | 79.17 | 78.71 | 75    | 70.65 | 76.97 | 74.41 | 43.98 |
| 7     | 80.66 | 80.61 | 76.69 | 75.3  | 78.64 | 77.81 | 42.85 |
| 8     | 72.72 | 71.95 | 64.42 | 60.19 | 68.09 | 65.26 | 44.77 | 

