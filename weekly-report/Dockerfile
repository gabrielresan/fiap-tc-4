FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY weekly-report/pom.xml ./
COPY weekly-report/src ./src
RUN mvn -B -ntp package -DskipTests

FROM public.ecr.aws/lambda/java:21
COPY --from=build /workspace/target/weekly-report-lambda.jar /var/task/lib/

CMD ["br.com.fiap.tc4.reporting.WeeklyReportHandler::handleRequest"]
