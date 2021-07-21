# Feature engineering experiments with DL

## Summary best results

In this table we show the best results in comparison with the baseline. For all the results, see the [table below](#summary-results). 

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| Baseline | Baseline | - | -  | - |
| [baseline_no_features](baseline/baseline_no_features) | 172 papers, no features | 77.00  |  77.20 |   77.09 |   0.61    |
| [baseline_with_features](baseline/baseline_features) | 172 papers, features | 77.95  |  77.27  |  77.60  |    0.99     |
| Other results | Baseline changes | - | -  | - |
| [baseline-by_sentences-updated_corpus-gloves-no_features](baseline/baseline-by_sentences-updated_corpus-glove-no_features) | 172 papers, gloVe, corpus manually segmented by sentences, filter out all sentences without entities | 78.87  |  80.35  |  79.60  | 0.82 |
| [baseline-by_sentences-updated_corpus-gloves-features](baseline/baseline-by_sentences-updated_corpus-glove) | 172 papers, features, gloVe, corpus manually segmented by sentences, filter out all sentences without entities | 80.47 | 80.66 | 80.56  | 0.59|
| [fastText-by_sentences-updated_corpus-oL+Sc+Sc-features](oL+Sc+Sm/fastText-oL+Sc+Sm-by_sentences-updated_corpus-fasttext) | 172 papers, features, oL+Sc+Sm , corpus manually segmented by sentences, filter out all sentences without entities | 79.96  |  79.99  | 79.97 | 0.76 |
| [fastText-by_sentences-updated_corpus-oL+Sc+Sc-no_features](oL+Sc+Sm/fastText-oL+Sc+Sm-by_sentences-updated_corpus-fasttext-no_features) | 172 papers, oL+Sc+Sm , corpus manually segmented by sentences, filter out all sentences without entities | 79.62  | 80.48 | 80.04 | 0.64 |
| [baseline-by_sentences-automatic_split-positive-glove-features](baseline/baseline_sentences_positive_features) | 172 papers, features, segmented by sentences, filter out all sentences without entities | 80.44 |   80.77 |   80.60  | 0.57 |
| [baseline-by_sentences-automatic_split-positive-oL+Sc+Sm-features](baseline/baseline_sentences_positive_features-fasttext) | 172 papers, features, oL+Sc+Sm , segmented by sentences, filter out all sentences without entities | 80.68  |  80.93  |  80.81 | 0.72 |  
| [baseline-by_sentences-minus_worst_10-features](baseline/baseline-by_sentences-minus_worst_10-features) | baseline_features + gloVe + by sentence + remove worst 10 documents  | 83.01  |  82.89  |  **82.95** | 0.58 |
| [baseline-by_sentences-updated_corpus-gloves-keep_all_sentences-no_features](baseline/baseline-by_sentences-updated_corpus-glove-no_features) | 172 papers, gloVe, corpus manually segmented by sentences |  77.08    |80.41  | 78.70  | 0.81 |
| [baseline-by_sentences-updated_corpus-oL+Sc+Sm-keep_all_sentences-no_features](oL+Sc+Sm/baseline-by_sentences-updated_corpus-oL+Sc+Sm-no_features) | 172 papers, gloVe, corpus manually segmented by sentences |  76.82  |80.05  |  78.38  | 1.08 |

## Embeddings

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

## Corpora

| Name | Description | Size | Url | 
|------|---------------|--------|------------------|
| oL | Oscar Large | - | [https://oscar-corpus.com/](Oscar) |
| oS | Oscar Small (1-12) | - |
| Sm | SuperMat | 172 articles on superconductors | [https://github.com/lfoppiano/SuperMat](SuperMat) |
| Sc | Science | 794197 articles from acs, aip, aps, elsevier, iop, jjap, rsc, springer, wiley | N/A |


## Experiments

### Removing batch 8
Batch 8 resulted to score the least in the cross-validation with only single batches

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline-batch_1_7-features](baseline/baseline-batch_1_7-features) | baseline_features + gloVe + bin | 77.81 | 79.09 | 78.43  | 0.66 |
| [baseline-batch_1_7-no_features](baseline/baseline-batch_1_7-no_features) | baseline_no_features + gloVe + bin |  77.18  | 79.43   | 78.29  | 0.61 |
| [fastText_oS+Sc+Sm-batch_1_7-features](oS+Sc+Sm/fastText_oS+Sc+Sm-batch_1_7-features) |  baseline_features + oS+Sc+Sm + bin |  74.69  |  79.10   | 76.81   | 0.80 |
| [fastText_oS+Sc+Sm-batch_1_7-no_features](oS+Sc+Sm/fastText_oS+Sc+Sm-batch_1_7-no_features) | baseline_no_features + oS+Sc+Sm + bin | 75.23  |  78.37  |  76.75  | 1.26 |
| [fastText_oL+Sc+Sm-batch_1_7-features](oL+Sc+Sm/fastText_oL+Sc+Sm-batch_1_7-features) |  baseline_features + oL+Sc+Sm + vec |  78.06  |  78.75  |  78.39 | 0.40 |
| [fastText_oL+Sc+Sm-batch_1_7-no_features](oL+Sc+Sm/fastText_oL+Sc+Sm-batch_1_7-no_features) | baseline_no_features + oL+Sc+Sm + vec | 77.80  |  78.49 |   78.13 | 0.99 | 
| [fastText_oL+Sm-batch_1_7-features](oS+Sm/fastText_oL+Sm-batch_1_7-features) |  baseline_features + oL+Sm + vec |  77.17  |  79.24  |  78.18  | 0.96 | 
| [fastText_oL+Sm-batch_1_7-no_features](oL+Sm/fastText_oL+Sm-batch_1_7-no_features) | baseline_no_features + oL+Sm + vec | 76.95  |  78.84  |  77.87 | 1.04 |

### Removing batch 8 and 2
Batch 8 and 2 resulted to score the least in the cross-validation with only single batches

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| [baseline-batches-28-features](baseline/baseline-batches-28-features) | baseline_features + gloVe + bin | 77.66  |  78.48 |   78.06   | 0.95 |
| [baseline-batches-28-no_features](baseline/baseline-batches-28-no_features) | baseline_no_features + gloVe + bin | 78.21  |  78.46  |  78.33 | 0.69 |
| [fastText_oS+Sc+Sm-batches-28-features](oS+Sc+Sm/fastText_oS+Sc+Sm-batches-28-features) |  baseline_features + oS+Sc+Sm + bin | 77.17  |  78.85  | 77.99 | 0.48 |
| [fastText_oS+Sc+Sm-batches-28-no_features](oS+Sc+Sm/fastText_oS+Sc+Sm-batches-28-no_features) | baseline_no_features + oS+Sc+Sm + bin | 76.87  |  78.51  |  77.68  | 0.84 |
| [fastText_oL+Sc+Sm-batches-28-features](oL+Sc+Sm/fastText_oL+Sc+Sm-batches-28-features) |  baseline_features + oL+Sc+Sm + vec | 78.17  |  79.09 | 78.63 | 0.57 |
| [fastText_oL+Sc+Sm-batches-28-no_features](oL+Sc+Sm/fastText_oL+Sc+Sm-batches-28-no_features) | baseline_no_features + oL+Sc+Sm + vec | 77.39  | 78.65  | 78.01 | 1.02 |
| [fastText_oL+Sm-batches-28-features](oL+Sm/fastText_oL+Sm-batches-28-features) |  baseline_features + oL+Sm + vec | 78.02  |  78.69  |  78.34 | 0.57 |
| [fastText_oL+Sm-batches-28-no_features](oL+Sm/fastText_oL+Sm-batches-28-no_features) | baseline_no_features + oL+Sm + vec | 76.69  |  78.90  |  77.77 | 1.32 | 

### Evaluation on each document (data stability)

#### Paragraph based 

| Filename                                             | Precision | Recall | F1     | Support |
| ---------------------------------------------------- | --------- | ------ | ------ | ------- |
| PHC2640145-CC                                        | 0.5385    | 0.28   | 0.3684 | 25      |
| PHC3130001-CC                                        | 0.5       | 0.4815 | 0.4906 | 27      |
| PHC4320193-CC                                        | 0.6575    | 0.6467 | 0.6521 | 184     |
| EPL0480073-CC                                        | 0.6471    | 0.6875 | 0.6667 | 16      |
| SST01600L7-CC                                        | 0.6809    | 0.6531 | 0.6667 | 49      |
| PHC1480411-CC                                        | 0.6863    | 0.6863 | 0.6863 | 51      |
| PHC4020152-CC                                        | 0.6648    | 0.7169 | 0.6899 | 166     |
| PR05514733-CC                                        | 0.6724    | 0.7091 | 0.6903 | 55      |
| PHC3910237-CC                                        | 0.6614    | 0.7368 | 0.6971 | 114     |
| Tc4k\_interfaceBi\_Ni-CC                             | 0.8409    | 0.5968 | 0.6981 | 124     |
| P070184523-CC                                        | 0.7177    | 0.7063 | 0.712  | 126     |
| P068100502-CC                                        | 0.7333    | 0.6962 | 0.7143 | 79      |
| L092157004-CC                                        | 0.7042    | 0.7463 | 0.7246 | 67      |
| P068180505-CC                                        | 0.7143    | 0.7432 | 0.7285 | 74      |
| PHC4200023-CC                                        | 0.7611    | 0.7478 | 0.7544 | 115     |
| PHC1580183-CC                                        | 0.7368    | 0.7778 | 0.7568 | 18      |
| APL0774202-CC                                        | 0.75      | 0.7759 | 0.7627 | 58      |
| PHC2240263-CC                                        | 0.6667    | 0.9    | 0.766  | 80      |
| P067104503-CC                                        | 0.7549    | 0.7857 | 0.77   | 98      |
| SSC1280097-CC                                        | 0.7333    | 0.8209 | 0.7746 | 67      |
| JPS0730819-CC                                        | 0.7111    | 0.8533 | 0.7758 | 75      |
| P066020503-CC                                        | 0.7407    | 0.8163 | 0.7767 | 49      |
| P066214509-CC                                        | 0.8103    | 0.746  | 0.7769 | 63      |
| P071184510-CC                                        | 0.7614    | 0.7979 | 0.7792 | 188     |
| L088207003-CC                                        | 0.7358    | 0.8298 | 0.78   | 47      |
| L091087001-CC                                        | 0.7727    | 0.7907 | 0.7816 | 86      |
| mydeen2010temperature-CC                             | 0.7545    | 0.8182 | 0.785  | 154     |
| L095117006-CC                                        | 0.8684    | 0.7174 | 0.7857 | 92      |
| PR0446999-CC                                         | 0.7742    | 0.809  | 0.7912 | 89      |
| P068132504-CC                                        | 0.7245    | 0.8765 | 0.7933 | 81      |
| PHC1910237                                           | 0.7818    | 0.8113 | 0.7963 | 53      |
| MCP0750110-CC                                        | 0.7846    | 0.8095 | 0.7969 | 126     |
| JPS0731123-CC                                        | 0.8028    | 0.7917 | 0.7972 | 72      |
| PHC1580178                                           | 0.8023    | 0.7931 | 0.7977 | 87      |
| P072214523-CC                                        | 0.8111    | 0.7849 | 0.7978 | 93      |
| P071134526-CC                                        | 0.9091    | 0.7143 | 0.8    | 98      |
| P072180504-CC                                        | 0.8302    | 0.7719 | 0.8    | 114     |
| NAT3500600                                           | 0.8654    | 0.75   | 0.8036 | 60      |
| P072014504-CC                                        | 0.7988    | 0.8086 | 0.8037 | 162     |
| PR06015055-CC                                        | 0.7959    | 0.8125 | 0.8041 | 48      |
| JPS0731131-CC                                        | 0.8132    | 0.7957 | 0.8043 | 93      |
| L090137001-CC                                        | 0.8444    | 0.7755 | 0.8085 | 49      |
| P071104513-CC                                        | 0.8207    | 0.8032 | 0.8118 | 188     |
| P070054519-CC                                        | 0.8652    | 0.7673 | 0.8133 | 159     |
| PHC4170033-CC                                        | 0.8       | 0.8276 | 0.8136 | 145     |
| JPS0731297-CC                                        | 0.8       | 0.832  | 0.8157 | 125     |
| SCinTiO\_1612.02502-CC                               | 0.8162    | 0.8162 | 0.8162 | 234     |
| PR05511832-CC                                        | 0.7843    | 0.8511 | 0.8163 | 47      |
| Li\_2018\_Supercond.\_Sci.\_Technol.\_31\_085001     | 0.8298    | 0.8041 | 0.8168 | 194     |
| SSC1270493-CC                                        | 0.807     | 0.8364 | 0.8214 | 110     |
| MAT0305503-CC                                        | 0.8077    | 0.84   | 0.8235 | 75      |
| P066024503-CC                                        | 0.8161    | 0.8353 | 0.8256 | 170     |
| 1802.03907-CC                                        | 0.8309    | 0.823  | 0.8269 | 418     |
| PCS2739-CC                                           | 0.8889    | 0.7742 | 0.8276 | 62      |
| Suzuki\_etal\_2015-CC                                | 0.7826    | 0.878  | 0.8276 | 82      |
| sun2012re-emerging-CC                                | 0.7669    | 0.8993 | 0.8278 | 139     |
| P070020503-CC                                        | 0.9266    | 0.7481 | 0.8279 | 135     |
| JPS0731655-CC                                        | 0.837     | 0.8191 | 0.828  | 94      |
| PR05009672-CC                                        | 0.8025    | 0.8553 | 0.828  | 76      |
| P072064520-CC                                        | 0.8545    | 0.8034 | 0.8282 | 117     |
| PR06006991-CC                                        | 0.7965    | 0.8654 | 0.8295 | 104     |
| P064172514-CC                                        | 0.7717    | 0.8987 | 0.8304 | 79      |
| L088207005-CC                                        | 0.8261    | 0.8382 | 0.8321 | 68      |
| P072174508-CC                                        | 0.8       | 0.8679 | 0.8326 | 106     |
| P068064507-CC                                        | 0.8293    | 0.8361 | 0.8327 | 122     |
| EPJ0290369-CC                                        | 0.7924    | 0.8779 | 0.833  | 213     |
| PHC3411655-CC                                        | 0.75      | 0.9375 | 0.8333 | 48      |
| Drozdov\_etal\_2015                                  | 0.8447    | 0.8229 | 0.8336 | 271     |
| PR05903948-CC                                        | 0.7952    | 0.88   | 0.8354 | 75      |
| JPS081113707-CC                                      | 0.8313    | 0.8415 | 0.8364 | 82      |
| PHC1980027                                           | 0.8305    | 0.8448 | 0.8376 | 58      |
| JP01103169-CC                                        | 0.8163    | 0.8602 | 0.8377 | 93      |
| JPC0150L17-CC                                        | 0.8295    | 0.8488 | 0.8391 | 86      |
| PHC3830337-CC                                        | 0.8235    | 0.866  | 0.8442 | 97      |
| P066104528-CC                                        | 0.8352    | 0.8539 | 0.8444 | 89      |
| JPS0710709-CC                                        | 0.8596    | 0.8305 | 0.8448 | 59      |
| L094037007-CC                                        | 0.8421    | 0.8477 | 0.8449 | 151     |
| Rb2Cr3As3\_quasi1D\_SC1412.2596-CC                   | 0.8144    | 0.8778 | 0.8449 | 90      |
| 1903.04321-CC                                        | 0.8451    | 0.8451 | 0.8451 | 71      |
| PR05914099-CC                                        | 0.7939    | 0.9034 | 0.8452 | 145     |
| L095167004-CC                                        | 0.8649    | 0.8266 | 0.8453 | 271     |
| P065104523-CC                                        | 0.7945    | 0.9062 | 0.8467 | 128     |
| P070052502-CC                                        | 0.7945    | 0.9062 | 0.8467 | 64      |
| JPS0732912-CC                                        | 0.8211    | 0.8764 | 0.8478 | 89      |
| JPS0723041-CC                                        | 0.8375    | 0.859  | 0.8481 | 78      |
| P066024502-CC                                        | 0.8701    | 0.8272 | 0.8481 | 81      |
| P069174506-CC                                        | 0.8177    | 0.8862 | 0.8506 | 167     |
| PHC4030200-CC                                        | 0.8       | 0.9091 | 0.8511 | 22      |
| PHC4210001-CC                                        | 0.8382    | 0.8683 | 0.8529 | 167     |
| P068132507-CC                                        | 0.8356    | 0.8714 | 0.8531 | 70      |
| PR05514152-CC                                        | 0.8421    | 0.8649 | 0.8533 | 37      |
| PHB1480442                                           | 0.8551    | 0.8551 | 0.8551 | 69      |
| P071100502-CC                                        | 0.8452    | 0.8659 | 0.8554 | 82      |
| PR06014617-CC                                        | 0.8696    | 0.8421 | 0.8556 | 95      |
| CoA4\_HTSC\_theory1807.00934-CC                      | 0.8476    | 0.8641 | 0.8558 | 103     |
| P070214505-CC                                        | 0.838     | 0.875  | 0.8561 | 136     |
| P063224522-CC                                        | 0.8726    | 0.8409 | 0.8565 | 220     |
| ying2011pressure-CC                                  | 0.8296    | 0.8852 | 0.8565 | 209     |
| P072220505-CC                                        | 0.8421    | 0.8727 | 0.8571 | 55      |
| chu2009high-CC                                       | 0.8447    | 0.8736 | 0.8589 | 554     |
| SST0180041-CC                                        | 0.8396    | 0.8812 | 0.8599 | 101     |
| PR06114350-CC                                        | 0.8875    | 0.8353 | 0.8606 | 85      |
| Tanaka\_etal\_2017-CC                                | 0.8503    | 0.8712 | 0.8606 | 163     |
| L094047006-CC                                        | 0.9011    | 0.8283 | 0.8632 | 99      |
| EPL0410207-CC                                        | 0.84      | 0.8936 | 0.866  | 47      |
| Liu\_2018\_Supercond.\_Sci.\_Technol.\_31\_125011-CC | 0.8182    | 0.9197 | 0.866  | 137     |
| PR06012475-CC                                        | 0.8393    | 0.8952 | 0.8664 | 105     |
| L088167005-CC                                        | 0.8553    | 0.8784 | 0.8667 | 74      |
| P068214517-CC                                        | 0.9209    | 0.8205 | 0.8678 | 156     |
| xing2014theAnomaly-CC                                | 0.8361    | 0.9027 | 0.8681 | 113     |
| PR05814617-CC                                        | 0.86      | 0.8776 | 0.8687 | 98      |
| hott2013review-CC                                    | 0.8725    | 0.8668 | 0.8697 | 766     |
| PR06114956-CC                                        | 0.8671    | 0.8732 | 0.8702 | 142     |
| PR05809504-CC                                        | 0.8649    | 0.8767 | 0.8707 | 73      |
| P071104516-CC                                        | 0.8684    | 0.8742 | 0.8713 | 151     |
| ivanovskii2008newHigh                                | 0.8589    | 0.8845 | 0.8715 | 970     |
| PR05003221-CC                                        | 0.85      | 0.8947 | 0.8718 | 38      |
| 1609.04957-CC                                        | 0.8677    | 0.877  | 0.8723 | 187     |
| L089147002-CC                                        | 0.8913    | 0.8542 | 0.8723 | 48      |
| P072064527-CC                                        | 0.9167    | 0.8333 | 0.873  | 66      |
| JPS081033701-CC                                      | 0.8673    | 0.8854 | 0.8763 | 96      |
| PR04909084-CC                                        | 0.86      | 0.8958 | 0.8776 | 96      |
| PhysRevX.9.021044-CC                                 | 0.8286    | 0.9355 | 0.8788 | 31      |
| L089157004-CC                                        | 0.8761    | 0.8839 | 0.88   | 112     |
| wang2011effect                                       | 0.8696    | 0.8955 | 0.8824 | 134     |
| PHC3660299-CC                                        | 0.8519    | 0.92   | 0.8846 | 75      |
| kotegawa2009contrasting-CC                           | 0.8814    | 0.8927 | 0.887  | 233     |
| MAT0106521-CC                                        | 0.8806    | 0.8939 | 0.8872 | 66      |
| PR06100107-CC                                        | 0.8701    | 0.9054 | 0.8874 | 148     |
| okada2008supercondctivity-CC                         | 0.8788    | 0.8992 | 0.8889 | 129     |
| P066132510-CC                                        | 0.8898    | 0.8898 | 0.8898 | 127     |
| L093157004-CC                                        | 0.8851    | 0.8953 | 0.8902 | 86      |
| P069014507-CC                                        | 0.8984    | 0.8842 | 0.8912 | 190     |
| P065172501-CC                                        | 0.8922    | 0.8922 | 0.8922 | 102     |
| PR06103604-CC                                        | 0.8778    | 0.908  | 0.8927 | 87      |
| P072104504-CC                                        | 0.878     | 0.9114 | 0.8944 | 79      |
| PR04310568-CC                                        | 0.8966    | 0.8966 | 0.8966 | 29      |
| JPS0722453-CC                                        | 0.8627    | 0.9362 | 0.898  | 47      |
| L092227003-CC                                        | 0.8689    | 0.9298 | 0.8983 | 57      |
| Carnicom                                             | 0.8671    | 0.932  | 0.8984 | 147     |
| piva2015combined-CC                                  | 0.8923    | 0.9062 | 0.8992 | 128     |
| P066020501-CC                                        | 0.8772    | 0.9259 | 0.9009 | 54      |
| PHC3200009-CC                                        | 0.9044    | 0.8978 | 0.9011 | 137     |
| P072224509-CC                                        | 0.9492    | 0.8615 | 0.9032 | 65      |
| PR05907184-CC                                        | 0.904     | 0.904  | 0.904  | 125     |
| PR05814581-CC                                        | 0.8605    | 0.961  | 0.908  | 77      |
| P065224520-CC                                        | 0.873     | 0.9483 | 0.9091 | 58      |
| P067172501-CC                                        | 0.8592    | 0.9839 | 0.9173 | 62      |
| P069184513-CC                                        | 0.8908    | 0.9464 | 0.9177 | 112     |
| MPL0150497-CC                                        | 0.8921    | 0.9466 | 0.9185 | 131     |
| PHC4020027-CC                                        | 0.8734    | 0.9718 | 0.92   | 71      |
| PHC3410729-CC                                        | 0.8889    | 0.96   | 0.9231 | 25      |
| PR05713422-CC                                        | 0.91      | 0.9381 | 0.9239 | 97      |
| SSC1230017-CC                                        | 0.9444    | 0.9067 | 0.9252 | 75      |
| EPL0580589                                           | 0.9062    | 0.9457 | 0.9255 | 92      |
| PR06013094-CC                                        | 0.8889    | 0.9655 | 0.9256 | 58      |
| P072094504-CC                                        | 0.9444    | 0.9107 | 0.9273 | 56      |
| HfV2O431805                                          | 0.9595    | 0.8987 | 0.9281 | 79      |
| SSC1310125-CC                                        | 0.9077    | 0.9516 | 0.9291 | 62      |
| L091217001-CC                                        | 0.9821    | 0.8871 | 0.9322 | 62      |
| L094047001-CC                                        | 0.9151    | 0.951  | 0.9327 | 102     |
| 1802.09882-CC                                        | 0.9333    | 0.9333 | 0.9333 | 60      |
| L094127001-CC                                        | 0.9239    | 0.9551 | 0.9392 | 89      |
| EPL0330153-CC                                        | 0.9118    | 0.9688 | 0.9394 | 32      |
| L088227002-CC                                        | 0.9423    | 0.9423 | 0.9423 | 104     |
| P065024523-CC                                        | 0.9265    | 0.9692 | 0.9474 | 65      |
| EPL0490086-CC                                        | 0.9429    | 0.9565 | 0.9496 | 69      |
| yamaguchi2014ac                                      | 0.9266    | 0.9806 | 0.9528 | 103     |
| PR06103808-CC                                        | 0.9726    | 0.9595 | 0.966  | 74      |
| L090137002-CC                                        | 1         | 1      | 1      | 14      |
| PR06006991-CC                                        | 1         | 1      | 1      | 8       |

#### Sentence based

| Name                                                   | Precision | Recall | F-Score | Support |
| ------------------------------------------------------ | --------- | ------ | ------- | ------- |
| PHC3130001-CC                                    | 0.6800    | 0.6296 | 0.6538  | 27      |
| P070184523-CC                                    | 0.7120    | 0.7063 | 0.7092  | 126     |
| PR05514733-CC                                    | 0.7018    | 0.7273 | 0.7143  | 55      |
| PHC4320193-CC                                    | 0.7403    | 0.7283 | 0.7342  | 184     |
| PHC1480411-CC                                    | 0.7091    | 0.7647 | 0.7358  | 51      |
| PHC1580183-CC                                    | 0.7000    | 0.7778 | 0.7368  | 18      |
| L092157004-CC                                    | 0.7123    | 0.7761 | 0.7429  | 67      |
| EPL0480073-CC                                    | 0.7500    | 0.7500 | 0.7500  | 16      |
| MCP0750110-CC                                    | 0.7623    | 0.7381 | 0.7500  | 126     |
| PHC3910237-CC                                    | 0.7436    | 0.7632 | 0.7532  | 114     |
| Tc4k_interfaceBi_Ni-CC                           | 0.8142    | 0.7419 | 0.7764  | 124     |
| PHC4020152-CC                                    | 0.7674    | 0.7904 | 0.7788  | 167     |
| PR0446999-CC                                     | 0.7717    | 0.7978 | 0.7845  | 89      |
| PHC4170033-CC                                    | 0.7785    | 0.8000 | 0.7891  | 145     |
| P071184510-CC                                    | 0.7884    | 0.7926 | 0.7905  | 188     |
| PHC3830337-CC                                    | 0.7619    | 0.8247 | 0.7921  | 97      |
| P068180505-CC                                    | 0.7973    | 0.7973 | 0.7973  | 74      |
| P066024502-CC                                    | 0.7927    | 0.8025 | 0.7975  | 81      |
| P066214509-CC                                    | 0.8197    | 0.7937 | 0.8065  | 63      |
| JPS0730819-CC                                    | 0.7558    | 0.8667 | 0.8075  | 75      |
| P068100502-CC                                    | 0.8182    | 0.7975 | 0.8077  | 79      |
| P067104503-CC                                    | 0.8000    | 0.8163 | 0.8081  | 98      |
| SST01600L7-CC                                    | 0.8000    | 0.8163 | 0.8081  | 49      |
| P071104513-CC                                    | 0.8315    | 0.7872 | 0.8087  | 188     |
| P070054519-CC                                    | 0.8592    | 0.7673 | 0.8106  | 159     |
| APL0774202-CC                                    | 0.8246    | 0.8103 | 0.8174  | 58      |
| PHC1980027                                       | 0.8824    | 0.7759 | 0.8257  | 58      |
| P072014504-CC                                    | 0.8313    | 0.8210 | 0.8261  | 162     |
| PHC4200023-CC                                    | 0.8261    | 0.8261 | 0.8261  | 115     |
| P072064520-CC                                    | 0.8545    | 0.8034 | 0.8282  | 117     |
| PR06006991-CC                                    | 0.8148    | 0.8462 | 0.8302  | 104     |
| PCS2739-CC                                       | 0.8814    | 0.7879 | 0.8320  | 66      |
| P072180504-CC                                    | 0.8624    | 0.8103 | 0.8356  | 116     |
| P070020503-CC                                    | 0.9358    | 0.7556 | 0.8361  | 135     |
| L090137001-CC                                    | 0.8864    | 0.7959 | 0.8387  | 49      |
| PR06015055-CC                                    | 0.8667    | 0.8125 | 0.8387  | 48      |
| JPS0731123-CC                                    | 0.8451    | 0.8333 | 0.8392  | 72      |
| 1903.04321-CC                                    | 0.8333    | 0.8451 | 0.8392  | 71      |
| PR05903948-CC                                    | 0.8049    | 0.8800 | 0.8408  | 75      |
| PHC1910237                                       | 0.8182    | 0.8654 | 0.8411  | 52      |
| P072174508-CC                                    | 0.8087    | 0.8774 | 0.8416  | 106     |
| PR06012475-CC                                    | 0.7966    | 0.8952 | 0.8430  | 105     |
| mydeen2010temperature-CC                         | 0.8302    | 0.8571 | 0.8435  | 154     |
| SCinTiO_1612.02502-CC                            | 0.8455    | 0.8419 | 0.8437  | 234     |
| P071134526-CC                                    | 0.9268    | 0.7755 | 0.8444  | 98      |
| L088207005-CC                                    | 0.8507    | 0.8382 | 0.8444  | 68      |
| JPS0731655-CC                                    | 0.8495    | 0.8404 | 0.8449  | 94      |
| PHC1580178                                       | 0.8333    | 0.8621 | 0.8475  | 87      |
| P070214505-CC                                    | 0.8369    | 0.8676 | 0.8520  | 136     |
| P064172514-CC                                    | 0.8000    | 0.9114 | 0.8521  | 79      |
| PR05514152-CC                                    | 0.8421    | 0.8649 | 0.8533  | 37      |
| PR06114350-CC                                    | 0.8861    | 0.8235 | 0.8537  | 85      |
| JPS0710709-CC                                    | 0.8621    | 0.8475 | 0.8547  | 59      |
| PR05914099-CC                                    | 0.8176    | 0.8966 | 0.8553  | 145     |
| PHC2240263-CC                                    | 0.7957    | 0.9250 | 0.8555  | 80      |
| Li_2018_Supercond._Sci._Technol._31_085001       | 0.8586    | 0.8543 | 0.8564  | 199     |
| JPS0723041-CC                                    | 0.8684    | 0.8462 | 0.8571  | 78      |
| Drozdov_etal_2015                                | 0.8509    | 0.8635 | 0.8571  | 271     |
| P068132504-CC                                    | 0.8202    | 0.9012 | 0.8588  | 81      |
| PR06114956-CC                                    | 0.8592    | 0.8592 | 0.8592  | 142     |
| L088207003-CC                                    | 0.8696    | 0.8511 | 0.8602  | 47      |
| PR05511832-CC                                    | 0.8511    | 0.8696 | 0.8602  | 46      |
| kotegawa2008abrupt-CC                            | 0.8651    | 0.8583 | 0.8617  | 127     |
| PHC2640145-CC                                    | 0.8462    | 0.8800 | 0.8627  | 25      |
| P064144524-CC                                    | 0.8201    | 0.9120 | 0.8636  | 125     |
| sun2012re-emerging-CC                            | 0.8194    | 0.9137 | 0.8639  | 139     |
| P072064527-CC                                    | 0.9153    | 0.8182 | 0.8640  | 66      |
| PHC4210001-CC                                    | 0.8497    | 0.8802 | 0.8647  | 167     |
| P066104528-CC                                    | 0.8652    | 0.8652 | 0.8652  | 89      |
| P068214517-CC                                    | 0.9085    | 0.8269 | 0.8658  | 156     |
| L091087001-CC                                    | 0.8621    | 0.8721 | 0.8671  | 86      |
| PHB1480442                                       | 0.8806    | 0.8551 | 0.8676  | 69      |
| SST0180041-CC                                    | 0.8491    | 0.8911 | 0.8696  | 101     |
| PHC4030200-CC                                    | 0.8333    | 0.9091 | 0.8696  | 22      |
| P072214523-CC                                    | 0.8791    | 0.8602 | 0.8696  | 93      |
| JPS0731131-CC                                    | 0.8791    | 0.8602 | 0.8696  | 93      |
| kotegawa2009contrasting-CC                       | 0.8644    | 0.8755 | 0.8699  | 233     |
| EPJ0290369-CC                                    | 0.8451    | 0.8967 | 0.8702  | 213     |
| JPS0731297-CC                                    | 0.8538    | 0.8880 | 0.8706  | 125     |
| L088167005-CC                                    | 0.8767    | 0.8649 | 0.8707  | 74      |
| NAT3500600                                       | 0.8947    | 0.8500 | 0.8718  | 60      |
| L095117006-CC                                    | 0.8876    | 0.8587 | 0.8729  | 92      |
| ying2011pressure-CC                              | 0.8507    | 0.8995 | 0.8744  | 209     |
| SSC1280097-CC                                    | 0.8571    | 0.8955 | 0.8759  | 67      |
| JPS081113707-CC                                  | 0.8875    | 0.8659 | 0.8765  | 82      |
| okada2008supercondctivity-CC                     | 0.8429    | 0.9147 | 0.8773  | 129     |
| P068064507-CC                                    | 0.8710    | 0.8852 | 0.8780  | 122     |
| xing2014theAnomaly-CC                            | 0.8571    | 0.9027 | 0.8793  | 113     |
| L094037007-CC                                    | 0.8654    | 0.8940 | 0.8795  | 151     |
| P066024503-CC                                    | 0.8824    | 0.8824 | 0.8824  | 170     |
| 1609.04957-CC                                    | 0.8913    | 0.8770 | 0.8841  | 187     |
| PR06100107-CC                                    | 0.8904    | 0.8784 | 0.8844  | 148     |
| P071104516-CC                                    | 0.8571    | 0.9139 | 0.8846  | 151     |
| chu2009high-CC                                   | 0.8722    | 0.8989 | 0.8853  | 554     |
| P070052502-CC                                    | 0.8551    | 0.9219 | 0.8872  | 64      |
| SSC1270493-CC                                    | 0.8761    | 0.9000 | 0.8879  | 110     |
| P068132507-CC                                    | 0.8649    | 0.9143 | 0.8889  | 70      |
| CoA4_HTSC_theory1807.00934-CC                    | 0.8846    | 0.8932 | 0.8889  | 103     |
| L093156802-CC                                    | 0.8810    | 0.9024 | 0.8916  | 41      |
| hott2013review-CC                                | 0.9013    | 0.8825 | 0.8918  | 766     |
| P071100502-CC                                    | 0.8721    | 0.9146 | 0.8929  | 82      |
| 1802.03907-CC                                    | 0.8884    | 0.8990 | 0.8937  | 416     |
| JPC0150L17-CC                                    | 0.8953    | 0.8953 | 0.8953  | 86      |
| P072220505-CC                                    | 0.8889    | 0.9032 | 0.8960  | 62      |
| ivanovskii2008newHigh                            | 0.8878    | 0.9052 | 0.8964  | 970     |
| PR05003221-CC                                    | 0.8750    | 0.9211 | 0.8974  | 38      |
| Suzuki_etal_2015-CC                              | 0.8824    | 0.9146 | 0.8982  | 82      |
| L095167004-CC                                    | 0.8857    | 0.9151 | 0.9002  | 271     |
| MAT0305503-CC                                    | 0.8947    | 0.9067 | 0.9007  | 75      |
| PR05009672-CC                                    | 0.8734    | 0.9324 | 0.9020  | 74      |
| Liu_2018_Supercond._Sci._Technol._31_125011-CC   | 0.8824    | 0.9247 | 0.9030  | 146     |
| wang2011effect                                   | 0.8971    | 0.9104 | 0.9037  | 134     |
| P072224509-CC                                    | 0.9344    | 0.8769 | 0.9048  | 65      |
| SSC1230017-CC                                    | 0.9178    | 0.8933 | 0.9054  | 75      |
| Tanaka_etal_2017-CC                              | 0.8929    | 0.9202 | 0.9063  | 163     |
| P066132510-CC                                    | 0.8992    | 0.9134 | 0.9063  | 127     |
| L089157004-CC                                    | 0.9027    | 0.9107 | 0.9067  | 112     |
| L089147002-CC                                    | 0.8980    | 0.9167 | 0.9072  | 48      |
| PR05814581-CC                                    | 0.8605    | 0.9610 | 0.9080  | 77      |
| JPS0732912-CC                                    | 0.8750    | 0.9438 | 0.9081  | 89      |
| PR05814617-CC                                    | 0.9000    | 0.9184 | 0.9091  | 98      |
| piva2015combined-CC                              | 0.9134    | 0.9062 | 0.9098  | 128     |
| JP01103169-CC                                    | 0.8958    | 0.9247 | 0.9101  | 93      |
| PHC3411655-CC                                    | 0.8679    | 0.9583 | 0.9109  | 48      |
| PhysRevX.9.021044-CC                             | 0.8378    | 1.0000 | 0.9118  | 31      |
| JPS081033701-CC                                  | 0.9072    | 0.9167 | 0.9119  | 96      |
| L094047006-CC                                    | 0.9184    | 0.9091 | 0.9137  | 99      |
| EPL0580589                                       | 0.9043    | 0.9239 | 0.9140  | 92      |
| PhysRevX.8.041024-CC                             | 0.9149    | 0.9149 | 0.9149  | 47      |
| PHC3200009-CC                                    | 0.9254    | 0.9051 | 0.9151  | 137     |
| P063224522-CC                                    | 0.9217    | 0.9091 | 0.9153  | 220     |
| SSC1310125-CC                                    | 0.8696    | 0.9677 | 0.9160  | 62      |
| L094127001-CC                                    | 0.9111    | 0.9213 | 0.9162  | 89      |
| HfV2O431805.08285-CC                             | 0.9231    | 0.9114 | 0.9172  | 79      |
| PR06013094-CC                                    | 0.8750    | 0.9655 | 0.9180  | 58      |
| P066020503-CC                                    | 0.9184    | 0.9184 | 0.9184  | 49      |
| PR05809504-CC                                    | 0.9067    | 0.9315 | 0.9189  | 73      |
| Rb2Cr3As3_quasi1D_SC1412.2596-CC                 | 0.8947    | 0.9444 | 0.9189  | 90      |
| PHC4020027-CC                                    | 0.8831    | 0.9577 | 0.9189  | 71      |
| P072104504-CC                                    | 0.9024    | 0.9367 | 0.9193  | 79      |
| Carnicom_2018_Supercond._Sci._Technol._31_115005 | 0.9073    | 0.9320 | 0.9195  | 147     |
| PHC3660299-CC                                    | 0.9200    | 0.9200 | 0.9200  | 75      |
| P067172501-CC                                    | 0.9062    | 0.9355 | 0.9206  | 62      |
| P065104523-CC                                    | 0.8905    | 0.9531 | 0.9208  | 128     |
| MAT0106521-CC                                    | 0.8767    | 0.9697 | 0.9209  | 66      |
| PHC3410729-CC                                    | 0.8889    | 0.9600 | 0.9231  | 25      |
| P065224520-CC                                    | 0.9016    | 0.9483 | 0.9244  | 58      |
| L088227002-CC                                    | 0.9074    | 0.9423 | 0.9245  | 104     |
| P069184513-CC                                    | 0.9130    | 0.9375 | 0.9251  | 112     |
| L091217001-CC                                    | 0.9492    | 0.9032 | 0.9256  | 62      |
| PR06103604-CC                                    | 0.9205    | 0.9310 | 0.9257  | 87      |
| PR05713422-CC                                    | 0.9278    | 0.9278 | 0.9278  | 97      |
| JPS0722453-CC                                    | 0.9000    | 0.9574 | 0.9278  | 47      |
| PR05907184-CC                                    | 0.9206    | 0.9355 | 0.9280  | 124     |
| P069014507-CC                                    | 0.9358    | 0.9211 | 0.9284  | 190     |
| PR04909084-CC                                    | 0.9100    | 0.9479 | 0.9286  | 96      |
| MPL0150497-CC                                    | 0.9118    | 0.9466 | 0.9288  | 131     |
| L093157004-CC                                    | 0.9302    | 0.9302 | 0.9302  | 86      |
| P069174506-CC                                    | 0.9286    | 0.9341 | 0.9313  | 167     |
| P066020501-CC                                    | 0.9123    | 0.9630 | 0.9369  | 54      |
| L092227003-CC                                    | 0.9310    | 0.9474 | 0.9391  | 57      |
| EPL0330153-CC                                    | 0.9118    | 0.9688 | 0.9394  | 32      |
| 1802.09882-CC                                    | 0.9206    | 0.9667 | 0.9431  | 60      |
| P072094504-CC                                    | 0.9808    | 0.9107 | 0.9444  | 56      |
| EPL0410207-CC                                    | 0.9565    | 0.9362 | 0.9462  | 47      |
| yamaguchi2014ac                                  | 0.9340    | 0.9612 | 0.9474  | 103     |
| PR04310568-CC                                    | 0.9333    | 0.9655 | 0.9492  | 29      |
| P065172501-CC                                    | 0.9423    | 0.9608 | 0.9515  | 102     |
| L094047001-CC                                    | 0.9423    | 0.9608 | 0.9515  | 102     |
| PR06014617-CC                                    | 0.9479    | 0.9579 | 0.9529  | 95      |
| P065024523-CC                                    | 0.9275    | 0.9846 | 0.9552  | 65      |
| PR06103808-CC                                    | 0.9726    | 0.9595 | 0.9660  | 74      |
| EPL0490086-CC                                    | 0.9853    | 0.9710 | 0.9781  | 69      |
| L090137002-CC                                    | 1.0000    | 1.0000 | 1.0000  | 14      |

### Removing the worst evaluated documents

#### Removing the top-worst-10

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| Paragraph-based | - | - | -  | - |
| [baseline-minus_worst_10-features](baseline/baseline-minus_worst_10-features) | baseline_features + gloVe + remove worst 10 documents  | 79.34   | 79.51  | 79.42 | 0.62 |
| [baseline-minus_worst_10-no_features](baseline/baseline-minus_worst_10-no_features) | baseline_no_features + gloVe + remove worst 10 documents | 78.43   | 79.28  | 78.85 | 0.72 |
| Sentences-based | - | - | -  | - |
| [baseline-by_sentences-minus_worst_10-features](baseline/baseline-by_sentences-minus_worst_10-features) | baseline_features + gloVe + by sentence + remove worst 10 documents  | 83.01  |  82.89  |  82.95 | 0.58 |
| [baseline-by_sentences-minus_worst_10-no_features](baseline/baseline-by_sentences-minus_worst_10-no_features) | baseline_no_features + gloVe + by sentence + remove worst 10 documents | 82.60  |  82.09   |82.33 | 0.61|

#### Removing the top-worst-20

| Name | Changes | Precision | Recall  | F1 | St Dev |
|------|---------|-----------|---------|----|--------|
| Paragraph-based | - | - | -  | - |
| [baseline-minus_worst_20-features](baseline/baseline-minus_worst_20-features) | baseline_features + gloVe + remove worst 20 documents  | 79.48  |  79.77  |  79.62 | 0.87 |
| [baseline-minus_worst_20-no_features](baseline/baseline-minus_worst_20-no_features) | baseline_no_features + gloVe + remove worst 20 documents | 78.06  |  78.73  |  78.39  | 0.56 |
| Sentences-based | - | - | -  | - |
| [baseline-by_sentences-minus_worst_20-features](baseline/baseline-by_sentences-minus_worst_20-features) | baseline_features + gloVe + by sentence + remove worst 20 documents  | 82.14  | 81.03  | 81.57 | 0.54 |
| [baseline-by_sentences-minus_worst_20-no_features](baseline/baseline-by_sentences-minus_worst_20-no_features) | baseline_no_features + by sentence + gloVe + remove worst 20 documents | 80.70  | 80.90  | 80.79 | 0.61 |

### Replace <other> with POS tag
The amount of token labeled with <other> is usually 10 times higher than all other tokens (e.g. <material> <class> etc... ). 
This could lead to a imbalance toward the <other> which reduce the recall of the model. 
With this experiment we want to proof that by reducing the amount of lables tagged with <other>, and replace them with the result of the POS tagging, we could recover some recall for out labels (e.g. <material> <class> etc...)

We replaced the `<other>` label with the relative POS tag of the word. 
In the summary table the baseline (previous result) is within parenthesis. 

#### Summary

|   Label               | Precision (baseline)      | Recall (baseline)     | F1 (baseline)     | Support   |
|---------              |----------                 |---------              |----               |--------   |
| No features layout    |  |   |   |
|         `<class>`     | 0.7233 (0.7371)           |  0.6339 (0.6784)      |  0.6754 (0.7062)  |     171   |
|      `<material>`     | 0.7663 (0.8029)           |  0.7767 (0.7992)      |  0.7714 (0.8010)  |     841   |
|     `<me_method>`     | 0.6790 (0.7116)           |  0.8195 (0.8319)      |  0.7425 (0.7668)  |     210   |
|      `<pressure>`     | 0.6564 (0.6730)           |  0.6163 (0.6408)      |  0.6298 (0.6496)  |      49   |
|            `<tc>`     | 0.7765 (0.7811)           |  0.7485 (0.7562)      |  0.7622 (0.7683)  |     402   |
|       `<tcValue>`     | 0.6832 (0.7154)           |  0.6537 (0.7165)      |  0.6676 (0.7149)  |     121   |
|   Features layout  |    |   |   |      
|         `<class>`     |   0.7155 (0.7378)         |  0.6404 (0.6825)      |  0.6757 (0.7087)  |    171    |
|      `<material>`     |   0.7797 (0.8111)         |  0.7811 (0.8033)      |  0.7803 (0.8071)  |    841    |
|     `<me_method>`     |   0.6884 (0.7285)         |  0.8167 (0.8195)      |  0.7469 (0.7705)  |    210    |
|      `<pressure>`     |   0.6713 (0.7403)         |  0.5408 (0.6286)      |  0.5923 (0.6749)  |     49    |
|            `<tc>`     |   0.7731 (0.7766)         |  0.7520 (0.7557)      |  0.7622 (0.7659)  |    402    |
|       `<tcValue>`     |   0.6738 (0.7474)         |  0.6496 (0.7207)      |  0.6609 (0.7331)  |    121    |

#### Result
| Name | Changes/Label | Precision | Recall  | F1 | Support |
|------|---------|-----------|---------|----|--------|
| [baseline-POS-no_features](baseline/baseline-POS-no_features) | baseline_no_features + gloVe + bin | | |
|  |           `<ADJ>`  | 0.9088  |  0.8901  |  0.8994  |    3928 |
|  |           `<ADP>`  | 0.9575  |  0.9507  |  0.9540  |    6091 |
|  |           `<ADV>`  | 0.9336  |  0.9263  |  0.9300  |    1429 |
|  |           `<AUX>`  | 0.9482  |  0.9492  |  0.9487  |     260 |
|  |         `<CCONJ>`  | 0.9618  |  0.9624  |  0.9621  |    1171 |
|  |           `<DET>`  | 0.9649  |  0.9666  |  0.9658  |    5371 |
|  |          `<INTJ>`  | 0.1967  |  0.0412  |  0.0669  |      17 |
|  |          `<NOUN>`  | 0.8989  |  0.9003  |  0.8996  |   11055 |
|  |           `<NUM>`  | 0.9243  |  0.9024  |  0.9132  |    2536 |
|  |          `<PART>`  | 0.8265  |  0.9066  |  0.8644  |     697 |
|  |          `<PRON>`  | 0.9523  |  0.9530  |  0.9527  |     568 |
|  |         `<PROPN>`  | 0.8082  |  0.6691  |  0.7317  |     641 |
|  |         `<PUNCT>`  | 0.9668  |  0.9552  |  0.9610  |    7214 |
|  |         `<SCONJ>`  | 0.9198  |  0.9443  |  0.9319  |     589 |
|  |           `<SYM>`  | 0.8684  |  0.9162  |  0.8916  |     247 |
|  |          `<VERB>`  | 0.9374  |  0.9376  |  0.9375  |    4540 |
|  |             `<X>`  | 0.7279  |  0.6642  |  0.6942  |     316 |
|  |         `<class>`  | 0.7233  |  0.6339  |  0.6754  |     171 |
|  |      `<material>`  | 0.7663  |  0.7767  |  0.7714  |     841 |
|  |     `<me_method>`  | 0.6790  |  0.8195  |  0.7425  |     210 |
|  |      `<pressure>`  | 0.6564  |  0.6163  |  0.6298  |      49 |
|  |            `<tc>`  | 0.7765  |  0.7485  |  0.7622  |     402 |
|  |       `<tcValue>`  | 0.6832  |  0.6537  |  0.6676  |     121 |
|  | all (micro avg.)   | 0.9239  |  0.9183  |  0.9211  | |
| [baseline-POS-features](baseline/baseline-POS-features) | baseline_features + gloVe + bin | | |
|  |           `<ADJ>`  |   0.9072  |  0.8946  |  0.9008   |   3928 |
|  |           `<ADP>`  |   0.9556  |  0.9531  |  0.9543   |   6091 |
|  |           `<ADV>`  |   0.9361  |  0.9262  |  0.9311   |   1429 |
|  |           `<AUX>`  |   0.9507  |  0.9492  |  0.9500   |    260 |
|  |         `<CCONJ>`  |   0.9612  |  0.9629  |  0.9621   |   1171 |
|  |           `<DET>`  |   0.9635  |  0.9664  |  0.9649   |   5371 |
|  |          `<INTJ>`  |   0.6067  |  0.0824  |  0.1410   |     17 |
|  |          `<NOUN>`  |   0.8989  |  0.9040  |  0.9014   |  11055 |
|  |           `<NUM>`  |   0.9205  |  0.9125  |  0.9164   |   2536 |
|  |          `<PART>`  |   0.8473  |  0.8898  |  0.8672   |    697 |
|  |          `<PRON>`  |   0.9524  |  0.9514  |  0.9519   |    568 |
|  |         `<PROPN>`  |   0.8027  |  0.6878  |  0.7405   |    641 |
|  |         `<PUNCT>`  |   0.9650  |  0.9580  |  0.9615   |   7214 |
|  |         `<SCONJ>`  |   0.9263  |  0.9448  |  0.9355   |    589 |
|  |           `<SYM>`  |   0.8635  |  0.9158  |  0.8888   |    247 |
|  |          `<VERB>`  |   0.9392  |  0.9369  |  0.9380   |   4540 |
|  |             `<X>`  |   0.7354  |  0.6750  |  0.7034   |    316 |
|  |         `<class>`  |   0.7155  |  0.6404  |  0.6757   |    171 |
|  |      `<material>`  |   0.7797  |  0.7811  |  0.7803   |    841 |
|  |     `<me_method>`  |   0.6884  |  0.8167  |  0.7469   |    210 |
|  |      `<pressure>`  |   0.6713  |  0.5408  |  0.5923   |     49 |
|  |            `<tc>`  |   0.7731  |  0.7520  |  0.7622   |    402 |
|  |       `<tcValue>`  |   0.6738  |  0.6496  |  0.6609   |    121 |
|  | all `(micro avg.)   |  0.9238  |  0.9208   | 0.9223 |          |


### Tokenization experiments
We try to change the tokenization to match the tokenization that might have been used for the embeddings.
The experiment requires different settings from other experiments above, and the absolute results cannot be compared. 
We are happy for a local improvement.. 

 
| Name | Changes/Label | Precision | Recall  | F1 | 
|------|---------|-----------|---------|---------|
| No layout features |
| [baseline_glove*](tokenization_experiments/10fold-superconductors-bidLSTM-glove-sentences-in-corpus.o23310) |glove + baseline + normal superconductors tokenization + original corpus | 78.87 |  80.35 |  79.60|
| [glove_baseline](tokenization_experiments/10fold-superconductors-bidLSTM+glove-noFeatures-baseline.o23305) |glove + baseline + normal superconductors tokenization | 78.19  |  78.81  |  78.49|
| [glove_tokenization](tokenization_experiments/10fold-superconductors-bidLSTM+glove-noFeatures-grobid_tokenization.o23302) |glove + baseline + grobid standard tokenization | 78.56 | 78.84 | 78.69|
| [oL+Sc+Sm*](tokenization_experiments/10fold-superconductors-bidLSTM+oL+Sc+Sm-sentences-in-corpus.o23311) |glove + baseline_features + normal superconductors tokenization | 79.62  |  80.48  |  80.04 |
| [oL+Sc+Sm](tokenization_experiments/10fold-superconductors-bidLSTM+oL+Sc+Sm-noFeatures-baseline.o23299) |oL+Sc+Sm + normal superconductors tokenization | 79.11  |  79.51  |  79.31|
| [oL+Sc+Sm_tokenization](tokenization_experiments/10fold-superconductors-bidLSTM+oL+Sc+Sm-noFeatures-baseline--FEATURES.o23298) |oL+Sc+Sm + grobid standard tokenization | 78.81  |  79.55  |  79.18 |
| With layout features |
| [glove_baseline_features](tokenization_experiments/10fold-superconductors-bidLSTM+glove-noFeatures-baseline--FEATURES.o23304) |glove + baseline_features + normal superconductors tokenization |79.05 |  78.78  |  78.91 |
| [glove_tokenization_features](tokenization_experiments/10fold-superconductors-bidLSTM+glove-noFeatures-grobid_tokenization-FEATURES.o23308) |glove + baseline_features  + grobid standard tokenization |  79.77  | 79.23  | 79.49  | 
| [oL+Sc+Sm_features](tokenization_experiments/10fold-superconductors-bidLSTM+oL+Sc+Sm-noFeatures-grobid_tokenization.o23301) |oL+Sc+Sm + normal superconductors tokenization + features | 78.89 | 79.24  |  79.06 |
| [oL+Sc+Sm_tokenization_features](tokenization_experiments/10fold-superconductors-bidLSTM+oL+Sc+Sm-noFeatures-grobid_tokenization-FEATURES.o23300) |oL+Sc+Sm + grobid standard tokenization + features | 79.02  |  80.13  |  79.57  |

(*) Using the original corpus with the features files. The rest of the data has been generated using only the XML to be able to manipulate the tokenization

## 6 vs 4 labels

| Name | Changes | Precision | Recall  | F1 |  
|------|---------|-----------|---------|----|
| [baseline](baseline/baseline_sentences-updated_corpus-glove) | 172 papers, features, gloVe, corpus manually segmented by sentences, filter out all sentences without entities | 80.47 | 80.66 | 80.56  | 0.59|
| [4label-features](baseline/fastText_glove-4labels-features) | baseline_features + fastText + oL+Sc+Sm  | 79.89 | 80.46 | 80.17  | 1.14 |
| [oL+Sc+Sc-baseline-features](oL+Sc+Sm/fastText_oL+Sc+Sc_sentences-updated-corpus-fasttext) | 172 papers, features, oL+Sc+Sm , corpus manually segmented by sentences, filter out all sentences without entities | 79.96  |  79.99  | 79.97 | 0.76 |
| [oL+Sc+Sm-4labels-features](oL+Sc+Sm/fastText_oL+Sc+Sm-4labels-features) | baseline_features + fastText + oL+Sc+Sm  |80.12  | 80.86  | 80.49 | 0.92 |
| [4labels-no_features](baseline/fastText_glove-4labels-no_features) | baseline_no_features + fastText + oL+Sc+Sm  | 79.44 | 79.60 | 79.52 | 0.61|
| [oL+Sc+Sm-4labels-no_features](oL+Sc+Sm/fastText_oL+Sc+Sm-4labels-no_features) | baseline_no_features + fastText + oL+Sc+Sm  | 79.42 | 79.98 | 79.69 | 0.75|


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



# Complete list of results 

| Name | Changes | Precision | Recall  | F1 | St Dev |  
|------|---------|-----------|---------|----|--------|
| [baseline_no_features](baseline/baseline_no_features) | 172 papers, no features | 77.00  |  77.20 |   77.09 |   0.61    |
| [baseline_with_features](baseline_features) | 172 papers, features | 77.95  |  77.27  |  77.60  |    0.99     |
| [baseline_by_sentences_with_features](baseline/baseline_sentences_features) | 172 papers, features, segmented by sentences, filter out paragraphs without entities | 77.48  |  79.31 |  78.38  | 0.53  |
| [baseline_by_sentences_positive_with_features](baseline/baseline_sentences_positive_features) | 172 papers, features, segmented by sentences, filter out all sentences without entities | 80.44 |   80.77 |   80.60  | 0.57 | 
| [baseline_by_sentences_positive_with_features-fastext](baseline/baseline_sentences_positive_features-fasttext) | 172 papers, features, oL+Sc+Sm , segmented by sentences, filter out all sentences without entities | 80.68  |  80.93  |  **80.81** | 0.72 |  
| [baseline_by_sentences-updated_corpus](baseline/baseline_sentences-updated_corpus-glove) | 172 papers, features, gloVe , corpus segmented by sentences, filter out all sentences without entities | 80.47 | 80.66 | 80.56  | 0.59|
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
| oL+Sc| Oscar Large + Science | - | -  | - |
| [fastText_oL+Sc_bin_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 76.74  |  76.77  |  76.75 | 0.87  |
| [fastText_oL+Sc_vec_skipgram_300d_no_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 76.42  |  76.04  |  76.21  | 0.75 |
| [fastText_oL+Sc_bin_skipgram_300d_features](oL+Sc/fastText_oL+Sc_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + bin  | 77.13  |  77.22  |  77.16| 1.03 |
| [fastText_oL+Sc_vec_skipgram_300d_features](oL+Sc/fastText_oL+Sc_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + skipgram + 300d + vec  | 77.60  |  77.39  |  77.49  | 0.59 |
| oL+Sm| Oscar Large + SuperMat | - | -  | - |
| [fastText_oL+Sm_bin_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 76.20  |  77.19  |  76.68 | 1.11 |
| [fastText_oL+Sm_vec_skipgram_300d_no_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.08  | 76.76  |  76.91 | 0.82 |
| [fastText_oL+Sm_bin_skipgram_300d_features](oL+Sm/fastText_oL+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + bin  | 78.04  |  77.92  |  77.98 | **0.21** |
| [fastText_oL+Sm_vec_skipgram_300d_features](oL+Sm/fastText_oL+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + SuperMat + skipgram + 300d + vec  | 77.60  |  78.42  |  **78.00** | 0.66|
| oL+Sc+Sm | Oscar Large + Science + SuperMat | - | -  | - |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 76.66 | 76.78  |76.70  | 0.80 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_no_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_no_features) | baseline_no_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 76.72  |  77.21 | 76.96  | 0.90 |
| [fastText_oL+Sc+Sm_bin_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_bin_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + bin  | 77.19 |  78.00  |  77.58 | 1.01 |
| [fastText_oL+Sc+Sm_vec_skipgram_300d_features](oL+Sc+Sm/fastText_oL+Sc+Sm_vec_skipgram_300d_features) | baseline_features + fastText + Oscar Large + Science + SuperMat + skipgram + 300d + vec  | 77.21 |  78.00  |  77.60 | 0.49 |
| [fastText_oL+Sc+Sc_by_sentences-updated_corpus-fastext](fastText_oL+Sc+Sc_sentences-updated-corpus-fasttext) | 172 papers, features, oL+Sc+Sm , corpus segmented by sentences, filter out all sentences without entities | 79.96  |  79.99  | 79.97 | 0.76 |
