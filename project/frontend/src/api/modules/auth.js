// src/api/modules/auth.js
import request from '@/utils/request'

const authAPI = {
  // 密码登录
  loginByPassword: (phone, password) => request.post('/auth/login', { phone, password }),
  // 短信验证码登录
  loginBySms: (phone, code) => request.post('/auth/login-sms', { phone, code }),
  // 注册
  register: (userData) => request.post('/auth/register', userData),
  // 退出登录
  logout: () => request.post('/auth/logout'),
}

export default authAPI