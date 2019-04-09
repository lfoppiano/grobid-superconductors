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

In this page we are describing the guidelines for annotating superconductor papers.
The Goal of this annotation exercise is to produce models allowing extraction of superconductor materials, critical temperatures and their value.
This is a living document that is updated directly as we go along with the project.

The component to be annotated are:
 - superconductor material
 - value substitution
 - property value
 - critical temperature expressions
 - other properties

They are detailed in the next sections

General principles
------------------
Here a list of general principles that should be followed by annotating:

- At this stage of the process, the annotator should ignore the deep meaning of the context, for example if a *normal* word is used as reference for a superconductor it should not be annotated, for example
    ::

        The material XYZ has tc 34K. Such wafer, show interesting characteristics at 22K.

  where ``wafer`` should not be annotated, even though it refers to ``XYZ``.

- In case of doubt is better to 1) not annotate or 2) comment out the sentence, specifying the reason (use standard XML comment ``<!-- -->``)

- In general, annotators should bear in mind that an additional cascade model can be applied to the result of the extraction for that specific annotation. For example the `quantities recognition <http://github.com/kermitt2/grobid-quantities>`_ can be applied to the extracted results of ``property values`` or ``substitution``.


superconductor material
-----------------------

Identify a superconductor material. This annotation are identified by the tag ``<supercon>``.
The material can be expressed as:
 - the final chemical form (e.g. LaFe03NaCl2),
 - in chemical form with substitutions La(1-x)Fe(x),
 - with natural name ``HDoped Ba111 serie``, ``iron-based pnictide``
 - with abbreviations like ``(TMTSF) 2 PF 6``
 - with series definition ``ba1111 serie`` or ``11 series FeSe``


There are few more annotation information that should be followed for materials:
 - Material type and name like ``type II Diract semimetal`` should be annotated as one single entity,
 - The description of a material, like ``electron-doped high-transition-temperature (Tc) iron-based pnictide`` should be annotated as one single annotation
    for example ``The <supercon>electron-doped high-transition-temperature (Tc) iron-based pnictide</supercon> superconductor <supercon>LaFeAsO1−xHx</supercon> has a unique phase diagram:``
 - Material that are not superconductor should not be annotated. Example, NiCrAl-CuBe is just a piston-cylinder-type cell, used for the experiment:
    ::

        The pressure was applied by using NiCrAl-CuBe hybrid piston-cylinder-type cells.


 - Materials that are declared as non-superconductors should not be annotated, example:
    ::

        The material XYZ show no superconductor properties.


Value substitution
------------------

Identify substitution of values; used to identify numeric values whose variables (``x``, ``y`` or other letters) are appearing as substitution. The goal is to be able to use the values to complete material names ore table lookups.
This information should be identified by the tag ``<substitution>``.

For example:
::
  For x=0.44, Curie-Weiss-like behavior, which implies AF [...]


should be annotated:
::
  For <substitution>x=0.44</substitution>, Curie-Weiss-like behavior, which implies AF [...]

For multiple values, the whole string should be annotated (see `issue #1 <https://github.com/lfoppiano/grobid-superconductors-data/issues/1>`_), for example:

- ``<substitution>x = 0.5, 0.3</substitution>``

- ``<substitution>0.5 < x < 0.9</substitution>``

- ``<substitution>x varying from 0.5 to 0.9</substitution>`` (In this case `varying` is not important, but because x is there, annotators should try to catch the variable name)

- ``<substitution>x =0.40 and 0.44</substitution>``


Critical temperature expressions
--------------------------------
Represent the critical temperature and any expression of it (e.g. high-critical-temperature, etc.. ). Expressed using the ``<tc>`` tag.

Sometimes ``tc`` is used to identify ``Curie Temperature``, which still refer to a temperature but with a different meaning.
Papers authors usually provide this information which can be used to avoid recognising critical temperature incorrectly.

Some basic rules:
   * Adjectives applied to critical temperature should be annotated, for example: ``high Tc cuprate``, ``maximum Tc`` or ``higher Tc`` having the adjective describing the temperature included in the annotation,
   * A sentence like ``the critical temperature (Tc)`` should be annotated with multiples tokens like: ``the <tc>critical temperature</tc> (<tc>Tc</tc>)``,
   * implicit description of critical temperature, like ``superconducts``, ``shows superconductor properties`` should be annotated as well
   * When the critical temperature is not directly referred as a property but an entity related to other materials should not be annotated, here an example:
     ::
        The conventional nature of the temperature dependence is also found in case [..]

Value of properties
-------------------
Identify the value of a property of a superconductor material using the tag ``<propertyValue>``.
example:
::
  maximum Tc that exceeds <propertyValue>45K</propertyValue> at a pressure of 3.0 GPa.

For this properties the general principles are:
 - discrete or relatives values for example ``remains unchanged``, ``is increating`` are ignored
 - critical pressure and any other property that is not a temperature are ignored

For multiple values, the whole string should be annotated as for `substitutions`:

- ``<propertyValue>from 1 K to 2K</propertyValue>``

- ``<propertyValue>less than 2K</propertyValue>``

- ``<propertyValue>0.40 and 0.44 K</propertyValue>``


Special cases and questions
---------------------------

N/A

Future work and improvements
============================

Results and additional information
----------------------------------

All information that are not numeric, thus important because referring to special properties or results of mentioned materials, should be excluded for the time being. They can be annotated anyway as ``<propertyOther>``, for example:
::

    and the <tc>maximum T c</tc> <propertyOther>occurs close to the phase boundary</propertyOther>


Material shape
--------------

Sometimes material mentioned in previous sentences, are referred by adjective such a shapes
::

    The 75 As-NMR results for the powder samples show that

They therefore can be annotated using the ``<shape>`` tag:
::

    The 75 As-NMR results for the <shape>powder samples</shape> show that


Critical Pressure
-----------------
Critical pressure would be also a ``<propertyValue>`` but for the time being should not be annotated.