// ==================== 环境配置 ====================
// 新增：环境开关，方便切换模拟数据和真实API
const ENV_CONFIG = {
    useMock: false, // true=使用模拟数据，false=使用真实API
    baseUrl: 'http://localhost:8080' // 真实后端地址
}

// ==================== 配置信息 ====================
const API_CONFIG = {
    baseUrl: ENV_CONFIG.baseUrl,
    endpoints: {
        register: '/api/auth/register'
    },
    storageKeys: {
        token: 'loan_app_token',
        userInfo: 'loan_app_user_info',
        registeredUsers: 'loan_app_registered_users'
    }
}

// ==================== JWT工具函数 ====================
// 保留：用于模拟环境，真实API环境下不会使用
const JWT_UTILS = {
    generateMockToken: (payload) => {
        const header = { alg: 'HS256', typ: 'JWT' };
        const payloadWithExp = {
            ...payload,
            exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60),
            iat: Math.floor(Date.now() / 1000)
        };
        const base64Header = btoa(JSON.stringify(header));
        const base64Payload = btoa(JSON.stringify(payloadWithExp));
        const signature = 'mock_signature_' + Date.now();
        return `${base64Header}.${base64Payload}.${signature}`;
    }
};

// ==================== 用户管理 ====================
// 保留：用于模拟环境
const USER_MANAGEMENT = {
    getAllUsers: () => {
        return JSON.parse(localStorage.getItem(API_CONFIG.storageKeys.registeredUsers) || '{}');
    },
    saveUser: (userData) => {
        const users = USER_MANAGEMENT.getAllUsers();
        users[userData.username] = {
            ...userData,
            id: Date.now().toString(),
            registerTime: new Date().toISOString(),
            status: 'active',
            creditScore: Math.floor(Math.random() * 100) + 600,
            loanLimit: Math.floor(Math.random() * 50000) + 5000
        };
        localStorage.setItem(API_CONFIG.storageKeys.registeredUsers, JSON.stringify(users));
        return users[userData.username];
    },
    checkUsernameExists: (username) => {
        const users = USER_MANAGEMENT.getAllUsers();
        return !!users[username];
    }
};

// ==================== DOM元素引用 ====================
const DOM_ELEMENTS = {
    registerForm: document.getElementById('registerForm'),
    registerBtn: document.querySelector('.register-btn'),
    closeBtn: document.querySelector('.close-btn'),
    loadingSpinner: document.getElementById('loadingSpinner'),
    successMessage: document.getElementById('successMessage'),
    usernameInput: document.getElementById('username'),
    passwordInput: document.getElementById('password'),
    confirmPasswordInput: document.getElementById('confirmPassword'),
    networkError: document.getElementById('networkError')
}

// ==================== 初始化函数 ====================
function init() {
    // 新增：显示当前环境
    console.log(`当前环境: ${ENV_CONFIG.useMock ? '模拟数据' : '真实API'}`);
    console.log(`后端地址: ${ENV_CONFIG.baseUrl}`);
    
    if (!DOM_ELEMENTS.registerForm) {
        console.error('注册表单元素未找到');
        return;
    }
    
    bindEventListeners();
    console.log('注册页面初始化完成');
}

// ==================== 事件绑定函数 ====================
function bindEventListeners() {
    DOM_ELEMENTS.registerForm.addEventListener('submit', handleRegisterSubmit);
    
    if (DOM_ELEMENTS.closeBtn) {
        DOM_ELEMENTS.closeBtn.addEventListener('click', handleClose);
    }
    
    bindInputEvents();
}

function bindInputEvents() {
    if (DOM_ELEMENTS.usernameInput) {
        DOM_ELEMENTS.usernameInput.addEventListener('input', () => {
            clearFieldError('username')
            clearGenericError()
        })
    }
    
    if (DOM_ELEMENTS.passwordInput) {
        DOM_ELEMENTS.passwordInput.addEventListener('input', () => {
            clearFieldError('password')
            clearGenericError()
        })
    }
    
    if (DOM_ELEMENTS.confirmPasswordInput) {
        DOM_ELEMENTS.confirmPasswordInput.addEventListener('input', () => {
            clearFieldError('confirmPassword')
            clearGenericError()
        })
    }
}

// ==================== 事件处理函数 ====================
async function handleRegisterSubmit(e) {
    e.preventDefault();
    
    console.log('开始处理注册请求...');
    
    const formData = getFormData();
    clearAllErrors();
    
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
        username: DOM_ELEMENTS.usernameInput ? DOM_ELEMENTS.usernameInput.value.trim() : '',
        password: DOM_ELEMENTS.passwordInput ? DOM_ELEMENTS.passwordInput.value.trim() : '',
        confirmPassword: DOM_ELEMENTS.confirmPasswordInput ? DOM_ELEMENTS.confirmPasswordInput.value.trim() : ''
    }
}

// ==================== 表单验证函数 ====================
function validateForm(formData) {
    const errors = {};
    
    // 验证用户名
    if (!formData.username) {
        errors.username = '请输入用户名';
    } else if (formData.username.length < 8) {
        errors.username = '用户名长度至少8位';
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
        errors.username = '用户名只能包含字母、数字和下划线';
    }
    // 修改：移除本地用户名检查，由后端验证
    // else if (USER_MANAGEMENT.checkUsernameExists(formData.username)) {
    //     errors.username = '用户名已存在，请选择其他用户名';
    // }
    
    // 验证密码
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
    });
    
    return {
        isValid: Object.keys(errors).length === 0,
        errors: errors
    };
}

function validatePasswordComplexity(password) {
    const specialChars = '!@#$%^&*()_+-=[]{}|;:,.<>?';
    let specialCharCount = 0;
    
    for (let char of password) {
        if (specialChars.includes(char)) {
            specialCharCount++;
        }
    }
    
    return specialCharCount >= 2;
}

// ==================== API请求函数 ====================
/**
 * 提交注册请求到后端API
 * @param {Object} formData - 表单数据
 */
async function submitRegister(formData) {
    console.log('正在提交注册请求...', formData);
    
    // 修改：根据环境开关选择使用模拟数据还是真实API
    if (ENV_CONFIG.useMock) {
        console.log('使用模拟数据注册');
        return simulateRegisterWithJWT(formData);
    } else {
        console.log('使用真实API注册');
        return submitToRealAPI(formData);
    }
}

/**
 * 新增：提交到真实后端API
 * @param {Object} formData - 表单数据
 */
async function submitToRealAPI(formData) {
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

/**
 * 模拟注册流程（仅用于开发测试）
 */
function simulateRegisterWithJWT(formData) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            try {
                // 模拟用户名重复检查
                if (USER_MANAGEMENT.checkUsernameExists(formData.username)) {
                    reject(new Error('用户名已存在，请选择其他用户名'));
                    return;
                }
                
                const savedUser = USER_MANAGEMENT.saveUser({
                    username: formData.username,
                    password: formData.password
                });
                
                const tokenPayload = {
                    userId: savedUser.id,
                    username: savedUser.username,
                    creditScore: savedUser.creditScore,
                    loanLimit: savedUser.loanLimit
                };
                
                const jwtToken = JWT_UTILS.generateMockToken(tokenPayload);
                
                const successResponse = {
                    success: true,
                    message: '注册成功！欢迎使用网贷平台',
                    data: {
                        token: jwtToken,
                        userInfo: {
                            id: savedUser.id,
                            username: savedUser.username,
                            registerTime: savedUser.registerTime,
                            creditScore: savedUser.creditScore,
                            loanLimit: savedUser.loanLimit,
                            status: savedUser.status
                        }
                    }
                };
                
                console.log('模拟注册成功:', successResponse);
                resolve(successResponse);
                
            } catch (error) {
                console.error('模拟注册过程出错:', error);
                reject(new Error('注册失败，请稍后重试'));
            }
        }, 1500);
    });
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