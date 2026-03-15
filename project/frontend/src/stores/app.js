import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    loading: false,
    loadingCount: 0,
    sidebarCollapsed: false,
    theme: 'light'
  }),

  getters: {
    isLoading: (state) => state.loadingCount > 0
  },

  actions: {
    showLoading() {
      this.loadingCount++
      this.loading = true
    },

    hideLoading() {
      if (this.loadingCount > 0) {
        this.loadingCount--
      }
      this.loading = this.loadingCount > 0
    },

    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    setTheme(theme) {
      this.theme = theme
    },

    resetState() {
      this.loading = false
      this.loadingCount = 0
    }
  },

  persist: {
    key: 'app-store',
    storage: localStorage,
    paths: ['sidebarCollapsed', 'theme']
  }
})
