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
      this.applyTheme()
    },

    toggleTheme() {
      this.theme = this.theme === 'light' ? 'dark' : 'light'
      this.applyTheme()
    },

    applyTheme() {
      if (this.theme === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },

    resetState() {
      this.loading = false
      this.loadingCount = 0
    },

    initTheme() {
      this.applyTheme()
    }
  },

  persist: {
    key: 'app-store',
    storage: localStorage,
    paths: ['sidebarCollapsed', 'theme']
  }
})
