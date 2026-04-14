FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TIMEPARSER_HOST=0.0.0.0
ENV TIMEPARSER_PORT=8080

RUN groupadd --system app && useradd --system --gid app --create-home --home-dir /app app

COPY --from=build /workspace/target/timeparser-2.0.0-SNAPSHOT.jar /app/timeparser.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/timeparser.jar"]