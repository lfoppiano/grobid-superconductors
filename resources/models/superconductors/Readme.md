# Models 

This table summarise the various models and evolution of evaluation. 

| date | number of documents | number of labels | F1 |changes | evaluation file |
|--------------|-----|----|-------|---------------------|---|
|  2019/01/23  | 85  |  6 | 65.07 | Adding 25 papers | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20200123/resources/models/superconductors/result-logs) |
|  2019/12/25  | 60  |  6 | 60.41 | Enabling features from quantity model | [results](https://github.com/lfoppiano/grobid-superconductors/tree/add-quantities-features/resources/models/superconductors/result-logs) |
|  2019/12/23  | 60  |  6 | 61.99 | Adding 16 papers | [`superconductors-evaluation-20191223.txt`](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191223.txt) |
|  2019/12/18  | 44  |  6 | 59.17 | Second review of annotations with updated guidelines | [`superconductors-10fold-cross-validation-20191218.txt`](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191218.txt) |
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213)  | 44  |  6 | 57.6  | Switch to 6 labels (including `<pressure>` and `<tcValue>`) | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213/resources/models/superconductors/result-logs) |
|  [2019/12/13](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels)  | 44  |  4 | 63.3  | Adding 22 more papers, corrected data from domain experts | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191213-4labels/resources/models/superconductors/result-logs) |
|  [2019/11/22](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122)  | 23  |  4 | 56.4 / 57.44 | First correction from domain experts | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191122/resources/models/superconductors/result-logs) |
|  [2019/11/04](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104)  | 23  |  4 | 55.5 / 56.17 | Features engineering: added layout information (superscript/subscript) and chemdataextractor  | [results](https://github.com/lfoppiano/grobid-superconductors/tree/training-20191104/resources/models/superconductors/results-log) |
|  2019/10/31  | 23  |  4 | 54.77 | Initial model | [`superconductors-10fold-cross-validation-20191031.txt`](https://github.com/lfoppiano/grobid-superconductors/blob/master/resources/models/superconductors/result-logs/superconductors-10fold-cross-validation-20191031.txt) |