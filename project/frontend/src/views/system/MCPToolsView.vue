<template>
  <div class="mcp-tools">
    <div class="header">MCP 服务器与工具管理</div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="main-tabs">
      <!-- Tab 1: MCP 服务器管理 -->
      <el-tab-pane label="MCP 服务器" name="mcp">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>{{ isEditing ? '编辑 MCP 服务器' : '添加 MCP 服务器' }}</span>
            </div>
          </template>
          <el-form :model="serverForm" label-width="100px" :rules="formRules" ref="formRef">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="服务器 ID" prop="server_id">
                  <el-input v-model="serverForm.server_id" placeholder="唯一标识符，如 weather-server" :disabled="isEditing" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="传输方式" prop="transport">
                  <el-select v-model="serverForm.transport" placeholder="选择传输方式">
                    <el-option label="SSE" value="sse" />
                    <el-option label="stdio" value="stdio" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20" v-if="serverForm.transport === 'sse'">
              <el-col :span="12">
                <el-form-item label="服务地址" prop="url">
                  <el-input v-model="serverForm.url" placeholder="http://localhost:8080/sse" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="API Key">
                  <el-input v-model="serverForm.api_key" placeholder="可选，用于认证" show-password />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20" v-if="serverForm.transport === 'stdio'">
              <el-col :span="12">
                <el-form-item label="命令" prop="command">
                  <el-input v-model="serverForm.command" placeholder="如 npx, node, python" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="参数">
                  <el-input v-model="argsInput" placeholder="用逗号分隔，如 -y,@modelcontextprotocol/server-weather" @change="parseArgs" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="超时时间(秒)">
                  <el-input-number v-model="serverForm.timeout" :min="5" :max="300" :step="5" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="saveServer">{{ isEditing ? '更新' : '添加' }}</el-button>
              <el-button v-if="isEditing" @click="cancelEdit">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="servers-card">
          <template #header>
            <div class="card-header">
              <span>已配置的 MCP 服务器（共 {{ serversList.length }} 个）</span>
              <div class="header-right">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索服务器 ID"
                  clearable
                  style="width: 220px; margin-right: 10px"
                  @clear="fetchServers"
                  @keyup.enter="searchServers"
                >
                  <template #suffix>
                    <el-icon class="search-icon" @click="searchServers" title="点击执行搜索"><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="fetchServers">刷新</el-button>
              </div>
            </div>
          </template>

          <el-table :data="serversList" stripe v-loading="loading" empty-text="暂无 MCP 服务器数据">
            <el-table-column prop="server_id" label="服务器 ID" width="200">
              <template #default="{ row }">
                <el-tag type="primary">{{ row.server_id }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="transport" label="传输方式" width="100">
              <template #default="{ row }">
                <el-tag :type="row.transport === 'sse' ? 'success' : 'warning'" size="small">
                  {{ row.transport?.toUpperCase() }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="url" label="服务地址 / 命令">
              <template #default="{ row }">
                <span v-if="row.transport === 'sse'">{{ row.url || '-' }}</span>
                <span v-else>{{ row.command }} {{ (row.args || []).join(' ') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="timeout" label="超时(s)" width="80" align="center">
              <template #default="{ row }">
                {{ row.timeout || 30 }}
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="创建时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editServer(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteServer(row.server_id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 工具管理 -->
      <el-tab-pane label="工具管理" name="tools">
        <el-card class="tools-card">
          <template #header>
            <div class="card-header">
              <span>已注册工具（共 {{ toolsList.length }} 个）</span>
              <div class="header-right">
                <el-input
                  v-model="toolSearchKeyword"
                  placeholder="搜索工具名称"
                  clearable
                  style="width: 220px; margin-right: 10px"
                  @clear="fetchTools"
                  @keyup.enter="searchTools"
                >
                  <template #suffix>
                    <el-icon class="search-icon" @click="searchTools" title="点击执行搜索"><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="fetchTools">刷新</el-button>
              </div>
            </div>
          </template>

          <el-table :data="toolsList" stripe v-loading="toolsLoading" empty-text="暂无工具数据">
            <el-table-column prop="name" label="工具名称" width="220">
              <template #default="{ row }">
                <el-tag type="primary">{{ row.name }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述">
              <template #default="{ row }">
                <ContentTooltip :content="row.description || '暂无描述'">
                  <span class="cell-text">{{ row.description || '暂无描述' }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="100">
              <template #default="{ row }">
                <el-tag :type="row.source === 'mcp' ? 'success' : 'info'" size="small">
                  {{ row.source === 'mcp' ? 'MCP' : '内置' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  :type="row.enabled ? 'warning' : 'success'"
                  @click="toggleToolStatus(row)"
                >
                  {{ row.enabled ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-model="chatVisible" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import toolsAPI from '@/api/modules/tools'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import ContentTooltip from '@/components/shared/ContentTooltip.vue'

const activeTab = ref('mcp')

const loading = ref(false)
const toolsLoading = ref(false)
const serversList = ref([])
const toolsList = ref([])
const searchKeyword = ref('')
const toolSearchKeyword = ref('')
const chatVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)
const argsInput = ref('')

const serverForm = ref({
  server_id: '',
  transport: 'sse',
  url: '',
  api_key: '',
  command: '',
  args: [],
  timeout: 30
})

const formRules = {
  server_id: [{ required: true, message: '请输入服务器 ID', trigger: 'blur' }],
  transport: [{ required: true, message: '请选择传输方式', trigger: 'change' }],
  url: [{ required: true, message: '请输入服务地址', trigger: 'blur' }],
  command: [{ required: true, message: '请输入命令', trigger: 'blur' }]
}

const parseArgs = () => {
  if (!argsInput.value.trim()) {
    serverForm.value.args = []
    return
  }
  serverForm.value.args = argsInput.value.split(',').map(a => a.trim()).filter(Boolean)
}

const resetForm = () => {
  serverForm.value = {
    server_id: '',
    transport: 'sse',
    url: '',
    api_key: '',
    command: '',
    args: [],
    timeout: 30
  }
  argsInput.value = ''
  isEditing.value = false
}

const fetchServers = async () => {
  loading.value = true
  try {
    const res = await toolsAPI.getMCPServers()
    serversList.value = res.data || []
  } catch {
    ElMessage.error('获取 MCP 服务器列表失败')
  } finally {
    loading.value = false
  }
}

const searchServers = async () => {
  if (!searchKeyword.value.trim()) {
    fetchServers()
    return
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  try {
    const res = await toolsAPI.getMCPServers()
    const all = res.data || []
    serversList.value = all.filter(s => s.server_id.toLowerCase().includes(keyword))
    if (serversList.value.length === 0) {
      ElMessage.warning('未找到匹配的服务器')
    }
  } catch {
    ElMessage.error('搜索失败')
  }
}

const saveServer = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const payload = { ...serverForm.value }

    try {
      if (isEditing.value) {
        await toolsAPI.removeMCPServer(payload.server_id)
        await toolsAPI.addMCPServer(payload)
        ElMessage.success('更新成功')
      } else {
        await toolsAPI.addMCPServer(payload)
        ElMessage.success('添加成功')
      }
      resetForm()
      fetchServers()
    } catch {
      ElMessage.error(isEditing.value ? '更新失败' : '添加失败')
    }
  })
}

const editServer = (row) => {
  isEditing.value = true
  serverForm.value = {
    server_id: row.server_id,
    transport: row.transport || 'sse',
    url: row.url || '',
    api_key: row.api_key || '',
    command: row.command || '',
    args: row.args || [],
    timeout: row.timeout || 30
  }
  argsInput.value = (row.args || []).join(',')
}

const cancelEdit = () => {
  resetForm()
}

const deleteServer = async (serverId) => {
  try {
    await ElMessageBox.confirm(`确定要删除 MCP 服务器「${serverId}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await toolsAPI.removeMCPServer(serverId)
    ElMessage.success('删除成功')
    if (isEditing.value && serverForm.value.server_id === serverId) {
      resetForm()
    }
    fetchServers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const fetchTools = async () => {
  toolsLoading.value = true
  try {
    const res = await toolsAPI.getAll()
    toolsList.value = res.data || []
  } catch {
    ElMessage.error('获取工具列表失败')
  } finally {
    toolsLoading.value = false
  }
}

const searchTools = async () => {
  if (!toolSearchKeyword.value.trim()) {
    fetchTools()
    return
  }
  try {
    const res = await toolsAPI.search(toolSearchKeyword.value.trim())
    if (res.data) {
      toolsList.value = [res.data]
    } else {
      toolsList.value = []
      ElMessage.warning('未找到匹配的工具')
    }
  } catch {
    ElMessage.error('搜索失败')
  }
}

const toggleToolStatus = async (tool) => {
  try {
    const newStatus = !tool.enabled
    await toolsAPI.updateStatus(tool.name, newStatus)
    ElMessage.success(newStatus ? '已启用' : '已禁用')
    await fetchTools()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleTabChange = (tab) => {
  if (tab === 'tools' && toolsList.value.length === 0) {
    fetchTools()
  }
}

onMounted(fetchServers)
</script>

<style scoped>
.mcp-tools {
  padding: 20px;
  padding-top: 0;
}
.form-card,
.servers-card,
.tools-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-table .cell) {
  padding: 0px 10px;
}

.chat-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: var(--fab-gradient);
  color: var(--fab-text);
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 4px 16px var(--fab-shadow);
  z-index: 9998;
  transition: transform 0.2s, box-shadow 0.2s;
}

.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px var(--fab-shadow-hover);
}

.search-icon {
  cursor: pointer;
  font-size: 16px;
  color: var(--color-info);
  transition: color 0.2s;
}

.search-icon:hover {
  color: var(--color-primary);
}
</style>
