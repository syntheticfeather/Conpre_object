const API_CONFIG = AdminWeb.API_CONFIG
const JWT_CONFIG = AdminWeb.JWT_CONFIG
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS
const API_CLIENT = AdminWeb.API_CLIENT
const JWT_UTILS = AdminWeb.JWT_UTILS

// ==================== 初始化函数 ====================
function init() {
    // 新增：显示当前环境
    console.log(`后端地址: ${API_CONFIG.baseUrl}`);
    
    if (!DOM_ELEMENTS.registerForm) {
        console.error('注册表单元素未找到')
        return
    }
    
    bindEventListeners()
    console.log('注册页面初始化完成')
}
// ==================== 事件绑定函数 ====================
function bindEventListeners() {
    // 注册表单提交按钮绑定
    if (DOM_ELEMENTS.registerForm) {
        DOM_ELEMENTS.registerForm.addEventListener('submit', handleRegisterSubmit)
    }
    
    //关闭注册页面按钮绑定
    if (DOM_ELEMENTS.closeBtn) {
        DOM_ELEMENTS.closeBtn.addEventListener('click', handleClose)
    }
    
    // // 获取验证码按钮绑定
    // if (DOM_ELEMENTS.getCodeBtn) {
    //     DOM_ELEMENTS.getCodeBtn.addEventListener('click', handleGetCode)
    // }
    
    // 输入框事件绑定
    bindInputEvents()
}
//输入框事件绑定
function bindInputEvents() {
    // 用户名输入框绑定
    if (DOM_ELEMENTS.adminNameInput) {
        DOM_ELEMENTS.adminNameInput.addEventListener('input', () => {
            clearFieldError('adminName')
            clearGenericError()
        })
    }
    // 密码输入框绑定
    if (DOM_ELEMENTS.passwordInput) {
        DOM_ELEMENTS.passwordInput.addEventListener('input', () => {
            clearFieldError('password')
            clearGenericError()
        })
    }
    // 确认密码输入框绑定
    if (DOM_ELEMENTS.confirmPasswordInput) {
        DOM_ELEMENTS.confirmPasswordInput.addEventListener('input', () => {
            clearFieldError('confirmPassword')
            clearGenericError()
        })
    }
    // 手机号输入框绑定
    if (DOM_ELEMENTS.phoneInput) {
        DOM_ELEMENTS.phoneInput.addEventListener('input', () => {
            clearFieldError('phone')
            clearGenericError()
        })
    }
    // 验证码输入框绑定
    if (DOM_ELEMENTS.smsCodeInput) {
        DOM_ELEMENTS.smsCodeInput.addEventListener('input', () => {
            clearFieldError('verificationCode')
            clearGenericError()
        })
    }
}

// ==================== 表单处理函数 ====================
// 获取表单数据
function getFormData() {
    return {
        adminName: DOM_ELEMENTS.adminNameInput ? DOM_ELEMENTS.adminNameInput.value.trim() : '',
        password: DOM_ELEMENTS.passwordInput ? DOM_ELEMENTS.passwordInput.value.trim() : '',
        confirmPassword: DOM_ELEMENTS.confirmPasswordInput ? DOM_ELEMENTS.confirmPasswordInput.value.trim() : '',
        phone: DOM_ELEMENTS.phoneInput ? DOM_ELEMENTS.phoneInput.value.trim() : '',
        smsCode: DOM_ELEMENTS.smsCodeInput ? DOM_ELEMENTS.smsCodeInput.value.trim() : ''
    }
}
// 验证表单数据
function validateForm(formData) {
    const errors = {}
    
    // 用户名格式
    if (!formData.adminName) {
        errors.adminName = '请输入用户名'
    } else if (formData.adminName.length < 0) {
        errors.adminName = '用户名长度至少1位'
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.adminName)) {
        errors.adminName = '用户名只能包含字母、数字和下划线'
    }

    //密码格式
    if (!formData.password) {
        errors.password = '请输入密码';
    } else if (formData.password.length < 6 || formData.password.length > 8) {
        errors.password = '密码长度需为6-8位';
    } else if (!validatePasswordComplexity(formData.password)) {
        errors.password = '密码需包含至少两种特殊符号';
    }
    
    // 确认密码验证
    if (!formData.confirmPassword) {
        errors.confirmPassword = '请确认密码';
    } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = '两次输入的密码不一致';
    }
    // 手机号验证
    if (!formData.phone) {
        errors.phone = '请输入手机号码'
    }else if (!/^1[3-9]\d{9}$/.test(formData.phone)){
        errors.phone = '请输入正确的手机号码'
    }
    // 验证码验证
    // if (!formData.smsCode) {
    //     errors.smsCode = '请输入短信验证码'
    // }else if (formData.smsCode.length !== 6){
    //     errors.smsCode = '请输入正确的短信验证码'
    // }
    
    // 显示错误
    Object.keys(errors).forEach(field => {
        showFieldError(`${field}Error`, errors[field])
    })
  
    return {
        isValid: Object.keys(errors).length === 0,// 若没有错误，说明验证通过
        errors: errors, // 返回所有错误信息
        formData: formData // 将收集的表单数据返回，避免作用域问题
    }
}
// 密码复杂度验证函数
function validatePasswordComplexity(password) {
    const specialChars = '!@#$%^&*()_+-=[]{}|;:,.<>?';
    let specialCharCount = 0
    
    for (let char of password) {
        if (specialChars.includes(char)) {
            specialCharCount++
        }
    }
    
    return specialCharCount >= 2
}

// ==================== 注册处理函数 ====================
// 注册表单提交处理函数
async function handleRegisterSubmit(e) {
    e.preventDefault()
    
    console.log('开始处理注册请求...')
    
    const formData = getFormData()
    clearAllErrors()
    
    const validationResult = validateForm(formData);
    if (!validationResult.isValid) {
        console.log('表单验证失败:', validationResult.errors);
        return;
    }
    
    try {
        showLoading(true)
        
        // 准备提交数据（移除确认密码字段）
        const submitData = {
            adminName: formData.adminName,
            password: formData.password,
            phone: formData.phone,
            smsCode: formData.smsCode || '000000'
        }
        
        console.log('提交注册数据:', submitData)
        
        // 调用API注册
        const result = await API_CLIENT.register(submitData)
        handleRegisterSuccess(result)
        
    } catch (error) {
        console.error('注册过程发生错误:', error)
        handleRegisterError(error)
    } finally {
        showLoading(false)
    }
}

// 获取验证码处理-待修改
async function handleGetCode() {
    const phone = DOM_ELEMENTS.phoneInput ? DOM_ELEMENTS.phoneInput.value.trim() : ''
    
    // 验证手机号格式
    if (!phone) {
        showFieldError('phone', '请输入手机号码')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(phone)) {
        showFieldError('phone', '请输入正确的手机号码')
        return
    }
    
    try {
        // 禁用按钮，防止重复点击
        DOM_ELEMENTS.getCodeBtn.disabled = true
        DOM_ELEMENTS.getCodeBtn.textContent = '发送中...'
        
        // 这里应该调用发送验证码的API
        // await API_CLIENT.sendSms(phone)
        
        // 模拟发送成功
        console.log('发送验证码到:', phone)
        startCountdown()
        
    } catch (error) {
        console.error('发送验证码失败:', error)
        showGenericError('发送验证码失败，请重试')
        DOM_ELEMENTS.getCodeBtn.disabled = false
        DOM_ELEMENTS.getCodeBtn.textContent = '获取验证码'
    }
}
// 倒计时函数-待修改
function startCountdown() {
    let countdown = 60
    DOM_ELEMENTS.getCodeBtn.textContent = `${countdown}秒后重试`
    
    const timer = setInterval(() => {
        countdown--
        DOM_ELEMENTS.getCodeBtn.textContent = `${countdown}秒后重试`
        
        if (countdown <= 0) {
            clearInterval(timer)
            DOM_ELEMENTS.getCodeBtn.disabled = false
            DOM_ELEMENTS.getCodeBtn.textContent = '获取验证码'
        }
    }, 1000)
}

// 关闭注册页面
function handleClose() {
    console.log('关闭注册页面，返回登录页')
    window.location.href = 'login.html'
}

// ==================== 结果处理函数 ====================
function handleRegisterSuccess(result) {
    console.log('注册成功:', result);
    
    showSuccessMessage();
    
    // 保存token和管理员信息
    const token = result.data?.token || result.token
    
    if (token) {
        JWT_UTILS.setToken(token)
        console.log('Token已保存')
    }
    
    // 保存管理员信息
    const adminInfo = {
        adminName: result.adminName || result.data?.adminName,
        phone: result.phone || result.data?.phone,
        registerTime: new Date().toISOString()
    }
    console.log('管理员信息已保存:', adminInfo)
    
    // 标记为已注册
    localStorage.setItem(API_CONFIG.storageKeys.registeredAdmin, 'true')
    
    setTimeout(() => {
        console.log('自动跳转到登录页面...')
        window.location.href = 'login.html'
    }, 2000)
}

function handleRegisterError(error) {
    let errorMessage = '注册失败，请稍后重试'
    
    if (error.message.includes('网络连接错误') || error.message.includes('Failed to fetch')) {
        errorMessage = '无法连接到服务器，请检查网络连接'
    } else if (error.message.includes('用户名已存在')) {
        errorMessage = '用户名已存在，请选择其他用户名'
    } else if (error.message.includes('手机号已注册')) {
        errorMessage = '该手机号已注册，请直接登录'
    } else {
        errorMessage = error.message || '注册失败，请检查输入信息'
    }
    
    showGenericError(errorMessage)
}

// ==================== UI更新函数 ====================
//显示单个错误信息
function showFieldError(field, message) {
    const errorElement = document.getElementById(`${field}Error`)
    if (errorElement) {
        errorElement.textContent = message
        errorElement.style.display = 'block'
    }
    
    const inputElement = document.getElementById(field)
    if (inputElement) {
        inputElement.classList.add('input-error')
    }
}
//清除单个错误信息
function clearFieldError(field) {
    const errorElement = document.getElementById(`${field}Error`)
    if (errorElement) {
        errorElement.style.display = 'none'
    }
    
    const inputElement = document.getElementById(field)
    if (inputElement) {
        inputElement.classList.remove('input-error')
    }
}
//清除所有错误信息
function clearAllErrors() {
    document.querySelectorAll('.error-message').forEach(element => {
        element.style.display = 'none'
    })
    
    document.querySelectorAll('input').forEach(input => {
        input.classList.remove('input-error')
    })
    
    clearGenericError()
}
// 清除网络错误信息
function clearGenericError() {
    if (DOM_ELEMENTS.networkError) {
        DOM_ELEMENTS.networkError.style.display = 'none'
    }
}
// 显示网络错误信息
function showGenericError(message) {
    if (DOM_ELEMENTS.networkError) {
        DOM_ELEMENTS.networkError.textContent = message
        DOM_ELEMENTS.networkError.style.display = 'block'
    }
}
// 显示加载动画
function showLoading(show) {
    if (DOM_ELEMENTS.loadingSpinner) {
        DOM_ELEMENTS.loadingSpinner.style.display = show ? 'block' : 'none'
        DOM_ELEMENTS.loadingSpinner.textContent = show ? '注册中，请稍候...' : ''
    }
    
    if (DOM_ELEMENTS.registerBtn) {
        DOM_ELEMENTS.registerBtn.disabled = show
        DOM_ELEMENTS.registerBtn.textContent = show ? '注册中...' : '注册'
    }
}
// 显示成功信息
function showSuccessMessage() {
    if (DOM_ELEMENTS.registerSuccessMessage) {
        DOM_ELEMENTS.registerSuccessMessage.style.display = 'block'
        DOM_ELEMENTS.registerSuccessMessage.textContent = '注册成功！正在跳转到登录页面...'
    }
}

// ==================== 页面初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM内容加载完成，开始初始化注册页面...')
    init()
})

// 调试函数-获取已注册管理员
window.getRegisteredAdmins = function() {
    return localStorage.getItem(API_CONFIG.storageKeys.registeredAdmin)
}
// 调试函数-清除已注册管理员
window.clearAllAdmins = function() {
    localStorage.removeItem(API_CONFIG.storageKeys.registeredAdmin)
    console.log('注册信息已清空')
}