// stores/auth.js
import { defineStore } from 'pinia'
import { authAPI } from '@/api'

export const useAuthStore = defineStore('auth', {
  // stores/auth.js
  state: () => {
    const token = localStorage.getItem('admin_token')
    let userInfo = null
    
    const userInfoStr = localStorage.getItem('user_info')
    if (userInfoStr && userInfoStr !== 'undefined' && userInfoStr !== 'null') {
      try {
        userInfo = JSON.parse(userInfoStr)
      } catch (e) {
        console.warn('Failed to parse user_info from localStorage', e)
        userInfo = null
      }
    }

    return {
      token: token || null,
      userInfo,
      isAuthenticated: !!token
    }
  },

  getters: {
    isAdmin() {
      return this.userInfo?.role === 'ADMIN'
    }
  },

  actions: {
    // 密码登录
    async loginByPassword(phone, password) {
      try {
        const response = await authAPI.loginByPassword(phone, password)
        return this.handleLoginResponse(response)
      } catch (error) {
        return this.handleError(error, '密码登录失败')
      }
    },

    // 短信验证码登录
    async loginBySms(phone, code) {
      try {
        const response = await authAPI.loginBySms(phone, code)
        return this.handleLoginResponse(response)
      } catch (error) {
        return this.handleError(error, '短信登录失败')
      }
    },

    // 公共成功处理逻辑
    handleLoginResponse(response) {
      if (response.data?.code === 200) {
        const { token, user } = response.data.data
        this.token = token
        this.userInfo = user
        this.isAuthenticated = true

        localStorage.setItem('admin_token', token)
        localStorage.setItem('user_info', JSON.stringify(user))

        return { success: true }
      }
      return { 
        success: false, 
        message: response.data?.message || '登录响应异常' 
      }
    },

    // 公共错误处理逻辑
    handleError(error, defaultMsg) {
      console.error(defaultMsg, error)
      return {
        success: false,
        message: error.response?.data?.message || defaultMsg || '登录失败，请稍后重试'
      }
    },

    // 用于设置已获取的认证信息
    setAuthInfo({ token, user }) {
      this.token = token
      this.userInfo = user
      this.isAuthenticated = true
      
      localStorage.setItem('admin_token', token)
      localStorage.setItem('user_info', JSON.stringify(user))
    },

    // 退出登录
    logout() {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('user_info')
      this.$reset() // Pinia 提供的重置 state 方法（推荐）
    }
  }
})