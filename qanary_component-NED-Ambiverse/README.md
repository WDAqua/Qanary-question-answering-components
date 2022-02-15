# NED Ambiverse

## Description

Receives a textual question recognizes and disambiguates named entities and stores them.

Comment: Ambirverse API is no longer maintained. A new local endpoint should be established https://github.com/ambiverse-nlu/ambiverse-nlu

## Input specification

Not applicable as the textual question is a default parameter

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
    oa:annotatedBy <urn:qanary.NED#Ambiverse> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
