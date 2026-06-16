# The jar is built on the host (mvn package) because the Qanary framework
# (qa.component 4.0.0) is a local build, not published to Maven Central — so it
# cannot be resolved from inside a clean build container.
FROM eclipse-temurin:21-jre

# Add the service itself (stable jar name set via <finalName> in pom.xml)
ARG JAR_FILE=target/qanary-component-copyvaluesofpriorgraph.jar
ADD ${JAR_FILE} /qanary-service.jar

ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/qanary-service.jar"]
