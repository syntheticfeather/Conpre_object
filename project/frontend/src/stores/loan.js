import { defineStore } from 'pinia'
import { loanAPI } from '@/api'

export const useLoanStore = defineStore('loan', {
  state: () => ({
    products: [],
    currentProduct: null,
    loading: false,
    error: null
  }),

  getters: {
    hasProducts: (state) => state.products.length > 0,
    activeProducts: (state) => state.products.filter((p) => p.status === 'ACTIVE')
  },

  actions: {
    async fetchProducts() {
      this.loading = true
      try {
        const response = await loanAPI.getProducts()
        if (response.code === 200) {
          this.products = response.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('获取产品列表出错:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchProduct(productId) {
      this.loading = true
      try {
        const response = await loanAPI.getProduct(productId)
        if (response.code === 200) {
          this.currentProduct = response.data
        }
      } catch (error) {
        this.error = error.message
        console.error('获取产品详情出错:', error)
      } finally {
        this.loading = false
      }
    },

    async addProduct(productData) {
      this.loading = true
      try {
        const response = await loanAPI.addProduct(productData)
        if (response.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '添加失败' }
      } finally {
        this.loading = false
      }
    },

    async updateProduct(productId, productData) {
      this.loading = true
      try {
        const response = await loanAPI.updateProduct(productId, productData)
        if (response.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '更新失败' }
      } finally {
        this.loading = false
      }
    },

    async deleteProduct(productId) {
      this.loading = true
      try {
        const response = await loanAPI.deleteProduct(productId)
        if (response.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '删除失败' }
      } finally {
        this.loading = false
      }
    },

    async toggleProductStatus(productId, action) {
      this.loading = true
      try {
        const response = await loanAPI.toggleStatus(productId, action)
        if (response.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.message }
      } catch (error) {
        this.error = error.message
        return { success: false, message: error.response?.data?.message || '操作失败' }
      } finally {
        this.loading = false
      }
    },

    clearCurrentProduct() {
      this.currentProduct = null
    },

    clearError() {
      this.error = null
    }
  }
})
