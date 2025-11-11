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
    // baseUrl: 'http://192.168.2.10:8080',
    baseUrl: 'http://localhost:8000',
    endpoints: {
        // 注册界面
        register: '/api/auth/register',// 注册接口
        // 登录界面
        login: '/api/auth/login',// 登录接口?
        passwordLogin: '/api/auth/password-login',// 密码登录接口?
        smsLogin: '/api/auth/sms-login',
        sendSms: '/api/auth/send-sms',

        refreshToken: '/api/auth/refresh',// 刷新token接口
        logout: '/api/auth/logout' // 退出接口
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
    // 注册页面
    registerForm: document.getElementById('registerForm'),
    registerBtn: document.querySelector('.register-btn'),
    closeBtn: document.querySelector('.close-btn'),
    loadingSpinner: document.getElementById('loadingSpinner'),
    registerSuccessMessage: document.getElementById('successMessage'),
    adminNameInput: document.getElementById('adminName'),
    registerPasswordInput: document.getElementById('password'),
    confirmPasswordInput: document.getElementById('confirmPassword'),
    networkError: document.getElementById('networkError'),

    // 登录页面
    // 密码登录
    passwordLoginSubmitBtn: document.getElementById('passwordLogin-btn-submit'),
    passwordLoginForm: document.getElementById('passwordLoginForm'),
    passwordLoadingSpinner: document.getElementById('passwordLoadingSpinner'),
    //验证码登录
    smsLoginSubmitBtn: document.getElementById('smsLogin-btn-submit'),
    smsLoginForm: document.getElementById('smsLoginForm'),
    getSmsBtn: document.getElementById('getSmsBtn'),
    smsLoadingSpinner: document.getElementById('smsLoadingSpinner'),

    loginBtn: document.getElementById('login-btn'),
    loginSuccessMessage: document.getElementById('successMessage'),
    // 输入字段
    phoneInput: document.getElementById('phone'),
    passwordInput: document.getElementById('password'),
    smsPhoneInput: document.getElementById('smsPhone'),
    smsCodeInput: document.getElementById('smsCode'),
    agreeCheckbox: document.getElementById('agreeCheckbox'),

    // 管理员中心页面
    // 五个控制面板
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
// ==================== API 请求封装/API客户端？ ====================
AdminWeb.API_CLIENT = {
    // 通用请求方法
    request: async function (url, options = {}) {
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
            AdminWeb.API_CONFIG.endpoints.smsLogin,
            AdminWeb.API_CONFIG.endpoints.sendSms
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
            console.log('没有刷新token');
            return false;
        }

        try {
            const response = await fetch(`${AdminWeb.API_CONFIG.baseUrl}${AdminWeb.API_CONFIG.endpoints.refreshToken}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                AdminWeb.JWT_UTILS.setToken(data.token, data.refreshToken);
                console.log('Token刷新成功');
                return true;
            }
        } catch (error) {
            console.error('刷新token失败:', error);
        }

        return false;
    },

    // 快捷方法
    get: function (url) {
        return this.request(url);
    },

    post: function (url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    put: function (url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    delete: function (url) {
        return this.request(url, {
            method: 'DELETE'
        });
    },

    // 专用方法 - 登录
    login: function (phone, password) {
        return this.post(AdminWeb.API_CONFIG.endpoints.login, {
            phone: phone,
            password: password
        });
    },

    // 专用方法 - 注册
    register: function (adminData) {
        return this.post(AdminWeb.API_CONFIG.endpoints.register, adminData);
    },

    // 专用方法 - 验证码登录
    smsLogin: function (phone, smsCode) {
        return this.post(AdminWeb.API_CONFIG.endpoints.smsLogin, {
            phone: phone,
            smsCode: smsCode
        });
    },

    // 专用方法 - 发送验证码
    sendSms: function (phone) {
        return this.post(AdminWeb.API_CONFIG.endpoints.sendSms, {
            phone: phone
        })
    }
}
// ==================== JWT 工具函数 ====================
AdminWeb.JWT_UTILS = {
    // 获取 token（返回带Bearer前缀的完整格式）
    getToken: function () {
        const token = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
        return token ? `Bearer ${token}` : null;  //  确保包含 Bearer 前缀
    },

    // 获取认证头（确保包含Bearer前缀）
    getAuthHeader: function () {
        const token = this.getToken(); // 带Bearer前缀的
        return token ? { 'Authorization': token } : {};
    },

    // 保存 token 和过期时间
    setToken: function (token, refreshToken) {
        // 确保存储的token不包含Bearer前缀
        const cleanToken = token.replace(/^Bearer\s+/i, '');
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenKey, cleanToken);

        if (refreshToken) {
            const cleanRefreshToken = refreshToken.replace(/^Bearer\s+/i, '');
            localStorage.setItem(AdminWeb.JWT_CONFIG.refreshTokenKey, cleanRefreshToken);
        }

        // 设置过期时间（当前时间 + 3分钟）
        const expiryTime = Date.now() + AdminWeb.JWT_CONFIG.tokenExpiryTime;
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenExpiryKey, expiryTime.toString());
    },

    // 获取原始token（不带Bearer前缀）
    getRawToken: function () {
        return localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
    },

    // 获取 refresh token
    getRefreshToken: function () {
        return localStorage.getItem(AdminWeb.JWT_CONFIG.refreshTokenKey);
    },

    // 检查 token 是否有效（3分钟内）
    isTokenValid: function () {
        const token = this.getRawToken() // 使用原始token检查
        const expiry = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenExpiryKey)

        if (!token) {
            console.log('Token 不存在')
            return false
        }

        if (expiry && Date.now() > parseInt(expiry)) {
            console.log('Token 已过期')
            return false;
        }

        console.log('Token 有效')
        return true
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
        localStorage.removeItem(AdminWeb.JWT_CONFIG.refreshTokenKey);
        localStorage.removeItem(AdminWeb.JWT_CONFIG.tokenExpiryKey);
        localStorage.removeItem('admin_is_logged');
        localStorage.removeItem('admin_info');
        localStorage.removeItem('phone');
    }
}


