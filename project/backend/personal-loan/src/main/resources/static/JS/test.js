// ==================== 管理后台Web端全局对象 ====================
const AdminWeb = {
    API_CONFIG: {},
    JWT_CONFIG: {},
    DOM_ELEMENTS: {},
    API_CLIENT: {},
    JWT_UTILS: {}
};

// ==================== API配置信息 ====================
AdminWeb.API_CONFIG = {
    baseUrl: 'http://localhost:8080',
    endpoints: {
        // 注册界面
        register: '/api/auth/register',// 注册接口
        // 登录界面
        login: '/api/auth/login',// 登录接口
        
        // 用户管理相关接口
        getUserInfo: '/api/users/me',
        updateUser: '/api/users/me',
        getBlacklist: '/api/users/blacklist/list',
        addToBlacklist: '/api/users/blacklist/add',
        removeFromBlacklist: '/api/users/blacklist/remove',
        getUserStats: '/api/users/admin/stats',
        getUserDetail: '/api/users/admin/{userId}',
        searchUsersByCredit: '/api/users/search-by-credit',
        
        // 贷款产品管理接口
        getAllProducts: '/api/loan-products/user',
        searchProducts: '/api/loan-products/user/search',
        adminGetAllProducts: '/api/loan-products/admin',
        adminGetProduct: '/api/loan-products/admin/{productId}',
        addLoanProduct: '/api/loan-products/admin',
        updateLoanProduct: '/api/loan-products/admin/products/{productId}',
        deleteLoanProduct: '/api/loan-products/admin/products/{productId}',
        batchDeleteProducts: '/api/loan-products/admin/products/batch-delete',
        batchCreateOptions: '/api/loan-products/admin/options/batch-create',
        deleteOption: '/api/loan-products/admin/options/{optionId}',
        batchDeleteOptions: '/api/loan-products/admin/options/batch-delete',
        
        // 贷款申请相关接口
        applyLoan: '/api/loan-applications',
        getMyApplications: '/api/loan-applications/my',
        getMyApplication: '/api/loan-applications/my/{applicationId}',
        withdrawApplication: '/api/loan-applications/my/{applicationId}/withdraw',
        adminGetApplication: '/api/loan-applications/{applicationId}',
        adminGetUserApplications: '/api/loan-applications/user/{userId}',
        
        // 人工审核相关接口
        getPendingApprovals: '/api/approval/pending',
        getApprovalDetail: '/api/approval/detail/{loanApplicationId}',
        submitApproval: '/api/approval/check'
    },
    storageKeys: {
        token: 'admin_token',
        refreshToken: 'admin_refresh_token',
        tokenExpiry: 'admin_token_expiry',
        adminInfo: 'admin_info',
        isLogged: 'admin_is_logged',// 是否已登录
        registeredAdmin: 'registered_admin'// 已注册的管理员
    }
};

// ==================== JWT配置信息 ==================== 
AdminWeb.JWT_CONFIG = {
    tokenKey: 'admin_token',
    refreshTokenKey: 'admin_refresh_token',
    tokenExpiryKey: 'admin_token_expiry',
    tokenExpiryTime: 3 * 60 * 1000
};

// ==================== DOM元素引用 ====================
AdminWeb.DOM_ELEMENTS = {
    // 主要内容区域
    loanApplyContent: document.getElementById('loan-apply-content'),
    homePageContent: document.getElementById('home-page-content'),
    loanManagementContent: document.getElementById('loan-management-content'),
    userManagementContent: document.getElementById('user-management-content'),
    riskAndCollectionManagementContent: document.getElementById('riskAndCollection-management-content'),
    dataAndSystemManagementContent: document.getElementById('dataAndSystem-management-content')
};

// ==================== API客户端 ====================
AdminWeb.API_CLIENT = {
    /**
     * 通用API请求方法
     * @param {string} url - 请求URL
     * @param {Object} options - 请求选项
     * @returns {Promise} - 返回Promise对象
     */
    request: async function (url, options = {}) {
        const fullUrl = url.startsWith('http') ? url : `${AdminWeb.API_CONFIG.baseUrl}${url}`;
        
        console.log(`🔄 API请求: ${options.method || 'GET'} ${fullUrl}`, {
            requiresAuth: this._requiresAuth(fullUrl),
            hasToken: !!AdminWeb.JWT_UTILS.getRawToken(),
            data: options.body ? JSON.parse(options.body) : null
        })

        // 检查是否需要认证且token有效
        if (this._requiresAuth(fullUrl) && !AdminWeb.JWT_UTILS.isTokenValid()) {
            this.handleUnauthorized();
            throw new Error('登录已过期，请重新登录');
        }

        const headers = {
            'Content-Type': 'application/json',
            ...(this._requiresAuth(fullUrl) ? AdminWeb.JWT_UTILS.getAuthHeader() : {}),
            ...options.headers
        };

        try {
            const response = await fetch(fullUrl, { ...options, headers });
            
            // 处理401未授权错误
            if (response.status === 401 && this._requiresAuth(fullUrl)) {
                const refreshed = await this.refreshToken();
                if (refreshed) {
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
        AdminWeb.JWT_UTILS.clearTokens()
        if (!window.location.href.includes('/login')) {
            alert('登录已过期，请重新登录')
            window.location.href = '/login'
        }
    },

    /**
     * 刷新Token
     * @returns {Promise<boolean>} - 是否刷新成功
     */
    refreshToken: async function () {
        const refreshToken = AdminWeb.JWT_UTILS.getRefreshToken();
        if (!refreshToken) {
            console.log('没有刷新token');
            return false;
        }
        try {
            const response = await fetch(`${AdminWeb.API_CONFIG.baseUrl}/api/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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

    // 基础HTTP方法封装
    get: function (url) {
        return this.request(url);
    },

    post: function (url, data) {
        return this.request(url, { method: 'POST', body: JSON.stringify(data) });
    },

    patch: function (url, data) {
        return this.request(url, { method: 'PATCH', body: JSON.stringify(data) });
    },

    delete: function (url) {
        return this.request(url, { method: 'DELETE' });
    },

    // ==================== 待办审核面板快捷请求 ====================
    getPendingApplications: function() {
        return this.get('/api/approval/pending');
    },

    getApplicationDetail: function(applicationId) {
        return this.get(`/api/approval/detail/${applicationId}`);
    },

    submitReview: function(loanApplicationId, approved) {
        return this.post('/api/approval/check', { loanApplicationId, approved });
    },

    // ==================== 贷款管理面板快捷请求 ====================
    getAllLoanProducts: function() {
        return this.get('/api/loan-products/admin');
    },

    getLoanProductById: function(productId) {
        return this.get(`/api/loan-products/admin/${productId}`);
    },

    addLoanProduct: function(productData) {
        return this.post('/api/loan-products/admin', productData);
    },

    updateLoanProduct: function(productId, productData) {
        return this.patch(`/api/loan-products/admin/products/${productId}`, productData);
    },

    deleteLoanProduct: function(productId) {
        return this.delete(`/api/loan-products/admin/products/${productId}`);
    },

    batchDeleteLoanProducts: function(ids) {
        return this.post('/api/loan-products/admin/products/batch-delete', { ids });
    },

    batchCreateOptions: function(productId, options) {
        return this.post('/api/loan-products/admin/options/batch-create', { productId, options });
    },

    deleteOption: function(optionId) {
        return this.delete(`/api/loan-products/admin/options/${optionId}`);
    },

    batchDeleteOptions: function(ids) {
        return this.post('/api/loan-products/admin/options/batch-delete', { ids });
    },

    // ==================== 用户管理面板快捷请求 ====================
    getUserStats: function() {
        return this.get('/api/users/admin/stats');
    },

    getUserDetail: function(userId) {
        return this.get(`/api/users/admin/${userId}`);
    },

    searchUsersByCredit: function(expr) {
        return this.get(`/api/users/search-by-credit?expr=${encodeURIComponent(expr)}`);
    },

    addToBlacklist: function(userId, blackLevel) {
        return this.post('/api/users/blacklist/add', { userId, blackLevel });
    },

    removeFromBlacklist: function(userId) {
        return this.post('/api/users/blacklist/remove', null, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `userId=${userId}`
        });
    },

    getBlacklist: function() {
        return this.get('/api/users/blacklist/list');
    },

    // ==================== 贷款申请相关 ====================
    getUserApplications: function(userId) {
        return this.get(`/api/loan-applications/user/${userId}`);
    },

    getApplicationById: function(applicationId) {
        return this.get(`/api/loan-applications/${applicationId}`);
    }
};

// ==================== JWT工具函数 ====================
AdminWeb.JWT_UTILS = {
    /**
     * 获取完整的Authorization头
     * @returns {string|null} - Authorization头值
     */
    getToken: function () {
        const token = localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
        return token ? `Bearer ${token}` : null;
    },

    /**
     * 获取认证请求头
     * @returns {Object} - 包含Authorization头的对象
     */
    getAuthHeader: function () {
        const token = this.getToken();
        return token ? { 'Authorization': token } : {};
    },

    /**
     * 设置Token到本地存储
     * @param {string} token - Token值
     * @param {string} refreshToken - 刷新Token值
     */
    setToken: function (token, refreshToken = null) {
        const cleanToken = token.replace(/^Bearer\s+/i, '');
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenKey, cleanToken);
        if (refreshToken) {
            localStorage.setItem(AdminWeb.JWT_CONFIG.refreshTokenKey, refreshToken);
        }
        const expiryTime = Date.now() + (24 * 60 * 60 * 1000);
        localStorage.setItem(AdminWeb.JWT_CONFIG.tokenExpiryKey, expiryTime.toString());
        console.log('Token保存成功，过期时间:', new Date(expiryTime).toLocaleString());
    },

    /**
     * 获取原始Token（不含Bearer前缀）
     * @returns {string|null} - 原始Token
     */
    getRawToken: function () {
        return localStorage.getItem(AdminWeb.JWT_CONFIG.tokenKey);
    },

    /**
     * 获取刷新Token
     * @returns {string|null} - 刷新Token
     */
    getRefreshToken: function () {
        return localStorage.getItem(AdminWeb.JWT_CONFIG.refreshTokenKey);
    },

    /**
     * 检查Token是否有效
     * @returns {boolean} - Token是否有效
     */
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
        console.log('Token 有效');
        return true;
    },

    /**
     * 清除所有Token和登录状态
     */
    clearTokens: function () {
        Object.values(AdminWeb.API_CONFIG.storageKeys).forEach(key => {
            localStorage.removeItem(key);
        });
        console.log('所有token和登录状态已清除');
    }
};