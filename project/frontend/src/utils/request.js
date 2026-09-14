import axios from 'axios'
import { authAPI } from '@/api'
import { useAuthStore } from '@/stores/auth'

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

// 用于标记是否正在刷新 token（单飞：同一时刻只允许一个刷新请求）
let isRefreshing = false
// 等待刷新完成的请求队列：每项为 { resolve, reject }
let refreshSubscribers = []

function onRefreshed(newToken) {
  refreshSubscribers.forEach((cb) => cb.resolve(newToken))
  refreshSubscribers = []
}

function onRefreshFailed(error) {
  refreshSubscribers.forEach((cb) => cb.reject(error))
  refreshSubscribers = []
}

// 会话彻底失效（refresh token 也过期/无效）时的统一收敛：
// 拒绝所有挂起请求 → 清空登录态 → 跳转登录页，避免请求永久挂起
function handleSessionExpired(error) {
  onRefreshFailed(error)
  try {
    useAuthStore().logout()
  } catch {
    localStorage.removeItem('auth-store')
  }
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
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

    // 文件上传时删除 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
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

      if (error.response.status === 401 && originalRequest) {
        // ① 刷新接口自身返回 401：refresh token 已失效（满 7 天 / Redis 丢失）。
        //    绝不能再走刷新分支，否则响应拦截器递归调用、Promise 永久挂起。
        const isRefreshRequest = originalRequest.url?.includes('/auth/refresh-token')
        if (isRefreshRequest) {
          handleSessionExpired(error)
          return Promise.reject(error)
        }

        // ② 已重放一次仍 401：新签发 token 也无效，收敛到登录，杜绝重试循环
        if (originalRequest._retry) {
          handleSessionExpired(error)
          return Promise.reject(error)
        }

        // ③ 已有刷新在进行中：挂入队列，等待同一个刷新结果（单飞，防刷新风暴）
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            refreshSubscribers.push({
              resolve: (newToken) => {
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                resolve(request(originalRequest))
              },
              reject
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
            // 通过 store action 单点更新：Pinia 内存态与 localStorage 持久化一次完成
            useAuthStore().setAuthInfo({ token, refreshToken: newRefreshToken })
            onRefreshed(token)
            originalRequest.headers.Authorization = `Bearer ${token}`
            return request(originalRequest)
          }
          throw new Error(response.message || 'Refresh token failed')
        } catch (refreshError) {
          console.error('Refresh token failed:', refreshError)
          handleSessionExpired(refreshError)
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
