# Docker file to create base image ot the project

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/dai-pw03-bader-santos-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
