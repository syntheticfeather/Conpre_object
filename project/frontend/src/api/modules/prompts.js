import request from '@/utils/request'

const promptsAPI = {
  getAll: () => request.get('/prompts'),
  getActive: () => request.get('/prompts/active'),
  getById: (id) => request.get(`/prompts/${id}`),
  create: (name, content, version) => request.post('/prompts', content, {
    params: { name, version: version || '1.0' }
  }),
  update: (id, data) => request.put(`/prompts/${id}`, data),
  activate: (id) => request.put(`/prompts/${id}/activate`),
  deactivate: (id) => request.put(`/prompts/${id}/deactivate`),
  delete: (id) => request.delete(`/prompts/${id}`),
}

export default promptsAPI
