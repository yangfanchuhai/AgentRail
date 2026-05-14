# ── Build stage ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy entire project for multi-module build
COPY pom.xml .
COPY agentrail-api/pom.xml agentrail-api/pom.xml
COPY agentrail-service/pom.xml agentrail-service/pom.xml

# Cache dependencies
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Build both modules (service depends on api being installed)
COPY agentrail-api/src agentrail-api/src
COPY agentrail-service/src agentrail-service/src
RUN mvn clean package -DskipTests -B -pl agentrail-service -am

# ── Runtime stage ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S agentrail && adduser -S agentrail -G agentrail

COPY --from=build /build/agentrail-service/target/agentrail-service-*.jar app.jar

ENV SERVER_PORT=8082
EXPOSE 8082

USER agentrail
ENTRYPOINT ["java", "-jar", "app.jar"]
