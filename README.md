<img width="200" align=right alt="Qanary logo" src="https://raw.githubusercontent.com/WDAqua/Qanary/master/doc/logo-qanary_s.png">

# In a Nutshell: Qanary Question Answering Components

The [Qanary Framework](https://github.com/WDAqua/Qanary/) is dedicated to create Question Answering systems. Question Answering (QA) is a task requiring different fields leading to expensive / time consuming engineering tasks which might block research as it is too expensive. Typical problems/usecases that might occur while developing a question answering system are:

 * an algorithm is require analyzing textual questions and annotating the found entities, relations, classes, etc. 
    * it is time consuming as there are many services / algorithms / tools which need to compared 
 * your QA process needs to be improved
    * following traditional development approaches requires additional efforts for testing and debugging of code to uncover possible flaws
 * the quality of components dedicated to a particular task need to be analyzed
    * it is expensive to integrate all of the particular components due to a missing generalized interface

In this repository, the [components of the Qanary framework](https://github.com/WDAqua/Qanary-question-answering-components)  are stored. All components are implemented in Java and provide a Docker container for lightweight maintaince.

## Big Picture
 * Qanary is providing the methodology for a knowledge-driven, vocabular-based approach. Our long-term agenda is to create a knowledge-driven ecosystem for the field of Question Answering. It is part of the [WDAqua project](http://wdaqua.eu) where question answering systems are researched and developed.
 * [Qanary Framework](https://github.com/WDAqua/Qanary/) is providing the core framework for creating Question Answering systems following the Qanary methodology. You might consider the Qanary Framework as reference implementation of the Qanary framework as microserivce-based component architecture.
 * [Qanary components](https://github.com/WDAqua/Qanary-question-answering-components) is covering the QA components compatible with the Qanary framework.
 * [Frankenstein](https://github.com/WDAqua/Frankenstein) is a supporting framework to establish a toolset for rapid orchestration and benchmarking of Qanary components. For example, it provides the tools to create from 29 components 380 QA systems.


Regarding questions, ideas or any feedback related to Qanary please do not hesitate to [contact the core developers](https://github.com/WDAqua/Qanary/wiki/Who-do-I-talk-to%3F). However, if you like to see a QA system originally built using the Qanary framework, one of our core developers has build a complete end-to-end QA system which allows to query several RDF data stores: [http://wdaqua.eu/qa](http://wdaqua.eu/qa).

Please go to the [GitHub Wiki page](https://github.com/WDAqua/Qanary/wiki) of Qanary repository to get more insights on how to use this framework, how to add new component etc.


## Qanary Components

### Question Answering Name Entity Recognition (NER) and Disambiguation Components (NED) Components

#### Entity Classifier 2 (NER)
It uses rule base grammar to extract entities in a text.

 * [Qanary Entity Classifier 2 for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-EntityClassifier2)

#### Stanford NLP Tool (NER)
Stanford named entity recogniser is an open source tool that uses Gibbs sampling for information extraction to spot entities in a text.

 * [Qanary Stanford NLP Tool for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-stanford)

#### Babelfy
is a multilingual, graph-based approach that uses random walks and the densest subgraph algorithm to identify and disambiguate entities present in a text.

 * [Qanary Babelfy for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Babelfy)
 * [Qanary Babelfy for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Babelfy)


#### AGDISTIS (NED)
It is a graph based disambiguation tool that couples the HITS algorithm with label expansion strategies and string similarity measures to disambiguate entities in a given text.

 * [Qanary AGDISTIS for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-AGDISTIS)

#### DBpedia Spotlight
It is a web service that uses vector-space representation of entities and using the cosine similarity, recognise and disambiguate the entities.

 * [Qanary DBpedia Spotlight for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-DBpedia-Spotlight)
 * [Qanary DBpedia Spotlight for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-DBpedia-Spotlight)

#### Tag Me
It matches terms in a given text with Wikipedia, \ie links text to recognise named entities. 
 Furthermore, it uses the in-link graph and the page dataset to disambiguate recognised entities to its Wikipedia URls.

  * [Qanary Tag Me for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-tagme)
  * [Qanary Tag Me for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-tagme)

#### Other NER and NED Tools
 * TextRazor ([homepage](https://www.textrazor.com/)) is a startup providing software that helps developers rapidly build text analytics into their applications.
    * [Qanary TextRazor for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-TextRazor)
 * Dandelion ([homepage](https://dandelion.eu/)) is a startup specialized in Semantics & Big Data.
    * [Qanary Dandelion for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Dandelion)
    * [Qanary Dandelion for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Dandelion)
 * Ontotext ([homepage](https://ontotext.com/)) provides a complete set of Semantic Technology enabling better content management, knowledge discovery and semantic search.
    * [Qanary Ontotext for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Ontotext)
    * [Qanary Ontotext for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Ontotext)
 * Ambiverse ([homepage](https://www.ambiverse.com/)) is a spin-off from the Max Planck Institute for Informatics, develops technologies to automatically understand, analyze, and manage Big Text collections.
    * [Qanary Ambiverse for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Ambiverse)
    * [Qanary Ambiverse for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Ambiverse)
 * Meaningcloud ([homepage](https://www.meaningcloud.com/)) is a company based in New York City, specialized in software for semantic analysis.
    * [Qanary Meaningcloud for *NED*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-Meaningcloud)
    * [Qanary Meaningcloud for *NER*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NER-Meaningcloud)
 
 
 
### Question Answering Relation Linking (RL) Components

#### ReMatch
 * It maps natural language relations to knowledge graph properties by using dependency parsing characteristics with adjustment rules.It then carries out a match against knowledge base properties, enhanced with word lexicon Wordnet via a set of similarity measures. It is an open source tool.
 * [Qanary ReMatch for *RL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-REL-ReMatch)

#### RelationLinker2 (RelationMatch)
 * It devise semantic-index based representation of PATTY~\cite{DBLP:conf/emnlp/NakasholeWS12} (a knowledge corpus of linguistic patterns and its associated properties in DBpedia) and a search mechanism over this index with the purpose of enhancing relation linking task.
 * [Qanary RelationLinker2 for *RL*](https://github.com/WDAqua/Qanary-question-answering-components/blob/master/qanary_component-REL-RelationLinker2/)

#### OKBQA DiambiguationProperty (ReLMatch)
 * The disambiguation module (DM) of OKBQA framework provides disambiguation of entities, classes, and relations present in a natural language question.
 * [Qanary DiambiguationProperty for *RL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-DiambiguationProperty-OKBQA)

#### RelNliodRel (RNLIWOD)
 * Natural Language Interfaces for the Web of Data ((NLIWOD) community group (https://www.w3.org/community/nli/) provides reusable components for enhancing the performance of QA systems. We utilise one of its components to build similar relation linking.
 * [Qanary RelNliodRel for *RL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-REL-RELNLIOD)

#### Spot Property (AnnotationofSpotProperty)
 * This component is the combination of RNLIWOD and OKBQA disambiguation module for relation linking task. 
 * [Qanary AnnotationofSpotProperty for *RL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-AnnotationofSpotProperty-tgm)



### Question Answering Class Linking (CL) Components

#### ClsNliodCls (NLIWOD CLS)
 * NLIWOD Class Identifier is one among the several other tools provided by NLIWOD community for reuse. The code for class identifier is available on GitHub.
 * [Qanary ClsNliodCls for *CL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-CLS-CLSNLIOD)

#### AnnotationofSpotClass (OKBQA Class linker)
 * This component is part of OKBQA disambiguation module.
 * [Qanary AnnotationofSpotClass for *CL*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-AnnotationofSpotClass-tgm)

### Question Answering Query Builder (QB) Components

#### QueryBuilder (NLIWOD Template-based QB)
 * Template-based query builders are widely used in QA community for SPARQL query construction. This component is similar to the existing template-based components.
 * [Qanary QueryBuilder for *QB*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qa.qanary_component-QueryBuilder)

#### SINA (QB)
 * SINA is a keyword and natural language query search engine that is based on Hidden Markov Models for choosing the correct dataset to query. We decoupled original implementation to get query builder.
 * [Qanary SINA for *QB*](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-QB-Sina)




 
 
