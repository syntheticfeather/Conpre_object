// // ==================== 配置JWT ==================== 
const JWT_CONFIG = {
    tokenKey: 'controller_token',
    refreshTokenKey: 'controller_refresh_token', 
    tokenExpiryKey: 'controller_token_expiry',
    tokenExpiryTime: 3 * 60 * 1000
}
// ==================== 配置信息 ====================
const API_CONFIG = {
    baseUrl: 'http://192.168.2.10:8080',
    endpoints: {
        login: '/api/auth/login',
        smsLogin: '/api/auth/sms-login',   
        sendSms: '/api/auth/send-sms' ,
        refreshToken: '/api/auth/refresh', // 新增刷新token接口
        logout: '/api/auth/logout' // 新增退出接口
    },
    storageKeys: {
        token: JWT_CONFIG.tokenKey,
        controllerInfo: 'controller_info',
        isLogged: 'controller_is_logged'
    }
}

// ==================== DOM元素引用 ====================
const DOM_ELEMENTS = {
    // 密码登录
    passwordLoginBtn: document.getElementById('passwordLogin-btn'),
    passwordLoginForm: document.getElementById('passwordLoginForm'),
    passwordLoadingSpinner: document.getElementById('passwordLoadingSpinner'),
    //验证码登录
    smsLoginBtn: document.getElementById('smsLogin-btn'),
    smsLoginForm: document.getElementById('smsLoginForm'),
    getSmsBtn: document.getElementById('getSmsBtn'),
    smsLoadingSpinner: document.getElementById('smsLoadingSpinner'),
    
    loginBtn: document.getElementById('login-btn'),
    successMessage: document.getElementById('successMessage'),
    // 输入字段
    phoneInput: document.getElementById('phone'),
    passwordInput: document.getElementById('password'),
    smsPhoneInput: document.getElementById('smsPhone'),
    smsCodeInput: document.getElementById('smsCode'),
    agreeCheckbox: document.getElementById('agreeCheckbox')
}
// ==================== API 请求封装 ====================
const API_CLIENT = {
    // 通用请求方法
    request: async function(url, options = {}) {
        // 检查token是否有效
        if (!JWT_UTILS.isTokenValid()) {
            this.handleUnauthorized();
            throw new Error('登录已过期，请重新登录');
        }

        // 添加认证头
        const headers = {
            'Content-Type': 'application/json',
            ...JWT_UTILS.getAuthHeader(),
            ...options.headers
        };

        try {
            const response = await fetch(`${API_CONFIG.baseUrl}${url}`, {
                ...options,
                headers
            });

            // Token 过期，尝试刷新
            if (response.status === 401) {
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

    // 刷新 token
    refreshToken: async function() {
        const refreshToken = JWT_UTILS.getRefreshToken();
        if (!refreshToken) {
            console.log('没有刷新token');
            return false;
        }

        try {
            const response = await fetch(`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.refreshToken}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                JWT_UTILS.setToken(data.token, data.refreshToken);
                console.log('Token刷新成功');
                return true;
            }
        } catch (error) {
            console.error('刷新token失败:', error);
        }

        return false;
    },

    // 处理未授权
    handleUnauthorized: function() {
        JWT_UTILS.clearTokens();
        // 如果是登录页面，不清除，否则跳转到登录页
        if (!window.location.href.includes('login.html')) {
            alert('登录已过期，请重新登录');
            window.location.href = 'login.html';
        }
    },

    // 快捷方法
    get: function(url) {
        return this.request(url);
    },

    post: function(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
}
// ==================== JWT 工具函数 ====================
const JWT_UTILS = {
    // 获取 token（返回带Bearer前缀的完整格式）
    getToken: function() {
        const token = localStorage.getItem(JWT_CONFIG.tokenKey);
        return token ? `Bearer ${token}` : null;  // ✅ 确保包含 Bearer 前缀
    },

    // 获取认证头（确保包含Bearer前缀）
    getAuthHeader: function() {
        const token = this.getToken(); // 这里返回的是带Bearer前缀的
        return token ? { 'Authorization': token } : {};
    },

    // 保存 token 和过期时间
    setToken: function(token, refreshToken) {
        // 确保存储的token不包含Bearer前缀
        const cleanToken = token.replace(/^Bearer\s+/i, '');
        localStorage.setItem(JWT_CONFIG.tokenKey, cleanToken);
        
        if (refreshToken) {
            const cleanRefreshToken = refreshToken.replace(/^Bearer\s+/i, '');
            localStorage.setItem(JWT_CONFIG.refreshTokenKey, cleanRefreshToken);
        }
        
        // 设置过期时间（当前时间 + 3分钟）
        const expiryTime = Date.now() + JWT_CONFIG.tokenExpiryTime;
        localStorage.setItem(JWT_CONFIG.tokenExpiryKey, expiryTime.toString());
    },

    // 获取原始token（不带Bearer前缀）
    getRawToken: function() {
        return localStorage.getItem(JWT_CONFIG.tokenKey);
    },

    // 获取 refresh token
    getRefreshToken: function() {
        return localStorage.getItem(JWT_CONFIG.refreshTokenKey);
    },

    // 检查 token 是否有效（3分钟内）
    isTokenValid: function() {
        const token = this.getRawToken(); // 使用原始token检查
        const expiry = localStorage.getItem(JWT_CONFIG.tokenExpiryKey);
        
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

    // 获取剩余有效时间（秒）
    getRemainingTime: function() {
        const expiry = localStorage.getItem(JWT_CONFIG.tokenExpiryKey);
        if (!expiry) return 0;
        
        const remaining = parseInt(expiry) - Date.now();
        return Math.max(0, Math.floor(remaining / 1000));
    },

    // 清除所有 token
    clearTokens: function() {
        localStorage.removeItem(JWT_CONFIG.tokenKey);
        localStorage.removeItem(JWT_CONFIG.refreshTokenKey);
        localStorage.removeItem(JWT_CONFIG.tokenExpiryKey);
        localStorage.removeItem('controller_is_logged');
        localStorage.removeItem('controller_info');
        localStorage.removeItem('phone');
    }
}
//==================== 函数 ====================
// 页面初始化函数
function init() {
    // 检查登录状态
    checkLoginStatus()
    // 绑定事件监听 
    bindEventListeners()
    // 启动token监控
    startTokenMonitor()
}

//绑定事件监听 
function bindEventListeners() {
    // 登录方式切换
    if (DOM_ELEMENTS.passwordLoginBtn) {
        DOM_ELEMENTS.passwordLoginBtn.addEventListener('click', (e) => {
            e.preventDefault()     //阻止表单默认提交
            switchLoginType('password')
        })
    }
    if (DOM_ELEMENTS.smsLoginBtn) {
        DOM_ELEMENTS.smsLoginBtn.addEventListener('click', (e) => {
            e.preventDefault()
            switchLoginType('sms')
        })
    }

    // 获取短信验证码
    if (DOM_ELEMENTS.getSmsBtn) {
        DOM_ELEMENTS.getSmsBtn.addEventListener('click', handleGetSmsCode);
    }

    // 表单提交
    if (DOM_ELEMENTS.passwordLoginForm) {
        DOM_ELEMENTS.passwordLoginForm.addEventListener('submit', handlePasswordLogin);
    }
    if (DOM_ELEMENTS.smsLoginForm) {
        DOM_ELEMENTS.smsLoginForm.addEventListener('submit', handleSmsLogin);
    }

    // 输入时清除错误提示
    bindInputEvents();
}
    
// 输入提醒绑定
function bindInputEvents() {
    // 输入错误提示
    const inputs = [
        { element: DOM_ELEMENTS.phoneInput, errorId: 'phoneError' },
        { element: DOM_ELEMENTS.passwordInput, errorId: 'passwordError' },
        { element: DOM_ELEMENTS.smsPhoneInput, errorId: 'smsPhoneError' },
        { element: DOM_ELEMENTS.smsCodeInput, errorId: 'smsCodeError' }
    ]
    // 输入时清除错误提示
    inputs.forEach(({ element, errorId }) => {
        if (element) {
            element.addEventListener('input', () => {
                clearErrorById(errorId)
            })
        }
    })
    
    //勾选协议
    if (DOM_ELEMENTS.agreeCheckbox) {
        DOM_ELEMENTS.agreeCheckbox.addEventListener('change', () => {
            clearErrorById('checkboxError')
        })
    }
}

// 登录后自动跳转到管理员中心
function checkLoginStatus() {
    const isLoginPage = window.location.href.includes('login.html')
    
    if (isLoginPage) {
        // 在登录页面，如果已登录且token有效，跳转到首页
        const isLogged = localStorage.getItem(API_CONFIG.storageKeys.isLogged)
        if (isLogged === 'true' && JWT_UTILS.isTokenValid()) {
            console.log('检测到已登录，自动跳转到管理员中心...')
            setTimeout(() => {
                window.location.href = "index.html"
            }, 1500)
        } else if (isLogged === 'true' && !JWT_UTILS.isTokenValid()) {
            // token过期，清除登录状态
            JWT_UTILS.clearTokens()
            console.log('Token已过期，请重新登录')
        }
    } else {
        // 在非登录页面，检查token有效性
        if (!JWT_UTILS.isTokenValid()) {
            JWT_UTILS.clearTokens()
            alert('登录已过期，请重新登录')
            window.location.href = 'login.html'
        }
    }
}

// 登录方式切换 
function switchLoginType(type) {
    if (type === 'password') {
        document.getElementById('passwordLogin-btn').classList.add('active')
        document.getElementById('smsLogin-btn').classList.remove('active')
        DOM_ELEMENTS.passwordLoginForm.style.display = 'block'
        DOM_ELEMENTS.smsLoginForm.style.display = 'none'
    } else {
        document.getElementById('smsLogin-btn').classList.add('active')
        document.getElementById('passwordLogin-btn').classList.remove('active')
        DOM_ELEMENTS.passwordLoginForm.style.display = 'none'
        DOM_ELEMENTS.smsLoginForm.style.display = 'block'
    }
    clearAllErrors();
}

// ==================== 密码登录处理 ====================
// 获取并验证密码登录数据
function validatePasswordLogin() {
    // 获取登录数据
    const formData = {
        phone: DOM_ELEMENTS.phoneInput ? DOM_ELEMENTS.phoneInput.value.trim() : '',
        password: DOM_ELEMENTS.passwordInput ? DOM_ELEMENTS.passwordInput.value.trim() : '',
        isAgreed: DOM_ELEMENTS.agreeCheckbox ? DOM_ELEMENTS.agreeCheckbox.checked : false
    }

    // 验证登录数据
    const errors = {}       
    if (!formData.phone) {
        errors.phone = '请输入手机号码'
    }else if (!/^1[3-9]\d{9}$/.test(formData.phone)){
        errors.phone = '请输入正确的手机号码'
    }
    if (!formData.password) {
        errors.password = '请输入密码'
    }
    if (!formData.isAgreed) {
        errors.checkbox = '请同意服务条款和隐私政策'
    }
    
    // 显示错误
    Object.keys(errors).forEach(field => {
        showErrorById(`${field}Error`, errors[field])
    })
  
    return {
        isValid: Object.keys(errors).length === 0,// 若没有错误，说明验证通过
        errors: errors, // 返回所有错误信息
        formData: formData // 将收集的表单数据返回，避免作用域问题
    }
}
// 密码登录异步处理
async function handlePasswordLogin(e) {
    e.preventDefault();
    console.log('开始密码登录...')

    clearAllErrors() // 清除之前的错误提示
    
    // 获取验证结果及表单数据
    const { isValid, errors, formData } = validatePasswordLogin()
    if (!isValid) {
        // 验证失败，直接返回
        console.log('表单有误', errors)
        return
    }
    try {
        showLoading('password', true) // 显示加载状态
        // 调用登录接口，传递验证后的formData
        const result = await submitPasswordLogin(formData)
        handleLoginSuccess(result, formData.phone)
    } catch (error) {
        console.error('密码登录失败:', error)
        handleLoginError(error) // 统一处理登录错误
    } finally {
        // 无论成功失败，都关闭加载状态
        showLoading('password', false) 
    }
}
// 密码登录
async function submitPasswordLogin(formData) {
    const response = await fetch(`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.login}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            phone: formData.phone,
            password: formData.password
        })
    })
    // 处理HTTP错误状态（如401、403、500等）
    if (!response.ok) {
         // 尝试解析后端返回的错误信息
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.message || `登录失败（状态码：${response.status}）`)
    }

    return await response.json()
}

// ==================== 验证码登录处理 ====================
// 验证码登录异步处理 
async function handleSmsLogin(e) {
    e.preventDefault()
    
    console.log('开始验证码登录...')
    
    const formData = getSmsLoginData()
    clearAllErrors()
    
    const validationResult = validateSmsLogin(formData)
    if (!validationResult.isValid) {
        console.log('验证码登录验证失败')
        return
    }
    
    try {
        showLoading('sms', true)
        const result = await submitSmsLogin(formData)
        handleLoginSuccess(result, formData.phone)
    } catch (error) {
        console.error('短信登录失败:', error)
        // 具体的错误提示
        handleLoginError(error.response?.data?.msg || error.message)
    } finally {
        showLoading('sms', false)
    }
}
// 获取验证码登录数据
function getSmsLoginData() {
    return {
        phone: DOM_ELEMENTS.smsPhoneInput ? DOM_ELEMENTS.smsPhoneInput.value.trim() : '',
        smsCode: DOM_ELEMENTS.smsCodeInput ? DOM_ELEMENTS.smsCodeInput.value.trim() : '',
        isAgreed: DOM_ELEMENTS.agreeCheckbox ? DOM_ELEMENTS.agreeCheckbox.checked : false
    }
}
// 验证验证码登录数据
function validateSmsLogin(formData) {
    const errors = {}
    
    if (!formData.phone) {
        errors.smsPhone = '请输入手机号码'
    } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
        errors.smsPhone = '请输入正确的手机号码'
    }
    if (!formData.smsCode) {
        errors.smsCode = '请输入验证码'
    } else if (!/^\d{6}$/.test(formData.smsCode)) { // 新增：验证验证码格式（6位数字）
        errors.smsCode = '请输入6位数字验证码'
    }
    if (!formData.isAgreed) {
        errors.checkbox = '请同意服务条款和隐私政策'
    }
    
    // 显示错误
    Object.keys(errors).forEach(field => {
        showErrorById(`${field}Error`, errors[field])
    })
    
    return {
        isValid: Object.keys(errors).length === 0,
        errors: errors
    }
}
// 验证码登录
async function submitSmsLogin(formData) {
    try {
        const response = await fetch(`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.smsLogin}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                phone: formData.phone,
                smsCode: formData.smsCode
            })
        })
        
        const data = await response.json() // 先解析响应体
        
        if (!response.ok) {
            // 优化：抛出后端返回的具体错误信息
            throw {
                response: { data }
            }
        }
        
        return data
    } catch (error) {
        // 网络错误处理
        if (!error.response) {
            error.response = {
                data: { msg: '网络异常，请检查网络连接' }
            }
        }
        throw error
    }
}

// ==================== 验证码发送处理 =========
// 验证码发送判定异步函数
async function handleGetSmsCode() { 
    const phone = DOM_ELEMENTS.smsPhoneInput ? DOM_ELEMENTS.smsPhoneInput.value.trim() : ''
    const btn = DOM_ELEMENTS.getSmsBtn
    
    // 倒计时中点击无反应
    if (btn.disabled) return
    
    clearErrorById('smsPhoneError')
    clearErrorById('smsCodeError') 
    
    if (!phone) {
        showErrorById('smsPhoneError', '请输入手机号码')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(phone)) {
        showErrorById('smsPhoneError', '请输入正确的手机号码')
        return
    }
    
    // 发送验证码
    try {
        await sendSmsCode(phone)
        // 后端返回成功后再开始倒计时
        startCountdown(btn)
    } catch (error) {
        console.error('发送验证码失败:', error)
        showErrorById('smsPhoneError', error.message || '验证码发送失败，请重试')
    }
}
// 发送验证码
async function sendSmsCode(phone) {
    const response = await fetch(`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.sendSms}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ phone })
    })
    
    const data = await response.json()
    
    if (!response.ok) {
        // 抛出后端返回的错误信息
        throw new Error(data.msg || '发送失败，请稍后再试')
    }
    
    console.log(`验证码已发送到手机: ${phone}`)
    return data
}
// 验证码发送倒计时
function startCountdown(btn) {
    let countdown = 60
    btn.disabled = true
    btn.textContent = `${countdown}秒后重新发送`
    
    const timer = setInterval(() => {
        countdown--
        btn.textContent = `${countdown}秒后重新发送`
        
        if (countdown <= 0) {
            clearInterval(timer)
            btn.disabled = false
            btn.textContent = '获取验证码'
        }
    }, 1000)
}

// ==================== 登录结果处理 ====================
// 登录成功处理
function handleLoginSuccess(result, phone) {
    console.log('登录成功:', result)
    
    showSuccessMessage()
    
    // 保存登录状态
    console.log('登录成功:', result)
    
    showSuccessMessage()
    
    // 保存登录状态和token（使用新的JWT工具）
    const token = result.data?.token || result.token
    const refreshToken = result.data?.refreshToken || result.refreshToken
    const controllerInfo = result.data?.controllerInfo || result.controllerInfo

    if (token) {
        JWT_UTILS.setToken(token, refreshToken)
        console.log(`Token已保存，将在${JWT_UTILS.getRemainingTime()}秒后过期`)
    }
    
    if (controllerInfo) {
        localStorage.setItem(API_CONFIG.storageKeys.controllerInfo, JSON.stringify(controllerInfo))
    }
    
    localStorage.setItem(API_CONFIG.storageKeys.isLogged, 'true')
    localStorage.setItem('phone', phone)
    
    // 跳转到管理员中心
    setTimeout(() => {
        window.location.href = "index.html"
    }, 1500)
}

// 统一登录错误处理
function handleLoginError(error) {
    let errorMessage = '登录失败，请稍后重试'
    if (error.message.includes('手机号码或密码错误')) {
        errorMessage = '手机号码或密码错误'
    } else if (error.message.includes('验证码错误')) {
        errorMessage = '验证码错误'
    } else if (error.message.includes('Failed to fetch')) {
        errorMessage = '网络连接失败，请检查网络'
    } else {
        errorMessage = error.message
    }
    alert(errorMessage)
}
// ==================== token检查函数 ====================
// 定时检查token状态（每分钟检查一次）
function startTokenMonitor() {
    setInterval(() => {
        const remainingTime = JWT_UTILS.getRemainingTime()
        if (remainingTime > 0 && remainingTime <= 60) {
            // token将在1分钟内过期，提示用户
            console.log(`Token将在${remainingTime}秒后过期`)
        }
    }, 60000) // 每分钟检查一次
}

// ==================== UI更新函数 ====================
// 显示/隐藏加载状态
function showLoading(type, show) {
    const spinner = type === 'password' ? DOM_ELEMENTS.passwordLoadingSpinner : DOM_ELEMENTS.smsLoadingSpinner
    if (spinner) {
        spinner.style.display = show ? 'block' : 'none'
        spinner.textContent = show ? '登录中，请稍候...' : ''
    }
}

// 显示成功消息
function showSuccessMessage() {
    if (DOM_ELEMENTS.successMessage) {
        DOM_ELEMENTS.successMessage.style.display = 'block'
    }
}

// 错误提示函数
function showErrorById(elementId, message) {
    const element = document.getElementById(elementId)
    if (element) {
        element.textContent = message
        element.style.display = 'block'
    }
}

// 单个错误提示清除函数
function clearErrorById(elementId) {
    const element = document.getElementById(elementId)
    if (element) {
        element.style.display = 'none'
    }
}

// 所有错误提示清除函数
function clearAllErrors() {
    const errorElements = document.querySelectorAll('.error-message')
    errorElements.forEach(element => {
        element.style.display = 'none'
    })
}

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM内容加载完成，开始初始化登录页面...')
    init()
})