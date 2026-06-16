# Monolitic Wrapper

## Description

A dummy component for monolithic systems that produce SPARQL queries. The queries are executed and stored as well as
their result.

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

Comment: no score in the output, `rdf:value` for the JSON annotation was not used

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output1> a 	qa:AnnotationOfAnswerSPARQL ;
	oa:hasTarget    <urn:qanary:myQanaryQuestion> ;
	oa:hasBody      "SPARQL query"^^xsd:string ;
	oa:annotatedBy  <urn:qanary:MonoliticWrapper> ;
	oa:annotatedAt  "2001-10-26T21:32:52"^^xsd:dateTime .

<urn:qanary:output2> a qa:AnnotationOfAnswerJson ;
    oa:hasTarget <urn:qanary:myQanaryQuestion> ;
    oa:hasBody "JSON result SPARQL query"^^xsd:string ;
    oa:annotatedBy <urn:qanary:MonoliticWrapper> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Build (Qanary 4.0.0)

Targets the current Qanary framework 4.0.0 (Spring Boot 3, Java 21). Install the
framework first (`mvn install` in the [Qanary](https://github.com/WDAqua/Qanary)
repository), then build clean with JDK 21:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qb-monoliticwrapper.jar -t qanary/qanary-component-qb-monoliticwrapper .
```
