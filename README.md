# Qanary Components Integrated in Frankenstein

We decoupled the Frankenstein architecture and presented reusable resources as an extension to the Qanary. Frankenstein is dedicated to extending
the Qanary ecosystem by the following contributions:
# R1
It contributes a large set of new components to the ecosystem of reusable components initiated by the Qanary. Consequently, researchers and practitioners are now enabled to create a large set of different QA systems outof-
the-box due to the composability features inherited from the Qanary. We calculated that only by using the components directly provided by Frankenstein/ Qanary 380 different ready-to-use QA pipelines can be created with small invest on time.

# Qanary
Qanary is a methodology for creating Question Answering Systems it is part of the [WDAqua project](http://wdaqua.eu) where question answering systems are researched and developed. For all the publications related to Qanary please see the section [publications](#qanarypublications). W.r.t. questions, ideas or any feedback related to Qanary please do not hesitate to [contact the core developers](https://github.com/WDAqua/Qanary/wiki/Who-do-I-talk-to%3F). However, if you like to see a QA system built using the Qanary framework, one of our core developers has build a complete end-to-end QA system which allows to query several RDF data stores: http://wdaqua.eu/qa.

Please go to the [GitHub Wiki page](https://github.com/WDAqua/Qanary/wiki) of this repository to get more insights on how to use this framework, how to add new component etc.

## Question Answering Name Entity Recognition and Disambiguation Components (NED) Components

### Entity Classifier 
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-EntityClassifier2)
It uses rule base grammar to extract entities in a text. 

### Stanford NLP Tool
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-stanford)
Stanford named entity recogniser is an open source tool that uses Gibbs sampling for information extraction to spot entities in a text.

### Babelfy
[source for NED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Babelfy)
[source for NER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Babelfy)
is a multilingual, graph-based approach that uses random walks and the densest subgraph algorithm to identify and disambiguate entities present in a text.


### AGDISTIS
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-AGDISTIS)
It is a graph based disambiguation tool that couples the HITS algorithm with label expansion strategies and string similarity measures to disambiguate entities in a given text.

### DBpedia Spotlight
[source for NED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-DBpedia-Spotlight)
[source for NER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-DBpedia-Spotlight)
 It is a web service that uses vector-space representation of entities and using the cosine similarity, recognise and disambiguate the entities.
 
 
 ### Tag Me
[source for NED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-tagme)
[source for NER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-tagme)
 It matches terms in a given text with Wikipedia, \ie links text to recognise named entities. 
 Furthermore, it uses the in-link graph and the page dataset to disambiguate recognised entities to its Wikipedia URls.
 
 ### Other NER and NED Tools 
 In Frankenstein, there are several other tools :
 
 TextRazor[TextRazor](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-TextRazor)
 
 Dandelion[DandelionNED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Dandelion)
 Dandelion[DandelionNER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Dandelion)
 
 Ontotext [OntotextNED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Ontotext)
  Ontotext [OntotextNER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Ontotext)
  
 Ambiverse [AmbiverseNED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Ambiverse)
  [AmbiverseNER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Ambiverse)
  
  Meaningcloud [MeaningcloudNED](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Meaningcloud)
  [MeaningcloudNER](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Meaningcloud)
 
 ## Question Answering Relation Linking (RL) Components

### ReMatch 
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-REL-ReMatch)
It maps natural language relations to knowledge graph properties by using dependency parsing characteristics with adjustment rules.It then carries out a match against knowledge base properties, enhanced with word lexicon Wordnet via a set of similarity measures. It is an open source tool.

### RelationMatch 
[source](https://github.com/WDAqua/Qanary-question-answering-components/blob/master/qanary_component-REL-RelationLinker2/src/main/java/eu/wdaqua/qanary/relationlinker2/RelationLinker2.java)
It devise semantic-index based representation of PATTY~\cite{DBLP:conf/emnlp/NakasholeWS12} (a knowledge corpus of linguistic patterns and its associated properties in DBpedia) and a search mechanism over this index with the purpose of enhancing relation linking task.

### ReMatch 
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-REL-ReMatch)
It maps natural language relations to knowledge graph properties by using dependency parsing characteristics with adjustment rules.It then carries out a match against knowledge base properties, enhanced with word lexicon Wordnet via a set of similarity measures. It is an open source tool.

### ReLMatch 
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-DiambiguationProperty-OKBQA)
The disambiguation module (DM) of OKBQA framework provides disambiguation of entities, classes, and relations present in a natural language question.

### RNLIWOD
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-REL-RELNLIOD)
Natural Language Interfaces for the Web of Data ((NLIWOD) community group (https://www.w3.org/community/nli/) provides reusable components for enhancing the performance of QA systems. We utilise one of its components to build similar relation linking.

### Spot Property
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-AnnotationofSpotProperty-tgm)
This component is the combination of RNLIWOD and OKBQA disambiguation module for relation linking task. 
We call this component Spot Property.


## Question Answering Class Linking (CL) Components

### NLIWOD CLS 
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-CLS-CLSNLIOD)
NLIWOD Class Identifier is one among the several other tools provided by NLIWOD community for reuse. The code for class identifier is available on GitHub.

### OKBQA Class linker
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-AnnotationofSpotClass-tgm)
This component is part of OKBQA disambiguation module.

## Question Answering Query Builder(QB) Components

### NLIWOD Template based QB
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-QueryBuilder)
Template based query builders are widely used in QA community for SPARQL query construction. This component is similar to the existing template based components.

### SINA
[source](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-QB-Sina)
SINA is a keyword and natural language query search engine that is based on Hidden Markov Models for choosing the correct dataset to query. We decoupled original implementation to get query builder.




 
 
