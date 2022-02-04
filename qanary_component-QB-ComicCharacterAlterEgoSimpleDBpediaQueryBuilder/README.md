# QB ComicCharacterAlterEgoSimpleQueryBuilder component

## Description

Receives a disambiguated entity and builds a SPARQL query over Wikidata about all the data related to birth of an entity

## Input specification

Comment: non standard type `qa:AnnotationOfSpotInstance` is used

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfSpotInstance .
<urn:qanary:input> oa:hasTarget [
    a    oa:SpecificResource;
    oa:hasSource    <urn:qanary:myQanaryQuestion> ;
    oa:hasSelector  [
        a oa:TextPositionSelector ;
        oa:start "0"^^xsd:nonNegativeInteger ;
        oa:end  "5"^^xsd:nonNegativeInteger
    ]
] .
<urn:qanary:input> oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Output specification

Comment: complex `oa:hasTarget` is used

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfAnswerSPARQL ;
    oa:hasTarget [
		a    oa:SpecificResource ;
	    oa:hasSource    <urn:qanary:qanaryQuestion.getUri> ;
	] ;
    oa:hasBody "sparql query" ;
    qa:score "0.5"^^xsd:float ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    oa:annotatedBy <urn:qanary:applicationName > .
```