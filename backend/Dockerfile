FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts .
COPY src ./src
RUN gradle shadowJar --no-daemon

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY firebase-service-account.json .
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
