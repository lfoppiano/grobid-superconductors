# Sampling experiments 

## Introduction

The dataset for training/evaluation/validation is composed by positive and negative examples. 
While this terminology could be referring to binary classification, in sequence labelling, we consider a positive example one sentence containing at least one entity. 

(Lopez et al, 2021) discuss in dept this problem in the case where the dataset is strongly unbalanced toward negative examples, e.g. negatives = 100/1000 * positive. 
In such cases they define two methods: 
 - random sampling: add randomly X time the positive examples, X = 5, 10, 15, 50 (15 being the best in the Lopez et al. experiments)
 - active sampling: pass the negative examples into a model (trained on positive examples only) and collect all the examples for which the model identify any entities

## Datasets

In this section we describe the different datasets used in this experiment. 

| Filename                                           | Notes                                                     | Type  | Sampling | Coverage (nb/%) | Job nb training BidLSTM_CRF_FEATURES |
|----------------------------------------------------|-----------------------------------------------------------|-------|----------|-----------------|--------------------------------------|
| superconductors-210713-positive_sampling-all.train | Training data used most of the time                       | train | positive | 164 (100%)      | N/A                                  |
| ---                                                |
| superconductors-220615-positive_sampling-90.train  | Training data with positive sampling on 90% of the papers | train | positive | 147 (90%)       |
| superconductors-220615-no_sampling-90.train        | Training data on 90% of the papers                        | train | no       | 147 (90%)       |
| superconductors-220615-no_sampling.test            | Test data on 10% of the papers                            | test  | no       | 17 (10%)        | N/A                                  |
| ---                                                |
| superconductors-220629-no_sampling-all.train       | Training data on all SuperMat                             | train | no       | 164 (100%)      | 24630                                |
| superconductors-220629-positive_sampling-all.train | Training data with positive sampling on all SuperMat      | train | positive | 164 (100%)      | 24632                                | 
| superconductors-220624-ScComics.test               | Test data on ScComics                                     | test  | N/A      | N/A             | N/A                                  |


## Evaluation

### Manually selected 

The holdout set is composed by 10% of the documents (17 papers) randomly selected.
This leave 90% (147) papers for training for evaluation comparison.

| files | sentences | tokens   | entities | uniq_entities | classes | positive examples | negative examples |
|-------|-----------|----------|----------|---------------|---------|-------------------|-------------------|
| 147   | 19162     | 1173529  | 17171    | 7410          | 6       | 9331              | 9620              |
| 17    | 1879      | 111040   | 1527     |  661          | 6       | 825               | 1031              |

| set      | `<material>`   | `<tc>`  | `<tcValue>`  | `<class>`   | `<me_method>` | `<pressure>` |
|----------|----------------|---------|--------------|-------------|---------------|--------------|
| training | 7881           | 4025    | 1176         | 1759        | 2056          | 274          |
| holdout  | 711            | 355     | 80           | 158         | 182           | 41           |


### With stratification and holdout 10%

| files | sentences | tokens   | entities | uniq_entities | classes | positive examples | negative examples |
|-------|-----------|----------|----------|---------------|---------|-------------------|-------------------|
| 148   | 18936     | 1174522  | 17018    | 7336          | 6       | 9187              | 9749              | 
| 16    | 1871      | 110047   | 1680     | 735           | 6       | 969               | 902               |

| set      | `<material>` | `<tc>` | `<tcValue>` | `<class>` | `<me_method>` | `<pressure>` |
|----------|--------------|--------|-------------|-----------|---------------|--------------|
| training | 7831         | 3951   | 1137        | 1801      | 2016          | 282          | 
| holdout  | 761          | 116    | 429         | 119       | 33            | 222          |

['material', 'tc', 'tcValue', 'class', 'me_method', 'pressure']
[]

### With stratification and holdout 20%

| files | sentences | tokens  | entities | uniq_entities | classes | positive examples | negative examples |
|-------|-----------|---------|----------|---------------|---------|-------------------|-------------------|
| 132   | 16857     | 1028446 | 14695    | 6387          | 6       | 8033              | 8824              | 
| 32    | 3950      | 256123  | 4003     | 1684          | 6       | 2123              | 1827              |

| set      | `<material>` | `<tc>` | `<tcValue>` | `<class>` | `<me_method>` | `<pressure>` |
|----------|--------------|--------|-------------|-----------|---------------|--------------|
| training | 6610         | 3527   | 1017        | 1374      | 1903          | 264          |
| holdout  | 1982         | 853    | 239         | 543       | 335           | 51           |

### ScComics adaptation

- `Elements` -> `<material>`
- `Main` -> `<material>`
- `SC` -> `<tc>` 
- `Property` -> `<me_method>` only if the value contains `resistivity`, `susceptibility`, `specific heat`
- `Value` -> 
  - `<pressure>` if ends with `Pa` or `bar`
  - `<tcValue>` if ends with `K` 


## Results

### 90% SuperMat + Holdout
Train with 90% of the papers in SuperMat and evaluation with 10% of the remaining papers. 

| Training info | Evaluation info | Sampling type     | precision  | recall    | f1-score | 
|---------------|-----------------|-------------------|------------|-----------|----------|
| 90% SuperMat  | Holdout         | no sampling       | 0.7474     | 0.8047    | 0.7750   |
| 90% SuperMat  | Holdout         | positive sampling | 0.6136     | 0.8512    | 0.7131   |


### SuperMat + ScComics
Train with SuperMat and Evaluation with ScComics adaptation 

| Training info | Evaluation info | Sampling type     | precision | recall | f1-score | 
|---------------|-----------------|-------------------|-----------|--------|----------|
| 90% SuperMat  | ScComics        | positive sampling | 0.5501    | 0.3107 | 0.3971   |
| 90% SuperMat  | ScComics        | no sampling       | 0.5632    | 0.2702 | 0.3652   |
| 100% SuperMat | ScComics        | positive sampling | 0.5592    | 0.3254 | 0.4114   |
| 100% SuperMat | ScComics        | no sampling       | 0.5782    | 0.2735 | 0.3713   |



