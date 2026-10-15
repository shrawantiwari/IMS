
FROM eclipse-temurin:26-jdk AS builder

WORKDIR /app

# Copy only the independent auth Gradle project
COPY . .

RUN chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:26-jre

WORKDIR /app

RUN useradd --system --no-create-home appuser

COPY --from=builder --chown=appuser:appuser \
    /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
