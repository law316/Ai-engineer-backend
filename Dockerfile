# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=builder /app/target/AiEngineerPractice-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
