
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/coffee-shop-backend-1.0.0.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]