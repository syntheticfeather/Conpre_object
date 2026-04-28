import { defineStore } from 'pinia'
import { applicationAPI } from '@/api'

export const useApplicationStore = defineStore('application', {
  state: () => ({
    pendingApplications: [],
    completedApplications: [],
    currentApplication: null,
    pendingPostponeRequests: [],
    completedPostponeRequests: [],
    loading: false,
    error: null
  }),

  getters: {
    hasPending: (state) => state.pendingApplications.length > 0,
    hasCompleted: (state) => state.completedApplications.length > 0,
    pendingCount: (state) => state.pendingApplications.length
  },

  actions: {
    async fetchPendingApplications() {
      this.loading = true
      try {
        const response = await applicationAPI.getPendingApplications()
        if (response.code === 200) {
          this.pendingApplications = response.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('获取待处理申请出错:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchCompletedApplications() {
      this.loading = true
      try {
        const response = await applicationAPI.getCompletedApplications()
        if (response.code === 200) {
          this.completedApplications = response.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('获取已完成申请出错:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchPendingPostponeRequests() {
      this.loading = true
      try {
        const response = await applicationAPI.getPendingPostponeRequests()
        if (response.code === 200) {
          this.pendingPostponeRequests = response.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('获取待审核延期申请出错:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchCompletedPostponeRequests() {
      this.loading = true
      try {
        const response = await applicationAPI.getCompletedPostponeRequests()
        if (response.code === 200) {
          this.completedPostponeRequests = response.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('获取已审核延期申请出错:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchApplicationDetail(applicationId) {
      this.loading = true
      try {
        const response = await applicationAPI.getApplicationDetail(applicationId)
        if (response.code === 200) {
          this.currentApplication = response.data
        }
      } catch (error) {
        this.error = error.message
        console.error('获取申请详情出错:', error)
      } finally {
        this.loading = false
      }
    },

    async submitReview(reviewData) {
      this.loading = true
      try {
        const response = await applicationAPI.submitReview(reviewData)
        if (response.code === 200) {
          await this.fetchPendingApplications()
          await this.fetchCompletedApplications()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '提交失败' }
      } finally {
        this.loading = false
      }
    },

    async approvePostpone(requestId) {
      this.loading = true
      try {
        const response = await applicationAPI.approvePostpone(requestId)
        if (response.code === 200) {
          await this.fetchPendingPostponeRequests()
          await this.fetchCompletedPostponeRequests()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '审核失败' }
      } finally {
        this.loading = false
      }
    },

    async rejectPostpone(requestId, reason) {
      this.loading = true
      try {
        const response = await applicationAPI.rejectPostpone(requestId, reason)
        if (response.code === 200) {
          await this.fetchPendingPostponeRequests()
          await this.fetchCompletedPostponeRequests()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '审核失败' }
      } finally {
        this.loading = false
      }
    },

    clearCurrentApplication() {
      this.currentApplication = null
    },

    clearError() {
      this.error = null
    }
  }
})
