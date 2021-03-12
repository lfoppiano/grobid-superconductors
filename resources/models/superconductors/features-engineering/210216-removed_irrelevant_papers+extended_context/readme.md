applied extended windows = 50
added more contextual features e.g.
````
# Unigram
U001:%x[-9,0]
U002:%x[-8,0]
U003:%x[-7,0]
U004:%x[-6,0]
U005:%x[-5,0]
U00:%x[-4,0]
U01:%x[-3,0]
U02:%x[-2,0]
U03:%x[-1,0]
U04:%x[0,0]
U05:%x[1,0]
U06:%x[2,0]
U07:%x[3,0]
U08:%x[4,0]
U081:%x[5,0]
U082:%x[6,0]
U083:%x[7,0]
U084:%x[8,0]
U085:%x[9,0]
U091:%x[-5,0]/%x[-4,0]
U092:%x[-4,0]/%x[-3,0]
U093:%x[-3,0]/%x[-2,0]
U094:%x[-2,0]/%x[-1,0]
U09:%x[-1,0]/%x[0,0]
U0A:%x[0,0]/%x[1,0]
U0B:%x[1,0]/%x[2,0]
U0C1:%x[-2,0]/%x[-1,0]
U0C2:%x[-3,0]/%x[-2,0]
U0C3:%x[-4,0]/%x[-3,0]
U0C4:%x[-5,0]/%x[-4,0]
U0D:%x[-2,0]/%x[-1,0]/%x[0,0]
U0E:%x[0,0]/%x[1,0]/%x[2,0]
```

removed papers where the rate between entities and paragraphs (entities/paragraphs) is less than 3
