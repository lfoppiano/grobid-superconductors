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
    - split back ``nohup awk -v RS= '{
      f=("split-" NR ".txt") 
      print > f 
      close(f)
      }' ../SuperMat+SciCorpus.RoBERTa.sentences.v2.txt  &``
      the split resulted in 795345 files
    - cleanup splits from empty lines ``sed -i '/^[[:space:]]*$/d' ${in};``
    - find empty files `find ./split/ -type f -empty -name '*.txt' > empty_files.txt`
    - divide using file list (``find -name *.txt > file-list.txt``)
      - `795345*0.8 = 636276` documents for training (`head -n 636276 split-file-list.txt > split-file-train.txt `)
      - `795345-636276 = 159069` (`tail -n 159069 split-file-list.txt > split-file-list-test-validation.txt`)
      - `159069/2 = 79534` documents for test (`head -n 79534 split-file-test-validation.txt > split-file-test.txt`)
      - `159069-79534 = 79535` document for validation (`tail -n 79535 split-file-test-validation.txt > split-file-valid.txt`)
    - aggregated back with one empty line in between each file (``sed -s -e $'$a\\\n' ./*.txt > concat.out`` it works but requires to be launched in the same directory or it will fail with the error )
    
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

