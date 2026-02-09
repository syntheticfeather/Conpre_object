import { defineStore } from 'pinia'
import { loanAPI } from '@/api'

export const useLoanStore = defineStore('loan', {
  state: () => ({
    products: [],
    currentProduct: null,
    loading: false,
    error: null
  }),

  actions: {
    async fetchProducts() {
      this.loading = true
      try {
        const response = await loanAPI.getProducts()
        if (response.data.code === 200) {
          this.products = response.data.data || []
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch products:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchProduct(productId) {
      this.loading = true
      try {
        const response = await loanAPI.getProduct(productId)
        if (response.data.code === 200) {
          this.currentProduct = response.data.data
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch product:', error)
      } finally {
        this.loading = false
      }
    },

    async addProduct(productData) {
      this.loading = true
      try {
        const response = await loanAPI.addProduct(productData)
        if (response.data.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.data.message }
      } catch (error) {
        this.error = error.message
        return { 
          success: false, 
          message: error.response?.data?.message || '添加失败' 
        }
      } finally {
        this.loading = false
      }
    },

    async updateProduct(productId, productData) {
      this.loading = true
      try {
        const response = await loanAPI.updateProduct(productId, productData)
        if (response.data.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.data.message }
      } catch (error) {
        this.error = error.message
        return { 
          success: false, 
          message: error.response?.data?.message || '更新失败' 
        }
      } finally {
        this.loading = false
      }
    },

    async deleteProduct(productId) {
      this.loading = true
      try {
        const response = await loanAPI.deleteProduct(productId)
        if (response.data.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.data.message }
      } catch (error) {
        this.error = error.message
        return { 
          success: false, 
          message: error.response?.data?.message || '删除失败' 
        }
      } finally {
        this.loading = false
      }
    },

    async toggleProductStatus(productId, action) {
      this.loading = true
      try {
        const response = await loanAPI.toggleStatus(productId, action)
        if (response.data.code === 200) {
          await this.fetchProducts()
          return { success: true }
        }
        return { success: false, message: response.data.message }
      } catch (error) {
        this.error = error.message
        return { 
          success: false, 
          message: error.response?.data?.message || '操作失败' 
        }
      } finally {
        this.loading = false
      }
    },

    clearCurrentProduct() {
      this.currentProduct = null
    }
  }
})