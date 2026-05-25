import request from '@/utils/request'

const promptsAPI = {
  getAll: () => request.get('/prompts'),
  getActive: () => request.get('/prompts/active'),
  getById: (id) => request.get(`/prompts/${id}`),
  create: (data) => request.post('/prompts', data.content, { 
    params: { name: data.name, version: data.version || '1.0' } 
  }),
  update: (id, data) => request.put(`/prompts/${id}`, data),
  delete: (id) => request.delete(`/prompts/${id}`),
}

export default promptsAPI
