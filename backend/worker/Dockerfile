FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY proto/pom.xml proto/pom.xml
COPY coordinator/pom.xml coordinator/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY proto/src/ proto/src/
COPY worker/src/ worker/src/

RUN mvn -pl worker -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/worker/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
