FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY critical-notifications/pom.xml ./
COPY critical-notifications/src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/critical-notifications-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
