// src/utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // 开发时通过 Vite 代理转发
  timeout: 10000
})

// 请求拦截器（可选）
request.interceptors.request.use(config => {
  // 可添加 token 等
  return config
})

// 响应拦截器（可选）
request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default request