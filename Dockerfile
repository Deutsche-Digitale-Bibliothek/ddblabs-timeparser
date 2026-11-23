FROM maven:3-amazoncorretto-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY src src

RUN mvn -q -DskipTests package \
	&& cp target/*-shaded.jar target/timeparser.jar

FROM amazoncorretto:25
WORKDIR /app

ENV TIMEPARSER_HOST=0.0.0.0
ENV TIMEPARSER_PORT=8080

RUN dnf install -y shadow-utils \
	&& dnf clean all \
	&& groupadd --system app \
	&& useradd --system --gid app --no-create-home --home-dir /app app

COPY --from=build /workspace/target/timeparser.jar /app/timeparser.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/timeparser.jar"]
