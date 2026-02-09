// stores/user.js
import { defineStore } from 'pinia'
import { userAPI } from '@/api'

export const useUserStore = defineStore('user', {
  // ========== state ==========
  state: () => ({
    // 用户列表
    users: [],
    currentPage: 1,
    pageSize: 10,

    // 黑名单列表
    blacklist: [],
    blacklistPage: 1,

    // 当前选中用户
    selectedUserId: null,
    userDetail: null,

    // 当前选中黑名单用户
    selectedBlacklistUserId: null,
    blacklistUserDetail: null
  }),

  // ========== getters ==========
  getters: {
    totalPages(state) {
      return Math.ceil(state.users.length / state.pageSize)
    },

    blacklistTotalPages(state) {
      return Math.ceil(state.blacklist.length / state.pageSize)
    },

    paginatedUsers(state) {
      const start = (state.currentPage - 1) * state.pageSize
      return state.users.slice(start, start + state.pageSize)
    },

    paginatedBlacklist(state) {
      const start = (state.blacklistPage - 1) * state.pageSize
      return state.blacklist.slice(start, start + state.pageSize)
    }
  },

  // ========== actions ==========
  actions: {
    // 获取用户统计/列表
    async fetchUserStats() {
      try {
        const res = await userAPI.getUserStats()
        if (res.data?.code === 200) {
          this.users = res.data.data || []
          this.currentPage = 1
        } else {
          console.warn('获取用户列表失败:', res.data?.message)
        }
      } catch (error) {
        console.error('请求用户列表出错:', error)
        throw error
      }
    },

    // 获取黑名单列表
    async fetchBlacklist() {
      try {
        const res = await userAPI.getBlacklist()
        if (res.data?.code === 200) {
          this.blacklist = res.data.data || []
          this.blacklistPage = 1
        } else {
          console.warn('获取黑名单失败:', res.data?.message)
        }
      } catch (error) {
        console.error('请求黑名单出错:', error)
        throw error
      }
    },

    // 获取普通用户详情
    async fetchUserDetail(userId) {
      try {
        const res = await userAPI.getUserDetail(userId)
        if (res.data?.code === 200) {
          this.userDetail = res.data.data
          this.selectedUserId = userId
        } else {
          console.warn('获取用户详情失败:', res.data?.message)
        }
      } catch (error) {
        console.error('获取用户详情出错:', error)
        throw error
      }
    },

    // 获取黑名单用户详情（复用同一接口）
    async fetchBlacklistUserDetail(userId) {
      try {
        const res = await userAPI.getUserDetail(userId)
        if (res.data?.code === 200) {
          this.blacklistUserDetail = res.data.data
          this.selectedBlacklistUserId = userId
        } else {
          console.warn('获取黑名单用户详情失败:', res.data?.message)
        }
      } catch (error) {
        console.error('获取黑名单用户详情出错:', error)
        throw error
      }
    },

    // 添加用户到黑名单
    async addToBlacklist(userId, blackLevel) {
      try {
        // 注意：后端期望接收一个对象，如 { userId, level }
        const res = await userAPI.addToBlacklist({ userId, blackLevel })
        if (res.data?.code === 200) {
          // 刷新两个列表
          await this.fetchUserStats()
          await this.fetchBlacklist()
        } else {
          console.warn('加入黑名单失败:', res.data?.message)
          throw new Error(res.data?.message || '操作失败')
        }
      } catch (error) {
        console.error('加入黑名单出错:', error)
        throw error
      }
    },

    // 从黑名单移除用户
    async removeFromBlacklist(userId) {
      try {
        const res = await userAPI.removeFromBlacklist(userId)
        if (res.data?.code === 200) {
          await this.fetchUserStats()
          await this.fetchBlacklist()
        } else {
          console.warn('解除黑名单失败:', res.data?.message)
          throw new Error(res.data?.message || '操作失败')
        }
      } catch (error) {
        console.error('解除黑名单出错:', error)
        throw error
      }
    },

    // 重置普通用户详情
    resetUserDetail() {
      this.userDetail = null
      this.selectedUserId = null
    },

    // 重置黑名单用户详情
    resetBlacklistUserDetail() {
      this.blacklistUserDetail = null
      this.selectedBlacklistUserId = null
    }
  }
})