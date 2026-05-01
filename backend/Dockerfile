FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io docker-compose git tar \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system pep && adduser --system --ingroup pep pep
COPY --from=build /workspace/target/*.jar /app/pep-backend.jar
USER pep
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/pep-backend.jar"]
