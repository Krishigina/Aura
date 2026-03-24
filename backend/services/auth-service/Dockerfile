FROM gradle:8.5-jdk17 AS builder
WORKDIR /build
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/wrapper/gradle-wrapper.properties ./gradle/wrapper/
COPY services/auth-service/src ./services/auth-service/src
COPY services/user-service/src ./services/user-service/src 2>/dev/null || true
COPY services/product-service/src ./services/product-service/src 2>/dev/null || true
COPY services/recommendation-service/src ./services/recommendation-service/src 2>/dev/null || true
COPY services/tracker-service/src ./services/tracker-service/src 2>/dev/null || true
RUN gradle :services:auth-service:build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/services/auth-service/build/libs/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "app.jar"]