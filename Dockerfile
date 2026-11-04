# ---------------------------------------------------------------------------
# StockSense container image (used by Railway, works with any Docker host).
#
# Stage 1 builds the WAR with Maven and runs the unit tests.
# Stage 2 runs it on Apache Tomcat 10.1 with Java 21 as the ROOT application,
# so the site is served at "/" instead of "/stock-recommendation/".
#
# Database and market data settings come from environment variables
# (DB_URL, DB_USER, DB_PASSWORD, MARKET_PROVIDER, ALPHA_VANTAGE_API_KEY),
# never from the image.
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package

FROM tomcat:10.1-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/stock-recommendation.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/start.sh /usr/local/bin/start.sh
RUN chmod +x /usr/local/bin/start.sh
EXPOSE 8080
CMD ["start.sh"]
