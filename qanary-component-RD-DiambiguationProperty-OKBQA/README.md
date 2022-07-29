<<<<<<< HEAD:qanary-component-RD-DiambiguationProperty-OKBQA/README.md
component source: https://www.okbqa.org/architecture/disambiguation
=======

# NER Diambiguation Property OKBQA

## Description

TBD

## Input specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:input> oa:hasTarget <urn:myQanaryQuestion> ;
    oa:hasBody "langCode"^^xsd:string ;
    a qa:AnnotationOfQuestionLanguage .
```

## Output specification

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:qanary:output> a qa:AnnotationOfRelation .
<urn:qanary:output> oa:hasTarget [
	a oa:SpecificClass ;
 	oa:hasSource <urn:myQanaryQuestion> ;
] .
<urn:qanary:output> oa:hasBody <urn:disambiguatedEntity> ;
    oa:annotatedBy <urn:qanary:DiambiguationClassOKBQA> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime  .
```

> > > > > > > master:qa.qanary_component-DiambiguationProperty-OKBQA/README.md
