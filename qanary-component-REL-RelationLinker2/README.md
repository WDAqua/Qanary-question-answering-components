# REL Relation Linker 2

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
    oa:annotatedBy <urn:qanary:RelationLinker2> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```

## Build (Qanary 4.0.0)

Targets the current Qanary framework 4.0.0 (Spring Boot 3, Java 21). Install the
framework first (`mvn install` in the [Qanary](https://github.com/WDAqua/Qanary)
repository), then build clean with JDK 21:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-rel-relationlinker2.jar -t qanary/qanary-component-rel-relationlinker2 .
```
