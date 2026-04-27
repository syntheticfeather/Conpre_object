# smart-customer-service/Dockerfile
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
# 禁止 Python 生成 .pyc 字节码文件
# 强制 Python 立即输出日志，不缓冲
# pip 安装时不保存缓存,减小 Docker 镜像体积
# 不检查 pip 新版本
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TZ=Asia/Shanghai

# 设置时区
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo $TZ > /etc/timezone

# 安装系统依赖（sentence-transformers 需要编译某些依赖）
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt \
    -i https://mirrors.aliyun.com/pypi/simple \
    --trusted-host mirrors.aliyun.com \
    --extra-index-url https://download.pytorch.org/whl/cpu

# 复制项目文件 复制到 /app
COPY . .

# 创建目录并设置权限 (为普通用户做准备)
RUN mkdir -p /app/logs /app/uploads \
    && groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000"]