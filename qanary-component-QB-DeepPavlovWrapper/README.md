# Query Candidate Fetcher for a DeepPavlov API

see the [Qanary wiki](https://github.com/WDAqua/Qanary/wiki/) for a [tutorial](https://github.com/WDAqua/Qanary/wiki/How-do-I-implement-a-new-Qanary-component-using-Java%3F) on how to use this Apache Maven archetype for creating a Qanary component.

## Build (Qanary 4.0.0)

Targets the current Qanary framework 4.0.0 (Spring Boot 3, Java 21). Install the
framework first (`mvn install` in the [Qanary](https://github.com/WDAqua/Qanary)
repository), then build clean with JDK 21:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qb-deeppavlovwrapper.jar -t qanary/qanary-component-qb-deeppavlovwrapper .
```
