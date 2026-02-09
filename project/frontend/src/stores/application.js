import { defineStore } from 'pinia'
import { applicationAPI } from '@/api'

export const useApplicationStore = defineStore('application', {
  state: () => ({
    pendingApplications: [],
    completedApplications: [],
    currentApplication: null,
    loading: false,
    error: null
  }),

  actions: {
    async fetchPendingApplications() {
      this.loading = true
      try {
        const response = await applicationAPI.getPendingApplications()
        if (response.data.code === 200) {
          this.pendingApplications = response.data.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch pending applications:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchCompletedApplications() {
      this.loading = true
      try {
        const response = await applicationAPI.getCompletedApplications()
        if (response.data.code === 200) {
          this.completedApplications = response.data.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch completed applications:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchApplicationDetail(applicationId) {
      this.loading = true
      try {
        const response = await applicationAPI.getApplicationDetail(applicationId)
        if (response.data.code === 200) {
          this.currentApplication = response.data.data
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch application detail:', error)
      } finally {
        this.loading = false
      }
    },

    async submitReview(reviewData) {
      this.loading = true
      try {
        const response = await applicationAPI.submitReview(reviewData)
        if (response.data.code === 200) {
          // 刷新列表
          await this.fetchPendingApplications()
          await this.fetchCompletedApplications()
          return { success: true }
        }
        return { success: false, message: response.data.message }
      } catch (error) {
        this.error = error.message
        return { 
          success: false, 
          message: error.response?.data?.message || '提交失败' 
        }
      } finally {
        this.loading = false
      }
    },

    clearCurrentApplication() {
      this.currentApplication = null
    }
  }
})