# --- Stage 1: Build code bằng Maven ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file pom.xml và source code vào
COPY pom.xml .
COPY src ./src

# Build ra file jar (bỏ qua test để build nhanh hơn)
RUN mvn clean package -DskipTests

# --- Stage 2: Chạy ứng dụng (Runtime) ---
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy file jar từ Stage 1 sang Stage 2
# Lưu ý: *.jar sẽ tự lấy file bất kể phiên bản là 1.0.0 hay thay đổi sau này
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]