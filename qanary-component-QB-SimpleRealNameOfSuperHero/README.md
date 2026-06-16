# Qanary component: QB-SimpleRealNameOfSuperHero

A Qanary **query builder** that, for questions of the form *"What is the real name
of &lt;superhero&gt;?"*, creates a **DBpedia** SPARQL query returning the real name
(the alter ego) of the superhero and stores it as an `AnnotationOfAnswerSPARQL`.
This is one of the two components of the minimal Qanary tutorial system.

## Build

Targets the current **Qanary framework 4.0.0** (Spring Boot 3, Java 21). Install
the framework first (`mvn install` in the
[Qanary](https://github.com/WDAqua/Qanary) repository), then build clean:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
```

The runnable Spring Boot jar is written to
`target/qanary-component-qb-simplerealnameofsuperhero.jar`.

## Run

```bash
java -jar target/qanary-component-qb-simplerealnameofsuperhero.jar
```

Configuration is in `src/main/resources/config/application.properties`
(`server.port`, `spring.application.name`, `spring.boot.admin.url`).

## Docker

```bash
mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qb-simplerealnameofsuperhero.jar \
  -t qanary/qanary-component-qb-simplerealnameofsuperhero .
```

## Tests

`mvn test` runs the full-context `QanaryServiceControllerTest` and the
`QueryBuilderTest` unit tests (question-support detection and the generated
INSERT query). The Spring Boot Admin client failing to reach a non-running
pipeline is logged but non-fatal.
