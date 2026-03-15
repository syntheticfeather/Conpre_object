import { defineStore } from 'pinia'
import { userAPI } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    users: [],
    currentPage: 1,
    pageSize: 10,
    blacklist: [],
    blacklistPage: 1,
    selectedUserId: null,
    userDetail: null,
    selectedBlacklistUserId: null,
    blacklistUserDetail: null
  }),

  getters: {
    totalPages: (state) => Math.ceil(state.users.length / state.pageSize),
    blacklistTotalPages: (state) => Math.ceil(state.blacklist.length / state.pageSize),
    paginatedUsers: (state) => {
      const start = (state.currentPage - 1) * state.pageSize
      return state.users.slice(start, start + state.pageSize)
    },
    paginatedBlacklist: (state) => {
      const start = (state.blacklistPage - 1) * state.pageSize
      return state.blacklist.slice(start, start + state.pageSize)
    }
  },

  actions: {
    async fetchUserStats() {
      try {
        const res = await userAPI.getUserStats()
        if (res.code === 200) {
          this.users = res.data || []
          this.currentPage = 1
        }
      } catch (error) {
        console.error('获取用户列表出错:', error)
        throw error
      }
    },

    async fetchBlacklist() {
      try {
        const res = await userAPI.getBlacklist()
        if (res.code === 200) {
          this.blacklist = res.data || []
          this.blacklistPage = 1
        }
      } catch (error) {
        console.error('获取黑名单出错:', error)
        throw error
      }
    },

    async fetchUserDetail(userId) {
      try {
        const res = await userAPI.getUserDetail(userId)
        if (res.code === 200) {
          this.userDetail = res.data
          this.selectedUserId = userId
        }
      } catch (error) {
        console.error('获取用户详情出错:', error)
        throw error
      }
    },

    async fetchBlacklistUserDetail(userId) {
      try {
        const res = await userAPI.getUserDetail(userId)
        if (res.code === 200) {
          this.blacklistUserDetail = res.data
          this.selectedBlacklistUserId = userId
        }
      } catch (error) {
        console.error('获取黑名单用户详情出错:', error)
        throw error
      }
    },

    async addToBlacklist(userId, blackLevel) {
      try {
        const res = await userAPI.addToBlacklist({ userId, blackLevel })
        if (res.code === 200) {
          await this.fetchUserStats()
          await this.fetchBlacklist()
          return { success: true }
        }
        return { success: false, message: res.message }
      } catch (error) {
        console.error('加入黑名单出错:', error)
        return { success: false, message: error.response?.data?.message || '操作失败' }
      }
    },

    async removeFromBlacklist(userId) {
      try {
        const res = await userAPI.removeFromBlacklist(userId)
        if (res.code === 200) {
          await this.fetchUserStats()
          await this.fetchBlacklist()
          return { success: true }
        }
        return { success: false, message: res.message }
      } catch (error) {
        console.error('解除黑名单出错:', error)
        return { success: false, message: error.response?.data?.message || '操作失败' }
      }
    },

    clearUserDetail() {
      this.userDetail = null
      this.selectedUserId = null
    },

    clearBlacklistUserDetail() {
      this.blacklistUserDetail = null
      this.selectedBlacklistUserId = null
    },

    setCurrentPage(page) {
      this.currentPage = page
    },

    setBlacklistPage(page) {
      this.blacklistPage = page
    }
  }
})
