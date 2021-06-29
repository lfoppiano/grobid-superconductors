# Feature engineering experiments with DL

## Summary best results

In this table are shown the best results in comparison with the baseline. For all the results, see the [table below](#summary-results). 

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline_no_features](baseline/baseline_no_features) | 172 papers, no features | 77.00  |  77.20 |   77.09 |   0.61    |
| [baseline_with_features](baseline_features) | 172 papers, features | 77.95  |  77.27  |  77.60  |    0.99     |
| oL+Sm| Oscar Large + SuperMat | - | -  | - |
| [fastText_oL+Sm_bin_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 76.20  |  77.19  |  76.68 | 1.11 |
| [fastText_oL+Sm_vec_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.08  | 76.76  |  76.91 | 0.82 |
| [fastText_oL+Sm_bin_skipgram_300d_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 78.04  |  77.92  |  77.98 | **0.21** |
| [fastText_oL+Sm_vec_skipgram_300d_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.60  |  78.42  |  **78.00** | 0.66|
| oL+Sc| Oscar Large + Science | - | -  | - |
| [fastText_oL+Sc_bin_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 76.74  |  76.77  |  76.75 | 0.87  |
| [fastText_oL+Sc_vec_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 76.42  |  76.04  |  76.21  | 0.75 |
| [fastText_oL+Sc_bin_skipgram_300d_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 77.13  |  77.22  |  77.16| 1.03 |
| [fastText_oL+Sc_vec_skipgram_300d_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 77.60  |  77.39  |  77.49  | 0.59 |
| oL+Sc+Sm | Oscar Large + Science + SuperMat | - | -  | - |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 76.66 | 76.78  |76.70  | 0.80 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 76.72  |  77.21 | 76.96  | 0.90 |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 77.19 |  78.00  |  77.58 | 1.01 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 77.21 |  78.00  |  77.60 | 0.49 |

## Other experiments 

### Removing batch 8
Batch 8 resulted to score the least in the cross-validation with only single batches

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline-batch_1_7-no_features](baseline/baseline-batch_1_7-no_features) | baseline_no_features + gloVe + bin |  77.18  | 79.43   | 78.29  | 0.61 |
| [baseline-batch_1_7-features](baseline/baseline-batch_1_7-features) | baseline_features + gloVe + bin | 77.81 | 79.09 | 78.43  | 0.66 |
| [fastText_oS+Sc+Sm-batch_1_7-no_features](oS+Sc+Sm/fastText_oS+Sc+Sm-batch_1_7-no_features) | baseline_no_features + oS+Sc+Sm + bin | 75.23  |  78.37  |  76.75  | 1.26 |
| [fastText_oS+Sc+Sm-batch_1_7-features](oS+Sc+Sm/fastText_oS+Sc+Sm-batch_1_7-features) |  baseline_features + oS+Sc+Sm + bin |  74.69  |  79.10   | 76.81   | 0.80 |

### Removing batch 8 and 2
Batch 8 and 2 resulted to score the least in the cross-validation with only single batches

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline-batches-28-no_features](baseline/baseline-batches-28-no_features) | baseline_no_features + gloVe + bin | 78.21  |  78.46  |  78.33 | 0.69 |
| [baseline-batches-28-features](baseline/baseline-batches-28-features) | baseline_features + gloVe + bin | 77.66  |  78.48 |   78.06   | 0.95 |
| [fastText_oS+Sc+Sm-batches-28-no_features](oS+Sc+Sm/fastText_oS+Sc+Sm-batches-28-no_features) | baseline_no_features + oS+Sc+Sm + bin | 76.87  |  78.51  |  77.68  | 0.84 |
| [fastText_oS+Sc+Sm-batches-28-features](oS+Sc+Sm/fastText_oS+Sc+Sm-batches-28-features) |  baseline_features + oS+Sc+Sm + bin | 77.17  |  78.85  | 77.99 | 0.48 |

### Replace <other> with POS tag
We replaced the `<other>` label with the relative POS tag of the word

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline-POS-no_features](baseline/baseline-POS-no_features) | baseline_no_features + gloVe + bin | 87.46  |  86.78  |  87.12 | |
| [baseline-POS-features](baseline/baseline-POS-features) | baseline_features + gloVe + bin | 87.53  |  87.10  |  87.32   | |
| [fastText_oS+Sc+Sm-POS-no_features](fastText_oS+Sc+Sm-POS-no_features) | baseline_no_features + oS+Sc+Sm + bin |  | |
| [fastText_oS+Sc+Sm-POS-features](fastText_oS+Sc+Sm-POS-features) |  baseline_features + oS+Sc+Sm + bin |  |  |

# Embeddings 

| Name | Source corpus | Size | Vector dimension | 
|------|---------------|--------|------------------|
| oS | Oscar 1-12 | 4M vocab | 100, 300| | 
| oL | Oscar | 4M vocab | 300| [https://oscar-corpus.com/](Oscar), [https://github.com/lfoppiano/SuperMat](SuperMat) | 
| Sc | Science | - | 300| -  | - |  
| oS+Sm | Oscar 1-12 + Supermat | 4M vocab | 100, 300| [https://oscar-corpus.com/](Oscar), [https://github.com/lfoppiano/SuperMat](SuperMat) | 
| oS+Sc | Oscar 1-12 + Science | - | 300| - |
| oS+Sc+Sm | Oscar 1-12 + Science + SuperMat| - | 300| - |
| oL+Sm | Oscar + Supermat | 4M vocab | 300| [https://oscar-corpus.com/](Oscar), [https://github.com/lfoppiano/SuperMat](SuperMat) | 
| oL+Sc | Oscar + Science | - | 300| - |
| oL+Sc+Sm | Oscar + Science + SuperMat| - | 300| - |
| oS+Sc+Sm | Oscar 1-12 + Science + SuperMat| - | 300| - |
| glove.840B.300d | Common Crawl | 840B tokens, 2.2M vocab, cased | 300 |https://nlp.stanford.edu/projects/glove/ | 
| crawl-300d-2M | Common Crawl | 600B [2M vocab? (trained with subword)] | 300 | https://fasttext.cc/docs/en/english-vectors.html | 

# Corpora

| Name | Description | Size | Url | 
|------|---------------|--------|------------------|
| oL | Oscar Large | - | [https://oscar-corpus.com/](Oscar) |
| oS | Oscar Small (1-12) | - |
| Sm | SuperMat | 172 articles on superconductors | [https://github.com/lfoppiano/SuperMat](SuperMat) |
| Sc | Science | 794197 articles from acs, aip, aps, elsevier, iop, jjap, rsc, springer, wiley | N/A |


# Summary results 

Results in this table are from settings that have been discarded for their poor performances:

| Name | Changes | Precision | Recall  | F1 |  
|------|---------|-----------|---------|----|
| [baseline_no_features](baseline/baseline_no_features) | 172 papers, no features | 77.00  |  77.20 |   77.09 |   0.61    |
| [baseline_with_features](baseline_features) | 172 papers, features | 77.95  |  77.27  |  77.60  |    0.99     |
| oS+Sm misc | Oscar Small + SuperMat with various settings | - | -  | - | - |
| [fastText_oS+Sm_vec_cbow_100d_no_features](old/fastText_oS+Sm_vec_cbow_100d_no_features) | baseline_no_features + fastText + oscarS_Supermat + cbow + 100d |   72.15 |   73.39 |   72.75 |
| [fastText_oS+Sm_vec_cbow_100d_features](old/fastText_oS+Sm_vec_cbow_100d_features) | baseline_features + fastText + oscarS_Supermat + cbow + 100d |74.06  |  73.92  |  73.98|
| [fastText_oS+Sm_vec_cbow_300d_no_features](old/fastText_oS+Sm_vec_cbow_300d_no_features) | baseline_no_features + fastText + oscarS_Supermat + cbow + 300d |   73.49  |  73.89  |  73.68  |
| [fastText_oS+Sm_vec_cbow_300d_features](old/fastText_oS+Sm_vec_cbow_300d_features) | baseline_features + fastText + oscarS_Supermat + cbow + 300d | 74.43  |  74.38  |  74.40  |
| [fastText_oS+Sm_vec_skipgram_100d_no_features](old/fastText_oS+Sm_vec_skipgram_100d_no_features) | baseline_no_features + fastText + oscarS_Supermat + skipgram + 100d | 74.22  |  75.99  |  75.08 | 
| [fastText_oS+Sm_vec_skipgram_100d_features](old/fastText_oS+Sm_vec_skipgram_100d_features) | baseline_features + fastText + oscarS_Supermat + skipgram + 100d | 75.72  |  75.89  |  75.78 |
| oS | Oscar Small | - | -  | - | - |
| [fastText_oS_vec_skipgram_300d_no_features](oS/fastText_oS_vec_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS + skipgram + 300d |   76.26  |  74.64  |  75.4|  1.42 |
| [fastText_oS_bin_skipgram_300d_no_features](oS/fastText_oS_bin_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS + skipgram + 300d + bin |   75.23  |  74.88  |  75.04| 0.95 |
| [fastText_oS_vec_skipgram_300d_features](oS/fastText_oS_vec_skipgram_300d_features) | baseline_features + fastText + oscarS + skipgram + 300d + vec |   76.32 |   75.76  |  76.01| 1.15 |
| [fastText_oS_bin_skipgram_300d_features](oS/fastText_oS_bin_skipgram_300d_features) | baseline_features + fastText + oscarS + skipgram + 300d + bin |  75.95  |  75.97  |  75.94 | 1.39 |
| oS+Sm | Oscar Small + SuperMat | - | -  | - |
| [fastText_oS+Sm_vec_skipgram_300d_no_features](oS+Sm/fastText_oS+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS_Supermat + skipgram + 300d + vec | 75.51  |  75.81 |   75.65  | 1.13 |
| [fastText_oS+Sm_bin_skipgram_300d_no_features](oS+Sm/fastText_oS+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS_Supermat + skipgram + 300d  + bin | 74.20  |  75.85  |  75.00  | 1.64 | 
| [fastText_oS+Sm_vec_skipgram_300d_features](oS+Sm/fastText_oS+Sm_vec_skipgram_300d_features) | baseline_features + fastText + oscarS + Supermat + skipgram + 300d + vec|  76.19  |  76.76  |  76.46| 1.08 |
| [fastText_oS+Sm_bin_skipgram_300d_features](oS+Sm/fastText_oS+Sm_bin_skipgram_300d_features) | baseline_features + fastText + oscarS + Supermat + skipgram + 300d + bin | 76.01  |  77.15  |  76.57 | 0.74 |
| oS+Sc | Oscar Small + Science | - | -  | - |
| [fastText_oS+Sc_vec_skipgram_300d_no_features](oS+Sc/fastText_oS+Sc_vec_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS + Science + skipgram + 300d + vec | 76.12 |  76.42  |  76.25 | 1.01 |
| [fastText_oS+Sc_bin_skipgram_300d_no_features](oS+Sc/fastText_oS+Sc_bin_skipgram_300d_no_features) | baseline_no_features + fastText + oscarS + Science + skipgram + 300d  + bin |75.43  |  76.56 |   75.99| 0.94 | 
| [fastText_oS+Sc_vec_skipgram_300d_features](oS+Sc/fastText_oS+Sc_vec_skipgram_300d_features) | baseline_features + fastText + oscarS + Science + skipgram + 300d + vec | 76.75  |  77.21  | 76.97 | 0.57 |
| [fastText_oS+Sc_bin_skipgram_300d_features](oS+Sc/fastText_oS+Sc_bin_skipgram_300d_features) | baseline_features + fastText + oscarS + Science + skipgram + 300d + bin | 76.36   | 77.61  |  76.97 | 0.84 |
| oS+Sc+Sm | Oscar Small + Science + SuperMat | - | -  | - |
| [fastText_oS+Sc+Sm_bin_skipgram_300d_no_features](oS+Sc+Sm/fastText_oS+Sc+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Small + Science + SuperMat + skipgram + 300d + bin  | 75.41 | 76.77 | 76.07 | 0.81 |
| [fastText_oS+Sc+Sm_bin_skipgram_300d_features](oS+Sc+Sm/fastText_oS+Sc+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Small + Science + SuperMat + skipgram + 300d + bin  | 75.31  | 77.24 |   76.26 | 1.14 |
| Sc | Science | - | -  | - |
| [fastText_Sc_vec_skipgram_300d_no_features](Sc/fastText_Sc_vec_skipgram_300d_no_features) | baseline_features + fastText + Science + skipgram + 300d + vec |   76.03  | 76.30  | 76.16 | 0.47 |
| [fastText_Sc_bin_skipgram_300d_no_features](Sc/fastText_Sc_bin_skipgram_300d_no_features) | baseline_features + fastText + Science + skipgram + 300d + bin |  75.78   | 76.57  |  76.16 | 0.94 |
| [fastText_Sc_vec_skipgram_300d_features](Sc/fastText_Sc_vec_skipgram_300d_features) | baseline_features + fastText + Science + skipgram + 300d + vec |   75.92  | 76.61  |  76.25 | 0.96 |
| [fastText_Sc_bin_skipgram_300d_features](Sc/fastText_Sc_bin_skipgram_300d_features) | baseline_features + fastText + Science + skipgram + 300d + bin |  76.14  |  77.46  |  76.79| 0.82 |
| Sc+Sm | Science + SuperMat | - | -  | - |
| [fastText_Sc+Sm_vec_skipgram_300d_no_features](Sc+Sm/fastText_Sc+Sm_vec_skipgram_300d_no_features) | baseline_features + fastText + Science + Supermat + skipgram + 300d + vec |   75.78  |  75.79  |  75.76| 1.35 |
| [fastText_Sc+Sm_bin_skipgram_300d_no_features](Sc+Sm/fastText_Sc+Sm_bin_skipgram_300d_no_features) | baseline_features + fastText + Science + Supermat + skipgram + 300d + bin |   75.93  |  76.71   | 76.31 | 0.73 |
| [fastText_Sc+Sm_vec_skipgram_300d_features](Sc+Sm/fastText_Sc+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Science + Supermat + skipgram + 300d + vec |   76.42  |  76.34  |  76.37| 0.95 |
| [fastText_Sc+Sm_bin_skipgram_300d_features](Sc+Sm/fastText_Sc+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Science + Supermat + skipgram + 300d + bin |   75.77   | 76.96  |  76.36| 0.81 |
| oL | Oscar Large | - | -  | - |
| [fastText_oL_bin_skipgram_300d_no_features](oL/fastText_oL_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + skipgram + 300d + bin  | 76.96 | 75.03  |  75.97 | 1.15 |
| [fastText_oL_vec_skipgram_300d_no_features](oL/fastText_oL_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + skipgram + 300d + vec  | 76.40 | 75.88  |  76.13 | 1.07 |
| [fastText_oL_bin_skipgram_300d_features](oL/fastText_oL_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + skipgram + 300d + bin  |77.83  | 77.12  |  77.46 | 0.68 |
| [fastText_oL_vec_skipgram_300d_features](oL/fastText_oL_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + skipgram + 300d + vec  | 77.47  |77.01  |  77.23  | 0.81 |
| oL+Sm| Oscar Large + SuperMat | - | -  | - |
| [fastText_oL+Sm_bin_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 76.20  |  77.19  |  76.68 | 1.11 |
| [fastText_oL+Sm_vec_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.08  | 76.76  |  76.91 | 0.82 |
| [fastText_oL+Sm_bin_skipgram_300d_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 78.04  |  77.92  |  77.98 | **0.21** |
| [fastText_oL+Sm_vec_skipgram_300d_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.60  |  78.42  |  **78.00** | 0.66|
| oL+Sc| Oscar Large + Science | - | -  | - |
| [fastText_oL+Sc_bin_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 76.74  |  76.77  |  76.75 | 0.87  |
| [fastText_oL+Sc_vec_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 76.42  |  76.04  |  76.21  | 0.75 |
| [fastText_oL+Sc_bin_skipgram_300d_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 77.13  |  77.22  |  77.16| 1.03 |
| [fastText_oL+Sc_vec_skipgram_300d_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 77.60  |  77.39  |  77.49  | 0.59 |
| oL+Sc+Sm | Oscar Large + Science + SuperMat | - | -  | - |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 76.66 | 76.78  |76.70  | 0.80 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 76.72  |  77.21 | 76.96  | 0.90 |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 77.19 |  78.00  |  77.58 | 1.01 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 77.21 |  78.00  |  77.60 | 0.49 |


# Comparisons

## Batches comparison

Uniqueness = nb of unique entities / total number of entities
Paragraphs includes titles, abstracts and keywords (one list of keywords is one paragraph)
Tokens are calculated from the grobid-tokenizer which splits even numbers into different tokens.

|       | Features  |             |      |          |       |      |      |
|-------|-----------|-------------|------|----------|-------|------|------|
| Batch | Precision | Recall | F1 | Rank | Uniqueness| Paragraphs | Tokens| 
| 1     | 78.42 | 81.39 | 79.88 | 1 | 40.55 | 933 | 158069|   
| 2     | 59.08 | 52.80 | 54.65 | 7 | 47.28 | 217 | 35484 |   
| 3     | 73.59 | 75.26 | 74.40 | 2 | 48.93 | 452 | 85780 |    
| 4     | 67.37 | 67.87 | 67.61 | 5 | 45.21 | 752 | 153217|   
| 5     | 61.43 | 64.41 | 62.87 | 6 | 49.12 | 440 | 57936 |   
| 6     | 72.15 | 71.41 | 71.77 | 4 | 43.98 | 578 | 82018 |   
| 7     | 72.02 | 76.80 | 74.29 | 3 | 42.85 | 442 | 88981 |   
| 8     | 38.93 | 30.18 | 33.59 | 8 | 44.77 | 236 | 42570 |   


|       | no Features |      |    |          |       |      |      |     
|-------|-----------|--------|----|----------|-------|------|------|
| Batch | Precision | Recall | F1 | Rank | Uniqueness|Paragraphs | Tokens |
| 1     | 77.28 | 80.05 | 78.63 | 1 | 40.55 | 933 |158069|  
| 2     | 57.67 | 46.92 | 50.47 | 7 | 47.28 | 217 |35484 |  
| 3     | 68.71 | 69.57 | 69.10 | 4 | 48.93 | 452 |85780 |  
| 4     | 65.92 | 67.59 | 66.74 | 5 | 45.21 | 752 |153217|  
| 5     | 60.93 | 55.94 | 58.14 | 6 | 49.12 | 440 |57936 |  
| 6     | 70.94 | 69.83 | 70.38 | 3 | 43.98 | 578 |82018 |  
| 7     | 72.50  |78.80 | 75.51 | 2 | 42.85 | 442 |88981 |  
| 8     | 20.33 | 12.95 | 15.64 | 8 | 44.77 | 236 |42570 |  


## Sentence based vs Paragraph based

Experiment definition: 
 - training: 3 articles  
 - evaluation: 1 article
 - Batch size: 5
 - Embeddings: glove / oS+Sc+Sm
 - Architecture: BidLSTM_CRF_FEATURES

### Experiment 1: glove

#### Paragraph based

|  label           | precision | recall | f1-score | support |
|------------------|-----------|--------|--------|------|
|         class    |  0.2500   | 0.0952 | 0.1379 | 21 |
|      material    |  0.3636   | 0.1333 | 0.1951 | 60 |
|     me_method    |  0.4500   | 0.2571 | 0.3273 | 35 |
|            tc    |  0.7143   | 0.7500 | 0.7317 | 20 |
|       tcValue    |  0.3158   | 0.5455 | 0.4000 | 11 |
| all (micro avg.) |  0.4444   | 0.2721 | 0.3376 | 147 |

#### Sentence based

|  label           | precision | recall | f1-score | support |
|------------------|-----------|--------|--------|------|
|         class    |  0.2727   | 0.1875 | 0.1429 | 21 |
|      material    |  0.4930   | 0.5344 | 0.5833 | 60 |
|     me_method    |  0.5600   | 0.4667 | 0.4000 | 35 |
|            tc    |  0.6429   | 0.7500 | 0.9000 | 20 |
|       tcValue    |  0.2727   | 0.3636 | 0.5455 | 11 |
| all (micro avg.) |  0.4841 | 0.5000 | 0.5170 | 147 |


### Experiment 2: oS+Sc+Sm

#### Paragraph based

|  label           | precision | recall | f1-score | support |
|------------------|-----------|--------|--------|------|
|         class    |  0.1538   |0.0952  | 0.1176 | 21  |
|      material    |  0.2258   |0.1167  | 0.1538 | 60  |
|     me_method    |  0.5000   |0.2571  | 0.3396 | 35  |
|            tc    |  0.6154   |0.8000  | 0.6957 | 20  |
|       tcValue    |  0.1852   |0.4545  | 0.2632 | 11  |
| all (micro avg.) |  0.3391   |0.2653  | 0.2977 | 147 |

#### Sentence based

|  label           | precision | recall | f1-score | support |
|------------------|-----------|--------|--------|------|
|         class    |  0.2778   |0.2381  | 0.2564 | 21  |
|      material    |  0.4800   |0.2000  | 0.2824 | 60  |
|     me_method    |  0.5833   |0.4000  | 0.4746 | 35  |
|            tc    |  0.6207   |0.9000  | 0.7347 | 20  |
|       tcValue    |  0.1000   |0.1818  | 0.1290 | 11  |
| all (micro avg.) |  0.4397   |0.3469  | 0.3878 | 147 |

