# QE Wikidata component

## Description

Receives a SPARQL query over Wikidata, executes it and writes result in JSON format.

## Input specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfAnswerSPARQL ;
   oa:hasTarget <urn:qanary:myQuestionUri> ;
   oa:hasBody "sparql query over dbpedia or wikidata" ;
   oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
   qa:score "0.5"^^xsd:float .
```

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<urn:qanary:output> a qa:AnnotationOfAnswerJson ;
    oa:hasTarget <urn:qanary:myQuestionUri> ;
    oa:hasBody ?answer ;
    oa:annotatedBy <urn:qanary:applicationName> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
    qa:score "0.5"^^xsd:float .
?answer a qa:AnswerJson ;
    rdf:value "jsonString"^^xsd:string  .
qa:AnswerJson rdfs:subClassOf qa:Answer .
```