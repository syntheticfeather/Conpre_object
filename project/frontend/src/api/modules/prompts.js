import request from '@/utils/request'

const promptsAPI = {
  getAll: () => request.get('/prompts'),
  // category：提示词档位（base/general/query_status/...），缺省 base
  getActive: (category = 'base') => request.get('/prompts/active', { params: { category } }),
  getById: (id) => request.get(`/prompts/${id}`),
  // 对齐后端 PromptCreate：name/category/version 进 JSON body（旧版 query 传参已废弃，会 422）
  create: (data) => request.post('/prompts', {
    name: data.name,
    category: data.category || 'base',
    version: data.version || undefined,
    content: data.content
  }),
  update: (id, data) => request.put(`/prompts/${id}`, data),
  // 激活/停用走后端专用接口（会清空组装缓存，不等 TTL）
  activate: (id) => request.put(`/prompts/${id}/activate`),
  deactivate: (id) => request.put(`/prompts/${id}/deactivate`),
  delete: (id) => request.delete(`/prompts/${id}`),
}

export default promptsAPI
