<template>
  <div class="mcp-tools">
    <div class="header">MCP 服务器与工具管理</div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="main-tabs">
      <!-- Tab 1: MCP 服务器管理 -->
      <el-tab-pane label="MCP 服务器" name="mcp">
        <!-- 工具条：CTA 折叠入口 + 搜索 / 刷新 -->
        <div class="tab-toolbar">
          <el-button type="primary" class="cta-btn" @click="openCreateForm">
            ＋ 添加 MCP 服务器
          </el-button>
          <div class="tab-toolbar__right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索服务器 ID"
              clearable
              class="toolbar-search"
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

        <!-- 添加 / 编辑表单：默认折叠，点 CTA 或列表「编辑」展开 -->
        <el-collapse-transition>
        <el-card v-show="showServerForm" class="form-card">
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
              <el-button v-else text @click="showServerForm = false">收起</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        </el-collapse-transition>

        <el-card class="servers-card">
          <template #header>
            <div class="card-header">
              <span>已配置的 MCP 服务器（共 {{ serversList.length }} 个）</span>
            </div>
          </template>

          <el-table :data="serversList" stripe v-loading="loading" empty-text="暂无 MCP 服务器数据">
            <el-table-column prop="server_id" label="服务器 ID" width="200">
              <template #default="{ row }">
                <el-tag type="primary">{{ row.server_id }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="传输方式" width="100">
              <template #default="{ row }">
                <el-tag :type="(row.config?.transport || 'sse') === 'sse' ? 'success' : 'warning'" size="small">
                  {{ (row.config?.transport || 'sse').toUpperCase() }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="服务地址 / 命令">
              <template #default="{ row }">
                <span v-if="(row.config?.transport || 'sse') === 'sse'">{{ row.config?.url || '-' }}</span>
                <span v-else>{{ row.config?.command }} {{ (row.config?.args || []).join(' ') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="超时(s)" width="80" align="center">
              <template #default="{ row }">
                {{ row.config?.timeout || 30 }}
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
        <div class="tab-toolbar">
          <span class="tab-toolbar__title">已注册工具（共 {{ toolsList.length }} 个）</span>
          <div class="tab-toolbar__right">
            <el-input
              v-model="toolSearchKeyword"
              placeholder="搜索工具名称"
              clearable
              class="toolbar-search"
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

        <el-card class="tools-card">

          <div class="tool-card-grid" v-loading="toolsLoading">
            <div v-if="!toolsList.length" class="tool-empty">暂无工具数据</div>
            <div v-for="tool in toolsList" :key="tool.id || tool.name" class="tool-card">
              <div class="tool-header">
                <div class="tool-icon" :class="{ 'tool-icon--mcp': tool.source === 'mcp' }">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 7h2a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h2"/>
                    <rect x="9" y="3" width="6" height="4" rx="1"/>
                  </svg>
                </div>
                <div class="tool-info">
                  <div class="tool-name">{{ tool.name || tool.toolName }}</div>
                  <div class="tool-desc-short">{{ tool.description || tool.desc || '暂无描述' }}</div>
                </div>
                <el-switch :model-value="tool.enabled" @change="toggleToolStatus(tool)" />
              </div>
              <div class="tool-desc">
                {{ tool.description || tool.desc || '暂无描述' }}
              </div>
              <div class="tool-footer">
                <el-tag size="small" :type="tool.source === 'mcp' ? 'warning' : 'info'">
                  {{ tool.source === 'mcp' ? 'MCP' : '内置' }}
                </el-tag>
                <el-button link type="primary">查看详情 ></el-button>
              </div>
            </div>
          </div>
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

// 添加/编辑表单默认折叠，点 CTA 按钮或"编辑"时展开
const showServerForm = ref(false)

const openCreateForm = () => {
  resetForm()
  showServerForm.value = true
}

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
  showServerForm.value = false
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

    const { server_id, url, api_key, command, args, timeout } = serverForm.value
    // 后端 MCPServerCreate 模型期望 config 是嵌套对象
    const payload = {
      server_id,
      config: { url, api_key, command, args, timeout }
    }

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
  showServerForm.value = true
  const cfg = row.config || {}
  serverForm.value = {
    server_id: row.server_id,
    transport: cfg.transport || 'sse',
    url: cfg.url || '',
    api_key: cfg.api_key || '',
    command: cfg.command || '',
    args: cfg.args || [],
    timeout: cfg.timeout || 30
  }
  argsInput.value = (cfg.args || []).join(',')
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

/* ===== Tab 内工具条：左 CTA / 标题，右搜索 + 刷新 ===== */
.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.tab-toolbar__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #303133);
}

.tab-toolbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-search {
  width: 220px;
}

.cta-btn {
  font-weight: 600;
  letter-spacing: 0.5px;
}

@media (max-width: 640px) {
  .tab-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .tab-toolbar__right {
    justify-content: flex-end;
  }

  .toolbar-search {
    width: 100%;
  }
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

.tool-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 16px;
}
@media (max-width: 1280px) {
  .tool-card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .tool-card-grid {
    grid-template-columns: 1fr;
  }
}
.tool-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #94a3b8;
  padding: 40px 0;
}
.tool-card {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  overflow: hidden;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}
.tool-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2D1B69, #25E0BF);
  border-radius: 12px 12px 0 0;
}
.tool-card:hover {
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  border-color: #cbd5e1;
}
.tool-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
  flex-shrink: 0;
}
.tool-icon--mcp {
  background: rgba(45, 27, 105, 0.1);
  color: #2D1B69;
}
.tool-info {
  flex: 1;
  min-width: 0;
}
.tool-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}
.tool-desc-short {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tool-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}
.tool-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}
</style>
