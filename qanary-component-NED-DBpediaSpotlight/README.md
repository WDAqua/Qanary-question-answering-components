# NED DBpedia Spotlight

## Description

Receives a textual question, forwards it to DBpedia Spotlight API and writes result in JSON format.

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

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
<urn:qanary:output> oa:hasBody <urn:dbr:Resource> ;
    oa:annotatedBy <https://api.dbpedia-spotlight.org/en/annotate> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    qa:score "0.5"^^xsd:decimal .
```
