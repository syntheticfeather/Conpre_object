from fastapi import FastAPI

# 创建 FastAPI 应用实例
app = FastAPI(
    title="测试 API",
    description="测试 FastAPI 应用",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

@app.get("/")
def read_root():
    return {"message": "测试 API 服务运行中"}

@app.get("/items/{item_id}")
def read_item(item_id: int, q: str = None):
    return {"item_id": item_id, "q": q}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)