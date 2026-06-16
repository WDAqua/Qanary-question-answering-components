# Qanary component: CopyValuesOfPriorGraph

A Qanary component that **copies the annotations of a prior process graph into the
current graph**. It looks up an annotated prior conversation/process in the
current `inGraph` and copies its triples over, so later components can build on
results produced in an earlier Qanary run (e.g. for multi-turn / cross-component
processing).

## What it does

1. reads the required annotations from the Qanary triplestore (`/queries/fetchRequiredAnnotations.rq`);
2. for each referenced prior graph, copies its data into the current graph
   (`/queries/addDataToGraph.rq`, executed via the pipeline's `/sparql` endpoint);
3. logs how many prior graphs were found and copied.

It also exposes a `POST /copyvaluestograph?sourceGraph=…&targetGraph=…` endpoint
to copy values between two graphs directly.

## Build

This component targets the current **Qanary framework 4.0.0** (Spring Boot 3,
Java 21). The framework artifacts (`qa.component`, `qa.commons`) are a local
build, so install the framework first (`mvn install` in the
[Qanary](https://github.com/WDAqua/Qanary) repository), then:

```bash
JAVA_HOME=/path/to/jdk21 mvn package
```

The runnable Spring Boot jar is written to
`target/qanary-component-copyvaluesofpriorgraph.jar`.

## Run

```bash
java -jar target/qanary-component-copyvaluesofpriorgraph.jar
```

Configuration is in `src/main/resources/config/application.properties`
(`server.port`, `spring.application.name`, `spring.boot.admin.url` — the URL of
the Qanary pipeline the component registers with). See
`src/main/resources/README.md` for the component's resource files (queries,
banner, RDF component description).

## Docker

```bash
mvn package
docker build --build-arg JAR_FILE=target/qanary-component-copyvaluesofpriorgraph.jar \
  -t qanary/qanary-component-copyvaluesofpriorgraph .
```

## Tests

`mvn test` runs:

- `QanaryServiceControllerTest` — boots the full Spring context and checks the
  Qanary service description endpoint is served (the Spring Boot Admin client
  failing to reach a non-running pipeline is logged but non-fatal);
- `CopyValuesOfPriorGraphTest` — fast unit tests that verify the Qanary message
  round-trips and that the required SPARQL query templates are present and
  non-empty.

> Build clean (`mvn clean ...`): an *as-is* (Spring Boot 2) build of this module
> uses the old parent's AspectJ plugin, which weaves framework classes into
> `target/classes`; those stale classes must not linger into a 4.0.0 build.
