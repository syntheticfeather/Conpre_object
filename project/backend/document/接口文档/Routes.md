# 路由总览

> 应用全局上下文路径：`/api`

## 认证 Auth
- POST `/api/auth/login` 登录
- POST `/api/auth/register` 注册

## 用户 Users
- GET `/api/users/me` 查看个人信息
- PATCH `/api/users/me` 更新个人信息
- POST `/api/users/blacklist/add` 管理员加入黑名单
- POST `/api/users/blacklist/remove?userId=...` 管理员移除黑名单
- GET `/api/users/blacklist/list` 黑名单列表
- GET `/api/users/admin/stats` 管理员用户统计列表
- GET `/api/users/admin/{userId}` 管理员查看指定用户详情
- GET `/api/users/search-by-credit?expr=...` 按信誉分搜索用户
- POST `/api/users/refresh-token` 刷新 token
- POST `/api/users` 新增用户（未使用）
- GET `/api/users` 全部用户（未使用）
- GET `/api/users/{id}` 查询用户（未使用）
- PATCH `/api/users/{id}` 更新用户（未使用）
- DELETE `/api/users/{id}` 删除用户（未使用）

## 贷款产品 Loan Products
- GET `/api/loan-products/user` 用户获取产品列表
- GET `/api/loan-products/user/search?name=...` 用户按名称搜索产品
- GET `/api/loan-products/admin` 管理端产品列表
- GET `/api/loan-products/admin/{productId}` 管理端查看单个产品
- POST `/api/loan-products/admin` 管理端创建产品
- PATCH `/api/loan-products/admin/products/{id}` 管理端更新产品
- DELETE `/api/loan-products/admin/products/{productId}` 管理端删除产品
- POST `/api/loan-products/admin/options/batch-create` 管理端批量创建选项
- DELETE `/api/loan-products/admin/options/{optionId}` 管理端删除单个选项
- POST `/api/loan-products/admin/options/batch-delete` 管理端批量删除选项
- POST `/api/loan-products/admin/products/batch-delete` 管理端批量删除产品

## 贷款申请 Loan Applications
- POST `/api/loan-applications` 用户提交申请
- GET `/api/loan-applications/my/{applicationId}` 用户查看单个申请
- GET `/api/loan-applications/my` 用户查看所有申请
- POST `/api/loan-applications/my/{applicationId}/withdraw` 用户撤回申请
- GET `/api/loan-applications/{applicationId}` 管理员查看单个申请详情
- GET `/api/loan-applications/user/{userId}` 管理员查看指定用户所有申请

## 人工审核 Manual Approval
- GET `/api/approval/pending` 管理员待审核列表
- GET `/api/approval/detail/{loanApplicationId}` 管理员查看申请详情
- POST `/api/approval/check` 管理员审核决策

## 订单 Orders
- GET `/api/orders/{orderId}` 用户查看订单
- GET `/api/orders/my` 用户查看所有订单
- POST `/api/orders/{orderId}/repay` 用户还款
- GET `/api/orders/admin/{orderId}` 管理员查看订单详情
- GET `/api/orders/admin/user/{userId}` 管理员查看某用户订单列表

## 模板视图 View Routes
- GET `/api/login` 显示登录页 `templates/login.html`
- GET `/api/registration` 显示注册页 `templates/registration.html`
- GET `/api/` 与 `/api/index` 显示首页 `templates/index.html`
- 不路由：`templates/test.html`、`templates/index copy.html`
