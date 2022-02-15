# NED AGDISTIS

## Description

Receives a textual question and the spots from an NER component, disambiguates the spots and stores the corresponding named entities.

## Input specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfSpotInstance . 
<urn:qanary:input> oa:hasTarget [ 
    a    oa:SpecificResource ;
    oa:hasSource  <urn:qanary:myQuestion> ;
    oa:hasSelector  [
        a oa:TextPositionSelector ;
        oa:start "0"^^xsd:nonNegativeInteger ;
        oa:end  "5"^^xsd:nonNegativeInteger .
    ]
] ;
```

## Output specification

Comment: no score in the output

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfInstance .
<urn:qanary:output> oa:hasTarget [
    a   oa:SpecificResource;
        oa:hasSource    <urn:qanary:myQanaryQuestion> ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:output> oa:hasBody <dbr:Resource> ;
    oa:annotatedBy <http://agdistis.aksw.org> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
