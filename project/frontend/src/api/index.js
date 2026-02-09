import axios from 'axios'

const api = axios.create({
  baseURL: '/api', // 代理到后端
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动加 token
api.interceptors.request.use(
  (config) => {
    // 跳过 token 的路径
    const publicPaths = ['/auth/login', '/auth/login-sms', '/auth/register', '/auth/logout']
    if (!publicPaths.some(path => config.url?.startsWith(path))) {
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
  }
)

// 返回拦截器：统一错误处理 
api.interceptors.response.use(
  (response) => {
    // 响应成功日志
    console.group(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`)
    console.log('Status:', response.status)
    console.log('Response Data:', response.data)
    console.groupEnd()

    return response
  },
  (error) => {
    // 响应失败日志
    if (error.response) {
      console.group(`[API Error Response] ${error.config?.method?.toUpperCase()} ${error.config?.url}`)
      console.log('Status:', error.response.status);
      console.log('Response Data:', error.response.data);
      console.groupEnd();

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
  }
)


/*************导出具体接口*********** */
// 认证相关 API
export const authAPI = {
  loginByPassword: (phone, password) => api.post('/auth/login', { phone, password }),
  loginBySms: (phone, code) => api.post('/auth/login-sms', { phone, code }),
  register: (userData) => api.post('/auth/register', userData),
  logout: () => api.post('/auth/logout')
}

// 贷款产品 API
export const loanAPI = {
  // 获取所有贷款产品
  getProducts: () => api.get('/loan-products/admin'),
  // 获取单个产品详情
  getProduct: (id) => api.get(`/loan-products/admin/${id}`),
  // 添加新产品
  addProduct: (productData) => api.post('/loan-products/admin', productData),
  // 更新产品
  updateProduct: (id, productData) => api.patch(`/loan-products/admin/products/${id}`, productData),
  // 删除产品
  deleteProduct: (id) => api.delete(`/loan-products/admin/products/${id}`),
  // 切换产品状态
  toggleStatus: (id, action) => api.post(`/loan-products/admin/${id}/${action}`),
  // 批量删除产品
  batchDeleteProducts: (productIds) =>api.post('/api/loan-products/admin/products/batch-delete', { productIds }),
  // 批量创建产品选项
  batchCreateOptions: (options) =>api.post('/api/loan-products/admin/options/batch-create', options),
  // 删除单个产品选项
  deleteOption: (optionId) => api.delete(`/api/loan-products/admin/options/${optionId}`),
  // 批量删除产品选项
  batchDeleteOptions: (optionIds) =>api.post('/api/loan-products/admin/options/batch-delete', { optionIds }),
  // 根据更新/创建时间查询产品列表
  searchProductsByTime: (params) => api.get('/loan-products', { params })
}

// 申请审核 API
export const applicationAPI = {
  // 获取待审核申请
  getPendingApplications: () => api.get('/approval/pending'),
  // 获取已完成审核申请
  getCompletedApplications: () => api.get('/approval/completed'),
  // 获取申请详情
  getApplicationDetail: (id) => api.get(`/approval/detail/${id}`),
  // 提交审核结果
  submitReview: (reviewData) => api.post('/approval/check', reviewData)
}

// 用户管理 API
export const userAPI = {
  // 获取用户统计信息
  getUserStats: () => api.get('/users/stats'),
  // 获取用户详情
  getUserDetail: (id) => api.get(`/users/${id}/detail`),
  // 获取黑名单列表
  getBlacklist: () => api.get('/users/blacklist/list'),
  // 添加到黑名单
  addToBlacklist: (blacklistData) => api.post('/users/blacklist/add', blacklistData),
  // 从黑名单移除
  removeFromBlacklist: (userId) => api.post('/users/blacklist/remove', { userId }),
  // 根据信誉分从高到低查询用户
  searchUsersByCredit: () => api.get('/api/users/search-by-credit')
}

// 贷款申请相关 API
export const loanApplicationAPI = {
  // 获取任意用户的单个贷款申请详情
  getApplication: (applicationId) => api.get(`/api/loan-applications/${applicationId}`),

  // 获取指定用户的所有贷款申请
  getUserApplications: (userId) => api.get(`/api/loan-applications/user/${userId}`)
}