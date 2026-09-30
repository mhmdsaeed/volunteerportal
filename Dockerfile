# syntax=docker/dockerfile:1
# Builds the Volunteer Portal server image (runs on x86-64 and ARM, e.g. an Oracle Cloud Ampere VM).
# Used by deploy/docker-compose.yml - see deploy/README.md. Tests need a database, so they aren't run
# here: run `./mvnw verify` before deploying.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
# Dependencies first, so they're cached between builds when only the code changes
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && cp target/volunteerportal-*.jar /src/app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home /app app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app
EXPOSE 8080
# Use up to 75% of the container's memory for the Java heap
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
