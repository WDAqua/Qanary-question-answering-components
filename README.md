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


## Build and run a *minimal* set of components

To show the Qanary methodology and it's functionality a tiny template-based Question Answering system was designed. It is capable of answering questions for a *real name* of a superhero like "What is the real name of Captain America?". For this purpose just two components were used:
 a) [Qanary DBpedia Spotlight component](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-DBpedia-Spotlight): The component is capable of finding superhero names and linking it to the [DBpedia knowledge base](https://wiki.dbpedia.org/) (such a process is called Named Entity Recognition and Disambiguation).
 b) [Qanary Query Builder for Superhero Names](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-QB-SimpleRealNameOfSuperHero): The component is capable of creating [SPARQL](https://www.w3.org/TR/sparql11-overview/) SELECT queries to be executed on DBpedia (such a component is typically called Query Builder) if the given question is following the template `What is the real name of <superheroname>`.

Hence, given a question following the described pattern the result will be SPARQL query that might be executed, s.t., the real name of a superhero is retrieved from DBpedia.


### Run a minimalistic Question Answering system

 1. [Install the Qanary core components](https://github.com/WDAqua/Qanary#how-to-run-the-code)
 2. Clone the current repository:
```
git clone https://github.com/WDAqua/Qanary-question-answering-components.git
```
 3. Switch to the folder `Qanary-question-answering-components`:
```
cd Qanary-question-answering-components
```
 4. Build the minimal set of components using the Maven profile "tinytutorial" (here we skip creating the corresponding Docker images by adding the parameter `-Ddockerfile.skip=true` to the Maven command):
```
mvn clean package -Ddockerfile.skip=true -P tinytutorial
```
    * The output should look like the following indicating that the component `qa.NED-DBpedia-Spotlight``and `qanary_component-QB-SimpleRealNameOfSuperHero` was created:
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] 
[INFO] qa.NED-DBpedia-Spotlight 2.1.0 ..................... SUCCESS [  3.717 s]
[INFO] qanary_component-QB-SimpleRealNameOfSuperHero 2.0.0  SUCCESS [  1.083 s]
[INFO] mvn.reactor 0.1.1-SNAPSHOT ......................... SUCCESS [  0.073 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

 5. Now, both components might be started using the JAR files:
```
java -jar qanary_component-NED-DBpedia-Spotlight/target/qa.NED-DBpedia-Spotlight-X.Y.Z.jar
java -jar qanary_component-QB-SimpleRealNameOfSuperHero/target/qanary_component-QB-SimpleRealNameOfSuperHero-X.Y.Z.jar
```

 6. [Build and start a Qanary pipeline](https://github.com/WDAqua/Qanary#without-creating-docker-images)
 
 7. While having installed the Qanary components and Qanary pipeline using the standard configuration you can access a trivial Question Answering frontend via http://localhost:8080/startquestionansweringwithtextquestion
    * Use the question "What is the real name of Captain America?". 
    * The question can be answered using the given two components. 
    * Thereafter, the triplestore will hold a SPARQL query that was created by the QueryBuilder component `SimpleRealNameOfSuperHero` (for [DBpedia](http://DBpedia.org/SPARQL)). It could be used to retrieve the actual answer from DBpedia. The UI shows the graph ID where the computed information was stored.
      * Retrieve the SPARQL query from your Qanary triplestore using:
```
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#> 

SELECT *
FROM <ADD-YOUR-GRAPH-ID-HERE>
WHERE {
    ?s a qa:AnnotationOfAnswerSPARQL.
    ?s oa:hasBody ?sparqlQueryOnDBpedia .
    ?s oa:annotatedBy ?annotatingService .
}
```



## Big Picture
 * Qanary is providing the methodology for a knowledge-driven, vocabular-based approach. Our long-term agenda is to create a knowledge-driven ecosystem for the field of Question Answering. It is part of the [WDAqua project](http://wdaqua.eu) where question answering systems are researched and developed.
 * [Qanary Framework](https://github.com/WDAqua/Qanary/) is providing the core framework for creating Question Answering systems following the Qanary methodology. You might consider the Qanary Framework as reference implementation of the Qanary framework as microserivce-based component architecture.
 * [Qanary components](https://github.com/WDAqua/Qanary-question-answering-components) is covering the QA components compatible with the Qanary framework.
 * [Frankenstein](https://github.com/WDAqua/Frankenstein) is a supporting framework to establish a toolset for rapid orchestration and benchmarking of Qanary components. For example, it provides the tools to create from 29 components 380 QA systems.


Regarding questions, ideas or any feedback related to Qanary please do not hesitate to [contact the core developers](https://github.com/WDAqua/Qanary/wiki/Who-do-I-talk-to%3F). However, if you like to see a QA system originally built using the Qanary framework, one of our core developers has build a complete end-to-end QA system which allows to query several RDF data stores: [http://wdaqua.eu/qa](http://wdaqua.eu/qa).

Please go to the [GitHub Wiki page](https://github.com/WDAqua/Qanary/wiki) of Qanary repository to get more insights on how to use this framework, how to add new component etc.


## How to Cite

### Introducing a Vocabulary for knowledge-driven Question Answering Processes

Kuldeep Singh, Andreas Both, Dennis Diefenbach, Saeedeh Shekarpour:
Towards a Message-Driven Vocabulary for Promoting the Interoperability of Question Answering Systems. ICSC 2016: 386-389
[DOI 10.1109/ICSC.2016.59](https://doi.org/10.1109/ICSC.2016.59)

### Introducing the Qanary Framework 

Andreas Both, Dennis Diefenbach, Kuldeep Singh, Saeedeh Shekarpour, Didier Cherix, Christoph Lange:
Qanary - A Methodology for Vocabulary-Driven Open Question Answering Systems. ESWC 2016: 625-641
[DOI 10.1007/978-3-319-34129-3_38](https://doi.org/10.1007/978-3-319-34129-3_38)

### Analytics of NER/NED Components

Dennis Diefenbach, Kuldeep Singh, Andreas Both, Didier Cherix, Christoph Lange, Sören Auer:
The Qanary Ecosystem: Getting New Insights by Composing Question Answering Pipelines. ICWE 2017: 171-189
[DOI 10.1007/978-3-319-60131-1_10](https://doi.org/10.1007/978-3-319-60131-1_10)

**For [further publications](https://github.com/WDAqua/Qanary/wiki/Publications-related-to-Qanary-Methodology-and-Framework) please see the following [wiki page](https://github.com/WDAqua/Qanary/wiki/Publications-related-to-Qanary-Methodology-and-Framework).**

***

## Qanary Components

The following components are contained in the   

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




 
 
