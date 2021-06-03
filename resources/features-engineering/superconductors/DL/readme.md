# Feature engineering experiments with DL

## Summary 

| Name | Changes | Precision | Recall  | F1 |  
|------|---------|-----------|---------|----|
| [baseline_no_features](baseline_no_features) | 172 papers, no features | 77.00  |  77.20 |   77.09 |
| [baseline_with_features](baseline_features) | 172 papers, features | 77.95  |  77.27  |  77.60  |
| [fastext_cbow_100d_no_features](fastext_cbow_100d_no_features) | baseline_no_features + fastext + oscar_Supermat + cbow + 100d |   72.15 |   73.39 |   72.75 |
| [fastext_cbow_100d_features](fastext_cbow_100d_features) | baseline_features + fastext + oscar_Supermat + cbow + 100d |  | | |
| [fastext_skipgram_100d_no_features](fastext_skipgram_100d_no_features) | baseline_no_features + fastext + oscar_Supermat + skipgram + 100d | 74.22  |  75.99  |  75.08 | 
| [fastext_skipgram_100d_features](fastext_skipgram_100d_features) | baseline_no_features + fastext + oscar_Supermat + skipgram + 100d |  | | |
