# QB BirthData Wikidata component

## Description

Receives a disambiguated entity and builds a SPARQL query over Wikidata about all the data related to birth of an entity

## Input specification

Comment: missing `<urn:qanary:input> a qa:AnnotationOfInstance .`

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> oa:hasBody <wd:Resource> ;
    qa:score "score"^^xsd:decimal .
<urn:qanary:input> oa:hasTarget [
    a    oa:SpecificResource;
    oa:hasSource <urn:qanary:myQuestionURI> ;
    oa:hasSelector  [
        a oa:TextPositionSelector ;
        oa:start "0"^^xsd:nonNegativeInteger ;
        oa:end  "5"^^xsd:nonNegativeInteger
    ]
] .
```

## Output specification

Comment: `qa:score` is `xsd:float` while in the ontology it is [decimal](https://github.com/WDAqua/QAOntology/blob/6d25ebc8970b93452b5bb970a8e2f526be9841a5/qanary.ttl#L31)

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfAnswerSPARQL ;
    oa:hasTarget <urn:qanary:myQuestionURI> ;
    oa:hasBody "sparql query" ;
    qa:score "0.5"^^xsd:float ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    oa:annotatedBy <urn:qanary:QB#applicationName > .
```