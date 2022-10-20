# SciBERT pre-training and fine-tuning experiments

* [Introduction](#introduction)
* [Preliminary studies](#preliminary-studies)
* [Pre-training](#pre-training)
    + [Preparation](#preparation)
    + [Number of steps calculation](#nb-steps-calculation)
    + [Pre-training execution](#pre-training-execution)
      - [Nims analysis cluster](#nims-analysis-cluster)
      - [Google Cloud Platform](#google-cloud-platform)
* [Fine-tuning](#fine-tuning)

## Introduction

This page contains notes and information about the process of pre-training [SciBERT](https://github.com/allenai/scibert) for improving the results in the task
of NER for materials science scientific papers. This process was performed following the
guide [here](https://github.com/google-research/bert#pre-training-with-bert).

The data used for fine-tuning is text data from scientific articles in material science
and [SuperMat](https://github.com/lfoppiano/SuperMat) for superconductors materials.

| Space | sentences | tokens |
|-------|---|---|
| ~21Gb | 142306511 (142M) | 3286118146 (3.2B) |

- Useful references:
    - SciBERT's'[cheatsheet](https://github.com/allenai/scibert/blob/master/scripts/cheatsheet.txt).
    - [memory consumption](https://github.com/google-research/bert#out-of-memory-issues)

## Preliminary studies

### Vocab comparison

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

The raw data can be found [here](features-engineering/superconductors/scibert/domain-specific-vocab):
 - `myvocab-100.clean.txt` (list 100 most frequent terms)
 - `myvocab-all.clean.txt` (list of all the extracted terms, sorted by frequency)
 - `myvocab-100.txt` (list 100 most frequent terms and frequency)
 - `myvocab-all.txt` (list of all the extracted terms and frequency, sorted by frequency)

#### Extract exclusive terms from the domain-specific vocabulary

This final task reduce the domain-specific vocabulary to the only terms that are not already included in the Bert/SCIBert pre-defined vocabulary.
``vocab.txt``: SciBERT/BERT vocabulary
``myvocab.txt``: domain-specific vocabulary (obtained from the procedure above)

##### Steps

1. Sort vocabularies
   ``sort vocab.txt > vocab.sorted.txt``
   ``sort myvocab-all.clean.txt > myvocab.sorted.txt``

2. Find terms that are only in the domain-specific vocabulary: 
   ``comm -13 vocab.sorted.txt myvocab.sorted.txt > myvocab.only.txt``

3. Now we need to find the frequencies, which are mapped into ``myvocab-all.txt``. Inside that files the terms are quoted, so we need to add the quotes to the file resulting from previous step:  
    ``awk '{ print "\""$0"\""}' myvocab.only.txt > myvocab.only.quoted.txt``

4. Sort everything 
    ``sort myvocab.only.quotedtxt > myvocab.only.quoted.sorted.txt``

5. Map the terms not in the BERT/SCIBert vocabulary to the frequencies:
   ``join -1 1 -2 1 -o 1.1 2.2 myvocab.only.quoted.sorted.txt myvocab.sorted.txt > myvocab.only.with_recomputed_frequencies.txt``

6. Sort the terms by frequencies:  
``sort -rnk2 myvocab.only.with_recomputed_frequencies.txt > myvocab-excluding_scibert_terms.all.txt``

7. Sort and extract the 100 most frequent terms: 
``sort -rnk2 intersection | head -n 100 > myvocab-excluding_scibert_terms.100.txt``

##### Results
 - ``myvocab-excluding_scibert_terms.100.txt``: vocabulary of the 100 most frequent keyterms that are not already included in the SciBERT vocabulary
 - ``myvocab-excluding_scibert_terms.all.txt``: complete domain-specific vocabulary for material science text excluding terms that are not already included in the SciBERT vocabulary. 

### Intersection between SciCorpus and SuperMat
This list contains the documents that are in both SciCorpus and SuperMat 
- `tdm-corpora/aps/2016/10.1103_PhysRevB.94.180509/10.1103_PhysRevB.94.180509_fulltext_20200826.txt:Bulk superconductivity at 84 K in the strongly overdoped regime of cuprates`

## Pre-training

### Preparation

Starting from a text file containing one paragraph per line, we performed the following operations:

1. Split paragraphs in sentences using [BlingFire](https://github.com/Microsoft/BlingFire), a sentence splitter ([script](sentence-splitter.py)).
2. Shard the obtained resulted large file, into several smaller (max 250000 sentences). E.g. ``split --lines=250000 --numeric-suffixes input prefix``. 
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

### Nb steps calculation

| Length                                             | nb_steps (relative) | nb_steps (absolute)           | batch size | total (relative)  |
|----------------------------------------------------|---------------------|-------------------------------|------------|-------------------| 
| SciBERT original work                              |                     |                               |            |                   | 
| 128                                                | 500000              | 500000                        | 256        | 128000000         | 
| 512                                                | 300000              | 800000                        | 64         | 19200000          |
| Adjusted number of steps due to the GPU limitation |                     |                               |            |                   |
| 128                                                | 500000*8=4000000    | 4000000 + 800000 = 4800000    | 32         |                   |
| 512                                                | 300000*8=2400000    | 4800000 + 2400000 = 7200000   | 8          |                   | 


### Pre-training execution 

#### NIMS Analysis cluster 

**NOTE**: The num_train_steps is used as "global max step", therefore when doing incremental training is important to
consider that as an absolute value. [Ref](https://github.com/google-research/bert/issues/632).

| Name  | Notes | max_sequence_lenght | train_batch_size | num_train_steps | learning_rate | max_prediction_seq | init_checkpoint | Masked accuracy | Masked loss  | Next sentence accuracy | Next sentence loss |
|--------|--------- |------|---------|----|--------|--------|---- | ---- | ---- | --- | --- |
| Baseline short sequences | total = 500000*256=12000000 | 128 | 256 | 500000 |  1000 | 1e-4 | 20 | |
| Baseline long sequences |total = 300000*64=19000200  | 512 | 64 | 800000 |  100 | 1e-5 | 76 |  |

##### Log and results

| Name  | Notes | max_sequence_lenght | train_batch_size | num_train_steps | learning_rate | max_prediction_seq | init_checkpoint | Masked accuracy | Masked loss  | Next sentence accuracy | Next sentence loss |
|--------|--------- |------|---------|----|--------|--------|---- | ---- | ---- | --- | --- |
| -- | SciBERT's original parameters |
| Sc+Sm pre-training short sequences | o23483, same parameters as described by SciBERT's authors |128 | 256 | 1300000 |  1000 | 1e-4 | 20 | OOM |
| Sc+Sm pre-training short sequences | o23485, same parameters as described by SciBERT's authors |128 | 128 | 2600000 |  1000 | 1e-4 | 20 | OOM |
| Sc+Sm pre-training short sequences | o23487, same parameters as described by SciBERT's authors |128 | 64  | 5200000 |  1000 | 1e-4 | 20 | OOM |
| -- | Test 0  | Lower number of steps |
| Sc+Sm pre-training short sequences (100K steps) | ~~o23497~~, 900k train steps |128 | 32 | 900000 |  1000 | 1e-4 | 20 | 0.7224342 | 1.2316597 | 0.9825 | 0.04809034 |
| Sc+Sm pre-training long sequences (100K steps), from o23497 | ~~o23529~~, 1M train steps | 128 | 8 | 900000 |  1000 | 1e-4 | 78 | 0.7668856 | 1.0131468 | 0.99625 | 0.021626918 |
| -- | Test 1 | Too high number of steps |
| Sc+Sm pre-training short sequences (~11M steps) | ~~o23490~~, 12M train steps |128 | 32 | 12000000 |  1000 | 1e-4 | 20 | 0.74782896 | 1.0797467 | 0.98875 | 0.023861337 |
| Sc+Sm pre-training long sequences, from o23490 | o23651, +2.4M (total: 14.4M) train steps | 512 | 8 | 14400000 |  1000 | 1e-5 | 78 | TBD |
| -- | Test 2 | Re-calculated number of steps | 
| Sc+Sm pre-training short sequences | o23652, Re-calculated number of steps 4.8M steps |128 | 32  | 4800000 |  1000 | 1e-4 | 20 | TBD |
| -- | Other cases |
| Sc+Sm pre-training short sequences (200K steps) | ~~o23489~~, 1M train steps |128 | 32 | 1000000 |  1000 | 1e-4 | 20 | 0.7258553 | 1.2150538 | 0.9875 | 0.03644385 |

#### Google Cloud Platform

- Create TPU
    > gcloud compute tpus execution-groups create --name=tpu1234 --zone=us-central1-a --tf-version=1.15.5  --machine-type=n1-standard-1  --accelerator-type=v3-8

ctpu up -name tpu2345 -zone "us-central1-a"  -tpu-size=v3-8 -tpu-only --preemptible -tf-version 1.15.5

- Run pre-training 
  - batch size 256
  - max seq length 512
  
  > python3 run_pretraining.py --input_file=gs://matscibert/pretrained_512.v2/science+supermat.tfrecord_sharded*  --output_dir=gs://matscibert/models/matscibert-myvocab_cased_512  --do_train=True --do_eval=True --bert_config_file=bert_config.json --train_batch_size=256 --max_seq_length=512 --max_predictions_per_seq=78 --num_train_steps=1100000 --num_warmup_steps=100 --learning_rate=1e-5 --use_tpu=True --tpu_name=tpu1234 --max_eval_steps=2000  --eval_batch_size 64 --init_checkpoint=gs://matscibert/scibert_scivocab_cased/bert_model.ckpt --tpu_zone=us-central1-a

- When TPU is preempted, delete the TPU and re-create... and pray that will not be preempted again
    ```
    export START=167000; 
    nohup python3 run_pretraining.py --input_file=gs://matscibert/pretrained_512.v2/science+supermat.tfrecord_sharded* --output_dir=gs://matscibert/models/matscibert-myvocab_cased_512 --do_train=True --do_eval=True --bert_config_file=bert_config.json --train_batch_size=256 --max_seq_length=512 --max_predictions_per_seq=78 --num_train_steps=1600000 --num_warmup_steps=${START} --learning_rate=1e-5 --use_tpu=True --tpu_name=tpu1234 --max_eval_steps=2000 --eval_batch_size 64 --init_checkpoint=gs://matscibert/models/matscibert-myvocab_cased_512/model.ckpt-${START} --tpu_zone=us-central1-a
    ```

## Fine-tuning

- Mat+SciBERT (TPU): TPU trained of SciBERT for 1600000 steps using SciCorpora+SuperMat split by sentences
- Mat+RoBERTa: Inria cluster trained RoBERTa from scratch using SciCorpora split by paragraphs
- SciBERT: The classical SciBERT from Allen AI
- matscibert: The material BERT from Gupta et al. (https://huggingface.co/m3rg-iitd/matscibert)

### Superconductors NER

| Run nb. | DeLFT | Architecture       | Transformer         | precision | recall   | f1-score | 
|---------|-------|--------------------|---------------------|-----------|----------|----------|
| 24560   | 0.2.8 | scibert            | SciBERT             | 0.8219    | 0.8520   | 0.8367   |
| 24464   | 0.2.8 | scibert            | Mat+Scibert (TPU)   | 0.8257    | 0.8532   | 0.8392   |
| 24576   | 0.2.8 | scibert            | Mat+Scibert (TPU)   | 0.8218    | 0.8518   | 0.8365   |
| 24304   | 0.3.0 | BERT_CRF           | scibert             | 0.8185    | 0.8417   | 0.8299   |
| 24307   | 0.3.0 | BERT_CRF_FEATURES  | scibert             | 0.8197    | 0.8468   | 0.8331   |
| 24307   | 0.3.0 | BERT_CRF           | matscibert          | 0.8145    | 0.8436   | 0.8288   |
| 24578   | 0.3.0 | BERT_CRF           | matscibert          | 0.8172    | 0.8450   | 0.8309   |
| 24575   | 0.3.0 | BERT_CRF           | Mat+Scibert (TPU)   | 0.8211    | 0.8479   | 0.8342   |
| 24615   | 0.3.0 | BERT_CRF_FEATURES  | Mat+Scibert (TPU)   | 0.8218    | 0.8482   | 0.8348   |


### Quantities NER

| Run nb. | DeLFT   | Architecture      | Transformer         | precision | recall | f1-score | 
|---------|---------|-------------------|---------------------|-----------|--------|----------|
| 24577   | 0.2.8   | scibert           | Mat+Scibert (TPU)   | 0.8866    | 0.8670 | 0.8767   |
| 24545   | 0.2.8   | scibert           | Mat+Scibert (TPU)   | 0.8857    | 0.8644 | 0.8749   |
| 24546   | 0.2.8   | scibert           | SciBERT             | 0.8893    | 0.8699 | 0.8795   |
| 24559   | 0.2.8   | scibert           | SciBERT             | 0.8873    | 0.8676 | 0.8773   |
| 24399   | 0.3.0   | BERT_CRF          | scibert             | 0.8469    | 0.9013 | 0.8733   |
| 24574   | 0.3.0   | BERT_CRF          | Mat+Scibert (TPU)   | 0.8578    | 0.9052 | 0.8809   |
| 24613   | 0.3.0   | BERT_CRF_FEATURES | scibert             | 0.8380    | 0.9039 | 0.8697   |
| 24614   | 0.3.0   | BERT_CRF_FEATURES | Mat+Scibert (TPU)   | 0.8470    | 0.9067 | 0.8758   |          

# Credits

Various people have helped with small feedback or more useful observations and ideas:

- Pedro Ortiz pedro.ortiz@inria.fr
- arij.riabi@inria.fr
- Romain roman.castagne@inria.fr
- Patrice Lopez patrice.lopez@science-miner.com

