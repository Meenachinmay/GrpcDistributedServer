# Use OpenJDK 17 for running the application
FROM openjdk:21-slim

# Set the working directory
WORKDIR /app

COPY /jars/*.jar ./app.jar

# Expose the port the app runs on
EXPOSE 8080 9090

# Run the jar file
ENTRYPOINT ["java","-jar","./app.jar"]