// src/api/modules/auth.js
import request from '@/utils/request'

const authAPI = {
  // 密码登录
  loginByPassword: (phone, password) => request.post('/auth/login', { phone, password }),
  // 短信验证码登录
  loginBySms: (phone, code) => request.post('/auth/login-sms', { phone, code }),
  // 注册
  register: (userData) => request.post('/auth/register', userData),
  // 刷新 token
  refreshToken: (refreshToken) => request.post('/auth/refresh-token', { refreshToken }),
  getCertInfo: () => request.get('/auth/cert-info'),
  // 获取工作认证信息
  getWorkCert: (workCertId) => request.get(`/auth/work-cert/${workCertId}`),
  // 获取第三方认证信息
  getTriCert: (triCertId) => request.get(`/auth/tri-cert/${triCertId}`),
  // 获取不动产认证信息
  getImmovablesCert: (immovableCertId) => request.get(`/auth/immovables-cert/${immovableCertId}`),
}

export default authAPI