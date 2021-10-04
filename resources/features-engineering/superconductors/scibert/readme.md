# Scibert fine-tuning

## Introduction

This page contains notes and information about the process of fine-tuning SciBERT for improving the results in the task
of NER for materials science scientific papers. This process was performed following the
guide [here](https://github.com/google-research/bert#pre-training-with-bert).

The data used for fine-tuning is text data from scientific articles in material science
and [SuperMat](https://github.com/lfoppiano/SuperMat) for superconductors materials.

| Space | sentences | tokens |
|-------|---|---|
| ~21Gb | 100001987 (100M) | 3260171825 (3.2B) |

- Useful references:
    - SciBERT's'[cheatsheet](https://github.com/allenai/scibert/blob/master/scripts/cheatsheet.txt).
    - [memory consumption](https://github.com/google-research/bert#out-of-memory-issues)

### Preliminary studies

#### Vocab comparison

This comparison attempt to figure out roughly how distant the vocabulary of the fine-tuning data (`myvocab`) and the
original SciBERT dataset (`scivocab`) are.

Notes:

- the vocabulary used in *BERT is limited to 31k items, so is very unlikely that the two vocabularies are going to
  overlap 100%
- what is the weight of each token in the vocabulary?
- there are 100 unused tokens that BERT allocate for "common tokens that could appear in a specific
  domain" ([cf](https://github.com/allenai/scibert/issues/38#issuecomment-490520991)). Could we get these most important
  100 tokesn from our `myvocab`?
- [thread](https://github.com/google-research/bert/issues/9) discussing how to enlarge the BERT vocabulary. Example script [here](https://github.com/huggingface/transformers/issues/1413#issuecomment-538083512).

##### Steps:

1. Generate a new vocabulary by training the sentencepiece model
   ``
   python import sentencepiece as spm spm.SentencePieceTrainer.Train('--input=[...] --model_prefix=100B_9999_cased --vocab_size=31000 --character_coverage=0.9999 --model_type=bpe --input_sentence_size=100001987 --shuffle_input_sentence=true')
   ``

2. Transform the output vocabulary (``100B_9999_cased.vocab``) as
   described [here](https://github.com/allenai/scibert/issues/38#issuecomment-488867883). The steps are roughly:
    1. ``mv 100B_9999_cased.vocab myvocab.sentencepiece.txt``
    2. ``cut -f1 myvocab.sentencepiece.txt  > myvocab.txt``
    3. ``sed -e 's/^\([^▁<]\)/##\1/' myvocab.txt > myvocab.tmp.txt``
    4. ``sed -e 's/^▁//' myvocab.tmp.txt > myvocab.postprocessed.txt``
    5. ``cat myvocab.postprocessed.txt scivocab.txt | gsort | guniq -d > scivocab.intersection.myvocab.txt``
    6. ``gsort myvocab.postprocessed.txt > myvocab.postprocessed.sorted.txt``
    7. ``gsort scivocab.txt > scivocab.sorted.txt``
    8. ``comm -23 myvocab.postprocessed.sorted.txt scivocab.sorted.txt > myvocab.only.txt``
    9. ``comm -13 myvocab.postprocessed.sorted.txt scivocab.sorted.txt > scivocab.only.txt``

3. Match each item from the [myvocab.only.txt](vocab/myvocab.only.txt) into the original line
   of [myvocab.sentencepiece.txt](vocab/myvocab.sentencepiece.txt) using the script [token-match.py](token-match.py):
    1. result considering any token [myvocab.only.txt](vocab/myvocab.only.txt) is matched and saved
       into [myvocab.lookedup_from_myvocab.only.txt](vocab/myvocab.lookedup_from_myvocab.only.txt)
    2. result considering only token that are not "subwords" (do not start with a `##`) is matched and saved
       into  [myvocab.lookedup_from_myvocab.only.only_full_words.txt](vocab/myvocab.lookedup_from_myvocab.only.only_full_words.txt)

##### Results

- Vocabulary length: 31000
- Vocabulary intersection (scivocab vs myvocab): 18107 (58.40%)
- Tokens only in scivocab: 12907
- Tokens only in myvocab: 12895

Output is [here](./vocab):


#### Vocab domain-specific additional terms

##### Steps

BERT's vocabulary allow 100 terms to be set in the configuration file. They represent eventual domain-specific terms that might be very frequent and relevant when pre-training on a new domain. 
In this section we discuss the process followed to extract such list of 100 terms. 

1. Ran [keybert](https://github.com/MaartenGr/KeyBERT) on each file with the following parameters ``stop_words=[], use_mmr=False, use_maxsum=False, nr_candidates=100, top_n=10``. The script is [here](https://github.com/lfoppiano/grobid-superconductors-tools/blob/master/vocab-builder/run_keybert.py) and outputs a JSONL where each line is a list of tuples with (term, score).   

2. Aggregate each file into a single one 
   1. ``for x in `find output -name *.json`; do cat $x >> aggregated_raw.jsonl; done``
   2. ``sed 's/\(\]\]\)\(\[\[\)/\1\n\2/g' aggregated_raw.jsonl > aggregated.jsonl``

3. Unwind each document's list as a line in a file: ``while read in; do echo "$in" | jq '.[][0]' >> aggregated.as_list.txt ; done < aggregated.jsonl`` 

4. Calculate and sort by frequency: ``awk '{A[$1]++}END{for(k in A) print k, A[k]}' aggregated.as_list.txt | sort -rnk2  | head -n 100``

5. Obtain the list of terms by removing unnecessary quotes and frequency  ``awk '{gsub(/\"|\;/,"",$1); print $1 }' filename``

##### Results
The output data is in the following form: 
```
polymer
materials
chemistry
polymers
catalytic
magnetic
semiconductor
chemical
spectroscopy
synthesis
nanoparticles
```

The raw data can be found [here](features-engineering/superconductors/scibert/domain-specific-vocab). 

### Pre-training

Starting from a text file containing one paragraph per line, we performed the following operations:

1. Split paragraphs in sentences using [BlingFire](https://github.com/Microsoft/BlingFire), a sentence splitter.
2. Shard the obtained resulted large file, into several smaller (max 250000 sentences).
   See [ref](https://github.com/google-research/bert/issues/117).
    ```
    -rw-r--r-- 1 lfoppian0 tdm  50M Aug 12 10:29 SciCorpora+SuperMat.sentences.sharded00
    -rw-r--r-- 1 lfoppian0 tdm  50M Aug 12 10:29 SciCorpora+SuperMat.sentences.sharded01
     [...]
    ```
3. Create pre-training, preprocess the sentences and add masking:
    1. short sequences (`max_sequence_lenght=128`):
       ```
        for x in ../sharded_corpus/*; do python create_pretraining_data.py    --input_file=${x}    --output_file=./pretrained_128/science+supermat.tfrecord_${x##*.}   --vocab_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/vocab.txt    --do_lower_case=False    --max_seq_length=128    --max_predictions_per_seq=20    --masked_lm_prob=0.15    --random_seed=12345    --dupe_factor=5; done
       ```
    2. for large sequences (`max_sequence_lenght=512` and `max_predictions_per_seq=78` (0.15*512)):
        ```
        for x in ../sharded_corpus/*; do python create_pretraining_data.py    --input_file=${x}    --output_file=./pretrained_512/science+supermat.tfrecord_${x##*.}   --vocab_file=/lustre/group/tdm/Luca/delft/delft/data/embeddings/scibert_scivocab_cased/vocab.txt    --do_lower_case=False    --max_seq_length=512    --max_predictions_per_seq=78    --masked_lm_prob=0.15    --random_seed=23233    --dupe_factor=5; done
        ```

4. Run pre-training ([ref](https://github.com/google-research/bert#pre-training-tips-and-caveats))

**NOTE**: The num_train_steps is used as "global max step", therefore when doing incremental training is important to
consider that as an absolute value. [Ref](https://github.com/google-research/bert/issues/632).

| Name  | Notes | max_sequence_lenght | train_batch_size | num_train_steps | learning_rate | max_prediction_seq | init_checkpoint | Masked accuracy | Masked loss  | Next sentence accuracy | Next sentence loss |
|--------|--------- |------|---------|----|--------|--------|---- | ---- | ---- | --- | --- |
| Baseline short sequences | total = 500000*256=12000000 | 128 | 256 | 500000 |  1000 | 1e-4 | 20 | |
| Baseline long sequences |total = 300000*64=19000200  | 512 | 64 | 800000 |  100 | 1e-5 | 76 |  |

## Log and results

| Name  | Notes | max_sequence_lenght | train_batch_size | num_train_steps | learning_rate | max_prediction_seq | init_checkpoint | Masked accuracy | Masked loss  | Next sentence accuracy | Next sentence loss |
|--------|--------- |------|---------|----|--------|--------|---- | ---- | ---- | --- | --- |
| SciBERT's original parameters |
| Sc+Sm fine tuning short sequences | o23483, same parameters as described by SciBERT's authors |128 | 256 | 12000000 |  1000 | 1e-4 | 20 | OOM |
| Sc+Sm fine tuning short sequences | o23485, same parameters as described by SciBERT's authors |128 | 128 | 24000000 |  1000 | 1e-4 | 20 | OOM |
| Sc+Sm fine tuning short sequences | o23487, same parameters as described by SciBERT's authors |128 | 64 | 48000000 |  1000 | 1e-4 | 20 | OOM |
| Sc+Sm fine tuning short sequences (SciBERT's original equivalent with lower batch size) | o23488, same parameters as described by SciBERT's authors (pretraining_output_128_1e4) |128 | 32 | 96000000 |  1000 | 1e-4 | 20 | TBD |
| Sc+Sm fine tuning short sequences (SciBERT's original equivalent with lower batch size), lower learning rate | TBD | 128 | 32 | 96000000 |  1000 | 1e-5 | 20 | TBD |
| Lower number of steps  |
| Sc+Sm fine tuning short sequences (100K steps) | ~~o23497~~, 900k train steps |128 | 32 | 900000 |  1000 | 1e-4 | 20 | 0.7224342 | 1.2316597 | 0.9825 | 0.04809034 |
| Sc+Sm fine tuning long sequences (100K steps), from o23497 | ~~o23529~~, 1M train steps | 128 | 32 | 900000 |  1000 | 1e-4 | 20 | 0.7668856 | 1.0131468 | 0.99625 | 0.021626918 |
| Sc+Sm fine tuning short sequences (200K steps) | ~~o23489~~, 1M train steps |128 | 32 | 1000000 |  1000 | 1e-4 | 20 | 0.7258553 | 1.2150538 | 0.9875 | 0.03644385 |
| Sc+Sm fine tuning short sequences (~11M steps) | o23490, 12M train steps |128 | 32 | 12000000 |  1000 | 1e-4 | 20 | TBD |

## Details parameters

## Additional information

# Credits

Various people have helped with small feedback or more useful observations and ideas:

- Pedro Ortiz pedro.ortiz@inria.fr
- arij.riabi@inria.fr
- Romain roman.castagne@inria.fr
- Patrice Lopez patrice.lopez@science-miner.com

