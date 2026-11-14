FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S club && adduser -S club -G club && mkdir -p /data/uploads && chown -R club:club /data
WORKDIR /app
COPY --from=build /build/target/club-site-1.0.0.jar app.jar
USER club
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
