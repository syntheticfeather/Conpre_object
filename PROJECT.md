# Project Overview

Personal Loan Management System - A comprehensive loan application platform with:
- **Backend**: Java Spring Boot
- **Frontend**: Vue 3 Web App
- **Android**: Kotlin Android App (In Progress)
- **AI Risk Control**: Python + AI/ML (To be developed)

## Project Structure

```
project/
├── backend/           # Java Spring Boot backend
│   └── personal-loan/ # Main backend application
│       └── src/main/java/com/example/personal_loan/
│           ├── controller/   # REST API controllers (6个)
│           ├── service/      # Business logic (含impl实现类)
│           ├── mapper/       # MyBatis mappers
│           ├── entity/       # Data entities (12个)
│           ├── dto/          # Data transfer objects
│           ├── config/       # Configuration classes
│           ├── utils/        # Utility classes
│           ├── enums/        # Enumerations
│           ├── exception/    # Exception handling
│           └── handler/      # Interceptors and handlers
├── frontend/          # Vue 3 frontend
│   └── src/
│       ├── api/       # API request modules (6个)
│       ├── views/    # Page components (11个页面)
│       ├── components/ # Reusable components (14个组件)
│       ├── stores/   # Pinia state management
│       ├── router/   # Vue Router配置
│       └── utils/    # Utility functions
├── Android/           # Android App (Kotlin)
│   ├── demo.kt       # Login demo (XML + Activity)
│   └── document/     # Learning notes
│       ├── JWT前端部分学习.md
│       ├── note/
│       └── 原型.md
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Java, Spring Boot, MyBatis, MySQL, Redis, RabbitMQ, JWT |
| Frontend | Vue 3, Vite, Pinia, Axios, TypeScript |
| Android | Kotlin, XML Views, Retrofit, OkHttp (demo only) |
| AI/ML | Python, Scikit-learn, LangChain, FastAPI |

---

## Backend - Completed

### 1. AuthController (`/api/auth`)
- `POST /auth/login` - 密码登录
- `POST /auth/register` - 用户注册
- `POST /auth/submit-all` - 提交认证材料（身份证、银行卡、房产证明、车辆证明、工作证明、工资证明、社保证明、征信报告）
- `GET /auth/score` - 计算贷款信用分（满分750分）
- `GET /auth/cert-info` - 获取已上传的认证信息

### 2. LoanProductController (`/api/loan-products`)
- `GET /loan-products/user` - 用户获取所有产品
- `GET /loan-products/user/search` - 用户搜索产品
- `GET /loan-products/admin` - 管理员获取产品列表
- `POST /loan-products/admin` - 创建产品
- `PATCH /loan-products/admin/products/{id}` - 更新产品
- `DELETE /loan-products/admin/products/{productId}` - 删除产品
- `POST /loan-products/admin/{productId}/active` - 上架产品
- `POST /loan-products/admin/{productId}/deactive` - 下架产品
- `POST /loan-products/admin/options/batch-create` - 批量创建选项
- `DELETE /loan-products/admin/options/{optionId}` - 删除选项
- `GET /api/loan-products` - 按时间范围搜索产品

### 3. ApplicationController (`/api/loan-applications`)
- `POST /loan-applications` - 用户提交贷款申请
- `GET /loan-applications/my` - 用户查看所有申请
- `GET /loan-applications/my/{applicationId}` - 用户查看单个申请
- `POST /loan-applications/my/{applicationId}/withdraw` - 用户撤回申请
- `GET /loan-applications/{applicationId}` - 管理员查看申请
- `GET /loan-applications/user/{userId}` - 管理员查看用户所有申请

### 4. ManualApproveController (`/api/approval`)
- `GET /approval/pending` - 获取待审批列表
- `GET /approval/completed` - 获取已完成列表
- `GET /approval/detail/{loanApplicationId}` - 获取审批详情（含用户认证材料）
- `POST /approval/check` - 审批操作（批准/拒绝）

### 5. OrderController (`/api/orders`)
- `GET /orders/{orderId}` - 用户查看订单
- `GET /orders/my` - 用户查看所有订单
- `POST /orders/{orderId}/repay` - 用户还款

### 6. UserController (`/api/users`)
- `GET /users/me` - 用户查看自己信息
- `PATCH /users/me` - 用户更新信息
- `POST /users/avatar` - 上传头像
- `GET /users/stats` - 管理员获取用户统计
- `GET /users/{userId}/detail` - 管理员获取用户详情
- `POST /users/blacklist/add` - 加入黑名单
- `POST /users/blacklist/remove` - 移除黑名单
- `GET /users/blacklist/list` - 获取黑名单列表
- `GET /users/search-by-credit` - 按信用分搜索用户

### 7. NotificationController (`/api/notification`)
- `GET /notifications/my` - 获取用户通知列表
- `PUT /notifications/{id}/read` - 标记通知为已读
- `GET /notifications/stream` - SSE用户实时通知流
- `GET /notifications/admin` - 管理员获取所有通知
- `GET /notifications/admin/stream` - SSE管理员实时通知流

### Backend Services (Business Logic)

| Service | Description |
|---------|-------------|
| AuthService | 认证服务（登录、注册、Token刷新） |
| UserService | 用户管理服务 |
| LoanProductService | 贷款产品管理服务 |
| ApplicationService | 贷款申请服务 |
| OrderService | 订单与还款服务 |
| ManualApproveService | 人工审批服务 |
| ManualApproveSendService | 手动触发审批流程服务 |
| AIApproveService | AI智能审批服务（待开发） |
| PayService | 支付服务（待开发） |
| CacheService | 缓存服务（Redis） |
| LocalFileStorageService | 本地文件存储服务 |
| NotificationService | 通知服务 |
| NotificationSseService | SSE实时通知推送服务 |

### Data Entities

| Entity | Description |
|--------|-------------|
| User | 用户（id, userName, phone, password, role, avatar） |
| LoanProduct | 贷款产品（名称、描述、金额范围、期限范围、状态） |
| LoanOption | 产品选项（利率、期限等） |
| LoanApplication | 贷款申请（用户、产品、状态、金额、利率、期限） |
| Order | 贷款订单（状态、已还金额、逾期天数） |
| UserCert | 用户认证主表 |
| WorkCert | 工作证明（就业证明、工资证明） |
| TriCert | 第三方认证（社保、征信） |
| ImmovablesCert | 不动产证明（房产、车辆） |
| BlackUser | 黑名单用户 |
| OutboxMessage | 消息队列（RabbitMQ） |
| Notification | 通知（用户、类型、内容、状态） |

### Enumerations

| Enum | Description |
|------|-------------|
| ApplicationStatus | 申请状态（待审批、已通过、已拒绝、已撤回） |
| ProductStatus | 产品状态（上架/下架） |
| OrderStatus | 订单状态（正常、已逾期、已完成） |
| RepaidType | 还款方式（等额本息、等额本金、先息后本、一次性还清） |

---

## Frontend - Completed

### API Modules (`src/api/modules/`)
- `auth.js` - 认证相关（登录、注册、登出）
- `user.js` - 用户管理、黑名单
- `loan.js` - 贷款产品管理
- `loanApplication.js` - 贷款申请管理

### Page Views (`src/views/`)
| Path | Description |
|------|-------------|
| `/login` | 登录页 |
| `/register` | 注册页 |
| `/dashboard` | 管理后台首页 |
| `/dashboard/pending-applications` | 待审批列表 |
| `/dashboard/completed-applications` | 已完成审批列表 |
| `/dashboard/products` | 贷款产品管理 |
| `/dashboard/add-pro` | 添加产品 |
| `/dashboard/users` | 用户管理 |
| `/dashboard/black-users` | 黑名单管理 |
| `/dashboard/risk` | 风险管理 |
| `/dashboard/risk/collect` | 催收管理 |

### Reusable Components (`src/components/`)
- **shared**: BaseTable, BasePagination, DateRangePicker, ImagePreview
- **layout**: NavbarMenu, SidebarMenu
- **product**: ProductTable, ProductDetailPanel
- **user**: UserTable, UserDetailPanel, BlacklistTable
- **application-review**: PendingApplications, CompletedApplications, ApplicationDetailModal

### Security Features (Backend)
- JWT authentication (Access Token + Refresh Token)
- Password validation (大小写字母+数字+特殊字符，8-20位）
- ID card and bank card format validation
- File storage service
- CORS configuration
- Blacklist mechanism
- Redis caching
- RabbitMQ message queue

---

## Android App - In Progress

### Completed
- **Login Demo** (`demo.kt`)
  - XML布局的登录页面
  - Retrofit + OkHttp网络请求封装
  - SharedPreferences存储Token
  - Token自动携带与过期处理

- **Learning Documents**
  - JWT前端部分学习（完整的Token处理流程）
  - 原型设计文档
  - Kotlin基础学习笔记

### To Be Done
- 项目框架搭建（Jetpack Compose）
- MVVM架构
- Room数据库
- Hilt依赖注入
- 更多业务功能页面

---

## Progress Summary

### Completed Features
- [x] 用户认证系统（登录、注册、Token刷新）
- [x] 贷款产品管理（CRUD、上下架、批量选项）
- [x] 贷款申请流程（提交、查看、撤回）
- [x] 人工审批流程（待审批、已完成、详情）
- [x] 订单与还款管理
- [x] 用户信息管理（查看、更新、头像上传）
- [x] 黑名单管理（添加、移除、查询）
- [x] Redis缓存集成
- [x] RabbitMQ消息队列
- [x] 前端管理后台（11个页面）
- [x] 通知系统（实时推送、消息管理）

### In Progress
- [ ] AI智能审批模块（AIApproveService）
- [ ] 支付模块（PayService）
- [ ] Android App 完整业务功能

### To Be Developed
- [ ] Python风控系统
- [ ] 催收管理系统

---

## Common Commands

### Frontend
```bash
cd project/frontend
npm run dev
# Access at http://localhost:3000/
```

### Backend
Run `PersonalLoanApplication.java` in your IDE (IntelliJ IDEA recommended)

### Database
- MySQL: Default port 3306
- Redis: Default port 6379
- RabbitMQ: Default port 5672

---

## Important Notes

- All project code goes in `project/` directory
- Backend API prefix is `/api` (configured in Controller annotations)
- Frontend routes require absolute paths (e.g., `/login` not `/login.html`)
- Token is stored in localStorage as `admin_token`
- Public paths that don't require token: `/auth/login`, `/auth/register`, `/auth/logout`
- Android app is in early development stage (demo only)
