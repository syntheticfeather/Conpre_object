<template>
  <div class="mcp-tools">
    <div class="header">MCP 工具管理</div>

    <!-- 工具列表 -->
    <el-card class="tools-card">
      <template #header>
        <div class="card-header">
          <span>已注册的 MCP 工具</span>
          <div class="header-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索工具名称（精确匹配）"
              clearable
              style="width: 260px; margin-right: 10px"
              @clear="fetchTools"
              @keyup.enter="searchTool"
            >
              <template #suffix>
                <el-icon
                  class="search-icon"
                  @click="searchTool"
                  title="点击执行搜索"
                >
                  <Search />
                </el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="fetchTools">刷新</el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="toolsList" stripe v-loading="loading">
        <el-table-column prop="name" label="工具名称" width="250">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述">
          <template #default="{ row }">
            <ContentTooltip :content="row.description">
              <span class="cell-text">{{ row.description }}</span>
            </ContentTooltip>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'">
              {{ row.isEnabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isEnabled" 
              @change="toggleTool(row)"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工具调用日志 -->
    <el-card class="logs-card">
      <template #header>
        <div class="card-header">
          <span>工具调用日志</span>
          <el-form :inline="true" style="margin-bottom: 0">
            <el-form-item label="工具名称">
              <el-select v-model="logFilter.toolName" placeholder="全部" clearable @change="fetchToolLogs">
                <template #loading><LoadingDots /></template>
                <el-option 
                  v-for="tool in toolsList" 
                  :key="tool.name" 
                  :label="tool.name" 
                  :value="tool.name" 
                />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="logFilter.result" placeholder="全部" clearable @change="fetchToolLogs">
                <template #loading><LoadingDots /></template>
                <el-option label="成功" value="success" />
                <el-option label="失败" value="error" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="fetchToolLogs">搜索</el-button>
          </el-form>
        </div>
      </template>
      
      <el-table :data="toolLogs" stripe v-loading="logsLoading" max-height="400">
        <el-table-column prop="timestamp" label="时间" width="180" sortable />
        <el-table-column prop="tool_name" label="工具名称" width="220">
          <template #default="{ row }">
            <el-tag size="small">{{ row.tool_name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="input_params" label="输入参数" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ JSON.stringify(row.input_params) }}
          </template>
        </el-table-column>
        <el-table-column prop="output" label="输出结果">
          <template #default="{ row }">
            <ContentTooltip :content="row.output">
              <span class="cell-text">{{ row.output }}</span>
            </ContentTooltip>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="80">
          <template #default="{ row }">
            {{ row.duration }}ms
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-model="chatVisible" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import ContentTooltip from '@/components/shared/ContentTooltip.vue'
import LoadingDots from '@/components/shared/LoadingDots.vue'

const API_BASE = '/tools'
const buildUrl = (path = '') => `${API_BASE}/${path}`.replace(/\/+/g, '/')

const loading = ref(false)
const logsLoading = ref(false)
const toolsList = ref([])
const toolLogs = ref([])
const searchKeyword = ref('')
const chatVisible = ref(false)
const logFilter = ref({
  toolName: '',
  result: ''
})

const fetchTools = async () => {
  loading.value = true
  try {
    const res = await request.get(buildUrl())
    toolsList.value = (res.data || []).map(tool => ({
      ...tool,
      isEnabled: tool.enabled
    }))
  } catch {
    ElMessage.error('获取工具列表失败')
  } finally {
    loading.value = false
  }
}

const searchTool = async () => {
  if (!searchKeyword.value.trim()) {
    fetchTools()
    return
  }
  loading.value = true
  try {
    const res = await request.get(buildUrl('search'), { params: { name: searchKeyword.value.trim() } })
    const data = res.data || []
    toolsList.value = (Array.isArray(data) ? data : [data]).map(tool => ({
      ...tool,
      isEnabled: tool.enabled
    }))
    if (toolsList.value.length === 0) {
      ElMessage.warning('未找到匹配的工具')
    }
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const toggleTool = async (tool) => {
  try {
    await request.put(buildUrl(tool.name), null, { params: { enabled: tool.isEnabled } })
    ElMessage.success(tool.isEnabled ? '工具已启用' : '工具已禁用')
  } catch {
    tool.isEnabled = !tool.isEnabled
    ElMessage.error('操作失败')
  }
}

const fetchToolLogs = () => {
  // TODO: 对接后端日志接口后实现
  // logsLoading.value = true
  // try {
  //   const params = new URLSearchParams()
  //   if (logFilter.value.toolName) params.append('tool', logFilter.value.toolName)
  //   if (logFilter.value.result) params.append('status', logFilter.value.result)
  //   const res = await request.get(`${API_BASE}/logs?${params}`)
  //   toolLogs.value = res.data || []
  // } catch {
  //   ElMessage.error('获取日志失败')
  // } finally {
  //   logsLoading.value = false
  // }
}

onMounted(fetchTools)
</script>

<style scoped>
.mcp-tools {
  padding: 20px;
  padding-top: 0;
}
.tools-card,
.logs-card {
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

.cell-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>
