# 1. 编译阶段
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. 运行阶段
# 换成 eclipse-temurin，这是目前最推荐的官方 OpenJDK 镜像
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/shop-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]