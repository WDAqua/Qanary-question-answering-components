# Docker Compose File writer

This small java application is writes a docker-compose.yml file containing all 
component modules listed in profile *all* in 
`Qanary-question-answering-components/pom.xml`.

The file `config.properties` allows for some configurations including 
image names and ports.

Keep in mind that the configuration ui uses REACT_APP environment variables 
to connect to the pipeline. Depending on the specified basePort those variables 
might have to be adjusted in `qanary-configuration-frontend/.env`.