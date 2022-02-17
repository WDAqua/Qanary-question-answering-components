# NER Tagme

## Description

Receives a textual question recognizes forwards it to the API of TagMe, gets back recognized entities and stores them.

Comment: This component does the same thing as the corresponding NED, but just saves not all the information.

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

Comment: no score in the output.

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
<urn:qanary:output> oa:annotatedBy <urn:qanary:Tagme> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
