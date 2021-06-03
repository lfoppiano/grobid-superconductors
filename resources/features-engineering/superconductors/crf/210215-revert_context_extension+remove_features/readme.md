removed features: fonts, italic, bold
reverted extended context 


template diff: 
```
--- a/superconductors/crfpp-templates/superconductors.template
+++ b/superconductors/crfpp-templates/superconductors.template
@@ -1,9 +1,4 @@
# Unigram
-U001:%x[-9,0]
-U002:%x[-8,0]
-U003:%x[-7,0]
-U004:%x[-6,0]
-U005:%x[-5,0]
U00:%x[-4,0]
U01:%x[-3,0]
U02:%x[-2,0]
@@ -13,24 +8,9 @@ U05:%x[1,0]
U06:%x[2,0]
U07:%x[3,0]
U08:%x[4,0]
-U081:%x[5,0]
-U082:%x[6,0]
-U083:%x[7,0]
-U084:%x[8,0]
-U085:%x[9,0]
-U091:%x[-5,0]/%x[-4,0]
-U092:%x[-4,0]/%x[-3,0]
-U093:%x[-3,0]/%x[-2,0]
-U094:%x[-2,0]/%x[-1,0]
U09:%x[-1,0]/%x[0,0]
U0A:%x[0,0]/%x[1,0]
U0B:%x[1,0]/%x[2,0]
-U0C1:%x[-2,0]/%x[-1,0]
-U0C2:%x[-3,0]/%x[-2,0]
-U0C3:%x[-4,0]/%x[-3,0]
-U0C4:%x[-5,0]/%x[-4,0]
-U0D:%x[-2,0]/%x[-1,0]/%x[0,0]
-U0E:%x[0,0]/%x[1,0]/%x[2,0]

# Lowercase token
U10:%x[-2,1]
@@ -98,24 +78,12 @@ UE1:%x[0,16]
UE2:%x[1,16]

# font status
-UB0:%x[-1,17]
-UB1:%x[0,17]
-UB2:%x[1,17]

# font size
-UB3:%x[-1,18]
-UB4:%x[0,18]
-UB5:%x[1,18]

# bold
-UF0:%x[-1,19]
-UF1:%x[0,19]
-UF2:%x[1,19]

# italic
-UF3:%x[-1,20]
-UF4:%x[0,20]
-UF5:%x[1,20]

# font style (superscript/subscript)
U70:%x[-2,21]
@@ -127,8 +95,6 @@ U75:%x[-2,21]/%x[-1,21]
U76:%x[-1,21]/%x[0,21]
U77:%x[0,21]/%x[1,21]
U78:%x[1,21]/%x[2,21]
-U79:%x[-2,21]/%x[-1,21]/%x[0,21]
-U7A:%x[0,21]/%x[1,21]/%x[2,21]


# chemspot
@@ -143,8 +109,6 @@ UFD:%x[3,22]
UFE:%x[4,22]
UFF:%x[0,22]/%x[1,22]
UFF1:%x[-1,22]/%x[0,22]
-UFF2:%x[-2,22]/%x[-1,22]/%x[0,22]
-UFF3:%x[0,22]/%x[1,22]/%x[2,22]

# Output
B
```
