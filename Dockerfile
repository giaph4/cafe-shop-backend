FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && apt-get install -y maven

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "target/coffee-shop-backend-1.0.0.jar"]
