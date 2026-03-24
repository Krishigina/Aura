FROM gradle:8.5-jdk17 AS builder
WORKDIR /build
COPY settings.gradle.kts ../build.gradle.kts ./
COPY services/user-service/build.gradle.kts ./build.gradle.kts
COPY services/user-service/src ./src
COPY services/auth-service/src ./auth-src
COPY services/product-service/src ./product-src
COPY services/recommendation-service/src ./recommendation-src
COPY services/tracker-service/src ./tracker-src
RUN gradle :user-service:build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/services/user-service/build/libs/*.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]