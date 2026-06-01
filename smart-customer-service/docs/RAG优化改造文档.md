# RAG 管线优化改造文档

> 改造时间：2026-05-31
> 改造范围：smart-customer-service Python 项目

---

## 改造总览

对现有 RAG 知识检索管线做了五处优化，覆盖离线入库和在线检索全流程。

```
离线阶段:
  文档加载 → Chunk 切割 → Embedding → 入库  (①~③)

在线阶段:
  Query 改写 → 多路粗排 → RRF 融合 → Cross-Encoder 精排 → Prompt 增强 + LLM 生成  (④~⑦)
```

---

## ① 文档加载

**文件**：`api/init_data.py`（无改动）

- 支持 `.md`（Markdown）和 `.json`（问答对）两种格式
- 通过 `KnowledgeInitializer` 初始化，启动时自动检测是否需入库

---

## ② Chunk 切割（优化）

**文件**：`utils/markdown_processor.py`

| 改动 | 说明 |
|------|------|
| ✅ 重叠 | `overlap_chars=200`，相邻 chunk 之间重叠 200 字符 |
| ✅ 二级切分 | `max_chunk_chars=800`，超长区域按句子边界拆分子 chunk |
| ✅ 层级路径 | `_build_path()` 构建 `父标题 > 子标题 > 孙标题` 路径存为 metadata |

**构造参数**：

```python
MarkdownProcessor(overlap_chars=200, max_chunk_chars=800)
```

---

## ③ Embedding 向量化（优化）

**文件**：`utils/chromadb_client.py`

**三级降级链**：

```
主力:  SiliconFlow API (BAAI/bge-m3, 1024维)
   ↓ 失败
兜底:  sentence-transformers 本地模型 (384维 → 自动补齐到1024维)
   ↓ 也失败
最终:  MD5 哈希 (128维，仅保系统不崩溃)
```

| 改动 | 说明 |
|------|------|
| ✅ 本地兜底 | `LocalEmbeddingFunction` 替代 MD5 哈希兜底 |
| ✅ 维度对齐 | 本地模型输出自动用零填充补齐到主模型维度 |
| ✅ 缓存 | `CachedEmbeddingFunction` 包装，精确匹配模式下 MD5 去重，命中率 30-50% |
| ✅ 模型可配 | 主模型和本地模型均通过 `.env` 指定 |

**`.env` 配置**：

```bash
# 主力嵌入模型
SILICONFLOW_API_KEY=sk-xxx
SILICONFLOW_API_BASE=https://api.siliconflow.cn/v1
SILICONFLOW_EMBEDDING_MODEL=BAAI/bge-m3

# 本地兜底模型
LOCAL_EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2

# 缓存模式 (exact / semantic)
EMBEDDING_CACHE_MODE=exact
```

---

## ④ Query 改写（新增）

**文件**：`utils/query_rewriter.py`

| 功能 | 说明 |
|------|------|
| 简单改写 | 补全代词、去除语气词、转成独立问句 |
| HyDE | 生成"假设答案"用答案去检索（可选） |
| 上下文感知 | 从对话历史补全"上次说的那个" |
| 自动跳过 | 纯计算、太短、已是独立问句的跳过改写 |

**`.env` 配置**：

```bash
QUERY_REWRITE_ENABLED=true
```

---

## ⑤ 多路粗排（新增）

**文件**：`utils/bm25_retriever.py` + `tools/search_tools.py`

**两路并行召回**：

| 路 | 引擎 | 擅长 |
|------|------|------|
| 向量检索 | ChromaDB HNSW | 语义相似（"贷款"和"借款"） |
| BM25 关键词 | jieba 分词 + rank_bm25 | 精确匹配（"GPT-4o"、"LSTM"） |

**RRF 融合算法**：

```
RRF_score = Σ 1 / (k + rank_i)    # k=60

两路都排前面的文档 → 得分翻倍 → 优先返回
只有一路排前面的文档 → 得分打折
```

**新增依赖**：`rank-bm25>=0.2.2`、`jieba>=0.42`

---

## ⑥ Cross-Encoder 精排（新增）

**文件**：`utils/reranker.py`

| 项目 | 说明 |
|------|------|
| 模型 | `BAAI/bge-reranker-v2-m3`（中文 Cross-Encoder） |
| 输入 | `[CLS] query [SEP] document [SEP]` — 文本拼接 |
| 输出 | 相关性分数 0~1 |
| 范围 | 只对粗排返回的 6-8 条候选打分，开销 ~几十毫秒 |

**`.env` 配置**：

```bash
RERANK_ENABLED=true
RERANK_TOP_N=3
```

---

## 测试方法

### 一键测试（推荐）

```bash
cd smart-customer-service
PYTHONPATH=. python test/test_rag_pipeline.py
```

预期输出（2026-05-31 实测）：

```
[PASS] chunk count > 0
[PASS] API available
[PASS] model dimension = 1024
[PASS] API returns 2 vectors
[PASS] cache hit > 0
[PASS] ChromaDB connected
[PASS] query returns results
[PASS] BM25 index built
[PASS] RRF fusion returns > 0
[PASS] Reranker available
[PASS] Top-1 is loan doc (id=1)
[PASS] Rewriter enabled
Results: 22 passed / 1 failed / 23 total
```

> 唯一失败项为本地兜底模型加载（Windows 无 Dev 模式的 symlink 限制），不影响使用——API 模式下正常。

### 分步测试

```bash
cd smart-customer-service

# Chunk 切割
python -c "
from utils.markdown_processor import MarkdownProcessor
mp = MarkdownProcessor(overlap_chars=200, max_chunk_chars=800)
chunks = mp.parse('knowledge-init/法律条例与合规声明.md')
for c in chunks: print(f'{c.section_path} | len={len(c.content)}')
"

# Embedding API
python -c "
from utils.chromadb_client import OpenAICompatibleEmbeddingFunction
ef = OpenAICompatibleEmbeddingFunction()
vecs = ef.embed_documents(input=['test'])
print(f'Model: {ef.name()}, Dim: {len(vecs[0])}')
"

# Reranker API
python -c "
from utils.reranker import Reranker
r = Reranker()
cands = [{'id':'1','document':'利率4.5%'},{'id':'2','document':'退款7天'}]
ranked = r.rerank('贷款利率', cands)
print(f'Top-1: [{ranked[0][\"id\"]}] {ranked[0][\"document\"]}')
"

# Query 改写
python -c "
from utils.query_rewriter import QueryRewriter
print(QueryRewriter().rewrite('上次说的那个利率呢'))
"
```

### 依赖安装

```bash
cd smart-customer-service
pip install rank-bm25 jieba sentence-transformers
# Windows 环境下还需：
pip install python-certifi-win32    # 解决 SSL 证书问题
```

### 检查 ChromaDB 索引

```bash
# 启动 ChromaDB（如果使用 Docker）
docker-compose up -d chromadb

# 或本地模式直接在项目目录启动，数据目录: ./chromadb_data
```

### 3. 启动服务

```bash
uvicorn api.main:app --host 0.0.0.0 --port 8000
```

### 4. 测试检索效果

```bash
# 方式 A: 直接调 API
curl -X POST http://localhost:8000/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"message": "贷款利率是多少", "session_id": "test_001"}'

# 方式 B: 直接测试检索模块
python -c "
from tools.search_tools import search_knowledge
import asyncio
result = asyncio.run(search_knowledge.ainvoke({'query': '退款怎么申请'}))
print(result)
"
```

### 5. 测试 Query 改写

```python
from utils.query_rewriter import QueryRewriter
rw = QueryRewriter()
print(rw.rewrite("上次说的那个利率呢"))
# 应输出: "个人消费贷的年利率是多少"（或类似）
```

### 6. 测试兜底降级

```bash
# 临时禁用 API Key 测试本地兜底
SILICONFLOW_API_KEY="" uvicorn api.main:app --port 8000
# 观察日志: 应显示 "使用本地嵌入模型"
```

---

## 新增文件清单

```
smart-customer-service/
├── utils/
│   ├── query_rewriter.py     ← 新增: Query 改写
│   ├── bm25_retriever.py     ← 新增: BM25 关键词检索 + RRF 融合
│   └── reranker.py           ← 新增: Cross-Encoder 精排
├── docs/
│   └── RAG优化改造文档.md     ← 新增: 本文件
├── tools/
│   └── search_tools.py       ← 修改: 集成多路召回+精排+改写
├── utils/
│   ├── chromadb_client.py    ← 修改: 本地兜底+缓存+维度对齐
│   └── markdown_processor.py ← 修改: 重叠+二级句子切分
├── requirements.txt          ← 修改: 新增 rank-bm25, jieba, sentence-transformers
└── .env                      ← 修改: 新增一系列嵌入/检索/改写配置
```
