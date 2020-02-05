# Models 


## Summary 
This table summarise the various models and evolution of evaluation. 

* In the BidLSTM+CRF column, the result between parenthesis is obtained by enabling [features layout](https://github.com/kermitt2/delft/pull/82). 
* Results might slightly variate. In some cases, we report multiple F1 results but a single detailed result. 

| date | number of documents | number of labels | changes | CRF (F1) | BidLSTM+CRF (F1)*  | 
|------|---------------------|------------------|---------|----------|------------------|
|  2020/01/31  | 85  |  6 | Enabling features from quantity model| 65.28   | (77.30) | |
|  [2020/01/25](https://github.com/lfoppiano/grobid-superconductors/tree/training-20200125)  | 85  |  6 | Adding 25 papers              |65.07 [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20200125/resources/models/superconductors/result-logs)     | 76.65 [details](https://github.com/lfoppiano/grobid-superconductors/blob/training-20200125/resources/models/superconductors/model.delft-no-features.evaluation.txt) (77.08/76.87/76.79 [details](https://github.com/lfoppiano/grobid-superconductors/blob/training-20200125/resources/models/superconductors/model.delft-with-features.evaluation.txt)) |
|  2019/12/25  | 60  |  6 | Enabling features from quantity model | 61.35 [details](https://github.com/lfoppiano/grobid-superconductors/tree/add-quantities-features/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191226-quantities-features.txt) | TBA | 
|  [2019/12/23](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191223)  | 60  |  6 | Adding 16 papers | 61.99 [details](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191223.txt) | 69.68/71.15 (73.15/72.27)| 
|  2019/12/18  | 44  |  6 | Enabling features from quantity model | 60.41 [details](https://github.com/lfoppiano/grobid-superconductors/tree/add-quantities-features/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191217-quantities-features.txt) | TBA |
|  [2019/12/18](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191218)  | 44  |  6 | Second review of annotations with updated guidelines | 59.17 [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191218/resources/models/superconductors/model.wapiti.evaluation.txt) | TBA | 
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213)  | 44  |  6 | Switch to 6 labels (including `<pressure>` and `<tcValue>`) |57.6  [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213/resources/models/superconductors/result-logs) | TBA |
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels)  | 44  |  4 | Adding 22 more papers, corrected data from domain experts |63.3  [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels/resources/models/superconductors/result-logs) | TBA | 
|  [2019/11/22](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122)  | 23  |  4 | First correction from domain experts | 56.4 / 57.44 [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122/resources/models/superconductors/result-logs) | TBA |  
|  [2019/11/04](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104)  | 23  |  4 | Features engineering: added layout information (superscript/subscript) and chemdataextractor  |55.5 / 56.17 [details](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104/resources/models/superconductors/results-log) | TBA | 
|  2019/10/31  | 23  |  4 | Initial model after meeting with domain experts | 54.77 [details](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191031.txt) | TBA | 
|  2019/05/25  | TBA  |  1 | Initial model | TBA | TBA | 

## Detailed evaluation 

TBD
