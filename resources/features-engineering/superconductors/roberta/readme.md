# RoBERTa pre-training and fine tuning experiments

* [Introduction](#introduction)
* [Data preparation](#data-preparation)
* [Processes](#processes)
* [Additional information](#additional-information)
* [Re-process using paragraphs](#re-process-using-paragraphs)
* [Credits](#credits)

## Introduction

This page contains notes and information about the process of [RoBERTa](https://github.com/facebook/fairseq) for improving the results in the task
of NER for materials science scientific papers.

The data used is summarised [here](../scibert/readme.md). 
The process followed is described [here](https://github.com/pytorch/fairseq/blob/main/examples/roberta/README.pretraining.md).

## Data preparation

### Split in text/train/valid
To split 80/10/10 for training/test/validation:
- total number of lines: `grep -c "^$" SuperMat+SciCorpus.RoBERTa.sentences.v2.txt`: `795437`    
- split back
    ```
    nohup awk -v RS= '{
    f=("split-" NR ".txt")
    print > f
    close(f)
    }' ../SuperMat+SciCorpus.RoBERTa.sentences.v2.txt  &
    ```
  
  alternative: 
   ```
  nohup csplit --digits=7 --prefix=split --suffix-format='%07d.txt' ../SciCorpus.RoBERTa.v3.txt '/^$/'+1 '{*}' > ../csplit.log &
  ```
  
 the split resulted in 795345 files
- cleanup splits from empty lines ``sed -i '/^[[:space:]]*$/d' ${in};``
- find empty files `find ./split/ -type f -empty -name '*.txt' > empty_files.txt`
- divide using file list (``find -name *.txt > file-list.txt``)
- `795345*0.8 = 636276` documents for training (`head -n 636276 split-file-list.txt > split-file-train.txt `)
- `795345-636276 = 159069` (`tail -n 159069 split-file-list.txt > split-file-list-test-validation.txt`)
- `159069/2 = 79534` documents for test (`head -n 79534 split-file-test-validation.txt > split-file-test.txt`)
- `159069-79534 = 79535` document for validation (`tail -n 79535 split-file-test-validation.txt > split-file-valid.txt`)

### Aggregate file for RoBERTa

To aggregated files with one empty line in between each file:
- easy one: ``sed -s -e $'$a\\\n' ./*.txt > concat.out`` it works with a limited amount of files
- alternative for large amount of files: ``find . -type f -name '*.txt' -exec sed -s -e $'$a\\\n' >> ../../SuperMat+SciCorpus.RoBERTa.sentences.v2.train.txt {} +``. [Ref](https://unix.stackexchange.com/questions/509749/how-to-deal-with-sed-if-argument-list-too-long).

### Cleanup
1. Remove tableXYZ or figureXYZ
   ``
   sed '/^\(figure\)\|\(table\)[0-9]\+$/d' SciCorpus.RoBERTa.txt > SciCorpus.RoBERTa.v2.txt
   ``
2. Remove non-utf8 characters

## Processes  
In this section we describe the processes

### File divided by sentences - no cleanup

- concatenate each document adding an empty line between each other (see [here](#aggregate-file-for-roberta))
- **Optional** split in sentences (with [this python script](sentence-splitter.py))
- split test/train/valid (see [here](#split-in-texttrainvalid))


### Pre-training
 - Run BPE:
   ```
    source activate roberta
    for SPLIT in train valid test; do \
    python -m examples.roberta.multiprocessing_bpe_encoder \
    --encoder-json /lustre/group/tdm/Luca/scibert/fairseq/gpt2_bpe/encoder.json \
    --vocab-bpe /lustre/group/tdm/Luca/scibert/fairseq/gpt2_bpe/vocab.bpe \
    --inputs  /lustre/group/tdm/Luca/aggregated/SuperMat+SciCorpus.RoBERTa.sentences.v2.${SPLIT}.txt \
    --outputs ./SuperMat+SciCorpus.sentences.v2.${SPLIT}.bpe \
    --keep-empty \
    --workers 72; \
    done
   ```
   
 - Run preprocessing:
    ```
     source activate roberta
     for SPLIT in train valid test; do \
     python -m examples.roberta.multiprocessing_bpe_encoder \
     --encoder-json /lustre/group/tdm/Luca/scibert/fairseq/gpt2_bpe/encoder.json \
     --vocab-bpe /lustre/group/tdm/Luca/scibert/fairseq/gpt2_bpe/vocab.bpe \
     --inputs  /lustre/group/tdm/Luca/aggregated/SuperMat+SciCorpus.RoBERTa.sentences.v2.${SPLIT}.txt \
     --outputs ./SuperMat+SciCorpus.sentences.v2.${SPLIT}.bpe \
     --keep-empty \
     --workers 72; \
     done
    ```
 - Run pre-training
  TBA

## Details parameters

## Additional information

How to extract the title (on the first line) from each file (SuperMat):
``find batch-* -name "*.txt" ! -name "*.features.txt" -exec head -n1 {} \; > titles.txt``

Filter documents by title: 
``while read in; do grep -r "$in" tdm-corpora; done < titles.txt > exclusion.txt``

### Steps calculation madness


## Re-process using paragraphs 

Reason: Pedro has a script that can split paragraphs when they are too long 


## Credits

Various people have helped with small feedback or more useful observations and ideas:

- Pedro Ortiz pedro.ortiz@inria.fr

