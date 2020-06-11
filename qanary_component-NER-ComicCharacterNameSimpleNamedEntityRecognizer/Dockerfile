FROM openjdk:11

# to prevent complications it is suggest to publish exactly the exposed port via
#    docker run -p ...:...
EXPOSE 5556

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /qanary-service.jar

ENTRYPOINT ["java", "-jar", "/qanary-service.jar"]
