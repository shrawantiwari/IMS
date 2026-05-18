# =========================
# Stage 1 - Build Stage
# =========================

FROM maven:4.0.0-rc-5-eclipse-temurin-26 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


# =========================
# Stage 2 - Runtime Stage
# =========================

FROM eclipse-temurin:26-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]