# 第一阶段：构建阶段 (Builder Stage)
# 使用一个包含Maven和JDK的官方镜像来构建项目
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

# 首先只复制pom.xml文件（利用Docker缓存层）
COPY pom.xml .

# 下载所有依赖（如果pom.xml没变，这一层会被缓存，极大加速后续构建）
RUN mvn dependency:go-offline

# 然后复制所有源代码
COPY src ./src

# 执行打包，生成jar文件
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段 (Runtime Stage)
# 使用一个更小的、仅包含JRE的官方镜像来运行应用，减小镜像体积
FROM openjdk:25-ea-17-jdk-slim

# 设置工作目录 inside the container
WORKDIR /app

# 从上一阶段（builder）复制打包好的jar文件
COPY --from=builder /app/target/*.jar app.jar

# 创建一个非root用户来运行应用，增强安全性
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

# 暴露应用运行端口（与你application.properties中的server.port一致）
EXPOSE 8080

# 执行命令来运行应用
# 使用显式的"java -jar"命令而不是shell形式，确保能正确处理信号（如docker stop）
ENTRYPOINT ["java", "-jar", "/app/app.jar"]