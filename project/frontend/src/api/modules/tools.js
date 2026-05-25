import request from '@/utils/request'

const toolsAPI = {
  getAll: () => request.get('/tools/'),
  search: (name) => request.get('/tools/search', { params: { name } }),
  updateStatus: (toolName, enabled) => request.put(`/tools/${toolName}`, null, { params: { enabled } }),
  getMCPServers: () => request.get('/tools/mcp/servers'),
  getMCPServer: (serverId) => request.get(`/tools/mcp/server/${serverId}`),
  addMCPServer: (data) => request.post('/tools/mcp/server', data),
  removeMCPServer: (serverId) => request.delete(`/tools/mcp/server/${serverId}`),
  refreshMCPTools: () => request.post('/tools/mcp/refresh'),
}

export default toolsAPI
