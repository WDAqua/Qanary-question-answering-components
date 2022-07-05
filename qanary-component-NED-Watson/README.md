# Wrapper component for IBM Watson Natural Language Understanding API

## Description

* API documentation: http://cloud.ibm.com/apidocs/natural-language-understanding
* request own API key: http://cloud.ibm.com/catalog/services/natural-language-understanding

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

Comment: `qa:score` is in `oa:hasSelector` and is of type `xsd:float`.

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
            oa:end  "5"^^xsd:nonNegativeInteger ;
            qa:score "0.5"^^xsd:float .
        ]
    ] .
<urn:qanary:output> oa:hasBody <urn:dbr:Resource> ;
    oa:annotatedBy <urn:qanary:Watson> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Remarks

* this Qanary component contains a file-based caching mechanism
