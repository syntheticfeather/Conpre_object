import { defineStore } from 'pinia'
import { authAPI } from '@/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    userInfo: null,
    isAuthenticated: false
  }),

  getters: {
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    isLoggedIn: (state) => !!state.token && state.isAuthenticated,
    userName: (state) => state.userInfo?.name || '',
    userPhone: (state) => state.userInfo?.phone || ''
  },

  actions: {
    async loginByPassword(phone, password) {
      try {
        const response = await authAPI.loginByPassword(phone, password)
        return this.handleLoginSuccess(response)
      } catch (error) {
        return this.handleLoginError(error, '密码登录失败')
      }
    },

    async loginBySms(phone, code) {
      try {
        const response = await authAPI.loginBySms(phone, code)
        return this.handleLoginSuccess(response)
      } catch (error) {
        return this.handleLoginError(error, '短信登录失败')
      }
    },

    handleLoginSuccess(response) {
      if (response.data?.code === 200) {
        const { token, user } = response.data.data
        this.setAuthInfo({ token, user })
        return { success: true }
      }
      return {
        success: false,
        message: response.data?.message || '登录响应异常'
      }
    },

    handleLoginError(error, defaultMsg) {
      console.error(defaultMsg, error)
      return {
        success: false,
        message: error.response?.data?.message || defaultMsg || '登录失败，请稍后重试'
      }
    },

    setAuthInfo({ token, user }) {
      this.token = token
      this.userInfo = user
      this.isAuthenticated = true
    },

    setToken(token, phone) {
      this.token = token
      this.isAuthenticated = true
      if (this.userInfo) {
        this.userInfo.phone = phone
      } else {
        this.userInfo = { phone }
      }
    },

    async logout() {
      try {
        await authAPI.logout()
      } catch (error) {
        console.warn('后端 logout 调用失败，但已清理本地状态:', error)
      } finally {
        this.$reset()
      }
    }
  },

  persist: {
    key: 'auth-store',
    storage: localStorage,
    paths: ['token', 'userInfo', 'isAuthenticated']
  }
})
