# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the application
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# wget is used by the compose healthcheck; present in alpine base
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
