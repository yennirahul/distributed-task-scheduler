
# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY proto/pom.xml proto/pom.xml
COPY coordinator/pom.xml coordinator/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY proto/src/ proto/src/
COPY coordinator/src/ coordinator/src/

RUN mvn -pl coordinator -am clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/coordinator/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
