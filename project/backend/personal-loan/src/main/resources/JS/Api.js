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
        // ========== 认证接口 ==========
        register: '/api/auth/register', // 注册接口
        login: '/api/auth/login',       // 登录接口

        //========== 管理员接口 ==========
        logout: '/api/auth/logout', // 退出接口

        // 用户管理相关接口
        getUserStats: '/api/users/stats',//查询用户状态列表
        getUserDetail: '/api/users/{userId}/detail',//查看单个用户详细信息
        getBlacklist: '/api/users/blacklist/list',//获取黑名单列表
        addToBlacklist: '/api/users/blacklist/add',//添加黑名单
        removeFromBlacklist: '/api/users/blacklist/remove',//解除黑名单
        searchUsersByCredit: '/api/users/search-by-credit',//据信誉分从高到低查询用
        
        // 贷款产品管理接口
        adminGetAllProducts: '/api/loan-products/admin',// 获取所有贷款产品接口
        adminGetProduct: '/api/loan-products/admin/{productId}', // 获取指定贷款产品详情接口
        addLoanProduct: '/api/loan-products/admin', // 增加贷款产品接口
        updateLoanProduct: '/api/loan-products/admin/products/{productId}', // 修改产品信息
        deleteLoanProduct: '/api/loan-products/admin/products/{productId}', // 删除贷款产品接口
        batchDeleteProducts: '/api/loan-products/admin/products/batch-delete',//批量删除产品
        batchCreateOptions: '/api/loan-products/admin/options/batch-create',//批量增加产品选项
        deleteOption: '/api/loan-products/admin/options/{optionId}',//删除产品的单个选项
        batchDeleteOptions: '/api/loan-products/admin/options/batch-delete',//批量删除产品选项
        searchProductByDeta: '/api/loan-products',//根据更新/创建时间查询产品列表
        
        // 贷款申请相关接口
        adminGetApplication: '/api/loan-applications/{applicationId}',// 获取任意用户的单个贷款申请详情
        adminGetUserApplications: '/api/loan-applications/user/{userId}',// 获取指定用户的所有贷款申请详情
        
        // 人工审核相关接口
        getPendingApprovals: '/api/approval/pending', // 查看代办审核列表
        getApprovalDetail: '/api/approval/detail/{loanApplicationId}',//查看单个代办审核申请详情
        submitApproval: '/api/approval/check' // 返回审核结果
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
    loginBtn: document.querySelector('.login-btn'), // 跳转登录按钮
    confirmBtn: document.querySelector('.confirm-btn'), // 确认注册信息，弹出验证弹窗
    closeBtn: document.querySelector('.close-btn'), // 关闭验证弹窗
    loadingSpinner: document.getElementById('loadingSpinner'), // 注册加载动画
    registerSuccessMessage: document.getElementById('successMessage'), // 注册成功提示信息
    networkError: document.getElementById('networkError'), // 网络错误提示信息
    showConfirmPasswordBtn: document.getElementById('showConfirmPassword-btn'), // 显示确认密码按钮

    // 登录页面
    passwordLoginForm: document.getElementById('passwordLoginForm'),
    passwordLoginSubmitBtn: document.getElementById('passwordLogin-submit-btn'), // 密码登录提交按钮
    passwordLoadingSpinner: document.getElementById('passwordLoadingSpinner'), // 密码登录加载动画
    loginSuccessMessage: document.getElementById('successMessage'), // 登录成功提示信息

    // 公共输入字段（注册 + 登录共用）
    adminNameInput: document.getElementById('adminName'),
    passwordInput: document.getElementById('password'),
    confirmPasswordInput: document.getElementById('confirmPassword'),
    phoneInput: document.getElementById('phone'),
    smsCodeInput: document.getElementById('smsCode'), // 虽未启用，保留占位
    agreeCheckbox: document.getElementById('agreeCheckbox'),

    // 公共按钮（注册 + 登录共用）
    showPasswordBtn: document.getElementById('showPassword-btn'), // 显示密码按钮

    //管理员控制面板
    //待办申请
    loanApplyContent: document.getElementById('loan-apply-content'),
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
    /**
     * 通用API请求方法
     * @param {string} url - 请求URL
     * @param {Object} options - 请求选项
     * @returns {Promise} - 返回Promise对象
     */
    request: async function (url, options = {}) {
        const baseUrl = AdminWeb.API_CONFIG.baseUrl
        const fullUrl = url.startsWith('http') ? url : baseUrl + url
        // 网络申请日志
        console.log(`🔄 API请求: ${options.method || 'GET'} ${fullUrl}`, {
            requiresAuth: this._requiresAuth(fullUrl),
            hasToken: !!AdminWeb.JWT_UTILS.getRawToken(),
            data: options.body ? JSON.parse(options.body) : null
        })
        
        // 检查token是否有效（只在需要认证的请求中检查）
        if (this._requiresAuth(fullUrl) && !AdminWeb.JWT_UTILS.isTokenValid()) {
            this.handleUnauthorized()
            throw new Error('登录已过期，请重新登录')
        }

        // 添加认证头（如果需要认证）
        const headers = {
            'Content-Type': 'application/json',
            ...(this._requiresAuth(fullUrl) ? AdminWeb.JWT_UTILS.getAuthHeader() : {}),
            ...options.headers
        }

        try {
            const response = await fetch(fullUrl, {
                ...options,
                headers
            })

            // 处理401未授权错误(Token 过期，尝试刷新)
            if (response.status === 401 && this._requiresAuth(fullUrl)) {
                const refreshed = await this.refreshToken()
                if (refreshed) {
                    // 重试原始请求
                    return this.request(url, options)
                } else {
                    this.handleUnauthorized()
                    throw new Error('认证失败，请重新登录')
                }
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}))
                throw new Error(errorData.message || `请求失败（状态码：${response.status}）`)
            }

            // 输出返回体
            const responseData = await response.json()
            console.log(`✅ API响应: ${options.method || 'GET'} ${fullUrl}`, responseData)
            return responseData

            return await response.json()
        } catch (error) {
            console.error('API请求失败:', error)
            throw error
        }
    },

    /**
     * 判断URL是否需要认证
     * @param {string} url - 请求URL
     * @returns {boolean} - 是否需要认证
     */
    _requiresAuth: function (url) {
        const publicEndpoints = [
            `${AdminWeb.API_CONFIG.baseUrl}${AdminWeb.API_CONFIG.endpoints.login}`,
            `${AdminWeb.API_CONFIG.baseUrl}${AdminWeb.API_CONFIG.endpoints.register}`
        ]
        return !publicEndpoints.includes(url)
    },

    /**
     * 处理未授权情况
     */
    handleUnauthorized: function () {
        AdminWeb.JWT_UTILS.clearTokens();
        // 如果是登录页面，不清除，否则跳转到登录页
        if (!window.location.href.includes('/login')) {
            alert('登录已过期，请重新登录');
            window.location.href = '/login'
        }
    },

    /**
     * 刷新Token
     * @returns {Promise<boolean>} - 是否刷新成功
     */
    refreshToken: async function () {
        const refreshToken = AdminWeb.JWT_UTILS.getRefreshToken()
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

    // 基础HTTP方法封装
    get: function (url) {
        return this.request(url)
    },

    post: function (url, data) {
        return this.request(url, { method: 'POST', body: JSON.stringify(data) })
    },

    patch: function (url, data) {
        return this.request(url, { method: 'PATCH', body: JSON.stringify(data) })
    },

    delete: function (url) {
        return this.request(url, { method: 'DELETE' })
    },

    // ==================== 认证相关快捷请求 ====================
    register: function(adminData) {
        return this.post('/api/auth/register', adminData);
    },

    login: async function (phone, password) {
        try {
            const response = await fetch(`/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ phone, password })
            })
            let result
            try {
                result = await response.json()
                } catch (e) {
                throw new Error('服务器返回格式异常')
            }

            if (response.ok && result.code === 200) {
                return result; // { code: 200, data: { token: '...' }, message: '...' }
            } else {
                const errorMsg = result.message || result.msg || '登录失败'
                throw new Error(errorMsg)
            }
        } catch (error) {
            console.error('登录请求失败:', error)
            throw error
        }
    },

    // ==================== 待办审核面板快捷请求 ====================
    // 获取所有待审核贷款申请（列表）
    getPendingApplications: function() {
        const url = AdminWeb.API_CONFIG.baseUrl + AdminWeb.API_CONFIG.endpoints.getPendingApprovals
        return this.get(url)
    },

    // 根据申请ID获取完整详情
    getApprovalDetail: function(applicationId) {
        const url = `/api/approval/detail/${applicationId}`
        return this.get(url)
    },

    // 提交审核结果
    submitReview: function(loanApplicationId, approved) {
        const url = AdminWeb.API_CONFIG.endpoints.submitApproval
        // approved 必须是字符串 "true" 或 "false"
        const payload = {
            loanApplicationId: loanApplicationId,
            approved: approved ? "true" : "false"  // 布尔转字符串
        }
        return this.post(url, payload)
    },

    // ==================== 贷款管理面板快捷请求 ====================
    // 获取所有贷款产品
    getAllLoanProducts: function() {
        return this.get('/api/loan-products/admin');
    },
    // 根据产品ID获取产品详情
    getLoanProductById: function(productId) {
        return this.get(`/api/loan-products/admin/${productId}`);
    },
    searchProductByDeta: function(expr1,exper2) {
        const url = `/api/loan-products/admin/search?expr1=${expr1}&expr2=${exper2}`
        return this.get(url)
    },
    //删除单个产品
    deleteLoanProduct: function(productId) {
        return this.delete(`/api/loan-products/admin/products/${productId}`);
    },
    //上架产品
    LoanProduct: function(productId) {
        return this.delete(`/api/loan-products/admin/${productId}/active`);
    },
    // 新增贷款产品
    addLoanProduct: function (productData) {
    const url = AdminWeb.API_CONFIG.baseUrl + AdminWeb.API_CONFIG.endpoints.addLoanProduct;
    return this.post(url, productData);
    },
    // 修改产品信息
    updateLoanProduct: function(productId, productData) {
        return this.patch(`/api/loan-products/admin/products/${productId}`, productData);
    },
    //批量删除产品
    batchDeleteLoanProducts: function(ids) {
        return this.post('/api/loan-products/admin/products/batch-delete', { ids });
    },
    //批量增加产品选项
    batchCreateOptions: function(productId, options) {
        return this.post('/api/loan-products/admin/options/batch-create', { productId, options });
    },
    //删除单个产品选项
    deleteOption: function(optionId) {
        return this.delete(`/api/loan-products/admin/options/${optionId}`);
    },
    //批量删除产品选项
    batchDeleteOptions: function(ids) {
        return this.post('/api/loan-products/admin/options/batch-delete', { ids });
    },

    // ==================== 用户管理面板快捷请求 ====================
    // 待确认
    getUserStats: function() {
        return this.get('/api/users/stats');
    },
    // 获取单个用户详情
    getUserDetail: function(userId) {
        return this.get(`/api/users/${userId}/detail`);
    },
    // 加入黑名单
    addToBlacklist: function(userId, blackLevel) {
        return this.post('/api/users/blacklist/add', { userId, blackLevel });
    },
    // 解除黑名单
    removeFromBlacklist: function(userId) {
        return this.post('/api/users/blacklist/remove', null, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `userId=${userId}`
        });
    },
    // 获取黑名单列表
    getBlacklist: function() {
        return this.get('/api/users/blacklist/list');
    },

    // 根据信誉分表达式查询用户
    searchUsersByCredit: function (expr) {
        const url = `/api/users/search-by-credit?expr=${encodeURIComponent(expr)}`
        return this.get(url)
    },

    // ==================== 贷款申请相关 ====================
    getUserApplications: function(userId) {
        return this.get(`/api/loan-applications/user/${userId}`);
    },

    getApplicationById: function(applicationId) {
        return this.get(`/api/loan-applications/${applicationId}`);
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


