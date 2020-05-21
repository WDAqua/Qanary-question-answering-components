FROM openjdk:11

# to prevent complications it is suggest to publish exactly the exposed port via 
#    docker run -p ...:...
EXPOSE 5555

# you need to pass the address of the Qanary Pipeline component (e.g., the "real" IP address, not localhost) via 
#    docker run --env qanary.pipeline.url=... 
# to the Qanary component inside the Docker container (which will perform a call back on the provided URL)
ENTRYPOINT ["java", "-jar", "-Dspring.boot.admin.url=${qanary.pipeline.url}", "/usr/share/qanary-components/qanary-service.jar"]

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/qanary-components/qanary-service.jar
