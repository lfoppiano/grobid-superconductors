Annotation Guidelines
#####################

Getting started
===============

The first step of the annotation process is to generate pre-annotated data from unlabeled documents based on the current models.
The annotation work then consists of manually checking the produced annotations and adding the missing one.
It is very important not to modify the text content in these generated files, not adding spaces or other characters, but only adding or moving XML tags.

When the training data has been manually corrected, move the file under the repository ``resouces/dataset/{model}/corpus/`` for made them available for training.
The evaluation corpus is partitioned automatically as 20% of the total corpus.


Annotations
===========

In this page we are describing the guidelines for annotating superconductor papers. This is a living document that is updated directly as we go along.


superconductor material
-----------------------

Identify a superconductor material in the chemical form (e.g. LaFe03NaCl2), with substitutions La(1-x)Fe(x), or with natural name HDoped Ba111 serie.
It uses the tag ``<supercon>``

Example:
::


Value substitution
------------------

Identify substitution of values, useful to identify values of x/y which are appearing as substituion in tables or material names.
It's identified by the tag ``<substitution>``

For example:
::
  For x=0.44, Curie-Weiss-like behavior, which implies AF [...]


should be annotated:
::
  For <substitution>x=0.44</substitution>, Curie-Weiss-like behavior, which implies AF [...]


Critical temperature expressions
--------------------------------
represent the critical temperature and any expression of it (e.g. high-critical-temperature, etc.. )
Some baskc rules:
   * Adjectives applied to critical temperature should not be annotated (e.g. ``higher Tc``, should be annotated as ``higher <tc>Tc</tc>``)
   * A sentence like ``the critical temperature (Tc)`` should be annotated with multples tokens like: ``the <tc>critical temperature</tc> (<tc>Tc</tc>)``

It uses the tag ``<tc>``

Value of properties
-------------------
Identify the value of a property of a superconductor material
example:
::
  maximum Tc that exceeds <propertyValue>45K</propertyValue> at a pressure of 3.0 GPa.

It uses the tag ``<propertyValue>``

Special cases and questions:
****************************

* type II Diract semimetal should be annotated? To be asked Takano-san teams
* "high Tc cuprate" should not be annotated because is just a general term


For example
::
  <p>The electron-doped <tc>high-transition-temperature</tc> (<tc>Tc</tc>) iron-based pnictide superconductor <supercon>LaFeAsO1−xHx</supercon> has a unique phase diagram: Superconducting (SC) double domes are sandwiched by antiferromagnetic phases at ambient pressure and they turn into a single dome with a maximum Tc that exceeds <propertyValue>45K</propertyValue> at a pressure of 3.0 GPa. We studied whether spin fluctuations are involved in increasing <tc>Tc</tc> under a pressure of 3.0 GPa by using the 75 As nuclear magnetic resonance (NMR) technique. The 75 As-NMR results for the powder samples show that <tc>Tc</tc> increases up to <propertyValue>48 K</propertyValue> without the influence of spin fluctuations. This fact indicates that spin fluctuations are not involved in raising <tc>Tc</tc>, which implies that other factors, such as orbital degrees of freedom, may be important for achieving a high <tc>Tc</tc> of almost <propertyValue>50 K</propertyValue>.</p>
  <p>The phase diagram of the electron-doped <tc>hightransition-temperature</tc> (<tc>T c</tc> ) iron-based pnictide <supercon>LaFeAsO 1−x H x</supercon> (<supercon>H-doped La1111 series<supercon>) is unique owing to the capability of electron doping: (i) It exhibits a superconducting (SC) phase with double domes covering a wide H-doping range from <substitution>x = 0.05</substitution> to <substitution>x = 0.44</substitution> 1 , (ii) the SC phase is sandwiched by antiferromagnetic (AF) phases appearing in heavily and poorly electron-doped regimes [see Fig. 1(a)] 2 , and (iii) the application of pressure transforms the double domes into a single dome 1, 3 . Intriguingly, upon applying pressure, the minimum <tc>T c</tc> at ambient pressure becomes the maximum <tc>T c</tc> of over <propertyValue>45 K</propertyValue> 1 , as shown by the solid arrow in Figs. 1(a) and 4 as described in detail below.</p>



