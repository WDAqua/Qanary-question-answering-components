# Qanary component: QC-AnswerTypeClassifier

## Build (Qanary 4.0.0)

Targets the current Qanary framework 4.0.0 (Spring Boot 3, Java 21). Install the
framework first (`mvn install` in the [Qanary](https://github.com/WDAqua/Qanary)
repository), then build clean with JDK 21:

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
docker build --build-arg JAR_FILE=target/qanary-component-qc-answertypeclassifier.jar -t qanary/qanary-component-qc-answertypeclassifier .
```
