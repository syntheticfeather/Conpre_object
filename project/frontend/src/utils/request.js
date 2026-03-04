// src/utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // 开发时通过 Vite 代理转发
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：自动加 token
request.interceptors.request.use(
  (config) => {
    // 跳过 token 的路径
    const publicPaths = ['/auth/login', '/auth/login-sms', '/auth/register', '/auth/logout']
    if (!publicPaths.some((path) => config.url?.startsWith(path))) {
      const token = localStorage.getItem('admin_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }

    // 请求日志
    console.group(`[API Request] ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`)
    console.log('Headers:', config.headers)
    if (config.data) {
      console.log('Request Body:', config.data)
    }
    console.groupEnd()

    return config
  },
  (error) => {
    console.error('[API Request Error]', error)
    return Promise.reject(error)
  },
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    // 响应成功日志
    console.group(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`)
    console.log('Status:', response.status)
    console.log('Response Data:', response.data)
    console.groupEnd()

    return response.data
  },
  (error) => {
    // 响应失败日志
    if (error.response) {
      console.group(
        `[API Error Response] ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
      )
      console.log('Status:', error.response.status)
      console.log('Response Data:', error.response.data)
      console.groupEnd()

      if (error.response.status === 401) {
        localStorage.removeItem('admin_token')
        window.location.href = '/login'
      }
    } else if (error.request) {
      console.error('[API Network Error] No response received:', error.request)
    } else {
      console.error('[API Config Error]', error.message)
    }

    return Promise.reject(error)
  },
)

export default request
