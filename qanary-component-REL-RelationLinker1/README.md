# REL Relation Linker 1

## Description

Receives a textual question forwards it to some local API, gets back recognized DBpedia relations and stores them.

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

Comment: no score in the output.

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfRelation .
<urn:qanary:output> oa:hasTarget [
    a   oa:SpecificResource;
        oa:hasSource    <urn:qanary:myQanaryQuestion> ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:output> oa:hasBody <dbr:Relation> ;
    oa:annotatedBy <urn:qanary:RelationLinker1> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
