# Introduction

Feel free to adapt the content of this file to describe your service. Note that in doing so all sections apart from 
the current introduction should be applicable as-is to your scenario.

**Sample implementation:** This is a sample GITB-compliant messaging service that is used by the test bed to send and
receive a text message. When told to `send` a message this service simply logs it. Regarding received messages, these
are provided via HTTP GET call upon which time the appropriate active test sessions get notified via callback. 

The service is implemented in Java, using the [Spring Boot framework](https://spring.io/projects/spring-boot). It is 
built and packaged using [Apache Maven](https://maven.apache.org/).

# Prerequisites

The following prerequisites are required:
* To build: JDK 8+, Maven 3.2+.
* To run: JRE 8+.

# Building and running

1. Build using `mvn clean package`.
2. Once built you can run the application in two ways:  
  a. With maven: `mvn spring-boot:run`.  
  b. Standalone: `java -jar .\target${artifactId}-VERSION.jar`.
3. The service's WSDL file is accessible at http://localhost:8080/services/messaging?WSDL.

## Live reload for development

This project uses Spring Boot's live reloading capabilities. When running the application from your IDE or through
Maven, any change in classpath resources is automatically detected to restart the application.

## Packaging using Docker

Running this application as a [Docker](https://www.docker.com/) container is very simple as described in Spring Boot's
[Docker documentation](https://spring.io/guides/gs/spring-boot-docker/). The first step is to 
[Install Docker](https://docs.docker.com/install/) and ensure it is up and running. The next steps depend on whether
or not you are running Docker natively or through a virtual machine.

### Packaging with native Docker
 
In this case you can build the Docker image through Maven:
1. Build JAR file with `mvn package`.
2. Build the Docker image with `mvn dockerfile:build`.

### Packaging with Docker running via virtual machine

In this case you will need to build the image manually:
1. Create a temporary folder.
2. Copy in this folder the JAR file from the `target` folder.
3. Copy in this folder the `Dockerfile` file from the project root. 
4. Build from this folder using `docker build -t local/devc --build-arg JAR_FILE=devc-VERSION.jar .`. 

Note that the *local/devc* name for the image matches what is configured for the Maven build. You can adapt this
as needed in the commands or the `pom.xml` file.

### Running the Docker container

Assuming an image name of `local/devc`, it can be ran using `docker --name devc -p 8080:8080 -d local/devc`. 

The WSDL file can now be accessed at http://DOCKER_MACHINE:8080/services/messaging?WSDL. 