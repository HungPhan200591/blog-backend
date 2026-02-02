# Multi-stage build for Spring Boot application with Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install git (required for JGit)
RUN apk add --no-cache git

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directory for Git content
RUN mkdir -p /app/git-content

# Expose port
EXPOSE 9999

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
