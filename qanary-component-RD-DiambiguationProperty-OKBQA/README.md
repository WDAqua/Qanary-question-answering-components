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

## Build (Qanary 4.0.0)

Targets the current Qanary framework 4.0.0 (Spring Boot 3, Java 21). Install the
framework first (`mvn install` in the [Qanary](https://github.com/WDAqua/Qanary)
repository), then build clean with JDK 21:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-rd-diambiguationproperty-okbqa.jar -t qanary/qanary-component-rd-diambiguationproperty-okbqa .
```
