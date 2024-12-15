# Start with a base image containing Java runtime
FROM eclipse-temurin:21-jdk as builder

# Set the working directory in the container
WORKDIR /app

# Copy the build files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Grant execute permission for gradlew
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew bootJar --no-daemon

# Create the runtime container
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]