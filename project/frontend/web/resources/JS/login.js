// 在文件开头新增：
const API_CONFIG = AdminWeb.API_CONFIG;
const JWT_CONFIG = AdminWeb.JWT_CONFIG;
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS;
const API_CLIENT = AdminWeb.API_CLIENT;
const JWT_UTILS = AdminWeb.JWT_UTILS;

// ==================== 初始化函数 ====================
function init() {
    // 检查登录状态
    checkLoginStatus()
    // 绑定事件监听 
    bindEventListeners()
    // 启动token监控
    startTokenMonitor()
}

// ==================== 事件绑定函数 ====================
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
    return await API_CLIENT.login(formData.phone, formData.password);
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
    return await API_CLIENT.smsLogin(formData.phone, formData.smsCode);
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
    return await API_CLIENT.sendSms(phone);
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
    
    if (adminInfo) {
        localStorage.setItem(API_CONFIG.storageKeys.adminInfo, JSON.stringify(adminInfo))
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
    } else if (error.message.includes('用户不存在')) {
        errorMessage = '用户不存在'
    } else if (error.message.includes('密码错误')) {
        errorMessage = '密码错误'
    } else {
        errorMessage = error.message || '登录失败，请稍后重试'
    }
    
    // 显示错误提示
    showErrorById('passwordError', errorMessage)
    showErrorById('smsCodeError', errorMessage)
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
    if (DOM_ELEMENTS.loginSuccessMessage) {
        DOM_ELEMENTS.loginSuccessMessage.style.display = 'block'
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