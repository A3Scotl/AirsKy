# ===== STAGE 1: Build the application =====
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven project files and build
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 2: Run the application =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar file from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
