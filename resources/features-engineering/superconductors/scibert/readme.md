# Scibert fine-tuning 

## Introduction

This fine-tuning was performed following the guide [here](https://github.com/google-research/bert#pre-training-with-bert).
The data used for fine-tuning is text data from scientific articles in material science (~20Gb) + [SuperMat](https://github.com/lfoppiano/SuperMat) for superconductors materials.

Sentences: 100001987
Tokens: 


## Summary results 

| Name  | training steps    | Notes | Masked accuracy | Masked loss  | Next sentence accuracy | Next sentence loss |
|--------|--------- |------|---------|----|--------|--------|
| o23445 | 20       | Just a test | 0.6763816 | 1.5105013 | 0.96125 | 0.121605136 | 
| o23446 | 100000   | running with 100000 steps and 10000 of warm up steps | 0.70375 | 1.3560376 | 0.97875 | 0.060185157 |
| o23449 | 1000000  | running with 1000000 asteps and 10000 of warm up steps | 0.7238158 | 1.2188154 | 0.97625 | 0.049554683 |


## Details parameters 

- o23445: `--input_file=pretrained/science+supermat.tfrecord_*   --output_dir=pretraining_output    --do_train=True    --do_eval=True    --bert_config_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_config.json    --init_checkpoint=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_model.ckpt    --train_batch_size=32    --max_seq_length=128    --max_predictions_per_seq=20    --num_train_steps=100000    --num_warmup_steps=10000    --learning_rate=2e-5`
- o23446: `--input_file=pretrained/science+supermat.tfrecord_*   --output_dir=pretraining_output    --do_train=True    --do_eval=True    --bert_config_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_config.json    --init_checkpoint=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_model.ckpt    --train_batch_size=32    --max_seq_length=128    --max_predictions_per_seq=20    --num_train_steps=100000    --num_warmup_steps=10000    --learning_rate=2e-5`
- o23449: `--input_file=pretrained/science+supermat.tfrecord_*   --output_dir=pretraining_output    --do_train=True    --do_eval=True    --bert_config_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_config.json    --init_checkpoint=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/bert_model.ckpt    --train_batch_size=32    --max_seq_length=128    --max_predictions_per_seq=20    --num_train_steps=1000000    --num_warmup_steps=10000    --learning_rate=2e-5`


## Additional information

### Preliminary steps 

1. Transformed paragraphs in sentences using [BlingFire](https://github.com/Microsoft/BlingFire)
2. Shard the initial large file. [Suggestion](https://github.com/google-research/bert/issues/117).  
3. Create pre-training for adding masking (max_sequence_lenght=128):  
   ```
    for x in ../sharded_corpus/*; do python create_pretraining_data.py    --input_file=${x}    --output_file=./pretrained_128/science+supermat.tfrecord_${x##*.}   --vocab_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/vocab.txt    --do_lower_case=False    --max_seq_length=128    --max_predictions_per_seq=20    --masked_lm_prob=0.15    --random_seed=12345    --dupe_factor=5; done
   ```
4. Create pre-training for adding masking (max_sequence_lenght=512):
    ```
    for x in ../sharded_corpus/*; do python create_pretraining_data.py    --input_file=${x}    --output_file=./pretrained_512/science+supermat.tfrecord_${x##*.}   --vocab_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/vocab.txt    --do_lower_case=False    --max_seq_length=512    --max_predictions_per_seq=20    --masked_lm_prob=0.15    --random_seed=23233    --dupe_factor=5; done
   ```

