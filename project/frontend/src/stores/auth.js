import { defineStore } from 'pinia'
import { authAPI, userAPI } from '@/api'
import { getUserInfoFromToken } from '@/utils/jwt'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    refreshToken: null,
    userInfo: null,
    isAuthenticated: false
  }),

  getters: {
    // 后端 role 约定：0-客户，1-管理员（注：当前后端接口不返回 role，留作未来扩展）
    isAdmin: (state) => state.userInfo?.role === 1,
    isLoggedIn: (state) => !!state.token && state.isAuthenticated,
    userName: (state) => state.userInfo?.name || state.userInfo?.userName || '',
    userPhone: (state) => state.userInfo?.phone || ''
  },

  actions: {
    async loginByPassword(phone, password) {
      try {
        const response = await authAPI.loginByPassword(phone, password)
        const result = this.handleLoginSuccess(response)
        if (result.success) await this.loadUserInfo()
        return result
      } catch (error) {
        return this.handleLoginError(error, '密码登录失败')
      }
    },

    async loginBySms(phone, code) {
      try {
        const response = await authAPI.loginBySms(phone, code)
        const result = this.handleLoginSuccess(response)
        if (result.success) await this.loadUserInfo()
        return result
      } catch (error) {
        return this.handleLoginError(error, '短信登录失败')
      }
    },

    handleLoginSuccess(response) {
      if (response.code === 200) {
        // 后端 LoginResponse 只返回 token / refreshToken；
        // userId、phone 从 JWT payload 中解码（sub=phone, claim.userId）
        const { token, refreshToken } = response.data
        const decoded = getUserInfoFromToken(token) || {}
        this.setAuthInfo({
          token,
          refreshToken,
          user: { userId: decoded.userId, phone: decoded.phone }
        })
        return { success: true }
      }
      return {
        success: false,
        message: response.message || '登录响应异常'
      }
    },

    handleLoginError(error, defaultMsg) {
      console.error(defaultMsg, error)
      return {
        success: false,
        message: error.response?.data?.message || defaultMsg || '登录失败，请稍后重试'
      }
    },

    setAuthInfo({ token, refreshToken, user }) {
      this.token = token
      this.refreshToken = refreshToken
      // 合并而非覆盖，避免后续 loadUserInfo 丢失已有字段
      this.userInfo = { ...this.userInfo, ...user }
      this.isAuthenticated = true
    },

    // 登录后 / 启动恢复时调用：补全 userName、avatar 等后端只在 /users/me 返回的字段
    async loadUserInfo() {
      if (!this.token) return
      try {
        const res = await userAPI.getCurrentUser()
        if (res.code === 200 && res.data) {
          const { userId, userName, avatar } = res.data
          this.userInfo = { ...this.userInfo, userId, name: userName, userName, avatar }
        }
      } catch (error) {
        // 获取用户信息失败不影响登录态，仅记录日志
        console.warn('[Auth] 加载用户信息失败:', error)
      }
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

    async refreshToken() {
      try {
        if (!this.refreshToken) {
          throw new Error('No refresh token available')
        }
        const response = await authAPI.refreshToken(this.refreshToken)
        if (response.code === 200) {
          const { token, refreshToken } = response.data
          this.token = token
          this.refreshToken = refreshToken
          return { success: true }
        }
        return {
          success: false,
          message: response.message || '刷新 token 失败'
        }
      } catch (error) {
        console.error('Refresh token failed', error)
        this.logout()
        return {
          success: false,
          message: error.response?.data?.message || '刷新 token 失败，请重新登录'
        }
      }
    },

    logout() {
      this.$reset()
    }
  },

  persist: {
    key: 'auth-store',
    storage: localStorage,
    paths: ['token', 'refreshToken', 'userInfo', 'isAuthenticated']
  }
})
