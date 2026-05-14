# ── Build stage ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy entire project for multi-module build
COPY pom.xml .
COPY agentrail-api/pom.xml agentrail-api/pom.xml
COPY agentrail-service/pom.xml agentrail-service/pom.xml

# Cache dependencies
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Build api module
COPY agentrail-api/src agentrail-api/src
RUN mvn clean package -DskipTests -B -pl agentrail-api -am

# ── Runtime stage ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S agentrail && adduser -S agentrail -G agentrail

COPY --from=build /build/agentrail-api/target/agentrail-api-*.jar app.jar

ENV SERVER_PORT=8081
EXPOSE 8081

USER agentrail
ENTRYPOINT ["java", "-jar", "app.jar"]
