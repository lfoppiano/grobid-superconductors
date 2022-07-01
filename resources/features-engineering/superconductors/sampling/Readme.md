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
| superconductors-220629-positive_sampling-all.train | Training data with positive sampling on all SuperMat | train | positive | 164 (100%)      |                                 |
| ---                                                |
| superconductors-220624-ScComics.test               | Test data on ScComics                                | test  | N/A      | N/A             | N/A                                  |

## Sampling 

### Random sampling 

| Ratio       | Negatives | Total  | Output                                  | Job nb |
|-------------|-----------|--------|-----------------------------------------|--------|
| No sampling | 8522      | 16902  | N/A                                     | N/A    | 
| 1           | 8380      | 16760  | sampling-random-1.0r-8380p-8380n.train  | 24639  | 
| 0.75        | 6285      | 14665  | sampling-random-0.75r-8380p-6285n.train | 24640  |   
| 0.5         | 4190      | 12570  | sampling-random-0.5r-8380p-4190n.train  | 24645  | 
| 0.25        | 2095      | 10475  | sampling-random-0.25r-8380p-2095n.train | 24642  | 
| 0.1         | 838       | 9218   | sampling-random-0.1r-8380p-838n.train   | 24643  | 



### Active sampling

Using a model trained on positive examples only 

| Ratio       | Negatives | Total  | Output                                  | Job nb |
|-------------|-----------|--------|-----------------------------------------|--------|
| No sampling | 8522      | 16902  | N/A                                     | N/A    | 
| 1           | 8380      | 16760  | sampling-active-1.0r-8380p-8380n.train  | 24649  | 
| 0.75        | 6285      | 14665  | sampling-active-0.75r-8380p-6285n.train | 24648  |  
| 0.5         | 4190      | 12570  | sampling-active-0.5r-8380p-4190n.train  | 24647  | 
| 0.25        | 2095      | 10475  | sampling-active-0.25r-8380p-2095n.train | 24646  | 
| 0.1         | 838       | 9218   | sampling-active-0.1r-8380p-838n.train   | 24644  | 


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

### 90% SuperMat + Holdout
Train with 90% of the papers in SuperMat and evaluation with 10% of the remaining papers. 

| Training info | Evaluation info | Sampling type     | precision   | recall | f1-score | 
|---------------|-----------------|-------------------|-------------|--------|----------|
| 80% SuperMat  | Holdout         | no sampling       | 0.7324      | 0.7637 | 0.7478   | 
| 80% SuperMat  | Holdout         | positive sampling | 0.7546      | 0.7837 | 0.7688   |


### SuperMat + ScComics
Train with SuperMat and Evaluation with ScComics adaptation 

| Training info | Evaluation info | Sampling type     | precision | recall | f1-score | 
|---------------|-----------------|-------------------|-----------|--------|----------|
| 100% SuperMat | ScComics        | positive sampling | 0.5592    | 0.3254 | 0.4114   |
| 100% SuperMat | ScComics        | no sampling       | 0.5782    | 0.2735 | 0.3713   |



