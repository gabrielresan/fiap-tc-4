FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY feedback-service/pom.xml ./
COPY feedback-service/src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/feedback-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
