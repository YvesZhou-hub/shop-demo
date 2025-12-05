# 1. 编译环境：使用 Maven 和 JDK 17
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
# 开始编译打包，跳过测试以节省时间
RUN mvn clean package -DskipTests

# 2. 运行环境：使用轻量级 JDK 17
FROM openjdk:17-jdk-slim
WORKDIR /app
# 从第一步中拷贝编译好的 jar 包
# 注意：这里的 jar 包名必须和你 pom.xml 里的 artifactId + version 对应
COPY --from=build /app/target/shop-demo-0.0.1-SNAPSHOT.jar app.jar

# 暴露 8080 端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]