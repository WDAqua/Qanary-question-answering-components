# NER Comic Character Name Simple Named Entity Recognizer

## Description

Receives a textual question recognizes and disambiguates named entities and stores them.

## Input specification

Not applicable as the textual question is a default parameter. The other input data is fectched from DBpedia external endpoint.

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
<urn:qanary:output> oa:annotatedBy <urn:qanary:component:ComicCharacterNameSimpleNamedEntityRecognizer> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
