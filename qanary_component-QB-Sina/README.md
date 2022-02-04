# QB SINA component

Receives 3 data structures: AnnotationOfClass, AnnotationOfRelation, and AnnotationOfInstance. Depending on the availability of them, generates a query and stores it.

## Input specification

Comment: `qa:AnnotationOfInstance` does not have `oa:annotatedAt`

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfClass .
<urn:qanary:input> oa:hasTarget [
    a oa:SpecificResource ;
    oa:hasSource ?q ;
];
    oa:hasBody ?uri ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfRelation .
<urn:qanary:input> oa:hasTarget [
    a oa:SpecificResource ;
    oa:hasSource ?q ;
];
    oa:hasBody ?uri ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> a qa:AnnotationOfInstance .
<urn:qanary:input> oa:hasTarget [
    a    oa:SpecificResource;
        oa:hasSource ?q ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:input> oa:hasBody <dbr:Resource> .
```

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfAnswerSPARQL ;
    oa:hasTarget <urn:qanary:myQanaryQuestion> ;
    oa:hasBody "sparql query" ;
    qa:score "0.5"^^xsd:float ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime ;
    oa:annotatedBy <urn:qanary:applicationName > .
```
