# Sampling experiments 

## Introduction

The dataset for training/evaluation/validation is composed by positive and negative examples. 
While this terminology could be referring to binary classification, in sequence labelling, we consider a positive example one sentence containing at least one entity. 

(Lopez et al, 2021) discuss in dept this problem in the case where the dataset is strongly unbalanced toward negative examples, e.g. negatives = 100/1000 * positive. 
In such cases they define two methods: 
 - random sampling: add randomly X time the positive examples, X = 5, 10, 15, 50 (15 being the best in the Lopez et al. experiments)
 - active sampling: pass the negative examples into a model (trained on positive examples only) and collect all the examples for which the model identify any entities

### Sampling script 

The sampling script works with the CRF Grobid data, parse it and re-balance the number of negative examples based on the two sampling strategies (active, random). 

```
usage: sampling.py [-h] --input INPUT [--input-negatives INPUT_NEGATIVES] [--model-config MODEL_CONFIG] --output OUTPUT [--output-only-negatives OUTPUT_ONLY_NEGATIVES] --action
{random_sampling,active_sampling} --ratio RATIO

Script for applying over and under sampling from a CRF training file

optional arguments:
-h, --help            show this help message and exit
--input INPUT         Input file containing examples.
--input-negatives INPUT_NEGATIVES Optional input file containing only negative examples. With this option it is assumed that --input provides only positive examples
--model-config MODEL_CONFIG DeLFT configuration file model to be used for active sampling.
--output OUTPUT       Directory where to save the sampled output.
--output-only-negatives OUTPUT_ONLY_NEGATIVES Indicate whether to write only negative examples in the output file.
--action {random_sampling,active_sampling}
--ratio RATIO         Sampling ratio
```

For active sampling is necessary to pass the path of the DeLFT model `config.json` (option `--model-config`) file in order to load the model used for selective the negative examples.   

## Datasets

In this section we describe the different datasets used in this experiment. 

| Filename                                           | Notes                                                | Type  | Sampling | Coverage (nb/%) | Job nb training BidLSTM_CRF_FEATURES |
|----------------------------------------------------|------------------------------------------------------|-------|----------|-----------------|--------------------------------------|
| superconductors-210713-positive_sampling-all.train | Training data used most of the time                  | train | positive | 164 (100%)      | N/A                                  |
| ---                                                |
| superconductors-220630-positive_sampling.train     | Training data with positive sampling (training set)  | train | positive | 132 (80%)       | 24636                                |
| superconductors-220630-no_sampling.train           | Training data (training set)                         | train | no       | 132 (80%)       | 24635                                |
| superconductors-220630-no_sampling.test            | Test data (holdout set)                              | test  | no       | 32 (20%)        | N/A                                  |
| ---                                                |
| superconductors-220629-no_sampling-all.train       | Training data on all SuperMat                        | train | no       | 164 (100%)      | 24630                                |
| superconductors-220629-positive_sampling-all.train | Training data with positive sampling on all SuperMat | train | positive | 164 (100%)      |                                      |
| ---                                                |
| superconductors-220624-ScComics.test               | Test data on ScComics                                | test  | N/A      | N/A             | N/A                                  |

## Sampling 

### Random sampling 

| Ratio       | Negatives | Total  | Output                                  | Job nb                 |
|-------------|-----------|--------|-----------------------------------------|------------------------|
| No sampling | 8522      | 16902  | N/A                                     | N/A                    | 
| 1           | 8380      | 16760  | sampling-random-1.0r-8380p-8380n.train  | ~~24639~~24678, 24690  | 
| 0.75        | 6285      | 14665  | sampling-random-0.75r-8380p-6285n.train | ~~24640~~24677, 24689  |   
| 0.5         | 4190      | 12570  | sampling-random-0.5r-8380p-4190n.train  | ~~24645~~24676, 24688  | 
| 0.25        | 2095      | 10475  | sampling-random-0.25r-8380p-2095n.train | ~~24642~~24675, 24688  | 
| 0.1         | 838       | 9218   | sampling-random-0.1r-8380p-838n.train   | ~~24643~~24674, 24686  |



### Active sampling

Using a model trained on positive examples only 

| Ratio       | Negatives | Total  | Output                                  | Job nb                |
|-------------|-----------|--------|-----------------------------------------|-----------------------|
| No sampling | 8522      | 16902  | N/A                                     | N/A                   | 
| 1           | 8380      | 16760  | sampling-active-1.0r-8380p-8380n.train  | ~~24649~~24673, 24685 | 
| 0.75        | 6285      | 14665  | sampling-active-0.75r-8380p-6285n.train | ~~24648~~24672, 24683 |  
| 0.5         | 4190      | 12570  | sampling-active-0.5r-8380p-4190n.train  | ~~24647~~24671, 24684 | 
| 0.25        | 2095      | 10475  | sampling-active-0.25r-8380p-2095n.train | ~~24646~~24670, 24682 | 
| 0.1         | 838       | 9218   | sampling-active-0.1r-8380p-838n.train   | ~~24644~~24669, 24681 | 


## Holdout set selection 

### Manually selected 

The holdout set is composed by 20% of the documents (32 papers) randomly selected.
This leave 80% (132) papers for training for evaluation comparison.

|          | files  | sentences  | tokens  | entities  | uniq_entities  | classes  | positive_examples  | negative_examples  |
|----------|--------|------------|---------|-----------|----------------|----------|--------------------|--------------------|
| training | 132    | 16902      | 1047574 | 15586     | 6699           | 6        | 8380               | 8522               |
| holdout  | 32     | 3905       | 236995  | 3112      | 1372           | 6        | 1776               | 2129               |
| ratio    | 24.24% | 23.10%     | 22.62%  | 19.97%    | 20.48%         | 100.00%  | 21.19%             | 24.98%             |

|          | tc     | class  | material  | me_method  | tcValue  | pressure  |
|----------|--------|--------|-----------|------------|----------|-----------|
| training | 3741   | 1646   | 6943      | 1883       | 1099     | 274       |
| holdout  | 1649   | 639    | 157       | 271        | 355      | 41        |
| ratio    | 17.08% | 16.46% | 23.75%    | 18.85%     | 14.29%   | 14.96%    |

### With stratification and holdout 20%

| files | sentences | tokens  | entities | uniq_entities | classes | positive examples | negative examples |
|-------|-----------|---------|----------|---------------|---------|-------------------|-------------------|
| 132   | 16857     | 1028446 | 14695    | 6387          | 6       | 8033              | 8824              | 
| 32    | 3950      | 256123  | 4003     | 1684          | 6       | 2123              | 1827              |

| set      | `<material>` | `<tc>` | `<tcValue>` | `<class>` | `<me_method>` | `<pressure>` |
|----------|--------------|--------|-------------|-----------|---------------|--------------|
| training | 6610         | 3527   | 1017        | 1374      | 1903          | 264          |
| holdout  | 1982         | 853    | 239         | 543       | 335           | 51           |
| ratio    | 30%          | 24%    | 23%         | 39%       | 17%           | 20%          |

### ScComics adaptation

- `Elements` -> `<material>`
- `Main` -> `<material>`
- `SC` -> `<tc>` 
- `Property` -> `<me_method>` only if the value contains `resistivity`, `susceptibility`, `specific heat`
- `Value` -> 
  - `<pressure>` if ends with `Pa` or `bar`
  - `<tcValue>` if ends with `K` 


## Results

### 80% SuperMat + 20% Holdout
Train with 80% of the papers in SuperMat and evaluation with a fixed corpus comprising the remaining papers. 

| Training info                                                 | Evaluation info | Sampling type                | precision     | recall        | f1-score              | Avg F1      |
|---------------------------------------------------------------|-----------------|------------------------------|---------------|---------------|-----------------------|-------------|
| superconductors-no_sampling-80supermat (LOWER BASELINE)       | Holdout         | no sampling                  | 0.7324        | 0.7637        | 0.7478                | 
| superconductors-positive_sampling-80supermat (UPPER BASELINE) | Holdout         | positive sampling            | 0.7546        | 0.7837        | 0.7688                |
| ---                                                           | 
| superconductors-random_sampling-0.1r                          | Holdout         | random undersampling at 0.1  | 0.7194/0.7393 | 0.7872/0.7949 | 0.7518/0.7661         | 0.7590      |
| superconductors-random_sampling-0.25r                         | Holdout         | random undersampling at 0.25 | 0.7221/0.7275 | 0.7895/0.7965 | 0.7543/0.7605         | 0.7574      |
| superconductors-random_sampling-0.50r                         | Holdout         | random undersampling at 0.5  | 0.7565/0.7589 | 0.7859/0.7862 | **0.7709**/**0.7723** | **0.7716**  |
| superconductors-random_sampling-0.75r                         | Holdout         | random undersampling at 0.75 | 0.7463/0.7393 | 0.7743/0.7830 | 0.7601/0.7605         | 0.7603      |
| superconductors-random_sampling-1.0r                          | Holdout         | random undersampling at 1.0  | 0.7701/0.7541 | 0.7644/0.7788 | 0.7672/0.7663         | 0.7668      |
| ---                                                           |
| superconductors-active_sampling-0.1r                          | Holdout         | active undersampling at 0.1  | 0.7274/0.7142 | 0.7917/0.7985 | 0.7582/0.7540         | 0.7561      | 
| superconductors-active_sampling-0.25r                         | Holdout         | active undersampling at 0.25 | 0.7042/0.7236 | 0.8075/0.7917 | 0.7523/0.7561         | 0.7542      |
| superconductors-active_sampling-0.50r                         | Holdout         | active undersampling at 0.5  | 0.7454/0.7331 | 0.8046/0.7991 | **0.7738**/0.7647     | **0.7692**  |
| superconductors-active_sampling-0.75r                         | Holdout         | active undersampling at 0.75 | 0.7334/0.7598 | 0.7940/0.7756 | 0.7625/0.7676         | 0.7650      |
| superconductors-active_sampling-1.0r                          | Holdout         | active undersampling at 1.0  | 0.7442/0.7474 | 0.7782/0.7959 | 0.7608/**0.7709**     | 0.7658      |

### SuperMat + ScComics
Train with SuperMat and Evaluation with ScComics adaptation 

| Training info                                 | Evaluation info   | Sampling type                | precision | recall | f1-score | 
|-----------------------------------------------|-------------------|------------------------------|-----------|--------|----------|
| superconductors-no_sampling-all               | ScComics          | no sampling                  | 0.5782    | 0.2735 | 0.3713   |
| grobid-superconductors-positive_sampling-all  | ScComics          | positive sampling            | 0.5592    | 0.3254 | 0.4114   |
| ---                                           |



Temporary information, will remove after I trained all the models with random and active sampling, using the 100% SuperMat data: 

| Training info                                 | Evaluation info   | Sampling type                | precision | recall | f1-score    | 
|-----------------------------------------------|-------------------|------------------------------|-----------|--------|-------------|
| superconductors-no_sampling-80supermat        | ScComics          | no sampling                  | 0.5341    | 0.2413 | 0.3324      |
| superconductors-positive_sampling-80supermat  | ScComics          | positive sampling            | 0.5633    | 0.2472 | 0.3436      |
| ---                                           |
| superconductors-random_sampling-0.1r          | ScComics          | random undersampling at 0.1  | 0.5484    | 0.2543 | **0.3475**  |
| superconductors-random_sampling-0.25r         | ScComics          | random undersampling at 0.25 | 0.5411    | 0.2517 | 0.3435      |
| superconductors-random_sampling-0.50r         | ScComics          | random undersampling at 0.5  | 0.5586    | 0.2455 | 0.3411      |
| superconductors-random_sampling-0.75r         | ScComics          | random undersampling at 0.75 | 0.5525    | 0.2487 | 0.3430      |
| superconductors-random_sampling-1r            | ScComics          | random undersampling at 1.0  | 0.5697    | 0.2479 | 0.3455      |
| ---                                           |
| superconductors-active_sampling-0.1r          | ScComics          | active undersampling at 0.1  | 0.5638    | 0.2619 | **0.3577**  |
| superconductors-active_sampling-0.25r         | ScComics          | active undersampling at 0.25 | 0.5496    | 0.2649 | 0.3574      |
| superconductors-active_sampling-0.50r         | ScComics          | active undersampling at 0.5  | 0.5516    | 0.2445 | 0.3388      |
| superconductors-active_sampling-0.75r         | ScComics          | active undersampling at 0.75 | 0.5516    | 0.2556 | 0.3494      |
| superconductors-active_sampling-1r            | ScComics          | active undersampling at 1.0  | 0.5516    | 0.2445 | 0.3388      |


## Implementation / Test

- random sampling at 0.5 using all the dataset
  - extracted data at 0.5: `sampling-random-0.5r-10156p-5078n.train`
  - Results: 
    - 10fold with BidLSTM_CRF_FEATURES: 24693
      - 0.8069    0.8125    0.8096
    - 5fold with BidLSTM_CRF_FEATURES: 24698
      - 0.8037    0.8141    0.8088
    - 10fold with BERT_CRF: 24694
      - 0.7862    0.8356    0.8101
- active sampling at 0.5 using all the dataset: 
  - train model with positive sampling: 24692
  - extracted data at 0.5: `sampling-active-0.5r-10156p-5078n.train`
  - Results:
    - 10fold with BidLSTM_CRF_FEATURES: 24695
      - 0.8081    0.8182    0.8129
    - 5fold with BidLSTM_CRF_FEATURES: 24697
      - 0.8041    0.8332    0.8183
    - 10fold with BERT_CRF: 24696
      - 0.7821    0.8413    0.8106
