# RoBERTa pre-training and fine tuning experiments

## Introduction

This page contains notes and information about the process of [RoBERTa](https://github.com/facebook/fairseq) for improving the results in the task
of NER for materials science scientific papers.

The data used is summarised [here](../scibert/readme.md). 
The process followed is described [here](https://github.com/pytorch/fairseq/blob/main/examples/roberta/README.pretraining.md).

### Preprocessing
In order to process the data with RoBERTa, we preprocessed the data to have an empty line as a document separator. 
 - concatenate each document adding an empty line between each other
 - split in sentences (with [this python script](sentence-splitter.py))
 - split 80/10/10 for training/test/validation:
    - total number of lines: `grep -c "^$" SuperMat+SciCorpus.RoBERTa.sentences.v2.txt`: `795437`
    - `795437*0.8 = 636349` documents for training
    - `795437-636349 = 159088`
    - `159088/2 = 79544` documents for test and validation
    
    - split back ``nohup awk -v RS= '{
      f=("split-" NR ".txt") 
      print > f 
      close(f)
      }' ../SuperMat+SciCorpus.RoBERTa.sentences.v2.txt  &``
    
### Pre-training

## Details parameters

## Additional information

How to extract the title (on the first line) from each file (SuperMat):
``find batch-* -name "*.txt" ! -name "*.features.txt" -exec head -n1 {} \; > titles.txt``

Filter documents by title: 
``while read in; do grep -r "$in" tdm-corpora; done < titles.txt > exclusion.txt``

### Steps calculation madness

# Credits

Various people have helped with small feedback or more useful observations and ideas:

- Pedro Ortiz pedro.ortiz@inria.fr

