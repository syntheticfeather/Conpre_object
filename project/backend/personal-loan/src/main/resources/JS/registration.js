import { API_CONFIG, JWT_CONFIG, DOM_ELEMENTS, API_CLIENT, JWT_UTILS } from './API.js'

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
    DOM_ELEMENTS.registerBtn.addEventListener('submit', handleRegisterSubmit)
    
    //关闭注册页面按钮绑定
    if (DOM_ELEMENTS.closeBtn) {
        DOM_ELEMENTS.closeBtn.addEventListener('click', handleClose)
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
    if (DOM_ELEMENTS.mobileInput) {
        DOM_ELEMENTS.mobileInput.addEventListener('input', () => {
            clearFieldError('mobile')
            clearGenericError()
        })
    }
    // 验证码输入框绑定
    if (DOM_ELEMENTS.verificationCodeInput) {
        DOM_ELEMENTS.verificationCodeInput.addEventListener('input', () => {
            clearFieldError('verificationCode')
            clearGenericError()
        })
    }
}

// ==================== 事件处理函数 ====================
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
        showLoading(true);
        await submitRegister(formData);
    } catch (error) {
        console.error('注册过程发生错误:', error);
        handleRegisterError(error);
    } finally {
        showLoading(false);
    }
}

function handleClose() {
    console.log('关闭注册页面，返回登录页')
    window.location.href = 'login.html'
}

// ==================== 数据获取函数 ====================
function getFormData() {
    return {
        adminName: DOM_ELEMENTS.adminNameInput ? DOM_ELEMENTS.adminNameInput.value.trim() : '',
        password: DOM_ELEMENTS.passwordInput ? DOM_ELEMENTS.passwordInput.value.trim() : '',
        confirmPassword: DOM_ELEMENTS.confirmPasswordInput ? DOM_ELEMENTS.confirmPasswordInput.value.trim() : ''
    }
}

// ==================== 表单验证函数 ====================
function validateForm(formData) {
    const errors = {}
    
    // 用户名格式
    if (!formData.adminName) {
        errors.adminName = '请输入用户名'
    } else if (formData.adminName.length < 2) {
        errors.adminName = '用户名长度至少2位'
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
    
    // 验证确认密码
    if (!formData.confirmPassword) {
        errors.confirmPassword = '请确认密码';
    } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = '两次输入的密码不一致';
    }
    
    Object.keys(errors).forEach(field => {
        showFieldError(field, errors[field]);
    })
    
    return {
        isValid: Object.keys(errors).length === 0,
        errors: errors
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

// ==================== API请求函数 ====================
/**
 * 提交到后端API
 * @param {Object} formData - 表单数据
 */
async function submit(formData) {
    // 构建请求数据
    const requestData = {
        username: formData.username,
        password: formData.password
        // 根据后端接口文档添加其他必要字段
    };
    
    try {
        // 发送POST请求到真实后端接口
        const response = await fetch(
            `${API_CONFIG.baseUrl}${API_CONFIG.endpoints.register}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestData)
            }
        );
        
        console.log('收到服务器响应，状态码:', response.status);
        
        // 检查HTTP响应状态
        if (!response.ok) {
            // 尝试解析错误信息
            let errorMessage = `HTTP错误: 状态码 ${response.status}`;
            try {
                const errorResult = await response.json();
                errorMessage = errorResult.message || errorMessage;
            } catch (e) {
                // 如果无法解析JSON，使用默认错误消息
            }
            throw new Error(errorMessage);
        }
        
        // 解析响应数据
        const result = await response.json();
        console.log('注册响应结果:', result);
        
        // 处理注册结果 - 根据后端实际返回结构调整
        if (result.success || result.code === 200) {
            return result;
        } else {
            throw new Error(result.message || result.msg || '注册失败，请重试');
        }
        
    } catch (error) {
        console.error('注册请求失败:', error);
        throw error;
    }
}

// ==================== 结果处理函数 ====================
function handleRegisterSuccess(result) {
    console.log('注册成功:', result);
    
    showSuccessMessage();
    
    // 修改：适配不同后端返回结构
    const token = result.data?.token || result.token;
    const userInfo = result.data?.userInfo || result.userInfo || result.data;
    
    if (token) {
        localStorage.setItem(API_CONFIG.storageKeys.token, token);
        console.log('Token已保存');
    }
    if (userInfo) {
        localStorage.setItem(API_CONFIG.storageKeys.userInfo, JSON.stringify(userInfo));
        console.log('用户信息已保存:', userInfo);
    }
    
    setTimeout(() => {
        console.log('自动跳转到登录页面...');
        window.location.href = 'login.html';
    }, 2000);
}

function handleRegisterError(error) {
    let errorMessage = '注册失败，请稍后重试';
    
    if (error.message.includes('HTTP错误')) {
        errorMessage = '网络连接错误，请检查网络后重试';
    } else if (error.message.includes('用户名已存在')) {
        errorMessage = '用户名已存在，请选择其他用户名';
    } else if (error.message.includes('Failed to fetch')) {
        errorMessage = '无法连接到服务器，请检查后端服务是否启动';
    } else {
        errorMessage = error.message;
    }
    
    showGenericError(errorMessage);
}

// ==================== UI更新函数 ====================
// ==================== UI更新函数 ====================
/**
 * 显示字段级别的错误提示
 * @param {string} field - 字段名称
 * @param {string} message - 错误消息
 */
function showFieldError(field, message) {
    const errorElement = document.getElementById(`${field}Error`);
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
    
    // 添加错误样式到输入框
    const inputElement = document.getElementById(field);
    if (inputElement) {
        inputElement.classList.add('input-error');
    }
}

/**
 * 清除指定字段的错误提示
 * @param {string} field - 字段名称
 */
function clearFieldError(field) {  // 新增：缺失的函数
    const errorElement = document.getElementById(`${field}Error`);
    if (errorElement) {
        errorElement.style.display = 'none';
    }
    
    // 同时清除输入框错误样式
    const inputElement = document.getElementById(field);
    if (inputElement) {
        inputElement.classList.remove('input-error');
    }
}

/**
 * 清除所有错误提示
 */
function clearAllErrors() {  // 新增：缺失的函数
    // 清除字段错误
    document.querySelectorAll('.error-message').forEach(element => {
        element.style.display = 'none';
    });
    
    // 清除输入框错误样式
    document.querySelectorAll('input').forEach(input => {
        input.classList.remove('input-error');
    });
    
    // 清除通用错误
    clearGenericError();
}

/**
 * 清除通用错误提示
 */
function clearGenericError() {
    if (DOM_ELEMENTS.networkError) {
        DOM_ELEMENTS.networkError.style.display = 'none';
    }
}

/**
 * 显示通用错误提示
 * @param {string} message - 错误消息
 */
function showGenericError(message) {
    const errorContainer = document.getElementById('networkError') || createGenericErrorContainer();
    if (errorContainer) {
        errorContainer.textContent = message;
        errorContainer.style.display = 'block';
    }
}

/**
 * 创建通用错误容器（如果不存在）
 * @returns {HTMLElement} 错误容器元素
 */
function createGenericErrorContainer() {
    const container = document.createElement('div');
    container.className = 'error-message generic-error';
    container.style.display = 'none';
    DOM_ELEMENTS.registerForm.appendChild(container);
    return container;
}

/**
 * 显示/隐藏加载状态
 * @param {boolean} show - 是否显示加载状态
 */
function showLoading(show) {
    if (DOM_ELEMENTS.loadingSpinner) {
        DOM_ELEMENTS.loadingSpinner.style.display = show ? 'block' : 'none';
        DOM_ELEMENTS.loadingSpinner.textContent = show ? '注册中，请稍候...' : '';
    }
    
    // 禁用/启用注册按钮
    if (DOM_ELEMENTS.registerBtn) {
        DOM_ELEMENTS.registerBtn.disabled = show;
        DOM_ELEMENTS.registerBtn.textContent = show ? '注册中...' : '注册';
    }
}

/**
 * 显示注册成功消息
 */
function showSuccessMessage() {
    if (DOM_ELEMENTS.successMessage) {
        DOM_ELEMENTS.successMessage.style.display = 'block';
        DOM_ELEMENTS.successMessage.textContent = '注册成功！正在跳转到登录页面...';
    }
}

// ==================== 页面加载初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM内容加载完成，开始初始化注册页面...');
    init();
});

// 页面卸载前的清理工作
window.addEventListener('beforeunload', function() {
    console.log('页面即将卸载，执行清理工作...');
    // 可以在这里添加清理逻辑，如取消未完成的请求等
});

// 新增：调试函数，在控制台查看注册的用户
window.getRegisteredUsers = function() {
    return USER_MANAGEMENT.getAllUsers();
};

// 新增：清空所有注册用户（开发测试用）
window.clearAllUsers = function() {
    localStorage.removeItem(API_CONFIG.storageKeys.registeredUsers);
    console.log('所有注册用户已清空');
    location.reload();
};