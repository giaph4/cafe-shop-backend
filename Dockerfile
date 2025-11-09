# Sử dụng JDK 21
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy file jar đã build sẵn
COPY target/coffee-shop-backend-1.0.0.jar app.jar

EXPOSE 8088

# Chạy Spring Boot jar
ENTRYPOINT ["java", "-jar", "app.jar"]
