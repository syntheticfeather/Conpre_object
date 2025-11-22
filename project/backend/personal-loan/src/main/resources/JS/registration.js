const API_CONFIG = AdminWeb.API_CONFIG
const JWT_CONFIG = AdminWeb.JWT_CONFIG
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS
const API_CLIENT = AdminWeb.API_CLIENT
const JWT_UTILS = AdminWeb.JWT_UTILS

// ==================== 初始化函数 ====================
function init() {
    // 显示当前环境
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
    
    // 绑定密码显示按钮
    if (DOM_ELEMENTS.showPasswordBtn) {
        DOM_ELEMENTS.showPasswordBtn.addEventListener('click', showPassword)
        document.querySelector('#showPassword-btn .icon-eye-close').style.display = 'inline-block'
    }
    // 绑定确认密码显示按钮
    if (DOM_ELEMENTS.showConfirmPasswordBtn) {
        DOM_ELEMENTS.showConfirmPasswordBtn.addEventListener('click', showConfirmPassword)
        document.querySelector('#showConfirmPassword-btn .icon-eye-close').style.display = 'inline-block'
    }

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
            clearFieldError('vsmsCode')
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
        // smsCode: DOM_ELEMENTS.smsCodeInput ? DOM_ELEMENTS.smsCodeInput.value.trim() : ''
    }
}
// 验证表单数据
function validateForm(formData) {
    const errors = {}
    
    // 用户名格式验证 (2-20位)
    if (!formData.adminName) {
        errors.adminName = '请输入用户名'
    } else if (formData.adminName.length < 2 || formData.adminName.length > 20) {
        errors.adminName = '用户名长度需为2-20位'
    } else if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(formData.adminName)) {
        errors.adminName = '用户名只能包含字母、数字、下划线和中文字符'
    }

    // 密码格式验证 (8-20位，包含大小写字母、数字和特殊字符)
    if (!formData.password) {
        errors.password = '请输入密码';
    } else if (formData.password.length < 8 || formData.password.length > 20) {
        errors.password = '密码长度需为8-20位';
    } else if (!validatePasswordComplexity(formData.password)) {
        errors.password = '密码需包含大小写字母、数字和特殊字符';
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
    } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
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
    // 检查是否包含小写字母
    const hasLowercase = /[a-z]/.test(password);
    // 检查是否包含大写字母
    const hasUppercase = /[A-Z]/.test(password);
    // 检查是否包含数字
    const hasNumber = /\d/.test(password);
    // 检查是否包含特殊字符
    const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
    
    return hasLowercase && hasUppercase && hasNumber && hasSpecialChar;
}
//密码显示函数
function showPassword() {
    const input = document.getElementById('password');
    if (!input) return;

    const close = document.querySelector('.icon-eye-close');
    const show = document.querySelector('.icon-browse');
    if (input.type === 'password') {
        // 显示密码
        input.type = 'text';
        close.style.display = 'none';  
        show.style.display = 'inline-block'; 
    } else {
        // 隐藏密码
        input.type = 'password';
        close.style.display = 'inline-block';  
        show.style.display = 'none';  
    }
}
//确认密码显示函数
function showConfirmPassword() {
    const input = document.getElementById('confirmPassword');
    if (!input) return;

    const close = document.querySelector('#showConfirmPassword-btn .icon-eye-close');
    const show = document.querySelector('#showConfirmPassword-btn .icon-browse');
    if (input.type === 'password') {
        // 显示密码
        input.type = 'text';
        close.style.display = 'none';  
        show.style.display = 'inline-block'; 
    } else {
        // 隐藏密码
        input.type = 'password';
        close.style.display = 'inline-block';  
        show.style.display = 'none';  
    }
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
        
        // 准备提交数据-待完善
        const submitData = { 
            name: formData.adminName,      // ✅ 字段名为 name
            phone: formData.phone,
            password: formData.password
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
// async function handleGetCode() {
//     const phone = DOM_ELEMENTS.phoneInput ? DOM_ELEMENTS.phoneInput.value.trim() : ''
    
//     // 验证手机号格式
//     if (!phone) {
//         showFieldError('phone', '请输入手机号码')
//         return
//     }
    
//     if (!/^1[3-9]\d{9}$/.test(phone)) {
//         showFieldError('phone', '请输入正确的手机号码')
//         return
//     }
    
//     try {
//         // 禁用按钮，防止重复点击
//         DOM_ELEMENTS.getCodeBtn.disabled = true
//         DOM_ELEMENTS.getCodeBtn.textContent = '发送中...'
        
//         // 这里应该调用发送验证码的API
//         // await API_CLIENT.sendSms(phone)
        
//         // 模拟发送成功
//         console.log('发送验证码到:', phone)
//         startCountdown()
        
//     } catch (error) {
//         console.error('发送验证码失败:', error)
//         showGenericError('发送验证码失败，请重试')
//         DOM_ELEMENTS.getCodeBtn.disabled = false
//         DOM_ELEMENTS.getCodeBtn.textContent = '获取验证码'
//     }
// }

// 倒计时函数-待修改
// function startCountdown() {
//     let countdown = 60
//     DOM_ELEMENTS.getCodeBtn.textContent = `${countdown}秒后重试`
    
//     const timer = setInterval(() => {
//         countdown--
//         DOM_ELEMENTS.getCodeBtn.textContent = `${countdown}秒后重试`
        
//         if (countdown <= 0) {
//             clearInterval(timer)
//             DOM_ELEMENTS.getCodeBtn.disabled = false
//             DOM_ELEMENTS.getCodeBtn.textContent = '获取验证码'
//         }
//     }, 1000)
// }

// 关闭注册页面
function handleClose() {
    console.log('关闭注册页面，返回登录页')
    window.location.href = 'login.html'
}

// ==================== 结果处理函数 ====================
function handleRegisterSuccess(result) {
    console.log('注册成功:', result)
    showSuccessMessage()
    
    // 成功时返回id、name、createTime
    const adminInfo = {
        id: result.data?.id,
        name: result.data?.name,
        registerTime: result.data?.createTime || new Date().toISOString()
    }
    console.log('管理员信息已保存:', adminInfo)
    
    localStorage.setItem(API_CONFIG.storageKeys.adminInfo, JSON.stringify(adminInfo))
    
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