// src/api/modules/loan.js
import request from '@/utils/request'

const loanAPI = {
  // 获取所有贷款产品
  getProducts: () => request.get('/loan-products/admin'),
  // 获取单个产品详情
  getProduct: (id) => request.get(`/loan-products/admin/${id}`),
  // 添加新产品
  addProduct: (productData) => request.post('/loan-products/admin', productData),
  // 更新产品
  updateProduct: (id, productData) =>
    request.patch(`/loan-products/admin/products/${id}`, productData),
  // 删除产品
  deleteProduct: (id) => request.delete(`/loan-products/admin/products/${id}`),
  // 切换产品状态
  toggleStatus: (id, action) => request.post(`/loan-products/admin/${id}/${action}`),
  // 批量删除产品
  batchDeleteProducts: (productIds) =>
    request.post('/loan-products/admin/products/batch-delete', { productIds }),
  // 批量创建产品选项
  batchCreateOptions: (options) =>
    request.post('/loan-products/admin/options/batch-create', options),
  // 删除单个产品选项
  deleteOption: (optionId) => request.delete(`/loan-products/admin/options/${optionId}`),
  // 批量删除产品选项
  batchDeleteOptions: (optionIds) =>
    request.post('/loan-products/admin/options/batch-delete', { optionIds }),
  // 根据更新/创建时间查询产品列表
  searchProductsByTime: (params) => request.get('/loan-products', { params }),
}

export default loanAPI
