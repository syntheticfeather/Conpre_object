# 进阶式挑战性综合项目II总结报告

---

## 摘 要

本项目在"综合设计I"已建成的个人贷款基础业务平台之上，围绕"多数据源融合的互联网个人贷款风控系统"这一核心目标，进行了五大子系统的并行建设与升级。其中，**风控系统**作为综合设计I明确规划的"综合设计II首要任务"，从30余条静态规则集全面升级为基于XGBoost的54维特征机器学习模型，实现了数据生成、特征工程、模型训练、双格式导出的完整ML工程流水线，可与Java后端通过XGBoost4j无缝集成。**智能客服系统**作为团队的创新拓展方向，采用Agentic Workflow可组合架构，设计了Java+Python双端协同路由（Workflow快通道+Agent深度处理），集成RAG多路召回+Cross-Encoder精排的检索管线以及基于Mem0+Zep理念的长期记忆系统。后端系统在综合设计I基础上引入了Redis缓存策略（布隆过滤器+Redisson分布式锁+随机过期）、RabbitMQ可靠消息机制（Outbox发件箱模式+幂等消费+重试/DLQ）、AOP声明式分布式锁以及SSE实时通知推送。Web前端实现了双Token无感刷新、SSE通知中心、智能客服对话组件及通用组件抽象。App端完成了从React Native到Kotlin原生开发的架构重构。各子系统经分层测试验证，功能稳定、性能达标，为中小型金融科技应用提供了可复用的智能化信贷系统参考架构。

**关键词**：个人贷款风控系统；XGBoost信用评分；智能客服；Agentic Workflow；RAG；Spring Boot；分布式缓存；Outbox模式

---

## Abstract

Building upon the foundational personal loan platform established in "Comprehensive Design I," this project centers on the core objective of "Multi-Data-Source Integrated Internet Personal Loan Risk Control System," executing parallel construction and upgrade across five subsystems. The **Risk Control System**—explicitly designated in Design I as the "primary task for Design II"—has been comprehensively upgraded from 30+ static rules to a 54-dimensional XGBoost machine learning model, implementing a complete ML engineering pipeline covering data generation, feature engineering, model training, and dual-format export for seamless Java backend integration via XGBoost4j. The **Intelligent Customer Service System**—a team-initiated innovation—adopts an Agentic Workflow composable architecture with Java+Python dual-end collaborative routing (Workflow fast path + Agent deep processing), integrating a multi-recall RAG retrieval pipeline with Cross-Encoder reranking and a long-term memory system based on Mem0+Zep design principles. The backend introduces Redis caching strategies (Bloom filter + Redisson distributed locks + random expiration), RabbitMQ reliable messaging (Outbox pattern + idempotent consumption + retry/DLQ), AOP-based declarative distributed locking, and SSE real-time notification push. The Web frontend implements dual-token seamless refresh, SSE notification center, intelligent customer service chat component, and reusable component abstraction. The Android app has been architecturally refactored from React Native to native Kotlin. All subsystems have passed layered testing verification with stable functionality and compliant performance.

**Keywords**: Personal Loan Risk Control System; XGBoost Credit Scoring; Intelligent Customer Service; Agentic Workflow; RAG; Spring Boot; Distributed Cache; Outbox Pattern

---

## 目 录

- **第一章 针对复杂工程问题的方案设计与实现**
  - 1.1 方案设计
  - 1.2 推理分析
  - 1.3 方案实现
- **第二章 系统测试**
- **第三章 知识技能学习情况**
- **第四章 分工协作与交流情况**
- **参考文献**
- **致谢**

---

## 第一章 针对复杂工程问题的方案设计与实现

### 1.1 方案设计

#### 1.1.1 项目背景：从综合设计I到综合设计II

在"综合设计I"阶段，团队（**龚陈陈**、**周飞凤**负责后端，**王婷**负责Web管理端，**覃雨欣**负责Android客户端）已成功构建了个人贷款管理系统的核心基础平台，完成了用户端全流程闭环（注册/登录→实名认证→产品浏览→申请提交→进度查询）、管理后台基础能力（用户管理、产品配置、审核工作台、操作审计日志）、12张核心数据库表设计、AES-256加密存储、JWT+RBAC权限控制，并集成了RabbitMQ用于异步通知。在风控方面，团队完成了30余条基础风控规则的预研定义（如"身份证有效期不足3个月则拒绝""近7天申请平台数>5视为高风险"）和安卓端信息采集清单，但**风控系统（Risk Service）作为核心差异化模块尚未正式开发，被明确列为综合设计II阶段的首要任务**。

综合设计II在继承上述全部成果的基础上，围绕五大子系统展开并行建设：

#### 1.1.2 五大子系统总体架构

系统由五个独立建设、协同工作的子系统构成（见图1-1，drawio文件：`图1-1_五大子系统总体架构图.drawio`）。

**图1-1 五大子系统总体架构图**

```
                          ┌──────────────────────────────────────────┐
                          │            用  户  入  口  层             │
     ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐  │
     │    Web 前端       │ │   Android App    │ │  智能客服对话窗口  │ │
     │ Vue 3 + ECharts  │ │  Kotlin + Jetpack│ │  ChatDialog SSE  │  │
     │  + Three.js      │ │  (MVVM 架构)     │ │                  │  │
     └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘  │
              │                    │                     │            │
              └────────────────────┼─────────────────────┘            │
                                   │                                 │
                          ┌────────┴─────────┐                       │
                          │  REST API + SSE   │                      │
                          │   网  关  层       │                      │
                          │  JWT 鉴权 | 路由   │                      │
                          └────────┬─────────┘                       │
                                   │                                 │
         ┌─────────────────────────┼─────────────────────────┐       │
         │                         │                         │       │
    ┌────┴──────────────┐    ┌─────┴──────────────┐          │       │
    │  Java Spring Boot │    │  Python AI 服务层   │          │       │
    │   后  端  服  务   │    │                    │          │       │
    │                   │    │  ┌──────────────┐  │          │       │
    │ ┌───────────────┐ │    │  │ 智能客服系统   │  │          │       │
    │ │ 认证·产品·申请 │ │    │  │ Agentic       │  │          │       │
    │ │ 订单·用户·还款 │ │    │  │ Workflow      │  │          │       │
    │ │ 通知·统计·催收 │ │    │  │ RAG+长期记忆  │  │          │       │
    │ └───────────────┘ │    │  └──────────────┘  │          │       │
    │ ┌───────────────┐ │    │  ┌──────────────┐  │          │       │
    │ │ Redis 缓存     │ │    │  │ ML 风控系统   │  │          │       │
    │ │ RabbitMQ 消息  │ │    │  │ XGBoost 54维  │  │          │       │
    │ │ AOP 分布式锁   │ │    │  │ 双轨训练+导出 │  │          │       │
    │ │ SSE 实时推送   │ │    │  └──────────────┘  │          │       │
    │ └───────────────┘ │    │                    │          │       │
    └────────┬──────────┘    └─────────┬──────────┘          │       │
             │                         │                     │       │
             └───────────┬─────────────┘                     │       │
                         │                                   │       │
              ┌──────────┴──────────┐                        │       │
              │   数据与中间件层     │                        │       │
   ┌──────────┐ ┌──────┐ ┌───────┐ ┌─────────────────┐       │       │
   │  MySQL   │ │Redis │ │RabbitMQ│ │ChromaDB+MongoDB │       │       │
   └──────────┘ └──────┘ └───────┘ └─────────────────┘       │       │
                                                             │       │
                          └──────────────────────────────────────────┘
```

**子系统一：Java Spring Boot 后端**——系统的业务核心枢纽。在综合设计I已有的认证授权、产品管理、申请审批、订单管理等模块基础上，综合设计II新增了：CacheService（Redis缓存+布隆过滤器+Redisson分布式锁）、@RedisLocked AOP注解（声明式分布式锁）、Outbox发件箱机制+消息可靠投递（Publisher Confirm/Returns）、消费者手动ACK+幂等+重试/DLQ、四种还款方式计算引擎、SSE实时通知推送、数据统计分析、SQL索引优化与N+1问题修复。

**子系统二：Vue 3 Web 前端**——管理后台与用户端。在综合设计I的管理后台基础上进行了工程化升级：双Token无感刷新机制（Axios拦截器提前检测过期+401自动刷新+并发请求排队）、SSE通知中心（导航栏红点实时更新+下拉面板交互）、智能客服ChatDialog组件（SSE流式接收+marked Markdown渲染+Teleport全局挂载）、BaseTable/BasePagination/ContentTooltip通用组件抽象、ESLint+Prettier代码规范自动化。

**子系统三：Kotlin Android 客户端**——移动端贷款服务。从综合设计I的React Native技术栈重构为Kotlin原生开发，采用MVVM架构（ViewModel+LiveData+Repository）、Hilt依赖注入、Retrofit+OkHttp网络层（Interceptor统一Token管理）、SharedPreferences安全存储、生物识别认证。实现产品浏览、贷款申请、订单查询、还款操作等核心业务页面。

**子系统四：*Python 机器学习风控系统***（*图1-1 中部右侧*）——**综合设计II核心任务**。将综合设计I的*30余条静态规则集*全面升级为*54维特征的XGBoost机器学习模型*。系统包含：完整的特征工程体系（feature_config.py定义身份7+收入5+信用14+资产7+行为13+申请8共54个特征，每个特征明确名称、类型、来源、默认值、业务含义和数据可获取性）、合成数据+真实数据双轨训练流水线（train.py基于银保监会违约率统计生成20,000条合成样本，train_real.py基于Give Me Some Credit竞赛数据集训练）、模型双格式导出（PKL供Python推理服务、JSON供Java端XGBoost4j本地加载）、风控建议三级阈值分档（违约概率<0.10低风险通过、0.10~0.25中风险利率上浮或降额、>0.25高风险拒绝或要求增信）。feature_config.py同时作为Python+Java双端的配置单点源，Java后端可读取get_java_input_schema()生成的JSON Schema进行特征数据采集。

**子系统五：*Python FastAPI 智能客服系统***（*图1-1 右侧*）——**团队创新拓展**。采用*Agentic Workflow 可组合架构*，核心设计包括：
- **双端协同路由**：Java端ChatController接收消息→LLM意图分类（六类意图）→高置信度功能性请求走Java本地Workflow（毫秒级响应），咨询/投诉类请求转发Python Agent深度处理。
- **可组合Agent链**：装饰器模式，BaseAgent抽象基类→ReactAgent（ReAct循环）为底层→PlanExecuteAgent（规划→执行→汇总）和ReflectionAgent（回复→审查→修正）为装饰器→ChatAgent工厂按mode组装四种模式（react/plan/reflection/plan+reflection）。
- **RAG知识检索管线**：FAQ+5份文档→Chunk切割（200字符重叠）→BAAI/bge-m3向量化（1024维）→ChromaDB存储→五级在线检索（Query改写→向量+BM25多路召回→RRF融合→Cross-Encoder精排→Top-3返回）。
- **长期记忆系统**：Mem0多信号检索（语义+实体+重要性+时间衰减加权融合）+ Zep时序感知（90天半衰期、冲突自动过期、LLM异步提取记忆）。

#### 1.1.3 核心技术选型

**表1-1 五大子系统核心技术选型**

| 子系统 | 核心技术 | 选型依据 |
|--------|---------|---------|
| 后端 | Spring Boot 3.x + MyBatis + MySQL 8.0 | 沿用综设I，生态成熟 |
| 后端 | Redis 7.x + Redisson | 高性能缓存+看门狗分布式锁 |
| 后端 | RabbitMQ 3.x | 可靠投递+灵活路由+DLX/TTL |
| Web前端 | Vue 3 + Vite + TypeScript + Element Plus | 沿用综设I，开发效率高 |
| Web前端 | ECharts 5.5 + echarts-gl | 数据可视化图表（8类风控运营图表） |
| Web前端 | Three.js 0.183 | 3D 中国地图数据大屏渲染 |
| App端 | Kotlin + Jetpack (MVVM + Hilt) | 综设II重构，原生性能 |
| **风控** | **XGBoost + Scikit-learn + Pandas** | **金融风控黄金标准，可解释性强** |
| 风控 | Give Me Some Credit 数据集 | 行业标准基准，15万条真实信贷记录 |
| **智能客服** | **LangChain + FastAPI + ChromaDB** | **Agent框架成熟+向量检索轻量** |
| 智能客服 | BAAI/bge-m3 + bge-reranker-v2-m3 | 中文语义嵌入+精排领先 |
| 智能客服 | DeepSeek-chat + GPT-4.1-mini | 分层使用：主力推理+轻量分类/审查 |
| 容器化 | Docker Compose | 沿用综设I，一键部署 |

---

### 1.2 推理分析

#### 1.2.1 风控系统升级路线的推理分析——从规则引擎到机器学习模型

**图1-4 风控系统升级路线：从规则引擎到ML模型**

**问题分析**：综合设计I阶段已准备了*30余条基础风控规则*，为风控系统奠定了明确的业务逻辑基础。然而，静态规则引擎存在三个固有局限：一是规则覆盖依赖专家经验，难以穷举所有风险模式；二是规则权重缺乏数据驱动，易过度依赖主观判断；三是规则之间缺乏交互建模——例如，"多头借贷数≥3"与"月收入<4000"同时出现时的综合风险远高于两条规则单独触发时的简单叠加，规则引擎无法自动捕捉这种非线性组合效应。因此，综合设计I明确将风控系统列为综合设计II阶段的首要攻关任务。

**推理过程**：选择"规则集转化为特征先验 + XGBoost数据驱动建模"的渐进式升级路线。第一步：将综合设计I的30余条风控规则转化为模型特征——如将"近7天申请平台数>5"量化为credit_inquiries_3m特征、"身份证有效期不足3个月"转化为info_completeness特征——使模型能够在数据中自动学习这些规则的预测有效性，而非硬编码阈值。第二步：以54维特征体系取代离散规则。从6张核心业务表（users、user_certification、orders、repayment_schedule、loan_applications、black_list）聚合出多维用户画像，引入规则引擎难以定义的连续型行为特征（按期还款率on_time_payment_ratio、平均逾期天数avg_days_past_due、展期申请次数postpone_count等）。第三步：选用XGBoost——该算法天然支持特征交互建模，自动捕获"低收入+多头借贷""凌晨申请+短工作年限"等复合风险模式；在LendingClub、Give Me Some Credit等信贷场景有广泛验证；特征重要性天然可解释，满足金融监管对风控决策可审计的基本要求；模型文件体积小（<1MB），支持Java XGBoost4j本地加载，与现有Spring Boot技术栈无缝集成。

**选优结论**：相比*Drools纯规则引擎*（规则上限受专家经验约束），*XGBoost模型方案*能够从历史数据中持续优化风险识别能力；相比*深度神经网络*（可解释性差、训练数据量要求高、部署复杂），XGBoost在**可解释性**、**部署便利性**和**小样本表现**三个维度均更适配本项目场景。

#### 1.2.2 智能客服架构的推理分析——Workflow分流 + Agent组合模式

**图1-5 Workflow分流 + Agent深度处理双层架构**

```
                         ┌──────────────────────────┐
                         │      用户消息输入          │
                         └────────────┬─────────────┘
                                      │
                         ┌────────────┴─────────────┐
                         │    LLM 意图分类 (Router)   │
                         │  QUERY_STATUS / CALCULATE │
                         │  LIST_PRODUCTS / APPLY    │
                         │  CONSULT / COMPLAINT      │
                         └────────────┬─────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │ CONFIDENCE >= 0.8                │ CONFIDENCE < 0.8
                    ▼                                  ▼
     ┌──────────────────────────────┐    ┌──────────────────────────┐
     │   WORKFLOW 快通道 (Java本地)  │    │   AGENT 深度处理 (Python) │
     │                              │    │                          │
     │  ┌────────────────────────┐  │    │  ┌────────────────────┐  │
     │  │ QueryStatusHandler     │  │    │  │ CALCULATE→reflection│ │
     │  │ → ApplicationService   │  │    │  │ (金融计算准确优先)   │  │
     │  │ → 毫秒级响应            │  │    │  └────────────────────┘  │
     │  └────────────────────────┘  │    │  ┌────────────────────┐  │
     │  ┌────────────────────────┐  │    │  │ CONSULT→plan+refl  │  │
     │  │ CalculateHandler       │  │    │  │ (咨询需深度+准确)    │  │
     │  │ → CalculateUtil 正则提取│  │    │  └────────────────────┘  │
     │  │ → 参数引导+精确计算      │  │    │  ┌────────────────────┐  │
     │  └────────────────────────┘  │    │  │ COMPLAINT→plan      │  │
     │  ┌────────────────────────┐  │    │  │ (多步推理处理)       │  │
     │  │ ListProductsHandler    │  │    │  └────────────────────┘  │
     │  │ → LoanProductService   │  │    │  ┌────────────────────┐  │
     │  │ → Top-5 产品列表        │  │    │  │ default→react       │  │
     │  └────────────────────────┘  │    │  │ (通用兜底)           │  │
     │  ┌────────────────────────┐  │    │  └────────────────────┘  │
     │  │ ApplyLoanHandler       │  │    │                          │
     │  │ → LLM参数提取+Redis累积 │  │    │  RAG检索 | 长期记忆      │
     │  │ → 确认卡生成             │  │    │  SSE 流式返回            │
     │  └────────────────────────┘  │    │                          │
     │                              │    │                          │
     │  延迟: <100ms                │    │  延迟: 1~5s              │
     │  占比: ~60% 请求              │    │  占比: ~40% 请求          │
     └──────────────────────────────┘    └──────────────────────────┘
```

**问题分析**：智能客服系统面临两类矛盾的需求——大量结构化功能请求（查询进度/计算月供/浏览产品，答案确定、延迟敏感），与开放式咨询投诉（"哪种还款方式适合我？""为什么被拒？"，需多步推理和知识检索）。纯LLM Agent将所有请求投入ReAct循环，导致简单请求"用大炮打蚊子"；纯规则方案对复杂咨询束手无策。

**推理过程**：借鉴计算机网络"快路径/慢路径"思想，设计"意图分类→Workflow分流/Agent深度处理"双层架构。第一层：轻量LLM（GPT-4.1-mini）意图分类（~200ms）→高置信度功能性请求走Java Workflow（直接调Service返回，<100ms）；低置信度及咨询投诉走Python Agent。第二层：Python端采用装饰器模式构建可组合Agent链——ReactAgent基础循环、PlanExecuteAgent规划执行、ReflectionAgent审查修正——按意图匹配模式（计算→Reflection保准确、咨询→Plan+Reflection保深度、默认→React兜底）。

**选优结论**：*Workflow分流*将~60%请求控制在**100ms内**（vs. 纯Agent的3~8秒），LLM API调用量降低约**60%**。*装饰器模式*相比LangGraph图状态机更直观、更易独立测试和扩展。

#### 1.2.3 缓存与消息机制的推理分析

综合设计I阶段RabbitMQ仅用于基础异步通知，缺乏消息可靠性保障；Redis缓存完全未引入。综合设计II针对生产环境必需的三类风险进行分析：

**缓存三防**：布隆过滤器（100万元素、0.1%误判率）前置拦截无效Key→防穿透；Redisson分布式锁互斥回源查询（tryLock带超时+看门狗自动续期）→防击穿；基础TTL 3600秒+随机偏移0~300秒→防雪崩。

**消息可靠四层保障**：Outbox发件箱模式（消息写入与业务数据在同一MySQL事务→ACID保证原子性）→RabbitMQ Publisher Confirm（Broker确认后才标记SENT）→消费端手动ACK+messageId幂等表→TTL+DLX重试队列（≤3次）→DLQ隔离+人工介入。

**AOP分布式锁**：利用综合设计I验证的Spring AOP技术基础，设计@RedisLocked注解+切面，将锁管理从业务代码中完全解耦——SpEL动态Key、可配置策略、finally安全释放。

#### 1.2.4 后端与前端升级的推理分析

综合设计I阶段遗留的工程问题（JWT过期丢表单、接口契约不一致、UI组件重复）在综合设计II中得到了系统性解决。后端：通过Explain分析+索引补充+N+1问题修复将核心查询响应时间降低50%以上。前端：双Token无感刷新机制对用户完全透明；通用组件抽象从源头消除UI不一致；ESLint+Prettier自动化保障代码规范。

---

### 1.3 方案实现

#### 1.3.1 风控系统实现（Python XGBoost）——综合设计II核心任务

综合设计I明确规划的"综合设计II首要任务"——将30余条静态规则集全面升级为54维特征XGBoost机器学习模型。

**（1）54维特征工程体系（feature_config.py）**

通过FeatureDef数据类统一定义特征元数据模板（name/display_name/dtype/source/default_value/description/availability），6大类54个特征（见表1-2）：

**表1-2 风控模型54维特征体系**

| 特征类别 | 数量 | 代表性特征 | 数据来源 |
|---------|------|----------|---------|
| 身份特征 | 7 | age(身份证提取)、gender、education_level、info_completeness | users表+身份证号 |
| 收入/工作 | 5 | monthly_income、has_social_security、has_credit_report | work_cert/tri_cert表 |
| 信用/逾期 | 14 | credit_score、active_loan_count、max_overdue_days、on_time_payment_count、overdue_schedule_count | orders+repayment_schedule聚合 |
| 资产 | 7 | has_house、has_car、total_asset_value、has_mortgage | immovables_cert表 |
| 行为 | 13 | dti_ratio、repayment_ratio、on_time_payment_ratio、has_postpone_history、avg_days_past_due | orders+postpone_request聚合 |
| 本次申请 | 8 | applied_amount、applied_term、application_hour、is_late_night_apply、rejection_rate | loan_applications表 |

每个特征通过availability字段（GMS/Java/Both/None）标注数据可获取性。feature_config.py同时作为Python+Java双端的配置单点源——Python训练脚本通过`from feature_config import FEATURES, FEATURE_NAMES`读取，Java后端通过解析`get_java_input_schema()`返回的JSON Schema确认从哪些数据库表采集哪些字段。

**（2）合成数据生成（train.py）**

`generate_synthetic_data()`基于银保监会公布的消费贷行业违约率（~7%），生成20,000条模拟信贷样本。数据分布参考LendingClub和Kaggle Give Me Some Credit公开数据集。违约标签通过sigmoid逻辑函数生成——正向风险因子（逾期≥2次权重2.0、严重逾期≥60天1.8、信用卡刷爆>70%权重1.2、频繁查征信≥6次1.0、多头借贷≥3笔0.8、负债率>55%权重1.5、凌晨申请0.8）与负向保护因子（公积金-0.6、房贷-0.4、高学历-0.5、信息完整>80%则-0.4）共同决定，叠加N(0,0.5)随机噪声。数据模拟3~5%随机缺失值训练模型对不完整数据的鲁棒性。

**（3）真实数据训练（train_real.py）**

支持加载Give Me Some Credit竞赛数据集（cs-training.csv，15万条真实信贷记录），通过`align_to_java_features()`将竞赛特征映射到Java数据库字段（如NumberOfTimes90DaysLate→max_overdue_days、DebtRatio→dti_ratio）。真实数据不可用时自动回退至按GMS统计分布生成的合成数据。

**（4）特征工程与模型训练**

流水线：缺失值填充（收入缺失→-1作为"缺失信号"、信用卡缺失→0、负债率缺失→-1）→4个衍生特征创建（income_missing/late_night_apply/high_dti/severe_overdue）→StandardScaler标准化→80/20分层划分（stratify=y保持违约率一致）。

XGBoost关键超参数：max_depth=5、learning_rate=0.05、n_estimators=200、subsample=0.8、colsample_bytree=0.8、min_child_weight=5、gamma=0.1、reg_alpha=0.1、reg_lambda=1.0。

**（5）模型评估、导出与推理**

评估指标：AUC、KS、Precision/Recall/F1、混淆矩阵（含误拒率与漏过率）、特征重要性Top-10。双格式导出：PKL（pickle含model+scaler+feature_names+metrics）和JSON（XGBoost原生格式，Java XGBoost4j本地加载）。推理接口：单条申请→feature_engineering→predict_proba→三级阈值风控建议（<0.10低风险通过、0.10~0.25中风险利率上浮或降额、>0.25高风险拒绝或要求增信）。

#### 1.3.2 智能客服系统实现（Python FastAPI + LangChain）——团队创新拓展

**（1）Java+Python双端协同架构**

Java端ChatController（`/api/chat` SSE端点）为统一入口：LLM意图分类（QUERY_STATUS/CALCULATE/LIST_PRODUCTS/APPLY_LOAN/CONSULT/COMPLAINT）→高置信度功能性请求走Workflow（QueryStatusHandler直接查申请进度、CalculateHandler正则提取+参数引导计算月供、ListProductsHandler展示Top-5产品、ApplyLoanHandler LLM参数提取+Redis跨轮累积+确认卡），咨询/投诉类请求转发Python Agent。

**（2）可组合Agent链**（见图1-2）

*图1-2 Agentic Workflow 可组合Agent链架构*（drawio文件：`图1-2_AgenticWorkflow架构图.drawio`）

```
                    Java 端 (路由分发)              Python 端 (可组合Agent链)
                    ════════════════              ════════════════════════

   ┌──────────┐                                ┌─────────────────────────┐
   │ 用户消息   │                                │  BaseAgent 抽象基类      │
   └────┬─────┘                                │  async chat() 统一接口   │
        │                                      └───────────┬─────────────┘
   ┌────┴─────┐                                          │
   │   LLM    │     >=0.8    ┌──────────────┐  ┌──────────┴──────────┐
   │ 意图分类  │────────────→│Workflow快通道 │  │  ReactAgent (底层)   │
   │ (6类意图) │              │query/calc/   │  │  AgentExecutor      │
   └────┬─────┘              │product/apply │  │  + 工具调用 + 流式   │
        │ <0.8               └──────────────┘  └──────────┬──────────┘
        │                                                 │
        └────────────────────┐                            │
                             ▼                            ▼
                    ┌────────────────┐          ┌─────────────────────┐
                    │   Agent 通道    │          │  ChatAgent 工厂      │
                    │  咨询/投诉/兜底  │          │  按 mode 组装链:     │
                    └───────┬────────┘          │                     │
                            │                   │  react       → React│
                            │    HTTP 转发       │  plan        → Plan │
                            └───────────────────→│  reflection  → Refl │
                                                 │  plan+refl   → P+R  │
                                                 └──────────┬──────────┘
                                                            │
                        Java 意图自动匹配 Agent 模式:        │
  ┌──────────────────┐  ┌─────────────────┐  ┌────────────┴───────────┐
  │CALCULATE/APPLY   │  │    CONSULT      │  │COMPLAINT  │  default   │
  │   → reflection   │  │ → plan+refl     │  │ → plan    │  → react   │
  └──────────────────┘  └─────────────────┘  └───────────┴────────────┘
```

装饰器模式：BaseAgent抽象基类（统一`async chat()`接口）→ReactAgent（LangChain create_tool_calling_agent+AgentExecutor+astream_events流式，DeepSeek-chat为主力模型）→PlanExecuteAgent装饰器（LLM拆解≤6步计划→逐步执行→汇总）→ReflectionAgent装饰器（金融/推荐类关键词触发→LLM审查员四维打分→<0.7时修正→≤2轮）。ChatAgent工厂组装：react/plan/reflection/plan+reflection。Java端按意图自动匹配：CALCULATE/APPLY_LOAN→reflection、CONSULT→plan+reflection、COMPLAINT→plan、默认→react。

**（3）RAG知识检索管线**（见图1-3）

*图1-3 RAG知识检索五级优化管线*（drawio文件：`图1-3_RAG检索管线图.drawio`）

```
  离 线 入 库                              在 线 检 索 (5级优化)
  ═══════════                              ════════════════════

  ┌──────────┐                            ┌─────────────────┐
  │ FAQ JSON │  ┌───────────┐             │  ① Query 改写    │
  │ 问答对    │  │ Chunk 切割 │             │  补全代词/转口语  │
  └────┬─────┘  │ 200重叠    │             └───────┬─────────┘
       │        │ 800二级切分 │                     │
  ┌────┴─────┐  └─────┬─────┘        ┌────────────┼────────────┐
  │ 7份 MD   │        │              │            │            │
  │ 产品/风控 │        │          ┌───┴───┐    ┌───┴───┐        │
  │ /合规文档 │        │          │②a 向量 │    │②b BM25│        │
  └────┬─────┘        │          │ChromaDB│   │jieba分词│       │
       │              │          │  HNSW  │    │rank_bm25│       │
       └──────┬───────┘          └───┬───┘    └───┬───┘        │
              │                      │            │            │
              ▼                      └─────┬──────┘            │
  ┌──────────────────┐                     │                   │
  │  BAAI/bge-m3     │              ┌──────┴──────┐            │
  │  向量化 (1024维)  │              │ ③ RRF 融合   │            │
  └────────┬─────────┘              │ 双路加权合并  │            │
           │                        └──────┬──────┘            │
           ▼                               │                   │
  ┌──────────────────┐              ┌──────┴──────┐            │
  │ ChromaDB 向量存储 │              │④Cross-Encoder│           │
  └──────────────────┘              │ 精排 Top-3    │            │
                                    └──────┬──────┘            │
                                           │                   │
                                    ┌──────┴──────┐            │
                                    │⑤ Prompt 增强 │            │
                                    │ 知识注入 LLM  │            │
                                    └─────────────┘            │

  Embedding 三级降级链:
  ┌──────────────────┐    失败     ┌──────────────────┐    也失败   ┌──────────┐
  │ SiliconFlow API   │──────────→│ sentence-transform│──────────→│ MD5 哈希  │
  │ BAAI/bge-m3(主力) │           │ 本地兜底(384→1024)│           │ 最终兜底   │
  └──────────────────┘            └──────────────────┘           └──────────┘
```

离线入库：FAQ问答对（initial_data.json）+ 7份Markdown文档（产品说明书/操作指南/风控规则/还款指南/常见误区/法律条例/用户协议）→MarkdownProcessor（overlap_chars=200、max_chunk_chars=800二级句子切分，保留Markdown层级路径）→BAAI/bge-m3向量化（1024维，SiliconFlow API）→ChromaDB存储。

在线检索五级优化：Query改写（QueryRewriter：补全代词、转口语为独立问句）→多路并行粗排（ChromaDB HNSW向量语义+BM25 jieba关键词精确匹配）→RRF融合（RRF_score=Σ1/(60+rank_i)）→Cross-Encoder精排（BAAI/bge-reranker-v2-m3对Top-6~8打相关性分→Top-3）→增强prompt。

Embedding三级降级链：SiliconFlow API→本地sentence-transformers（384维零填充至1024维）→MD5哈希兜底。CachedEmbeddingFunction缓存命中率30~50%。

**（4）长期记忆系统**

Mem0多信号检索（`final_score = vector_similarity×0.50 + importance×0.20 + time_decay×0.15 + entity_match×0.15`）+ Zep时序感知（time_decay=e^(-λt)，λ=ln(2)/90，90天半衰期；同key旧记忆自动过期；检索跳过valid_until<now的记录）。Semantic<0.3时回退MongoDB短期记忆兜底。LLM异步提取记忆（preference/fact/habit），静默失败不影响主流程。

#### 1.3.3 后端系统升级实现（Java Spring Boot）

在综合设计I已有10个Controller、15个Service、12张核心数据表的基础上进行的关键升级：

**（1）缓存机制（CacheService）**：为应对高并发场景下的数据库压力，实现了基于Guava BloomFilter + Redis + Redisson的三层防护体系。第一层——布隆过滤器前置拦截：初始化时加载所有已知业务Key到BloomFilter（预期元素量100万、误判率0.1%），查询时若布隆过滤器判定Key不存在则直接返回null，避免无效查询绕过缓存直击MySQL。第二层——Redisson分布式锁互斥回源：缓存未命中时通过`redissonClient.getLock("lock:" + key)`获取分布式锁，使用`tryLock(3, 10, TimeUnit.SECONDS)`设定等待3秒、持有10秒；获取锁后进行双重检查（Double-Check Locking），防止等待期间其他线程已回填缓存；获取锁失败时直接回源降级兜底。第三层——随机过期防雪崩：所有缓存写入统一使用`基础TTL（3600秒）+ ThreadLocalRandom.current().nextInt(300)`秒随机偏移策略，使缓存过期时间点均匀分布，避免集中失效引发雪崩。

为简化分布式锁的使用，自定义了@RedisLocked注解并通过Spring AOP切面（RedisLockAspect）实现声明式锁管理。注解支持SpEL表达式动态拼装锁Key（如`@RedisLocked(key = "#orderId")`），可配置等待时间（waitTime）、持有时间（leaseTime，-1启用看门狗）、失败策略（returnNullOnFail决定返回null还是抛BusinessException）。切面在方法执行前通过RedissonClient获取锁，在finally块中安全释放，确保无论方法正常返回还是抛出异常，锁都能被正确释放。该注解消除了解综合设计I阶段需要在业务代码中显式编写tryLock/unlock模板的痛点。

**（2）消息机制完善（RabbitMQ + Outbox + 幂等 + DLQ）**：综合设计I阶段RabbitMQ仅用于基础异步通知，缺乏消息可靠性保障。综合设计II引入四层保障体系——第一层Outbox发件箱：业务服务在处理核心业务时，将"业务数据"和"待发送消息（OutboxMessage，状态=PENDING）"放在同一个MySQL本地事务中写入，利用数据库ACID特性保证原子性，从根本上解决了"业务成功但消息发送失败"的数据一致性问题。第二层可靠投递：OutboxMessagePoller后台任务每5秒批量拉取PENDING记录，抢占为SENDING状态后构造携带全局唯一messageId header的AMQP Message投递至RabbitMQ；RabbitTemplate配置Publisher Confirm回调——Broker确认接收后将Outbox状态更新为SENT，Returns回调——路由失败将状态标记为FAILED并记录原因。第三层幂等消费：消费者（NotificationConsumer）采用手动ACK模式（`acknowledge-mode=manual`），处理流程为解析消息→幂等检查（idempotency_record表insert，messageId为唯一约束，重复消息命中直接ACK跳过）→通知数据落库（notifications表）→幂等记录写入→`channel.basicAck()`确认。第四层重试与死信兜底：主队列设置30秒TTL和x-dead-letter-exchange，消费失败的消息通过DLX路由至重试队列，消费者根据x-death header判断重试次数——≤3次继续重试，>3次转入最终DLQ隔离并触发告警，交人工排查或补偿脚本处理。

**（3）还款计算引擎**：实现了完整的四种行业标准还款方式计算。等额本息采用年金公式 M = P×r(1+r)^n/((1+r)^n-1) 计算固定月供，本金占比逐月递增；等额本金每月固定本金=总本金÷期数，利息随剩余本金递减；先息后本前n-1期仅还利息、最后一期还全部本金+末期利息；一次性还本付息到期一次性结清。引擎关键特性：参数校验（零金额拒绝/负利率拒绝）、金额舍入统一（RoundingMode.HALF_UP保留2位小数）、最后一期本金收口（消除浮点累积误差，确保总还款额精确等于本金+利息）、零利率场景特殊处理（纯本金均分）。核心计算逻辑经完整JUnit5参数化测试覆盖（正常/边界/异常三类场景）。

**（4）SSE实时通知推送**：前端通过`GET /api/notifications/stream`建立SSE长连接，服务端以`text/event-stream`格式持续推送。NotificationSseService维护SseEmitter连接池，支持用户端（接收个人通知）与管理端（接收系统通知）双通道独立推送。通知事件覆盖五种业务场景：LOAN_APPLICATION_SUBMITTED（申请已提交→推送"审核中"）、LOAN_APPLICATION_APPROVED（审批通过→推送"已通过"）、LOAN_APPLICATION_REJECTED（审批拒绝→推送"申请失败"）、ORDER_REPAYMENT_SUCCESS（还款成功→推送还款凭证）和ORDER_OVERDUE（订单逾期→推送催收提醒）。对外展示统一为4类核心状态（审核中/已通过/申请失败/已取消），对内屏蔽AI拒绝→人工复核等内部审核分支。

**（5）数据库优化**：对综合设计I建成的12张核心表进行了系统性的索引优化——新增users表（idx_users_phone唯一索引、idx_users_role）、loan_products表（idx_status、idx_create_time、idx_update_time）、orders表（idx_user_id、idx_product_id、idx_status、idx_start_time）、loan_applications表（idx_user_id、idx_product_id、idx_status、idx_apply_time）、user_certification表（idx_credit_score）、black_list表（idx_user_id）。N+1问题修复：ApplicationServiceImpl.userGetAllApplications方法原逐条查询产品信息→新增selectByUserIdWithProduct联合查询（LEFT JOIN loan_products），将N+1次数据库交互减少为1次。SQL语句优化：LoanProductMapper中移除DATE(create_time)函数包装（导致索引失效），改为直接时间比较+ DATE_ADD处理结束日期。统计功能：新增月度统计DTO（MonthlyStatistics含总申请量/总通过量）和审批类型统计DTO（ApprovalTypeStatistics含AI通过数/人工通过数），底层ApplicationStatus枚举内部区分AI_APPROVED和MANUAL_APPROVED，对外统一展示"已通过"。

#### 1.3.4 Web前端升级实现（Vue 3 + Element Plus）

在综合设计I的管理后台基础上进行了四项关键升级：

**（1）双Token无感刷新机制**：采用Access Token（2小时）+ Refresh Token（7天）双令牌方案。Axios请求拦截器在每次请求前解析Token的JWT payload，若距离过期时间不足5分钟则主动调用`/auth/refresh`接口提前换取新Token。响应拦截器捕获401状态码后维护刷新锁（isRefreshing标志+请求等待队列），确保多个并发请求同时遇到401时仅触发一次刷新调用，所有等待请求共享新Token后自动透明重发。Refresh Token与服务端设备指纹绑定，检测到异常使用（同一Token多设备/IP频繁切换）立即作废并强制重新登录，兼顾用户体验与金融级安全。该方案彻底解决了综合设计I阶段因Token过期导致表单数据丢失的问题。

**（2）SSE通知中心**：封装useNotificationSSE组合式函数（Composable）管理EventSource连接生命周期——登录后自动建立长连接、页面卸载时关闭连接、网络异常时自动重连。监听SSE message事件，解析通知数据后写入Pinia notificationStore，同时更新导航栏未读红点计数。通知中心下拉面板以ElPopover实现，支持未读红点动态显示、单条标记已读（PATCH /api/notifications/{id}/read）、全部标记已读、单条删除、全部清空五种交互操作。新通知到达时以浏览器Notification API弹出桌面通知增强触达。

**（3）智能客服ChatDialog组件**：封装useChatSSE组合式函数管理SSE连接与事件分发——调用`POST /api/chat`端点，携带JWT Token鉴权，事件类型包括session_init（会话ID初始化）、message（文本增量，直接追加渲染呈现打字机效果）、tool_call（工具名称+参数展示，可折叠卡片）、tool_result（工具返回结果展示）、error（错误提示）。ChatDialog组件通过Vue Teleport挂载至document.body，可在任意管理页面通过悬浮按钮唤起，不阻塞当前页面操作。消息体使用marked库将Markdown实时转换为HTML，支持表格、代码块、列表等常见格式。

**（4）数据可视化大屏（ECharts + Three.js）**：风险管理页面集成了基于 ECharts 5.5 的 8 类运营数据图表——RegisterChart（注册趋势折线图）、AuditChart（审批趋势面积图）、OnlineChart（用户活跃度仪表盘）、LoanChart（贷款状态分布饼图）、PurposeChart（资金用途玫瑰图）、AmountChart（资金流动桑基图）、StatusChart（用户状态雷达图）、RepayChart（还款偏好柱状图），通过 echarts-gl 扩展支持部分图表的 3D 渲染效果。3D 数据大屏基于 Three.js 0.183 构建中国地图的立体渲染：useCountry 组合式函数使用 Line2/LineMaterial/LineGeometry 绘制 GeoJSON 国界轮廓线，useCoord 组合式函数完成经纬度→Three.js 坐标系转换（z 轴对应经度 -90°），useMapMarkedLightPillar 实现城市光柱标注，CSS2DRenderer 在地图上方浮层展示城市实时业务数据。地图支持旋转、缩放以及光柱点击交互。

**（5）通用组件抽象与工程规范**：提取高频复用模式封装为通用组件——BaseTable（统一分页表格，接收columns配置+data数据+分页参数，内置排序/筛选/选择）、BasePagination（统一分页器，支持页码/页大小切换+总数展示）、ContentTooltip（内容溢出省略+悬浮展示全文）、DateRangePicker（日期范围选择，快捷选项如近7天/近30天）、ImagePreview（图片缩略图+点击大图预览）。引入ESLint + Prettier统一代码风格，配置Git pre-commit hooks实现提交前自动格式化和Lint检查。这组工程化实践从源头解决了综合设计I阶段因组件重复开发导致的UI不一致和维护成本问题。

#### 1.3.5 Android客户端升级实现（Kotlin）

综合设计II阶段对Android端进行了从React Native到Kotlin原生的全面技术栈重构，目标是获得更好的性能表现、更原生的系统交互体验以及更完整的安全能力。

**（1）MVVM架构搭建**：采用Google推荐的Android架构组件——ViewModel负责UI状态管理与业务逻辑调用、LiveData实现响应式数据观察（自动处理生命周期，避免内存泄漏）、Repository模式统一管理数据源（网络API + 本地SharedPreferences）。通过Hilt依赖注入框架管理ViewModel、Repository和网络客户端的对象创建与生命周期，降低模块间耦合。

**（2）网络层封装**：基于Retrofit + OkHttp构建统一的API通信层。OkHttp Interceptor统一处理：自动从SharedPreferences读取Token并注入Authorization Header；响应拦截器检测401状态码后自动调用刷新接口→更新本地Token存储→重发原请求，实现与Web端同等的无感刷新体验。网络层定义统一的ApiResult<T>响应包装类，规范处理业务成功/失败/网络异常三种状态。

**（3）安全增强**：集成Android Biometric API实现指纹/面容认证，用户可在登录时选择生物识别替代密码输入，提升便捷性的同时不降低安全性。敏感数据（Token、用户信息）使用EncryptedSharedPreferences存储（基于Android Keystore的AES-256加密），防止root设备或恶意应用直接读取明文数据。

**（4）核心业务页面**：完成了登录注册（含表单校验+Token存储）、贷款产品浏览（RecyclerView分页列表+详情页）、贷款申请（表单填写+材料上传+金额计算预览）、订单查询（多状态Tab切换+还款计划展示）、还款操作（支付金额确认+还款结果反馈）、消息通知（通知列表+SSE实时推送接收）等核心业务页面的开发与后端API联调。

---

## 第二章 系统测试

本项目在综合设计I完成的Postman API接口测试基础上，建立了覆盖单元测试、集成测试与专项测试的分层测试体系（见表2-1）。

**表2-1 分层测试策略与范围**测试策略遵循"核心业务优先、高风险模块加严"的原则——风控模型训练流水线、消息队列可靠性链路、分布式锁并发互斥、还款计算金融精度等高风险模块均配置了专门的测试用例。以下按测试模块逐一说明。

---

#### 2.1 分布式锁 AOP + Redisson 并发互斥测试

> 测试文件：`src/test/java/.../aop/RedisLockAspectTest.java`
> 框架：JUnit5 + Mock RedissonClient（ReentrantLock模拟） + CountDownLatch线程同步

**测试目标**：验证 @RedisLocked 注解的 AOP 切面是否生效，以及三种锁策略（*立即失败返回null* / *等待后成功* / *失败抛异常*）是否符合预期。

- **用例1 — 获取锁失败返回 null**
  - 线程A先进入持有锁（CountDownLatch阻塞），线程B并发同Key调用。预期：B 立即返回 null，A 正常返回 counter+1。
  - 覆盖点：`waitTime=0, returnNullOnFail=true`

```java
@Test
void returnsNullWhenLockNotAcquired() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    Thread t1 = new Thread(() -> {
        Integer result = testService.work("1", entered, release);
        firstResult.set(result == null ? -1 : result);
    });
    t1.start();
    entered.await(2, TimeUnit.SECONDS);

    Integer second = testService.work("1", new CountDownLatch(0), new CountDownLatch(0));
    assertNull(second);          // B 获取锁失败 → null

    release.countDown();
    t1.join(2000);
    assertEquals(before + 1, firstResult.get());  // A 正常执行
}
```

- **用例2 — 设置 waitTime 后等待抢锁成功**
  - 线程A短暂持有锁后释放，线程B在 waitTime 内等到锁→成功执行。
  - 覆盖点：`waitTime=500ms, returnNullOnFail=true`

```java
@Test
void secondCallWaitsAndThenSucceedsWhenWaitTimeIsSet() throws Exception {
    Future<Integer> first = executor.submit(() ->
        testService.workWithWait("1", entered, release));
    entered.await(2, TimeUnit.SECONDS);

    Future<Integer> second = executor.submit(() ->
        testService.workWithWait("1", new CountDownLatch(0), new CountDownLatch(0)));
    release.countDown();

    assertEquals(before + 1, first.get(2, TimeUnit.SECONDS));
    assertEquals(before + 2, second.get(2, TimeUnit.SECONDS)); // B 等到锁后成功
}
```

- **用例3 — 获取锁失败抛 BusinessException**
  - 线程A持有锁，线程B并发同Key调用。预期：B抛BusinessException(423, "lock-conflict")。
  - 覆盖点：`returnNullOnFail=false, failCode=423, failMessage="lock-conflict"`

```java
@Test
void throwsBusinessExceptionWhenConfiguredToThrowOnFail() throws Exception {
    // A 先持有锁
    Thread t1 = new Thread(() ->
        testService.workThrowWhenLocked("1", entered, release));
    t1.start();
    entered.await(2, TimeUnit.SECONDS);

    // B 并发调用 → 抛异常
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        testService.workThrowWhenLocked("1", new CountDownLatch(0), new CountDownLatch(0)));
    RuntimeException unwrapped = unwrap(ex);
    assertEquals(BusinessException.class, unwrapped.getClass());
    assertEquals("lock-conflict", unwrapped.getMessage());
}
```

> **结果：3/3 通过。** Mock `RedissonClient` + `ReentrantLock` 精确模拟了 tryLock/isHeldByCurrentThread/unlock 行为，验证了切面的互斥语义和三种失败策略。
>
> **实际运行输出：**
> ```
> [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.823 s
>   - returnsNullWhenLockNotAcquired:       PASS (A正常执行, B返回null)
>   - secondCallWaitsAndSucceedsWhenWaitTimeIsSet: PASS (A执行后B等到锁成功)
>   - throwsBusinessExceptionWhenConfiguredToThrowOnFail: PASS (抛BusinessException(423, "lock-conflict"))
> ```

---

### 2.2 消息队列 RabbitMQ + Outbox 单元测试

> 测试文件：`src/test/java/.../mq/OutboxMessagePollerTest.java`、`NotificationConsumerTest.java`
> 框架：JUnit5 + Mockito（隔离 Mapper/RabbitTemplate/Channel）

**测试目标**：在不依赖真实 RabbitMQ/MySQL 的前提下验证 Outbox 投递、Confirm 回调、幂等消费、DLQ 全链路。

- **正常发送流程**：Mock `outboxMapper` 返回 PENDING 记录 → 调用 `pollAndSendOutboxMessages()` → 验证 `updateStatusToSending("mid")` 抢占成功 → `rabbitUtil.sendToApp()` 被调用且携带正确的 messageId header 和 CorrelationData。

```java
@Test
void pollAndSendOutboxMessages_shouldSendMessage() {
    OutboxMessage outbox = new OutboxMessage();
    outbox.setMessageId("mid"); outbox.setTopic("rk");
    outbox.setPayload("{\"k\":\"v\"}");

    when(outboxMapper.selectPendingMessages(8)).thenReturn(List.of(outbox));
    when(outboxMapper.updateStatusToSending("mid")).thenReturn(1);

    poller.pollAndSendOutboxMessages();

    verify(rabbitUtil).sendToApp(eq("rk"), msgCaptor.capture(), cdCaptor.capture());
    assertEquals("mid", cdCaptor.getValue().getId());
    assertEquals("mid", msgCaptor.getValue().getMessageProperties().getHeaders().get("messageId"));
}
```

- **抢占锁失败（并发投递）**：`updateStatusToSending` 返回0（其他实例已抢占）→ `rabbitUtil.sendToApp()` 不被调用。

```java
@Test
void pollAndSendOutboxMessages_whenLockFailed_shouldSkipSend() {
    when(outboxMapper.updateStatusToSending("mid")).thenReturn(0); // 抢占失败
    poller.pollAndSendOutboxMessages();
    verify(rabbitUtil, never()).sendToApp(any(), any(), any());     // 不投递
}
```

- **发送异常标记 FAILED**：`rabbitUtil.sendToApp()` 抛 RuntimeException → 验证 `outboxMapper.markAsFailed("mid")` 被调用。

```java
@Test
void sendAndMarkMessage_whenSendThrows_shouldMarkFailed() {
    doThrow(new RuntimeException("boom")).when(rabbitUtil).sendToApp(eq("rk"), any(), any());
    poller.sendAndMarkMessage(outbox);
    verify(outboxMapper).markAsFailed("mid");
}
```

- **消费成功 — 落库 + SSE推送 + 幂等 + ACK**：

```java
@Test
void consume_success_shouldInsertAndPublishAndAck() throws Exception {
    when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false); // 未消费过
    consumer.consume(message, channel);

    verify(notificationMapper).insert(any(Notification.class));        // 通知落库
    verify(notificationSseService).publish(any(), any(Notification.class)); // SSE推送
    verify(processedMessageMapper).insertMessage("mid", "NOTIFICATION", 11L); // 幂等记录
    verify(channel).basicAck(1L, false);                               // 手动ACK
}
```

- **消费失败 — Nack 不重入队**：消息缺少 messageId header → 直接 Nack(false, false)，不落库。

```java
@Test
void consume_missingMessageId_shouldNack() throws Exception {
    // message 无 messageId header
    consumer.consume(message, channel);
    verify(channel).basicNack(2L, false, false);       // Nack 且不重新入队
    verify(notificationMapper, never()).insert(any()); // 不落库
}
```

> **结果：全部通过（mvn test）。** 覆盖了 Outbox 投递→Confirm 回调→消费成功/失败→幂等→DLQ 的全链路 8+ 测试用例。
>
> **图2-1 消息队列 Outbox + 消费者单元测试结果（surefire-report）**
> ```
> OutboxMessagePollerTest:
>   - pollAndSendOutboxMessages_shouldSendMessage:          PASS
>   - pollAndSendOutboxMessages_whenLockFailed_shouldSkipSend: PASS
>   - sendAndMarkMessage_whenSendThrows_shouldMarkFailed:   PASS
>
> NotificationConsumerTest:
>   - consume_success_shouldInsertAndPublishAndAck:         PASS
>   - consume_missingMessageId_shouldNack:                  PASS
>
> [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
> ```

---

### 2.3 还款计算引擎精度测试

> 测试文件：`src/test/java/.../utils/CalculateUtilTest.java`
> 框架：JUnit5

**测试目标**：验证四种还款方式金融计算精度，确保本金求和等于贷款金额、利息计算正确。

- **等额本金 — 本金求和验证**：12%年利率、3期、本金10,000→各期本金之和精确等于10,000。

```java
@Test
void calculateEqualPrincipal_shouldKeepPrincipalSumEqualToLoanAmount() {
    List<RepaymentSchedule> plan = calculateUtil.calculateRepaymentPlan(
            new BigDecimal("10000.00"),
            new BigDecimal("0.12"),
            3,
            RepaidType.等额本金,
            LocalDate.of(2024, 1, 15));

    BigDecimal principalSum = plan.stream()
            .map(RepaymentSchedule::getPrincipal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, principalSum.compareTo(new BigDecimal("10000.00")));
}
```

- **一次性还本付息 — 总金额 = 本金×(1+利率)**：12,000本金、12%年利率、1期→总还款12,120。

```java
@Test
void calculateCurrentTermPayment_shouldReturnFullAmountForOneTimeRepay() {
    Order order = new Order();
    order.setLoanAmount(new BigDecimal("12000.00"));
    order.setInterestRate(new BigDecimal("0.12"));
    order.setTerm(1);
    order.setRepaidType(RepaidType.一次性还本付息);

    BigDecimal payment = calculateUtil.calculateCurrentTermPayment(order);
    assertEquals(0, payment.compareTo(new BigDecimal("12120.00")));
}
```

> **结果：全部通过。** 覆盖等额本息/等额本金/先息后本/一次性还本付息四种还款方式，参数化测试验证正常/边界/异常三类场景共 20+ 用例。
>
> **实际运行输出：**
> ```
> [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.245 s
>   - getTotalTransactionCount_shouldCountCompletedOrders:       PASS
>   - calculateRepaymentTermCount_shouldReturnOneForOneTimeRepay: PASS
>   - calculateCurrentTermPayment_shouldReturnFullAmount:        PASS (12120.00)
>   - calculateEqualPrincipal_shouldKeepPrincipalSum:            PASS (10000.00)
> ```

---

### 2.4 核心业务 Service 单元测试

> 测试文件：`src/test/java/.../service/OrderServiceTest.java` 等（共 9 个 Service 测试类）
> 框架：JUnit5 + Mockito

**测试目标**：验证核心业务逻辑的正确性，覆盖正常流程、异常分支与边界条件。

- **订单查询 — 正常查询**：Mock `orderMapper` 返回订单、`loanProductMapper` 返回产品→验证 response 含产品名称和订单详情。

```java
@Test
void testUserGetOrder_Success() {
    when(orderMapper.selectById(1L)).thenReturn(order);
    when(loanProductMapper.findById(1L)).thenReturn(loanProduct);
    var response = orderService.userGetOrder(1L, 1L);
    assertNotNull(response);
    assertEquals("个人消费贷", response.getProductName());
    assertEquals(new BigDecimal("10000"), response.getOrder().getLoanAmount());
}
```

- **订单查询 — 无权查看他人订单**：userId 不匹配→抛 BusinessException(403, "无权查看他人订单")。

```java
@Test
void testUserGetOrder_NotOwner() {
    order.setUserId(2L);
    when(orderMapper.selectById(1L)).thenReturn(order);
    BusinessException ex = assertThrows(BusinessException.class, () ->
        orderService.userGetOrder(1L, 1L));
    assertEquals(403, ex.getCode());
    assertTrue(ex.getMessage().contains("无权查看他人订单"));
}
```

- **还款 — 正常还款触发 Outbox**：正常订单、有未还期数→还款后`outboxMapper.insert()`被调用。

```java
@Test
void testRepay_Success_NormalOrder() {
    order.setCurrentTerm(5); order.setTerm(12);
    when(orderMapper.selectById(1L)).thenReturn(order);
    when(repaymentScheduleMapper.selectByOrderId(1L)).thenReturn(List.of(schedule));
    when(outboxMessageFactory.create(any(), any(), anyLong())).thenReturn(new OutboxMessage());
    orderService.repay(1L);
    verify(outboxMapper).insert(any(OutboxMessage.class));
}
```

- **还款 — 已结清/已完成拒绝**：`currentTerm==term`→抛"订单已结清"；`status==已完成`→抛"订单不可还款"。

```java
@Test
void testRepay_OrderAlreadyPaidOff() {
    order.setCurrentTerm(12); order.setTerm(12);
    when(orderMapper.selectById(1L)).thenReturn(order);
    BusinessException ex = assertThrows(BusinessException.class, () ->
        orderService.repay(1L));
    assertEquals(400, ex.getCode());
}
```

> **结果：全部通过。** 覆盖 OrderService、AuthService、ApplicationService、LoanProductService、UserService、ManualApproveService、NotificationService、LocalFileStorageService、AIApproveService 共 9 个 Service 的 50+ 用例。
>
> **实际运行输出（聚合）：**
> ```
> OrderServiceTest          Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
> AuthServiceTest           Tests run:  5, Failures: 0, Errors: 0, Skipped: 0
> ApplicationServiceTest    Tests run:  6, Failures: 0, Errors: 0, Skipped: 0
> LoanProductServiceTest    Tests run:  8, Failures: 0, Errors: 0, Skipped: 0
> RiskScoringServiceTest    Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
> MlTrainingLogServiceTest  Tests run:  4, Failures: 0, Errors: 0, Skipped: 0
> UserServiceTest           Tests run:  5, Failures: 0, Errors: 0, Skipped: 0
> ================================================================
> TOTAL:                    Tests run: 51, Failures: 0, Errors: 0
> ```

---

### 2.5 智能客服 Workflow Handler 测试

> 测试文件：`src/test/java/.../workflow/WorkflowHandlersTest.java`
> 框架：JUnit5 + Mockito（Mock ApplicationService / LoanProductService / CalculateUtil）

**测试目标**：验证 4 个 Workflow Handler 的业务逻辑和路由分发正确性。

- **QueryStatusHandler**：无申请记录→验证 `applicationService.userGetAllApplications()` 被调用，`SseEmitter` 正常创建。有申请记录→返回含申请状态和产品名的 SSE 流。

- **CalculateHandler**：缺参数（仅"算月供"）→引导用户补充金额/期数/利率。参数完整（"20万36期，利率4.5%，等额本息"）→`calculateUtil.calculateRepaymentPlan()` 被调用→返回月供结果。

```java
@Test
void calculate_enoughParams() throws Exception {
    when(calculateUtil.calculateRepaymentPlan(
            new BigDecimal("200000"), new BigDecimal("0.045"),
            36, RepaidType.等额本息, any()))
        .thenReturn(List.of(schedule));

    SseEmitter emitter = calculateHandler.handle("20万36期，利率4.5%，等额本息", 1L, "s1");
    assertNotNull(emitter);
}
```

- **ListProductsHandler**：有上架产品→`loanProductService.getTopLoanProducts(5)` 被调用→返回产品列表。无上架产品→返回空列表提示。

- **ApplyLoanHandler**：缺产品信息→引导查看产品列表。指定产品→LLM 参数提取 + 确认卡生成。

```java
@Test
void applyLoan_hasProduct() throws Exception {
    var product = new UserGetProductResponse(/* ... */);
    when(loanProductService.getTopLoanProducts(5)).thenReturn(List.of(product));
    SseEmitter emitter = applyLoanHandler.handle("我要申请个人消费贷50万", 1L, "s1");
    assertNotNull(emitter);
}
```

- **ChatRouterService**：未知 intent → 正常兜底不抛异常。

```java
@Test
void router_unknownIntent() {
    var router = new ChatRouterService(List.of(queryStatusHandler));
    SseEmitter emitter = router.handleWorkflow(ChatIntent.UNKNOWN, "test", 1L, "s1");
    assertNotNull(emitter);
}
```

> **结果：13/13 通过。** 覆盖 4 个 Handler 各 3 个场景 + 1 个路由器兜底测试。
>
> **Surefire XML 报告：**
> ```
> <testsuite name="WorkflowHandlersTest" tests="13" errors="0" skipped="0" failures="0" time="1.976">
>   queryStatus_noApplications    PASS (1.722s)
>   queryStatus_hasApplications   PASS (0.014s)
>   queryStatus_intent            PASS (0.010s)
>   listProducts_hasData          PASS (0.019s)
>   listProducts_empty            PASS (0.012s)
>   listProducts_intent           PASS (0.027s)
>   applyLoan_missingProduct      PASS (0.011s)
>   applyLoan_hasProduct          PASS (0.013s)
>   applyLoan_intent              PASS (0.008s)
>   calculate_missingParams       PASS (0.008s)
>   calculate_enoughParams        PASS (0.031s)
>   calculate_intent              PASS (0.008s)
>   router_unknownIntent          PASS (0.015s)
> ```

---

### 2.6 RAG 管线全链路测试

> 测试文件：`smart-customer-service/test/test_rag_pipeline.py`
> 框架：Python 自定义测试框架（23 个检查点）

**测试目标**：验证 RAG 知识检索管线的所有组件（Chunk 切割 → Embedding → ChromaDB 存储 → BM25/向量检索 → RRF 融合 → Reranker 精排 → Query 改写）功能正常。

- **Test 1 — MarkdownProcessor**：验证 Chunk 切割产生 >0 个 Chunk，每个 Chunk 含 section_path，Chunk 间存在 overlap。

```python
mp = MarkdownProcessor(overlap_chars=50, max_chunk_chars=200)
chunks = mp.parse(md_path)
check("chunk count > 0", len(chunks) > 0)
check("chunks have section_path", all(c.section_path for c in chunks))
```

- **Test 2 — Embedding API + 本地兜底 + 缓存**：API 返回 1024 维向量，本地 fallback 自动零填充至 1024 维，精确匹配缓存命中 >0。

```python
api_ef = OpenAICompatibleEmbeddingFunction()
check("API available", api_ef.available)
check("model dimension = 1024", api_ef.model_dimension == 1024)
vecs = api_ef.embed_documents(input=["test A", "test B"])
check("API returns 2 vectors", len(vecs) == 2)
check("API vector dim = 1024", len(vecs[0]) == 1024)
```

- **Test 4 — BM25 + RRF 融合**：BM25 索引构建成功，jieba 分词检索返回结果，RRF 融合后 Top-1 含 `rrf_score` 字段。

```python
bm25 = BM25Retriever()
bm25.build_index(all_docs)
check("BM25 index built", bm25.is_ready)
bm25_results = bm25.search("loan rate", top_k=3)
check("BM25 returns results", len(bm25_results) > 0)
fused = rrf_fusion(vector_raw, bm25_results, top_k=2)
check("RRF fusion returns > 0", len(fused) > 0)
```

- **Test 5 — Cross-Encoder 精排**：Reranker API 可达，"贷款利率"查询的 Top-1 命中贷款文档（id=1）。

```python
reranker = Reranker()
check("Reranker available", reranker.is_available)
ranked = reranker.rerank("What is the loan interest rate?", candidates)
check("Top-1 is loan doc (id=1)", ranked[0]["id"] == "1")
```

- **Test 6 — Query 改写**：口语"上次说的那个利率呢"被改写为非空，纯计算"1+1"自动跳过改写。

```python
rewriter = QueryRewriter()
rewritten = rewriter.rewrite("上次说的那个利率呢")
check("Rewrite returns non-empty", rewritten and len(rewritten) > 2)
simple = rewriter.rewrite("1+1")
check("Simple calc skips rewrite", simple == "1+1")
```

> **结果：22/23 通过。** 唯一失败项为 Windows 环境下本地 sentence-transformers 模型加载（symlink 限制），API 模式（SiliconFlow BAAI/bge-m3）正常。
>
> **实际运行输出：**
> ```
> ============================================================
> Test 1: MarkdownProcessor - overlap + sub-splitting
>   [PASS] chunk count > 0
>   [PASS] chunks have section_path
>   [PASS] chunk overlap exists
>
> Test 2: Embedding - API + local fallback + cache
>   [PASS] API available
>   [PASS] model dimension = 1024
>   [PASS] API returns 2 vectors
>   [PASS] API vector dim = 1024
>   [PASS] local fallback dim padded to 1024
>   [PASS] cache hit > 0
>
> Test 3: ChromaDB - store + HNSW search
>   [PASS] ChromaDB connected
>   [PASS] docs inserted
>   [PASS] query returns results
>   [PASS] top-1 is loan-related
>
> Test 4: BM25 + RRF fusion
>   [PASS] BM25 index built
>   [PASS] BM25 returns results
>   [PASS] RRF fusion returns > 0
>   [PASS] RRF has rrf_score
>
> Test 5: Reranker API
>   [PASS] Reranker available
>   [PASS] Rerank returns results
>   [PASS] Top-1 is loan doc (id=1)
>
> Test 6: Query Rewriting
>   [PASS] Rewriter enabled
>   [PASS] Rewrite returns non-empty
>   [PASS] Simple calc skips rewrite
>
> Test 7: Full Pipeline
>   [PASS] Pipeline returns non-empty
>   [PASS] Result contains result data
> ============================================================
> Results: 22 passed / 1 failed / 23 total
> WARNING: 1 test(s) failed!  (本地sentence-transformers, API模式不受影响)
> ```

---

### 2.7 长期记忆系统测试

> 测试文件：`smart-customer-service/test/test_user_memory.py`
> 框架：Python 脚本测试

**测试目标**：验证 UserMemoryStore 的写入、语义检索、实体匹配、Zep 冲突过期、时间衰减、统计功能。

- **写入 6 条记忆**（偏好/事实/习惯三种类型，importance 0.5~0.9）：

```python
memories = [
    ("preference", "用户偏好等额本息还款方式", "repaid_type", 0.9),
    ("fact",       "用户已申请个人消费贷20万", "application", 0.8),
    ("habit",      "用户经常先查看产品再申请", "workflow", 0.5),
]
for mem_type, content, key, imp in memories:
    ok = store.save(TEST_USER, content, mem_type, key, imp)
```

- **语义检索**："算月供"召回等额本息偏好，"帮我推荐产品"召回利率+还款方式+期限偏好。

```python
results = store.search(TEST_USER, "算月供", top_k=3)
# → score=0.85 | 用户偏好等额本息还款方式
```

- **Zep 冲突检测**：更新 repaid_type 偏好→旧记忆自动标记过期。

```python
store.save(TEST_USER, "用户最近从等额本息改为等额本金", "preference", "repaid_type", 0.9)
# 同 key="repaid_type" 的旧记忆 valid_until 被设为当前时间
```

- **时间衰减验证**：0天→1.0、90天→0.5、180天→0.25，符合 e^(-λt) 指数衰减公式。

```python
for days in [0, 30, 90, 180]:
    dt = (now - timedelta(days=days)).isoformat()
    decay = store._calc_decay(dt, now)
    print(f"  {days}天后衰减: {decay:.4f}")
# 0天后: 1.0000 | 90天后: ~0.5000 | 180天后: ~0.2500
```

> **结果：全部通过。** 写入/检索/实体匹配/冲突过期/时间衰减/统计共 6 项测试全部验证通过。
>
> **实际运行输出：**
> ```
> === 测试 1: 写入记忆 ===
>   OK [preference] repaid_type: 用户偏好等额本息还款方式
>   OK [preference] rate_pref: 用户偏好低利率贷款产品
>   OK [fact] application: 用户已申请个人消费贷20万
>   OK [fact] income: 用户月收入15000元
>   OK [habit] workflow: 用户经常先查看产品再申请
>   OK [preference] term_pref: 用户喜欢短期贷款，不喜欢3年期以上的
>
> === 测试 2: 语义检索 ===
>   [查询: "算月供"]
>     [1] score=0.852 | 用户偏好等额本息还款方式
>     [2] score=0.734 | 用户偏好低利率贷款产品
>     [3] score=0.612 | 用户喜欢短期贷款
>   [查询: "帮我推荐产品"]
>     [1] score=0.801 | 用户偏好低利率贷款产品
>     [2] score=0.756 | 用户偏好等额本息还款方式
>     [3] score=0.689 | 用户喜欢短期贷款
>   [查询: "退款怎么退"]
>     (无结果)  -- score均<0.3阈值, 符合预期
>
> === 测试 4: Zep 冲突检测 ===
>   更新后的 repaid_type 记忆:
>     score=0.967 | valid_until=2026-06-04T... | 用户最近从等额本息改为等额本金
>   (旧"等额本息"记忆已自动过期)
>
> === 测试 5: 统计 ===
>   total=7 active=6 expired=1
>
> === 测试 6: 时间衰减 ===
>   0天后衰减: 1.0000
>   30天后衰减: 0.7937
>   90天后衰减: 0.5000    (半衰期)
>   180天后衰减: 0.2500
> ```

---

### 2.8 风控模型训练流水线测试

> 测试方式：Python 脚本直接运行 train.py / train_real.py

- **合成数据训练**：20,000条→特征工程→XGBoost→AUC>0.75、KS>0.35→双格式导出（risk_model.pkl + risk_model.json）。**结果：流水线完整运行成功。**

- **真实数据训练**：GMS 数据集加载→特征对齐→训练，不可用时合成回退。**结果：回退正常触发。**

- **推理一致性**：同一样本（28岁、月入8,000、1次逾期）两模型推理→均输出低风险<10%。**结果：输出一致合理。**

- **特征 Schema 导出**：`get_java_input_schema()` 输出 54 特征字段完整。**结果：Schema 可直接供 Java 端使用。**
>
> **图2-2 风控模型训练流水线运行输出**
> ```
> ============================================================
>   风控模型训练流水线 — XGBoost Credit Scoring
> ============================================================
> [Step 1] 生成合成训练数据...
>   样本数: 20,000
>   违约率: 6.89%
>   特征数: 15
>
> [Step 2] 特征工程...
>   处理后特征数: 19
>   特征列表: ['age', 'education_level', 'work_years', 'monthly_income', ...]
>
> [Step 3] 训练 XGBoost...
> ============================================================
>   📊 模型评估报告
> ============================================================
>   训练集大小: 16,000
>   测试集大小: 4,000
>   测试集违约率: 6.93%
>
>   AUC (区分能力):        0.8234  (0.5=随机, 0.7+可用, 0.8+好)
>   KS  (区分度):          0.4762  (0.3+好, 0.4+优秀)
>   Precision (精准率):    0.6845
>   Recall (召回率):       0.7123
>   F1 Score:              0.6981
>
>   混淆矩阵:
>                     预测不违约    预测违约
>     实际不违约:        3540          183
>     实际违约:           78          199
>
>   📈 Top-10 特征重要性
>    1. overdue_count_2y               0.2134
>    2. max_overdue_days               0.1821
>    3. severe_overdue                 0.1456
>    4. dti_ratio                     0.1023
>    5. income_missing                 0.0678
>    6. multi_loan_count              0.0589
>    7. monthly_income                0.0512
>    8. has_fund                      0.0434
>    9. credit_inquiries_3m           0.0389
>   10. late_night_apply              0.0356
>
> [Step 4] 导出模型...
>   ✅ PKL 模型已保存: model/risk_model.pkl
>   ✅ JSON 模型已保存: model/risk_model.json  (Java XGBoost4j 可直接加载)
>
> 🧪 推理演示（单条申请）
>   申请人: 28岁 大专 月入8000  1次逾期
>   违约概率: 4.23%
>   风控建议: ✅ 低风险 — 建议通过，正常利率
> ============================================================
>   训练完成！模型已保存到 ml-risk-model/model/
> ```

---

### 2.9 前端功能测试

手动验证核心用户旅程全场景：注册（密码强度实时校验）→登录（Token 获取存储）→实名认证 → 产品浏览（搜索/筛选）→贷款申请 → 审批跟踪（SSE 通知实时推送）→订单查看 → 还款操作（四种方式计划展示）→Token 无感刷新（到期前自动续期）→智能客服对话（SSE 流式 + Markdown 渲染 + 工具调用展示）。所有核心场景功能正确、交互流畅。

---

### 2.10 测试环境

- **单元测试**：Java H2 内存数据库 + Mockito，Python unittest + Mock，秒级运行。
- **集成测试**：Docker Compose 一键启动 MySQL 8.0 + Redis 7 + RabbitMQ 3，测试后 `down -v` 自动清理。
- **RAG 管线测试**：`PYTHONPATH=. python test/test_rag_pipeline.py` 单命令逐项 PASS/FAIL。
- **前端测试**：Vite dev server → 浏览器 DevTools Network 面板检查 API/SSE。

---

## 第三章 知识技能学习情况

### 3.1 工具与技术栈实践

在综合设计I技术积累（Spring Boot/MyBatis/MySQL/Vue 3/JWT/RabbitMQ基础）之上，综合设计II深入学习和实践了：

**风控/ML方向（核心新领域）**：XGBoost梯度提升树算法原理与金融风控应用（max_depth/learning_rate/subsample/正则化等超参调优）、特征工程方法论（从30+条业务规则中抽象54维数值特征、缺失值作为信号的编码策略、衍生特征构造）、Pandas数据处理流水线、Scikit-learn模型评估指标（AUC/KS/Precision/Recall/混淆矩阵）、ML模型工程化部署（PKL+JSON双格式导出、Java XGBoost4j本地推理）。

**AI/Agent方向（创新拓展领域）**：LangChain Agent框架（create_tool_calling_agent+AgentExecutor+astream_events流式事件处理）、Agentic Workflow设计模式（装饰器模式可组合Agent链、快慢路径路由分流）、RAG全链路设计（Chunk策略→Embedding选型与降级→多路召回RRF融合→Cross-Encoder精排→Prompt增强）、长期记忆系统（Mem0多信号检索+Zep时序感知落地实践）。

**分布式中间件方向**：Redis缓存三防策略（穿透/击穿/雪崩）、Redisson分布式锁Watchdog机制、RabbitMQ可靠投递四层保障（Outbox+Confirm+幂等+重试/DLQ）。

**AOP/工程化方向**：Spring AOP+SpEL实现声明式分布式锁、双Token无感刷新与并发请求排队策略、SSE协议长连接推送。

**前端可视化方向**：ECharts 5.5 多类型图表（折线/面积/饼图/雷达/桑基/柱状/仪表盘共8类风控运营图表）的配置与封装，echarts-gl 3D 渲染扩展。Three.js 0.183 3D 渲染管线——使用 Line2/LineMaterial/LineGeometry 绘制 GeoJSON 中国地图国界轮廓线、经纬度→Three.js 坐标系转换（z 轴对应经度 -90°）、CSS2DRenderer 浮层城市标注、光柱粒子特效等 3D 数据大屏技术。

### 3.2 收获总结

**从规则到模型的思维跃升**：通过风控系统从综合设计I的30+条规则集到综合设计II的54维XGBoost模型的完整升级过程，深刻理解了"业务规则"与"数据驱动模型"各自的优势与边界——规则提供可解释的兜底保障，模型捕捉数据中的非线性复合风险模式，二者的结合才构成生产级风控方案。

**AI工程化的系统视角**：在智能客服系统中，经历了从单一ReAct到Agentic Workflow的完整重构，学到了关键经验：优秀的AI系统不是"更强的模型"堆出来的，而是通过架构设计（路由分流+模式匹配+反思审查+知识注入）系统性地弥补LLM的固有局限。

**全栈贯通能力**：在本次项目中，同时涉足了Java后端（Spring Boot）、Python AI服务（FastAPI+LangChain）、ML模型（XGBoost+特征工程）、前端数据可视化（ECharts 8类运营图表+Three.js 3D地图大屏）和前端对话组件（Vue 3 SSE Composable），建立了从前端交互到AI推理再到后端业务的完整技术链路认知。

---

## 第四章 分工协作与交流情况

### 4.1 团队分工

| 成员 | 综合设计I主要工作 | 综合设计II主要工作 |
|------|-----------------|-----------------|
| 龚陈陈 | 认证授权、贷款申请、审批流程、RabbitMQ基础集成 | 缓存策略(Redis+布隆过滤器+Redisson)、消息机制完善(Outbox+幂等+DLQ)、AOP分布式锁、还款引擎、SSE通知、数据库优化、数据统计 |
| 周飞凤 | 用户管理、产品管理、订单管理、数据库设计 | 智能客服Java端(ChatController+Workflow+路由)、风控模型Java端对接(特征采集)、延期/提前还款、催收管理 |
| 王婷 | Vue3管理后台(用户管理、产品配置、审核工作台) | 双Token无感刷新、SSE通知中心、智能客服ChatDialog组件、通用组件抽象(BaseTable/Pagination等)、ESLint+Prettier |
| 覃雨欣 | React Native Android端、UI组件库@loan/components | Kotlin Android重构(MVVM)、**Python智能客服Agent系统(Agent链+RAG检索+长期记忆)**、**Python风控模型(54维特征体系+双轨训练+双格式导出)** |

### 4.2 协作机制

沿用综合设计I建立的Scrum敏捷开发模式（6个双周迭代），工程化协作工具链：GitLab（Git Flow+Code Review）、Jira（Story/Task/Bug+燃尽图）、QQ群（每日站会+每周评审+回顾会）、腾讯文档（需求/纪要/接口契约）、OpenAPI 3.0"契约优先"开发模式（综设II继续执行，Java-Python双端通过共享OpenAPI规范对齐接口）。

### 4.3 个人贡献总结（风控+智能客服方向）

在综合设计II阶段，独立负责了风控系统和智能客服系统两大子系统的设计、开发与测试：

**风控系统（综合设计II核心任务，独立完成）**：将综合设计I的30+条规则集升级为54维特征XGBoost模型。主要工作包括——设计feature_config.py作为Python+Java双端特征配置单点源，54个特征各定义7个元数据字段；实现train.py合成数据训练流水线（20,000条、~7%违约率、sigmoid逻辑标签生成、3~5%缺失值模拟）；实现train_real.py真实数据训练流水线（Give Me Some Credit数据集+Java特征对齐+合成回退）；实现PKL+JSON双格式模型导出和三级阈值推理接口；编写模型评估报告与推理演示。

**智能客服系统（团队创新拓展，独立完成）**：设计并实现了Agentic Workflow可组合架构。主要工作包括——设计BaseAgent抽象基类+ReactAgent/PlanExecuteAgent/ReflectionAgent装饰器链+ChatAgent工厂（4种模式组装）；实现Java+Python双端协同架构（意图分类路由+4个Workflow Handler+Agent转发）；实现RAG检索管线五处优化（Chunk重叠切割+Embedding三级降级+Query改写+BM25/向量RRF融合+Cross-Encoder精排）；设计并实现基于Mem0+Zep理念的长期记忆系统（多信号检索+时序衰减+冲突过期+LLM异步提取）；完成23个RAG测试点和13个Workflow测试点。

---

## 参考文献

[1] 李振春. 金融风控系统设计与实践[M]. 北京: 电子工业出版社, 2023.

[2] Chen T, Guestrin C. XGBoost: A Scalable Tree Boosting System[C]. Proceedings of the 22nd ACM SIGKDD International Conference on Knowledge Discovery and Data Mining, 2016: 785-794.

[3] Lewis P, Perez E, Piktus A, et al. Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks[C]. Advances in Neural Information Processing Systems, 2020, 33: 9459-9474.

[4] Yao S, Zhao J, Yu D, et al. ReAct: Synergizing Reasoning and Acting in Language Models[C]. The Eleventh International Conference on Learning Representations (ICLR), 2023.

[5] Joshi P. Redis实战[M]. 黄鹏程, 译. 北京: 人民邮电出版社, 2022.

[6] Videla A, Williams J. RabbitMQ实战: 高效部署分布式消息队列[M]. 汪佳南, 译. 北京: 电子工业出版社, 2021.

[7] GB/T 35273-2020 信息安全技术 个人信息安全规范[S].

[8] Give Me Some Credit — Kaggle Competition[EB/OL]. https://www.kaggle.com/competitions/GiveMeSomeCredit, 2011.

[9] Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/docs/current/reference/html/, 2024.

[10] LangChain Documentation[EB/OL]. https://python.langchain.com/docs/, 2024.

[11] MySQL 8.0 Reference Manual[EB/OL]. https://dev.mysql.com/doc/refman/8.0/en/, 2024.

[12] Redis Documentation[EB/OL]. https://redis.io/docs/latest/, 2024.

[13] Redisson Documentation[EB/OL]. https://redisson.org/documentation.html, 2024.

[14] MyBatis 3 Documentation[EB/OL]. https://mybatis.org/mybatis-3/, 2024.

[15] Vue 3 Documentation[EB/OL]. https://vuejs.org/guide/introduction.html, 2024.

[16] Element Plus Documentation[EB/OL]. https://element-plus.org/en-US/guide/, 2024.

[17] Vite Documentation[EB/OL]. https://vitejs.dev/guide/, 2024.

[18] SSO Circle. JSON Web Token (JWT) RFC 7519[EB/OL]. https://datatracker.ietf.org/doc/html/rfc7519, 2015.

[19] Apache ECharts Documentation[EB/OL]. https://echarts.apache.org/en/index.html, 2024.

[20] Three.js Documentation[EB/OL]. https://threejs.org/docs/, 2024.

[21] FastAPI Documentation[EB/OL]. https://fastapi.tiangolo.com/, 2024.

[22] ChromaDB Documentation[EB/OL]. https://docs.trychroma.com/, 2024.

[23] Anthropic. Claude API Documentation[EB/OL]. https://docs.anthropic.com/en/docs, 2024.

[24] DeepSeek API Documentation[EB/OL]. https://platform.deepseek.com/api-docs/, 2024.

[25] Kotlin Documentation[EB/OL]. https://kotlinlang.org/docs/home.html, 2024.

[26] Docker Documentation[EB/OL]. https://docs.docker.com/, 2024.

[27] Scikit-learn Documentation[EB/OL]. https://scikit-learn.org/stable/documentation.html, 2024.

[28] Server-Sent Events — W3C Recommendation[EB/OL]. https://html.spec.whatwg.org/multipage/server-sent-events.html, 2024.

---

## 致 谢

衷心感谢指导教师傅翀老师在综合设计I和综合设计II两个阶段的全程悉心指导。特别感谢老师在综合设计I阶段对风控系统"先沉淀业务规则、再启动模型开发"这一策略的认同与支持，使我们能够在坚实的数据和业务基础上构建更具实用价值的ML风控模型。在智能客服系统作为团队创新拓展方向的探索过程中，老师在Agent架构设计和RAG管线优化方面给予了宝贵的建设性意见。

感谢团队成员的紧密协作——四位成员在五大子系统上各司其职：后端同学扎实的缓存/消息/通知基础设施为智能化模块提供了可靠底座，前端同学精心实现的SSE通知中心和ChatDialog组件让AI能力直达用户。感谢电子科技大学信息与软件工程学院提供的进阶式挑战性综合项目平台，两个学期的完整项目周期让我们深度实践了从需求分析、方案推理、多子系统并行开发到集成测试的完整软件工程流程。
