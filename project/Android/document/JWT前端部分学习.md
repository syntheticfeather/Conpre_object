好的，我们来系统地学习一下 JWT 在前端的部分。这将是一个从入门到实践的完整指南。

### 1. JWT简介

JWT（JSON Web Token）是一种开放标准，用于在网络应用环境间安全地传递信息。它通常用于**身份认证**和**信息交换**。
相当于一个令牌？
- 组成：
```
xxxxx.yyyyy.zzzzz
```
- **Header（头部）**：包含令牌类型和签名算法。
- **Payload（负载）**：包含需要传递的声明（信息），如用户ID、用户名、过期时间等。
- **Signature（签名）**：用于验证消息在传递过程中没有被篡改，由服务器使用秘钥生成。

### 2. JWT 在前端的工作流程（非常重要）

典型的登录流程如下：
1.  **登录**：用户在前端输入用户名和密码。
2.  **发送凭证**：前端通过 POST 请求将凭证发送到后端认证接口。
3.  **验证并生成 JWT**：后端验证凭证无误后，生成一个 JWT（通常包含用户ID和过期时间），并将其返回给前端。
4.  **前端存储 JWT**：前端收到 JWT 后，需要将其**安全地存储**起来（后面会详细讲存储方式）。
5.  **携带 JWT 发起后续请求**：在后续需要认证的 API 请求中，前端需要在 **HTTP 请求头**中附带这个 JWT。
    - 通常是 `Authorization` 头，值为 `Bearer <你的JWT令牌>`。
    ```
    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    ```
6.  **后端验证 JWT**：后端收到请求后，验证 JWT 的签名和有效性。如果有效，则处理请求并返回数据。
7.  **前端处理响应**：前端收到数据，渲染页面。

### 3. 前端核心操作

#### 3.1 存储 JWT
这是前端安全性的关键。主要有三种方式，各有优劣：

| 存储方式 | 优点 | 缺点 | 适用场景 |
| :--- | :--- | :--- | :--- |
| **LocalStorage** | 容量大，不会随请求发送，不易受 XSS | **易受 XSS 攻击**，令牌被窃取后无法挽回 | 需要较大存储空间，且对 XSS 有充分防护的应用 |
| **SessionStorage** | 同源标签页内有效，关闭标签页即清除，不易受 XSS | **易受 XSS 攻击**，页面刷新后不丢失 | 单次会话应用，希望用户关闭浏览器后自动退出 |
| **HttpOnly Cookie** | **非常安全**，免疫 XSS，可由服务端设置过期时间 | 容量小，**可能受到 CSRF 攻击**，前端 JS 无法直接操作 | **安全性要求高的首选方案**，需配合 CSRF 防护 |

**结论：**
- **对于大多数普通应用**，使用 **LocalStorage** 是比较简单直接的选择，但需要做好对 XSS 的防范（如对用户输入进行转义）。
- **对于安全性要求极高的应用（如金融）**，最好使用 **HttpOnly Cookie**。后端在 Set-Cookie 时设置 `HttpOnly` 和 `Secure`（仅 HTTPS）属性。此时，前端无需手动处理 Token，浏览器会自动在每次请求中携带 Cookie。

#### 3.2 发送 JWT

根据存储方式，发送方式也不同：
- **如果存在 LocalStorage/SessionStorage：**
  你需要手动从存储中取出 Token，并添加到请求的 `Authorization` 头中。
  ```javascript
  // 使用 Axios 的例子
  import axios from 'axios';

  // 创建一个 axios 实例，并设置请求拦截器
  const api = axios.create({
    baseURL: 'https://your-api.com'
  });

  // 请求拦截器：在每次请求发出前，自动加上 Token
  api.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('jwt_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // 现在使用 api 发起的任何请求都会自动携带 Token
  api.get('/user/profile').then(response => { ... });
  ```

- **如果使用 HttpOnly Cookie：**
  前端不做额外操作，后端需要配置好 CORS 的 `credentials`。
  ```javascript
  // 使用 Fetch API
  fetch('https://your-api.com/user/profile', {
    method: 'GET',
    credentials: 'include' // 重要！告诉浏览器要发送跨域 Cookie
  })

  // 使用 Axios
  const api = axios.create({
    baseURL: 'https://your-api.com',
    withCredentials: true // 重要！等同于 fetch 的 credentials: 'include'
  });
  ```

#### 3.3 处理 Token 过期

JWT 通常有一个过期时间。前端需要处理 Token 过期的情况。
1.  **主动检查**：在发送请求前，解码 JWT 的 Payload（这部分是 Base64Url 编码，可解码），检查 `exp` 字段是否已过期。
    ```javascript
    function isTokenExpired(token) {
      const payload = JSON.parse(atob(token.split('.')[1])); // 解码 Payload
      return payload.exp * 1000 < Date.now(); // exp 是秒，Date.now() 是毫秒
    }

    if (isTokenExpired(token)) {
      // 触发刷新令牌或退出登录
      logout();
    }
    ```
2.  **被动处理**：直接发送请求。如果后端返回 `401 Unauthorized` 状态码，说明 Token 无效或已过期。此时，前端：
    - **尝试刷新 Token**（如果实现了刷新令牌机制）。
    - **直接跳转到登录页**，让用户重新登录。

    ```javascript
    // Axios 响应拦截器
    api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // 触发刷新令牌逻辑
          // 或者清除本地 Token 并跳转到登录页
          localStorage.removeItem('jwt_token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
    ```

#### 3.4 退出登录（注销）：
1.  清除本地存储的 Token。
2.  清除应用状态（如 Vuex/Pinia/Redux 中的用户状态）。
3.  跳转到登录页。

### 4. 安全保障措施
1.  **永远使用 HTTPS**：防止 Token 在传输过程中被窃听。
2.  **尽量使用 HttpOnly Cookie**：这是防御 XSS 攻击窃取 Token 的最有效手段。
3.  **如果使用 LocalStorage，严防 XSS**：
    - 对所有的用户输入进行转义和过滤。
    - 谨慎使用 `innerHTML`，优先使用 `textContent`。
    - 使用 CSP（内容安全策略）。
4.  **设置较短的过期时间**：减少 Token 被盗后的有效窗口期。
5.  **实现刷新令牌机制**：使用一个长周期的 Refresh Token 和一个短周期的 Access Token（JWT）。当 Access Token 过期后，用 Refresh Token 去获取新的 Access Token，提升用户体验和安全性。
6.  **防范 CSRF**：如果使用 Cookie，必须实施 CSRF 防护措施，如：
    - 使用 SameSite Cookie 属性（`SameSite=Strict/Lax`）。
    - 要求客户端携带一个自定义 Header（因为跨站请求无法携带自定义 Header）。
    - 使用 Anti-CSRF Token。
  
### 5. 实战代码示例（使用 LocalStorage + Axios）
```javascript
// authService.js
import axios from 'axios';

const API_URL = 'https://your-api.com/api';

// 创建 axios 实例
const api = axios.create({
  baseURL: API_URL,
});

// 请求拦截器：添加 Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 Token 过期
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 清除本地 token 并跳转登录
      localStorage.removeItem('jwt_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 登录函数
export const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  const { token } = response.data;

  if (token) {
    localStorage.setItem('jwt_token', token);
  }
  return response.data;
};

// 退出登录
export const logout = () => {
  localStorage.removeItem('jwt_token');
  // 跳转到登录页
  window.location.href = '/login';
};

// 获取受保护的数据
export const getProtectedData = () => {
  return api.get('/protected-data');
};

export default api;
```
### 总结
前端处理 JWT 的**流程**：登录 -> 收 Token -> 存 Token -> 发请求带 Token -> 处理 Token 过期。
