# Qanary component: QB-BirthDataWikidata

A Qanary **query builder** that creates **Wikidata** SPARQL queries to find the
**birth place and birth date** of named entities (persons) recognised in a text
question. It reads the entities annotated by upstream NED/NER components from the
Qanary triplestore and stores a ready-to-execute Wikidata query as an
`AnnotationOfAnswerSPARQL`.

It supports two inputs:

- a linked **Wikidata person resource** (from a prior NED component), and
- a **first name / last name** pair.

The corresponding query templates live in `src/main/resources/queries/`.

## Build

Targets the current **Qanary framework 4.0.0** (Spring Boot 3, Java 21). Install
the framework first (`mvn install` in the
[Qanary](https://github.com/WDAqua/Qanary) repository), then build clean:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
```

The runnable Spring Boot jar is written to
`target/qanary-component-qb-birthdata-wikidata.jar`.

## Run

```bash
java -jar target/qanary-component-qb-birthdata-wikidata.jar
```

Configuration is in `src/main/resources/config/application.properties`
(`server.port`, `spring.application.name`, `spring.boot.admin.url`). An OpenAPI /
Swagger UI is served via springdoc (`/swagger-ui.html`).

## Docker

```bash
mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qb-birthdata-wikidata.jar \
  -t qanary/qanary-component-qb-birthdata-wikidata .
```

## Tests

`mvn test` runs the full-context `QanaryServiceControllerTest` (boots the
component; the Spring Boot Admin client failing to reach a non-running pipeline is
non-fatal) and `QueryTest`, which validates the generated SPARQL against the
expected query templates.
