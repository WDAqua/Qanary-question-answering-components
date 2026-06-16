# Qanary component: QB-DateOfDeathDBpedia

A Qanary **query builder** that creates a **DBpedia** SPARQL query for the
**date of death** of a person recognised in the question. It reads the entity
annotated by an upstream NED component from the Qanary triplestore and stores a
ready-to-execute DBpedia query as an `AnnotationOfAnswerSPARQL`. The query
templates live in `src/main/resources/queries/`.

## Build

Targets the current **Qanary framework 4.0.0** (Spring Boot 3, Java 21). Install
the framework first (`mvn install` in the
[Qanary](https://github.com/WDAqua/Qanary) repository), then build clean:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
```

The runnable Spring Boot jar is written to
`target/qanary-component-qb-dateofdeathdbpedia.jar`.

## Run

```bash
java -jar target/qanary-component-qb-dateofdeathdbpedia.jar
```

Configuration is in `src/main/resources/config/application.properties`
(`server.port`, `spring.application.name`, `spring.boot.admin.url`). An OpenAPI /
Swagger UI is served via springdoc. The component uses an in-memory H2 database
(Spring Data JPA) for its internal bookkeeping.

## Docker

```bash
mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qb-dateofdeathdbpedia.jar \
  -t qanary/qanary-component-qb-dateofdeathdbpedia .
```

## Tests

`mvn test` runs `QanaryServiceQueryBuilderDateOfDeathDBpediaControllerTest`
(boots the component and exercises the query-building logic; some live-endpoint
cases are `@Disabled`). The Spring Boot Admin client failing to reach a
non-running pipeline is logged but non-fatal.
