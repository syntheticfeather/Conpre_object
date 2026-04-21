<template>
  <div class="conversation-logs">
    <h2>对话日志管理</h2>

    <!-- 搜索和过滤 -->
    <el-card class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="会话 ID">
          <el-input 
            v-model="searchForm.sessionId" 
            placeholder="输入会话 ID" 
            clearable
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="标记">
          <el-select v-model="searchForm.flagged" placeholder="全部" clearable>
            <el-option label="正常" :value="false" />
            <el-option label="问题对话" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchLogs">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="exportLogs">导出日志</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 会话列表 -->
    <el-card class="logs-list">
      <div class="list-header">
        <h3>会话列表（共 {{ total }} 条）</h3>
        <el-button type="danger" :disabled="selectedSessions.length === 0" @click="batchDelete">
          批量删除 ({{ selectedSessions.length }})
        </el-button>
      </div>
      
      <el-table 
        :data="paginatedSessions" 
        stripe 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="session_id" label="会话 ID" width="280" show-overflow-tooltip />
        <el-table-column prop="message_count" label="消息数" width="90" sortable />
        <el-table-column prop="start_time" label="开始时间" width="180" sortable />
        <el-table-column prop="last_message" label="最后一条消息" show-overflow-tooltip />
        <el-table-column label="标记" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_flagged ? 'danger' : 'success'">
              {{ row.is_flagged ? '问题对话' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.session_id)">查看详情</el-button>
            <el-button 
              :type="row.is_flagged ? 'warning' : 'danger'" 
              size="small" 
              @click="toggleFlag(row)"
            >
              {{ row.is_flagged ? '取消标记' : '标记问题' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 会话详情对话框 -->
    <el-dialog v-model="detailVisible" title="会话详情" width="900px">
      <div class="chat-detail">
        <div v-for="(msg, index) in currentMessages" :key="index" class="message-item">
          <div class="message-header">
            <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small">
              {{ msg.role === 'user' ? '用户' : '客服' }}
            </el-tag>
            <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
          <div class="message-content">
            {{ msg.content }}
          </div>
          <div v-if="msg.is_tool_call" class="tool-call-tag">
            <el-tag type="warning" size="small">工具调用</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/logs'

const loading = ref(false)
const detailVisible = ref(false)

const searchForm = ref({
  sessionId: '',
  dateRange: [],
  flagged: null
})

const sessionsList = ref([])
const filteredSessions = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedSessions = ref([])

const currentMessages = ref([])
const currentSessionId = ref('')

const paginatedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredSessions.value.slice(start, end)
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await axios.get(API_BASE)
    sessionsList.value = res.data || []
    filteredSessions.value = sessionsList.value
    total.value = filteredSessions.value.length
  } catch (error) {
    ElMessage.error('获取日志失败')
    console.error('Error fetching logs:', error)
  } finally {
    loading.value = false
  }
}

const searchLogs = () => {
  const { sessionId, dateRange, flagged } = searchForm.value
  
  filteredSessions.value = sessionsList.value.filter(session => {
    const matchSessionId = !sessionId || session.session_id.includes(sessionId)
    const matchFlagged = flagged === null || session.is_flagged === flagged
    
    let matchDate = true
    if (dateRange && dateRange.length === 2) {
      const sessionDate = new Date(session.start_time)
      const startDate = new Date(dateRange[0])
      const endDate = new Date(dateRange[1])
      endDate.setHours(23, 59, 59, 999)
      matchDate = sessionDate >= startDate && sessionDate <= endDate
    }
    
    return matchSessionId && matchFlagged && matchDate
  })
  
  total.value = filteredSessions.value.length
  currentPage.value = 1
}

const resetSearch = () => {
  searchForm.value = {
    sessionId: '',
    dateRange: [],
    flagged: null
  }
  searchLogs()
}

const viewDetail = async (sessionId) => {
  try {
    const res = await axios.get(`${API_BASE}/${sessionId}`)
    currentMessages.value = res.data.messages || []
    currentSessionId.value = sessionId
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取会话详情失败'+error)
  }
}

const toggleFlag = async (row) => {
  try {
    await axios.post(`${API_BASE}/${row.session_id}/flag`, {
      is_flagged: !row.is_flagged
    })
    ElMessage.success(row.is_flagged ? '已取消标记' : '已标记为问题对话')
    fetchLogs()
  } catch (error) {
    ElMessage.error('操作失败'+error)
  }
}

const handleSelectionChange = (selection) => {
  selectedSessions.value = selection.map(s => s.session_id)
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedSessions.value.length} 条会话吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await axios.post(`${API_BASE}/batch-delete`, {
      session_ids: selectedSessions.value
    })
    
    ElMessage.success('批量删除成功')
    selectedSessions.value = []
    fetchLogs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
  // 分页变化
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

const exportLogs = () => {
  ElMessage.info('导出功能开发中...')
}

onMounted(fetchLogs)
</script>

<style scoped>
.conversation-logs {
  padding: 20px;
}
.search-form {
  margin-bottom: 20px;
}
.logs-list {
  margin-top: 10px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.list-header h3 {
  margin: 0;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.chat-detail {
  max-height: 600px;
  overflow-y: auto;
}
.message-item {
  padding: 15px;
  margin-bottom: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.message-time {
  font-size: 12px;
  color: #909399;
}
.message-content {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
}
.tool-call-tag {
  margin-top: 8px;
}
</style>
