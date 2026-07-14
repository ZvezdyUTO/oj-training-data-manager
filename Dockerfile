FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY backend ./backend
RUN mvn -q -pl backend/training-api -am package -DskipTests \
    && cp backend/training-api/target/training-api-0.1.0-SNAPSHOT.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-alpine

ENV SERVER_PORT=8190

WORKDIR /app
RUN addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /app/logs \
    && chown -R app:app /app
COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE 8190
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

