import axios from 'axios'
import { authAPI } from '@/api'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

function getToken() {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return null
  try {
    const data = JSON.parse(stored)
    return data.token || null
  } catch {
    return null
  }
}

function getRefreshToken() {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return null
  try {
    const data = JSON.parse(stored)
    return data.refreshToken || null
  } catch {
    return null
  }
}

function updateAuthStore(newToken, newRefreshToken) {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return
  try {
    const data = JSON.parse(stored)
    data.token = newToken
    data.refreshToken = newRefreshToken
    localStorage.setItem('auth-store', JSON.stringify(data))
  } catch {
    // 忽略错误
  }
}

// 用于标记是否正在刷新 token
let isRefreshing = false
// 用于存储等待刷新 token 完成的请求
let refreshSubscribers = []

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb)
}

function onRefreshed(newToken, newRefreshToken) {
  refreshSubscribers.forEach(cb => cb(newToken, newRefreshToken))
  refreshSubscribers = []
}

request.interceptors.request.use(
  (config) => {
    const publicPaths = ['/auth/login', '/auth/login-sms', '/auth/register', '/auth/refresh-token']
    if (!publicPaths.some((path) => config.url?.startsWith(path))) {
      const token = getToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }

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

request.interceptors.response.use(
  (response) => {
    console.group(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`)
    console.log('Status:', response.status)
    console.log('Response Data:', response.data)
    console.groupEnd()

    return response.data
  },
  async (error) => {
    if (error.response) {
      console.group(
        `[API Error Response] ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
      )
      console.log('Status:', error.response.status)
      console.log('Response Data:', error.response.data)
      console.groupEnd()

      const originalRequest = error.config

      if (error.response.status === 401 && !originalRequest._retry) {
        if (isRefreshing) {
          // 正在刷新 token，将请求加入队列
          return new Promise((resolve) => {
            subscribeTokenRefresh((newToken) => {
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(request(originalRequest))
            })
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const refreshToken = getRefreshToken()
          if (!refreshToken) {
            throw new Error('No refresh token available')
          }

          const response = await authAPI.refreshToken(refreshToken)
          if (response.code === 200) {
            const { token, refreshToken: newRefreshToken } = response.data
            updateAuthStore(token, newRefreshToken)
            onRefreshed(token, newRefreshToken)
            originalRequest.headers.Authorization = `Bearer ${token}`
            return request(originalRequest)
          } else {
            throw new Error('Refresh token failed')
          }
        } catch (refreshError) {
          console.error('Refresh token failed:', refreshError)
          localStorage.removeItem('auth-store')
          window.location.href = '/login'
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
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
