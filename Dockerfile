# Build stage: Maven on Eclipse Temurin 21 (LTS). The dependency step is cached
# separately from the sources so a code change does not re-download the world.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml /build/
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline || true

COPY src /build/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

# Runtime stage: a JRE-only Temurin 21 image (replaces the 2022 early-access JDK 18)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/target/nanobot-backend-1.0.0.jar /app/nanobot-backend-1.0.0.jar

# Let the JVM size its heap from the container memory limit (compose: mem_limit 4g)
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/nanobot-backend-1.0.0.jar"]
