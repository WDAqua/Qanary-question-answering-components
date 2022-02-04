# Query Builder

## Description

Receives 3 optional data structures: AnnotationOfClass, AnnotationOfRelation, and AnnotationOfInstance. Depending on the availability of them, generates a query over DBpedia and stores it.

## Input specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfClass .
<urn:qanary:input> oa:hasTarget [
    a oa:SpecificResource ;
    oa:hasSource ?q ;
];
    oa:hasBody ?uri .
```

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfRelation .
<urn:qanary:input> oa:hasTarget [
    a oa:SpecificResource ;
    oa:hasSource ?q ;
];
    oa:hasBody ?uri .
```

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfInstance .
<urn:qanary:input> oa:hasTarget [
    a    oa:SpecificResource;
        oa:hasSource ?q ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:input> oa:hasBody <dbr:Resource> .
```

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfAnswerSPARQL ;
    oa:hasTarget <answerID> ;
    oa:hasBody "sparql query" ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    oa:annotatedBy <urn:qanary:QB#applicationName > .
```

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<urn:qanary:output> a qa:AnnotationOfAnswerJson ;
    oa:hasTarget <answerID> ;
    oa:hasBody "jsonString"^^xsd:string ;
    oa:annotatedBy <urn:qanary:QB#applicationName > ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .

# in SparqlExecuter ?answer a qa:AnswerJson is used
```