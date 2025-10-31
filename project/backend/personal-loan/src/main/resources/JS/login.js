// ==================== 环境配置 ====================
const ENV_CONFIG = {
    useMock: true,      // true=使用模拟数据，false=使用真实API
    baseUrl: 'http://localhost:8080'
}

// ==================== 配置信息 ====================
const API_CONFIG = {
    baseUrl: ENV_CONFIG.baseUrl,
    endpoints: {
        login: '/api/auth/login',
        smsLogin: '/api/auth/sms-login',
        sendSms: '/api/auth/send-sms'
    },
    storageKeys: {
        token: 'loan_app_token', 
        userInfo: 'loan_app_user_info',
        isLogged: 'loan_app_is_logged'
    }
}

// ==================== DOM元素引用 ====================
const DOM_ELEMENTS = {
    passwordLoginBtn: document.getElementById('passwordLogin-btn'),
    smsLoginBtn: document.getElementById('smsLogin-btn'),
    loginForm: document.getElementById('loginForm'),
    smsLoginForm: document.getElementById('smsLoginForm'),
    getSmsBtn: document.getElementById('getSmsBtn'),
    loadingSpinner: document.getElementById('loadingSpinner'),
    smsLoadingSpinner: document.getElementById('smsLoadingSpinner'),
    successMessage: document.getElementById('successMessage'),
    // 输入字段
    usernameInput: document.getElementById('username'),
    passwordInput: document.getElementById('password'),
    smsPhoneInput: document.getElementById('smsPhone'),
    smsCodeInput: document.getElementById('smsCode'),
    agreeCheckbox: document.getElementById('agreeCheckbox')
}

// 页面初始化函数
function init() {
    console.log(`登录页面初始化 - 环境: ${ENV_CONFIG.useMock ? '模拟数据' : '真实API'}`)
    
    // 检查登录状态
    checkLoginStatus()
    
    bindEventListeners()
}

// ==================== 事件绑定函数 ====================
//表单控件绑定
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
    
    // 表单提交
    if (DOM_ELEMENTS.loginForm) {
        DOM_ELEMENTS.loginForm.addEventListener('submit', handlePasswordLogin);
    }
    if (DOM_ELEMENTS.smsLoginForm) {
        DOM_ELEMENTS.smsLoginForm.addEventListener('submit', handleSmsLogin);
    }
    
    // 获取短信验证码
    if (DOM_ELEMENTS.getSmsBtn) {
        DOM_ELEMENTS.getSmsBtn.addEventListener('click', handleGetSmsCode);
    }
    
    // 输入时清除错误提示
    bindInputEvents();
}

// 输入提醒绑定
function bindInputEvents() {
    // 输入错误提示
    const inputs = [
        { element: DOM_ELEMENTS.usernameInput, errorId: 'usernameError' },
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

// 登录方式切换 
function switchLoginType(type) {
    const passwordForm = document.getElementById('loginForm')
    const smsForm = document.getElementById('smsLoginForm')
    const passwordLogin = document.getElementById('passwordLogin-btn')
    const smsLogin = document.getElementById('smsLogin-btn')
    if (type === 'password') {
        passwordLogin.classList.add('active')
        smsLogin.classList.remove('active')
        passwordForm.classList.add('active')
        smsForm.classList.remove('active')
    } else {
        smsLogin.classList.add('active')
        passwordLogin.classList.remove('active')
        smsForm.classList.add('active')
        passwordForm.classList.remove('active')
    }
    // 清除所有错误提示
    clearAllErrors();
}

// 密码登录异步处理，使用async与await
async function handlePasswordLogin(e) {
    e.preventDefault();
    console.log('开始密码登录...')
    
    const formData = getPasswordLoginData()
    clearAllErrors()
    
    const validationResult = validatePasswordLogin(formData)
    if (!validationResult.isValid) {
        console.log('密码登录验证失败')
        return
    }
    
    try {
        showLoading('password', true)
        const result = await submitPasswordLogin(formData)
        handleLoginSuccess(result, formData.username)
    } catch (error) {
        console.error('密码登录失败:', error)
        handleLoginError(error)
    } finally {
        showLoading('password', false)
    }
}

// 获取密码登录数据
function getPasswordLoginData() {
    return {
        username: DOM_ELEMENTS.usernameInput ? DOM_ELEMENTS.usernameInput.value.trim() : '',
        password: DOM_ELEMENTS.passwordInput ? DOM_ELEMENTS.passwordInput.value.trim() : '',
        isAgreed: DOM_ELEMENTS.agreeCheckbox ? DOM_ELEMENTS.agreeCheckbox.checked : false
    }
}

// 验证密码登录数据
function validatePasswordLogin(formData) {
    const errors = {}       //收集验证错误
    
    if (!formData.username) {
        errors.username = '请输入用户名或手机号码'
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
        isValid: Object.keys(errors).length === 0,
        errors: errors
    }
}

// 验证码登录处理 
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
        handleLoginError(error)
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

// 统一API请求
async function submitPasswordLogin(formData) {
    if (ENV_CONFIG.useMock) {
        console.log('使用模拟密码登录')
        return simulatePasswordLogin(formData)
    } else {
        console.log('使用真实API密码登录')
        return submitToRealPasswordLogin(formData)
    }
}

async function submitSmsLogin(formData) {
    if (ENV_CONFIG.useMock) {
        console.log('使用模拟验证码登录')
        return simulateSmsLogin(formData)
    } else {
        console.log('使用真实API验证码登录')
        return submitToRealSmsLogin(formData)
    }
}

// 模拟密码登录
function simulatePasswordLogin(formData) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            const testAccounts = {
                'admin': '123456',
                '13800138000': '123456',
                'testuser': 'Test@123'
            }
            const expirationTime = Math.floor(Date.now() / 1000) + (3 * 60); // 3分钟过期
            if (testAccounts[formData.username] === formData.password) {
                resolve({
                    success: true,
                    message: '登录成功',
                    data: {
                        token: 'mock_jwt_token_' + Date.now(),
                        userInfo: {
                            id: Date.now().toString(),
                            username: formData.username,
                            phone: formData.username === '13800138000' ? formData.username : '13800138000'
                        },
                        expiresIn: expirationTime // 返回过期时间
                    }
                })
            } else {
                reject(new Error('用户名或密码错误'))
            }
        }, 1500)
    })
}

// 真实API密码登录
async function submitToRealPasswordLogin(formData) {
    const response = await fetch(`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.login}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            username: formData.username,
            password: formData.password
        })
    })
    if (!response.ok) {
        throw new Error('登录请求失败')
    }
    return await response.json()
}

// 真实API验证码登录
async function submitToRealSmsLogin(formData) {
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
    if (!response.ok) {
        throw new Error('登录请求失败')
    }
    return await response.json()
}

// ==================== 登录结果处理 ====================
// 登录成功处理
function handleLoginSuccess(result, username) {
    console.log('登录成功:', result)
    
    showSuccessMessage()
    
    // 保存登录状态
    const token = result.data?.token || result.token
    const userInfo = result.data?.userInfo || result.userInfo
    
    if (token) {
        localStorage.setItem(API_CONFIG.storageKeys.token, token)
    }
    if (userInfo) {
        localStorage.setItem(API_CONFIG.storageKeys.userInfo, JSON.stringify(userInfo))
    }
    
    localStorage.setItem(API_CONFIG.storageKeys.isLoggedIn, 'true')
    localStorage.setItem('username', username)
    
    // 跳转到用户中心
    setTimeout(() => {
        window.location.href = "userCenter.html"
    }, 1500)
}

// 统一登录错误处理
function handleLoginError(error) {
    let errorMessage = '登录失败，请稍后重试'
    if (error.message.includes('用户名或密码错误')) {
        errorMessage = '用户名或密码错误'
    } else if (error.message.includes('验证码错误')) {
        errorMessage = '验证码错误'
    } else if (error.message.includes('Failed to fetch')) {
        errorMessage = '网络连接失败，请检查网络'
    } else {
        errorMessage = error.message
    }
    alert(errorMessage)
}

// 验证码发送判定
function handleGetSmsCode() {
    const phone = DOM_ELEMENTS.smsPhoneInput ? DOM_ELEMENTS.smsPhoneInput.value.trim() : ''
    
    clearErrorById('smsPhoneError')
    
    if (!phone) {
        showErrorById('smsPhoneError', '请输入手机号码')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(phone)) {
        showErrorById('smsPhoneError', '请输入正确的手机号码')
        return
    }
    
    // 发送验证码
    sendSmsCode(phone)
}

// 发送验证码
function sendSmsCode(phone) {
    const btn = DOM_ELEMENTS.getSmsBtn
    let countdown = 60
    
    btn.disabled = true;
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
    
    // 模拟发送验证码
    console.log(`验证码已发送到手机: ${phone}`)
    if (ENV_CONFIG.useMock) {
        alert('验证码已发送：123456（测试用途）')
    }
}

// ==================== UI更新函数 ====================
// 显示/隐藏加载状态
function showLoading(type, show) {
    const spinner = type === 'password' ? DOM_ELEMENTS.loadingSpinner : DOM_ELEMENTS.smsLoadingSpinner
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

function clearErrorById(elementId) {
    const element = document.getElementById(elementId)
    if (element) {
        element.style.display = 'none'
    }
}

function clearAllErrors() {
    const errorElements = document.querySelectorAll('.error-message')
    errorElements.forEach(element => {
        element.style.display = 'none'
    })
}

// 登录后自动跳转个人中心
function checkLoginStatus() {
    const isLoginPage = window.location.href.endsWith('login.html')
    if (isLoginPage) {
        const isLoggedIn = localStorage.getItem(API_CONFIG.storageKeys.isLoggedIn)
        if (isLoggedIn === 'true') {
            console.log('检测到已登录，自动跳转到用户中心')
            window.location.href = 'userCenter.html'
        }
    }
}

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM内容加载完成，开始初始化登录页面...')
    init()
})