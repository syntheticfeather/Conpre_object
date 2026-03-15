import axios from 'axios'

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

request.interceptors.request.use(
  (config) => {
    const publicPaths = ['/auth/login', '/auth/login-sms', '/auth/register']
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
  (error) => {
    if (error.response) {
      console.group(
        `[API Error Response] ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
      )
      console.log('Status:', error.response.status)
      console.log('Response Data:', error.response.data)
      console.groupEnd()

      if (error.response.status === 401) {
        localStorage.removeItem('auth-store')
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
