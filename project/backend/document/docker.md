# Docker部署部分

### 第一部分：核心概念与本地准备

我们先理解一下工作流程：

1.  **编写 Dockerfile**：一个文本文件，包含了构建 Docker 镜像所需的所有指令。
2.  **构建镜像 (Build Image)**：根据 `Dockerfile` 的指令，将你的应用代码、依赖和环境打包成一个独立的、可移植的镜像。
3.  **运行容器 (Run Container)**：将镜像实例化，在一个隔离的环境中运行起来，这就成为了一个容器。

#### 步骤 1：确保你的 Spring Boot 项目可打包

在项目根目录下，使用 Maven 或 Gradle 打包，生成 `.jar` 文件。这是构建 Docker 镜像的基础。

```bash
# Maven
mvn clean package -DskipTests
```
检查 `target/` 或 `build/libs/` 目录下是否生成了 `your-app-name-0.0.1-SNAPSHOT.jar` 文件。

#### 步骤 2：创建 Dockerfile

在项目的**根目录**下创建一个名为 `Dockerfile` 的文件（没有后缀）。这个文件是构建镜像的蓝图。

我们准备创建一个**两阶段构建**的 `Dockerfile`。它可以生成更小、更安全的生产镜像。

```dockerfile
# 第一阶段：构建阶段 (Builder Stage)
# 使用一个包含Maven和JDK的官方镜像来构建项目
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app
# 首先只复制pom.xml文件（利用Docker缓存层）
COPY pom.xml .
# 下载所有依赖（如果pom.xml没变，这一层会被缓存，极大加速后续构建）
# 虽然说的是缓存，但是我一直没看到效果，每次build都下载一堆东西
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

# 可以添加JVM参数，例如调整内存、启用调试等（按需修改）
# ENTRYPOINT ["java", "-Xmx512m", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]
```

#### 步骤 3：构建 Docker 镜像

在包含 `Dockerfile` 的项目根目录下，打开终端/命令行，执行以下命令：

```bash
# -t 参数给镜像打一个标签（名称:版本），方便后续使用
# 注意最后有一个点 (.)，表示当前目录是构建上下文
docker build -t my-spring-app:1.0.0 .

# 构建成功后，查看镜像列表
docker images
```
你应该能看到一个名为 `my-spring-app`，标签为 `1.0.0` 的镜像。

---

**很多时候不太连的上**

需要配置一下镜像源(从github上找的比较好的:
[爬爬虾](https://github.com/tech-shrimp/docker_installer))
```bash
sudo vi /etc/docker/daemon.json

# 复制进去
{
    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://docker.1panel.live",
        "https://hub.rat.dev"
    ]
}

:wq
```

#### 步骤 4：在本地运行 Docker 容器测试

在部署到服务器之前，先在本地测试一下镜像是否能正常工作。

```bash
# 最基本的运行命令
docker run -p 8080:8080 my-spring-app:1.0.0

# 更实用的后台运行命令（添加 -d 参数）
docker run -d -p 8080:8080 --name spring-app-container my-spring-app:1.0.0
```

*   `-p 8080:8080`：将**宿主机的 8080 端口**映射到**容器的 8080 端口**。这样你访问 `http://localhost:8080` 就能访问到容器内的应用。
*   `-d`：让容器在**后台**运行（Detached mode）。
*   `--name`：给容器起一个名字，方便管理。

**测试：** 

打开浏览器访问 `http://localhost:8080`。如果应用正常响应，说明镜像构建成功

或者指令`docker ps`查看你命名的容器是否在。

---

**如果没有成功可以用以下指令进行排查:**

**查看日志：**
```bash
docker logs -f spring-app-container
```

**如果不想用了:**

**停止容器：**
```bash
docker stop spring-app-container
```

**删除容器:**

```bash
docker rm spring-app-container
```


---

### 第二部分：部署到服务器

现在我们将本地构建好的镜像部署到远程服务器。

#### 步骤 1：将镜像上传到镜像仓库（Docker Hub）

你不能直接把本地镜像复制到服务器，最佳实践是使用镜像仓库（Registry）作为中介。Docker Hub 是免费的公共仓库。

1.  **注册 Docker Hub 账号**（如果你还没有）：https://hub.docker.com/

2.  **在本地登录 Docker Hub**：
    ```bash
    sudo docker login
    ```
    输入你的用户名和密码。

    **这一步也可能很卡，得用梯子**

    ---

3.  **重新标记你的镜像**，使其符合 Docker Hub 的命名规范（`你的用户名/镜像名:标签`）：
    ```bash
    sudo docker tag my-spring-app:1.0.0 your-dockerhub-username/my-spring-app:1.0.0
    ```
    ---
4.  **推送镜像到 Docker Hub**：
    ```bash
    sudo docker push your-dockerhub-username/my-spring-app:1.0.0
    ```
    推送成功后，你可以在 Docker Hub 网站的个人仓库里看到它。

    可以下载**docker desktop**方便查看

#### 步骤 2：在服务器上拉取并运行镜像

通过 SSH 连接到你的远程服务器。

1.  **安装 Docker**（如果服务器上尚未安装）：
    官方安装脚本是最简单的方式（请始终从官方文档获取最新命令）：
    ```bash
    # 对于大多数Linux发行版
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh

    # 将当前用户加入docker组，避免每次用sudo
    # 并且后续使用docker安全进程这步也是必须的
    sudo usermod -aG docker $USER 

    # 执行完 usermod 后，需要退出SSH再重新登录生效
    ```

2.  **从 Docker Hub 拉取镜像**：
    ```bash
    docker pull your-dockerhub-username/my-spring-app:1.0.0
    ```

3.  **以生产环境方式运行容器**：
    我们使用更完善的命令，包含资源限制、重启策略和日志管理。

    ```bash
    docker run -d \
      --name my-spring-app-prod \
      -p 80:8080 \  # 将宿主机的80端口（HTTP默认端口）映射到容器的8080端口
      --restart=always \ # 容器总是重启，除非手动停止（实现开机自启）
      --memory=512m \ # 限制容器最大使用内存为512MB
      --cpus="1.0" \ # 限制容器最多使用1个CPU核心
      -e "SPRING_PROFILES_ACTIVE=prod" \ # 设置环境变量，指定Spring使用生产环境配置
      -v /host/path/to/logs:/app/logs \ # 挂载日志卷，将容器内日志持久化到主机磁盘
      iamfs/my-pro:1.0.0
    ```

    **重要参数解释：**
    *   `--restart=always`：确保容器在意外退出或服务器重启后自动启动，非常适合生产环境。
    *   `-e`：设置环境变量。`SPRING_PROFILES_ACTIVE=prod` 是Spring Boot读取的配置，它会激活 `application-prod.properties` 文件中的配置。
    *   `-v`：**卷挂载**。这是**极其重要**的一步。
        *   `/host/path/to/logs`：是服务器上的一个真实目录，你需要提前创建（`sudo mkdir -p /host/path/to/logs`）。
        *   `/app/logs`：是你Spring Boot应用在容器内写入日志的路径（你需要确保你的应用配置`logging.file.path=/app/logs`）。
        *   这样做的目的是**数据持久化**。即使容器被删除，日志文件仍然保留在服务器上。同样，你也可以挂载配置文件。

---

# 小总结

**单纯的docker使用流程:**

- 相应应用安装

- Dockerfile 书写

- 指令构建

```bash
# 本地
Docker Build -t jotang2025-gcc-app:latest .
Docker images
Docker tag jotang2025-gcc-app:latest iamfs/jotang2025-gcc-app:latest
Docker login
Docker push iamfs/jotang2025-gcc-app:latest
# 服务器
DOcker pull iamfs/jotang2025-gcc-app:latest
Docker run -d -p 80:8080 --name jotang2025-gcc-app-prod -v /host/path/to/logs:/app/logs -e "SPRING_PROFILES_ACTIVE=prod" iamfs/my-pro:1.0.0
```

# 更为强大的docker-compose。

但是docker肯定不止这些，

docker-compose是docker官方推出的一个编排工具，可以实现多个容器的编排，这是一个集成镜像部署的工具。

## QUICKSTART

### 编写一个ngnix的docker-compose.yml文件

```yml
version: '3.7'

services:
  nginx:
    image: nginx:latest
    container_name: nginx_container
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./html:/usr/share/nginx/html
    restart: always
```

这是一个ngnix的镜像，拉去后访问 http://localhost 即可看到nginx的欢迎页面。

可以看到整体框架

    version定义版本

    services定义服务

    每一个服务用一个名字定义，

        服务内写拉取的镜像tag(images)

        容器名称(ngnix_container)

        还有映射的端口(ports)

        挂载的脚本或者日志文件(volumes)

        是否重启(restart)

    还有一些:

        环境配置(environment)，sql的创建用户我也不想在服务器自己建，怎么办呢？写这里面

        运行指令(command)

## 那么docker-compose比我们之前用单docker好在哪呢？

### 环境配置

    docker部署我们的spring项目时，

    我们需要在服务器安装并配置sql和redis。

    我们还要改配置文件里的连接ip地址。

    并且还要开放端口，允许访问。

**但是强大的docker-compose:**

    直接使用docker-compose拉取镜像sql和镜像redis。

    直接连接镜像sql和镜像redis

    免去了服务器上配置环境的繁琐步骤

### 多容器集成启动

    docker进行容器运行时需要一个一个启动，

~~宛如乐迪的**启动启动启动，还有这个**~~

**但是强大的docker-compose:**

    docker-compose up -d

    一键启动所有容器，真正的一行代码，完整部署

## 那我们来实践一下

### 为我们的后端项目搭配镜像sql和镜像redis

```yml
# docker-compose-dev.yml
version: '3.7'
services:
  app:
    # 从hub上拉取你自己的项目镜像
    image: iamfs/jotang2025-gcc-app:latest
    # 如果你不想把你得项目放到hub上，那你可以把你得项目一起放到服务器，然后docker-compose手动进行镜像的生成和容器的运行操作
    # build: .
    container_name: my-app-dev
    ports:
      - "8080:8080"
    # 关键：将本地代码目录“绑定挂载”到容器内，覆盖掉构建时复制的文件。
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      # mysql 配置
      # mysql-db就是镜像sql
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/JoTang2025?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=gcc
      - SPRING_DATASOURCE_PASSWORD=1234 
      # redis 配置  
      # 同理
      - SPRING_REDIS_HOST=redis-cache  
      - SPRING_REDIS_PASSWORD=1234  
      - SPRING_REDIS_PORT=6379
    depends_on:
      - mysql-db
      - redis-cache

  mysql-db:
    image: mysql:8.0
    container_name: mysql-db-dev
    environment:
      MYSQL_ROOT_PASSWORD: my_strong_password
      MYSQL_DATABASE: JoTang2025
      MYSQL_USER: gcc
      MYSQL_PASSWORD: 1234
      # MYSQL_HOST: '%'
    volumes:
      - ./db-init:/docker-entrypoint-initdb.d  # 挂载初始化脚本目录
      - mysql_data:/var/lib/mysql
    # 映射端口：格式 - "宿主机端口:容器内端口"
    ports:
      - "3306:3306" # 可选，如果宿主机需要直接连接数据库才开放
    # MySQL 8.x 的认证插件配置
    command: 
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    restart: unless-stopped


  redis-cache:
    image: redis:6-alpine
    container_name: redis-cache-dev
    command: redis-server --requirepass 1234 --appendonly yes
    ports:
      - "6379:6379" # 暴露端口到本地
    volumes:
      - redis_data:/data

volumes:
  mysql_data:
  redis_data:
```

把init-db和docker-compose一起复制到服务器


```bash
docker-compose up -d
```

我们就完成部署了，真正的可以直接运行的部署

## 其他操作

### 后期项目数据库改动时，可能需要删卷(删数据库)

docker-compose down -v  # 删除容器同时删除卷

### 更新的时候备份数据库

docker exec mysql_container mysqldump -u root -p database_name > backup.sql

然后放到init-db脚本文件夹里面，就可以下次启动时恢复数据库



# docker指令表：

| 指令 | 说明 |
| --- | --- |
|docker build -t 镜像名:标签 路径 | 构建镜像|
|docker images |查看镜像|
|docker image prune -f |删除所有未命名镜像|
|docker rmi 镜像名/id |删除镜像|
|docker rm 容器名/id |删除容器|
|docker ps -a |查看所有容器|
|docker ps | 查看运行的容器| 
|docker run -d -p 端口:容器端口 --name 容器名 镜像名 |运行容器|
