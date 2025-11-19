>> [参考文献](docker.md)

## 下载docker并配好环境变量

https://docs.docker.com/desktop/setup/install/windows-install/

```bash
# 验证docker是否配好
docker --version

>> Docker version 28.4.0, build d8eb465

docker-compose --version

>> Docker Compose version v2.39.2-desktop.1   w Enable Watch
```

## 确保3306和6379端口未被其他应用占用

```bash
# 如果占用
netstat -ano | findstr :3306

>> TCP    0.0.0.0:3306           0.0.0.0:0              LISTENING       2468

# 拿你机子上的PID，也就是最后那个
tasklist | findstr 2468

taskkill /PID 2468 /F
```

## 镜像源配置

```bash
C:\Users\<你的用户名>\.docker\daemon.json进入该文件

复制进去
{
    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://docker.1panel.live",
        "https://hub.rat.dev"
    ]
}

#如果文件里面有东西，不复制最外层大括号

{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false, 这要加逗号，这句话不加

    "registry-mirrors": [
        "https://docker.m.daocloud.io",
        "https://docker.1panel.live",
        "https://hub.rat.dev"
    ]
}

```

## build

```bash
cd project/backend/personal-loan

# 携带日志输出的启动
# 如果build不起，开梯子
# 就可以本机测试了
docker-compose up

# 如果后台运行不要日志输出

docker-compose up -d
```

## 如果后期项目进行了修改，那么需要删除images，重新进行build

```bash

# 先关闭容器
docker-compose down

# 查看所有镜像
docker images

REPOSITORY                 TAG        IMAGE ID       CREATED          SIZE
personal-loan-app          latest     36e4e721c7a6   21 minutes ago   800MB
<none>                     <none>     16e6f275e117   41 minutes ago   800MB
redis                      6-alpine   37e002448575   11 days ago      44.6MB
mysql                      8.0        f37951fc3753   3 weeks ago      1.07GB
docker/welcome-to-docker   latest     c4d56c24da4f   3 months ago     22.2MB

# 删personal-loan-app和<none>的镜像,用ID或者REPOSITORY都可以删
docker rmi personal-loan-app:latest
docker rmi 16e6f275e117

docker-compose up

==================================
|         还有另一种方式           |
==================================

# 重新build
docker-compose build 
# 重新启动
docker-compose up

```

## 如果sql的初始化脚本create-table-template.sql文件进行了修改，那么就需要全删


```bash
docker-compose down

docker image prune -a
```

## 很多时候，可能不想编译我们的项目，只是想用一下redis和sql的容器

那就在docker-desktop上确认两个容器都是开启的，然后直接运行java项目，也行。