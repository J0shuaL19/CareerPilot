FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.24

RUN addgroup -S careerpilot && adduser -S -G careerpilot careerpilot
WORKDIR /app
COPY --from=build /workspace/target/backend-*.jar /app/careerpilot.jar
USER careerpilot

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/careerpilot.jar"]
