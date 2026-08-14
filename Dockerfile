# Define the build stage
FROM maven:3.8.7-openjdk-18 AS build

# Set the working directory in the Maven image
WORKDIR /build

# Copy the source code to the image
COPY src /build/src

# Copy the Maven settings
COPY pom.xml /build

# Build the application
RUN mvn clean install -DskipTests

# Define the final image
FROM openjdk:18-ea-jdk-oracle

# Set the working directory in the image
WORKDIR /app

# Copy the built jar file from the build stage to the final image
COPY --from=build /build/target/nanobot-backend-1.0.0.jar /app/nanobot-backend-1.0.0.jar

# Specify the default command to run the app
CMD ["java", "-jar", "/app/nanobot-backend-1.0.0.jar"]