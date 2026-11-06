# ---------------------------------------------------------------------------
# StockSense container image (used by Railway, works with any Docker host).
#
# Stage 1 builds the Spring Boot jar with Maven and runs the tests.
# Stage 2 runs it on a Java 21 JRE. The embedded Tomcat listens on $PORT
# (server.port=${PORT:8080} in application.properties).
#
# Database and market data settings come from environment variables
# (DB_URL, DB_USER, DB_PASSWORD, DB_INIT, MARKET_PROVIDER, ALPHA_VANTAGE_API_KEY),
# never from the image.
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/stock-recommendation.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
