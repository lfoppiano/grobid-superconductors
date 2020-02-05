# Models 


## Summary 
This table summarise the various models and evolution of evaluation. 

* in parenthesis the results enabling features based on layout

| date | number of documents | number of labels | changes | CRF (F1) | BidLSTM+CRF (F1)*  | evaluation files |
|------|---------------------|------------------|---------|----------|------------------|------------------|
|  2020/01/31  | 85  |  6 | Enabling features from quantity model| 65.28   | (77.30) | |
|  [2020/01/25](https://github.com/lfoppiano/grobid-superconductors/tree/training-20200125)  | 85  |  6 | Adding 25 papers              |65.07     | 76.65 (77.08/76.87/76.79) | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20200125/resources/models/superconductors/result-logs) |
|  2010/12/25  | 60  |  6 | Enabling features from quantity model | 60.41 | TBA | [results](https://github.com/lfoppiano/grobid-superconductors/tree/add-quantities-features/resources/models/superconductors/result-logs) |
|  [2019/12/23](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191223)  | 60  |  6 | Adding 16 papers | 61.99 | 69.68/71.15 (73.15/72.27)| [`superconductors-evaluation-20191223.txt`](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191223.txt) |
|  [2019/12/18](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191218)  | 44  |  6 | Second review of annotations with updated guidelines | 59.17 | TBA | [results CRF](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191218/resources/models/superconductors/model.wapiti.evaluation.txt) |
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213)  | 44  |  6 | Switch to 6 labels (including `<pressure>` and `<tcValue>`) |57.6  | TBA |  [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213/resources/models/superconductors/result-logs) |
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels)  | 44  |  4 | Adding 22 more papers, corrected data from domain experts |63.3  | TBA | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels/resources/models/superconductors/result-logs) |
|  [2019/11/22](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122)  | 23  |  4 | First correction from domain experts | 56.4 / 57.44 | TBA |  [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122/resources/models/superconductors/result-logs) |
|  [2019/11/04](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104)  | 23  |  4 | Features engineering: added layout information (superscript/subscript) and chemdataextractor  |55.5 / 56.17 | TBA | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104/resources/models/superconductors/results-log) |
|  2019/10/31  | 23  |  4 | Initial model after meeting with domain expertes | 54.77 | TBA | [`superconductors-10fold-cross-validation-20191031.txt`](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191031.txt) |
|  2019/05/25  | TBA  |  1 | Initial model | TBA | TBA |  |

## Detailed evaluation 

TBD
