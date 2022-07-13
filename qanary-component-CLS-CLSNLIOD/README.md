# CLS CLSNLIOD

## Description

The component fetches the textual question and annotates a recognized and disambiguated class and save it to the
triplestore.

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output> a qa:AnnotationOfClass .
<urn:qanary:output> oa:hasTarget [
	a oa:SpecificResource ;
 	oa:hasSource <urn:myQanaryQuestion> ;
] .
<urn:qanary:output> oa:hasBody <dbr:disambiguatedClass> ;
    oa:annotatedBy <urn:qanary:AnnotationofSpotProperty> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime  .
```
