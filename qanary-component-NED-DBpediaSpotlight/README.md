# NED DBpedia Spotlight

## Description

Receives a textual question, forwards it to DBpedia Spotlight API and writes result in JSON format.

## Input specification

```ttl
# TODO: get textual representation of question
```

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
<urn:qanary:output> oa:hasBody <dbr:Resource> ;
    oa:annotatedBy <urn:qanary:myDBpediaSpotlightConfiguration:getEndpoint> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    qa:score "0.5"^^xsd:decimal .
```
