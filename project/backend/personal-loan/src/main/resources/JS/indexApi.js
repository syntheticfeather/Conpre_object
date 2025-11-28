// ==================== 管理后台Web端全局对象 ====================
const AdminWeb = {
    API_CONFIG: {},
    JWT_CONFIG: {},
    DOM_ELEMENTS: {},
    API_CLIENT: {},
    JWT_UTILS: {}
}
// ==================== API配置信息 ====================
AdminWeb.API_CONFIG = {
    baseUrl: 'http://localhost:8080',
    endpoints: {
        logout: '/api/auth/logout', // 退出接口
        addLoanProduct: '/api/loan-products/admin', // 新增贷款产品接口
    },
    storageKeys: {
        token: 'admin_token',
        refreshToken: 'admin_refresh_token',
        tokenExpiry: 'admin_token_expiry',
        adminInfo: 'admin_info',
        isLogged: 'admin_is_logged',// 是否已登录
        registeredAdmin: 'registered_admin'// 已注册的管理员
    }
}
// ==================== JWT配置信息 ==================== 
AdminWeb.JWT_CONFIG = {
    tokenKey: 'admin_token',
    refreshTokenKey: 'admin_refresh_token',
    tokenExpiryKey: 'admin_token_expiry',
    tokenExpiryTime: 3 * 60 * 1000
}
// ==================== DOM元素引用 ====================
AdminWeb.DOM_ELEMENTS = {
    // 五？个控制面板
    //待办申请
    loanApplyContent: document.getElementById('loan-apply-content'),
    // 首页
    homePageContent: document.getElementById('home-page-content'),
    // 贷款管理
    loanManagementContent: document.getElementById('loan-management-content'),
    // 用户管理
    userManagementContent: document.getElementById('user-management-content'),
    // 风险与催收管理
    riskAndCollectionManagementContent: document.getElementById('riskAndCollection-management-content'),
    // 数据统计与系统管理
    dataAndSystemManagementContent: document.getElementById('dataAndSystem-management-content')
}
// ==================== API 请求封装 ====================
AdminWeb.API_CLIENT = {
    // 通用请求方法

    request: async function (url, options = {}) {
        // 网络申请日志
        console.log(`🔄 API请求: ${options.method || 'GET'} ${url}`, {
            requiresAuth: this._requiresAuth(url),
            hasToken: !!AdminWeb.JWT_UTILS.getRawToken(),
            data: options.body ? JSON.parse(options.body) : null
        })
        // 检查token是否有效（只在需要认证的请求中检查）
        if (this._requiresAuth(url) && !AdminWeb.JWT_UTILS.isTokenValid()) {
            this.handleUnauthorized();
            throw new Error('登录已过期，请重新登录');
        }

        // 添加认证头（如果需要认证）
        const headers = {
            'Content-Type': 'application/json',
            ...(this._requiresAuth(url) ? AdminWeb.JWT_UTILS.getAuthHeader() : {}),
            ...options.headers
        };

        try {
            const response = await fetch(`${AdminWeb.API_CONFIG.baseUrl}${url}`, {
                ...options,
                headers
            });

            // Token 过期，尝试刷新
            if (response.status === 401 && this._requiresAuth(url)) {
                const refreshed = await this.refreshToken();
                if (refreshed) {
                    // 重试原始请求
                    return this.request(url, options);
                } else {
                    this.handleUnauthorized();
                    throw new Error('认证失败，请重新登录');
                }
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `请求失败（状态码：${response.status}）`);
            }

            return await response.json();
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    },

    // 判断请求是否需要认证
    _requiresAuth: function (url) {
        const publicEndpoints = [
            AdminWeb.API_CONFIG.endpoints.login,
            AdminWeb.API_CONFIG.endpoints.register,
            // AdminWeb.API_CONFIG.endpoints.smsLogin,
            // AdminWeb.API_CONFIG.endpoints.sendSms
        ];
        return !publicEndpoints.includes(url);
    },

    // 处理未授权
    handleUnauthorized: function () {
        AdminWeb.JWT_UTILS.clearTokens();
        // 如果是登录页面，不清除，否则跳转到登录页
        if (!window.location.href.includes('login.html')) {
            alert('登录已过期，请重新登录');
            window.location.href = 'login.html';
        }
    },

    // 刷新 token
    refreshToken: async function () {
        const refreshToken = AdminWeb.JWT_UTILS.getRefreshToken();
        if (!refreshToken) {
            console.log('没有刷新token')
            return false
        }

        try {
            const response = await fetch(`${AdminWeb.API_CONFIG.baseUrl}${AdminWeb.API_CONFIG.endpoints.refreshToken}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            })

            if (response.ok) {
                const data = await response.json()
                AdminWeb.JWT_UTILS.setToken(data.token, data.refreshToken)
                console.log('Token刷新成功')
                return true
            }
        } catch (error) {
            console.error('刷新token失败:', error)
        }

        return false
    },

    // 快捷方法
    get: function (url) {
        return this.request(url)
    },

    post: function (url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        })
    },

    // 添加基础URL获取方法
    _getBaseUrl: function () {
        return AdminWeb.API_CONFIG.baseUrl
    },
    // 新增贷款产品
    addLoanProduct: function (productData) {
        // 调用API
        return this.post(AdminWeb.API_CONFIG.endpoints.addLoanProduct, productData)
    },
    // 根据信誉分表达式查询用户
    searchUsersByCredit: function (expr) {
        const url = `/api/users/search-by-credit?expr=${encodeURIComponent(expr)}`
        return this.get(url)
    }
}
// ==================== JWT 工具函数 ====================
AdminWeb.JWT_UTILS = {
    // 获取 token（返回带Bearer前缀的完整格式）
    getToken: function () {
        const token = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
        return token ? `Bearer ${token}` : null;
    },

    // 获取认证头
    getAuthHeader: function () {
        const token = this.getToken();
        return token ? { 'Authorization': token } : {};
    },

    // 保存 token（简化版本）
    setToken: function (token) {
        // 确保存储的token不包含Bearer前缀
        const cleanToken = token.replace(/^Bearer\s+/i, '');
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenKey, cleanToken);

        // 设置过期时间（当前时间 + 24小时，根据jwt.expiration=86400000）
        const expiryTime = Date.now() + (24 * 60 * 60 * 1000); // 24小时
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenExpiryKey, expiryTime.toString());

        console.log('Token保存成功，过期时间:', new Date(expiryTime).toLocaleString());
    },

    // 获取原始token（不带Bearer前缀）
    getRawToken: function () {
        return localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
    },

    // 检查 token 是否有效（24小时内）
    isTokenValid: function () {
        const token = this.getRawToken();
        const expiry = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenExpiryKey);

        if (!token) {
            console.log('Token 不存在');
            return false;
        }

        if (expiry && Date.now() > parseInt(expiry)) {
            console.log('Token 已过期');
            return false;
        }

        console.log('Token 有效，剩余时间:', Math.floor((parseInt(expiry) - Date.now()) / 1000 / 60), '分钟');
        return true;
    },

    // 获取剩余有效时间（秒）
    getRemainingTime: function () {
        const expiry = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenExpiryKey);
        if (!expiry) return 0;

        const remaining = parseInt(expiry) - Date.now();
        return Math.max(0, Math.floor(remaining / 1000));
    },

    // 清除所有 token
    clearTokens: function () {
        localStorage.removeItem(AdminWeb.JWT_CONFIG.tokenKey);
        localStorage.removeItem(AdminWeb.JWT_CONFIG.tokenExpiryKey);
        localStorage.removeItem('admin_is_logged');
        localStorage.removeItem('admin_info');
        localStorage.removeItem('phone');
        console.log('所有token和登录状态已清除');
    }
}


