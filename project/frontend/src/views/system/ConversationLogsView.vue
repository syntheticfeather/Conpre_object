<template>
  <div class="cs-manage-page">
    <div class="header">对话统计</div>

    <!-- ==================== 数据统计 ==================== -->

    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="fetchStatsData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchStatsData">查询</el-button>
          <el-button @click="exportReport">导出报表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-header">
            <span class="metric-title">对话总数</span>
            <el-tag type="primary">今日 {{ stats.todayConversations }}</el-tag>
          </div>
          <div class="metric-value">{{ stats.totalConversations }}</div>
          <div class="metric-footer">
            <span :class="stats.conversationTrend >= 0 ? 'up' : 'down'">
              {{ stats.conversationTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.conversationTrend) }}%
            </span>
            <span class="metric-label">较昨日</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-header">
            <span class="metric-title">活跃用户</span>
            <el-tag type="success">今日 {{ stats.todayActiveUsers }}</el-tag>
          </div>
          <div class="metric-value">{{ stats.totalActiveUsers }}</div>
          <div class="metric-footer">
            <span :class="stats.userTrend >= 0 ? 'up' : 'down'">
              {{ stats.userTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.userTrend) }}%
            </span>
            <span class="metric-label">较昨日</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-header">
            <span class="metric-title">知识库问答对</span>
            <el-tag type="warning">{{ stats.knowledgeItems }}</el-tag>
          </div>
          <div class="metric-value">{{ stats.knowledgeItems }}</div>
          <div class="metric-footer">
            <span class="metric-label">总计</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-header">
            <span class="metric-title">平均响应时间</span>
            <el-tag :type="stats.avgResponseTime < 1000 ? 'success' : 'warning'">
              {{ stats.avgResponseTime }}ms
            </el-tag>
          </div>
          <div class="metric-value">{{ stats.avgResponseTime }}ms</div>
          <div class="metric-footer">
            <span :class="stats.responseTimeTrend <= 0 ? 'up' : 'down'">
              {{ stats.responseTimeTrend >= 0 ? '↑' : '↓' }} {{ Math.abs(stats.responseTimeTrend) }}%
            </span>
            <span class="metric-label">较昨日</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="metrics-row">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>工具调用次数</span></template>
          <div class="chart-container"><div ref="toolCallChartRef" class="chart"></div></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>问题分类分布</span></template>
          <div class="chart-container"><div ref="categoryChartRef" class="chart"></div></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>对话趋势（7 天）</span></template>
          <div class="chart-container"><div ref="trendChartRef" class="chart"></div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="metrics-row">
      <el-col :span="12">
        <el-card class="list-card">
          <template #header><span>热门问题 TOP 10</span></template>
          <el-table :data="topQuestions" stripe :show-header="false">
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="question" label="问题">
              <template #default="{ row }">
                <ContentTooltip :content="row.question || ''">
                  <span class="cell-text">{{ row.question }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="次数" width="80" sortable>
              <template #default="{ row }"><el-tag type="success">{{ row.count }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="list-card">
          <template #header><span>未解决问题 TOP 10</span></template>
          <el-table :data="unresolvedQuestions" stripe :show-header="false">
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="question" label="问题">
              <template #default="{ row }">
                <ContentTooltip :content="row.question || ''">
                  <span class="cell-text">{{ row.question }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="次数" width="80" sortable>
              <template #default="{ row }"><el-tag type="danger">{{ row.count }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="addToKnowledge(row)">添加</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="metrics-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>用户满意度</span></template>
          <div class="satisfaction-container">
            <el-progress
              type="dashboard"
              :percentage="stats.satisfactionRate"
              :color="getSatisfactionColor(stats.satisfactionRate)"
            />
            <div class="satisfaction-detail">
              <div><span class="label">好评</span><span class="value">{{ stats.positiveFeedback }}</span></div>
              <div><span class="label">差评</span><span class="value">{{ stats.negativeFeedback }}</span></div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>响应时间分布</span></template>
          <div class="chart-container"><div ref="responseTimeChartRef" class="chart"></div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />

    <!-- ==================== 对话日志 ==================== -->

    <h3>对话日志</h3>

    <el-card class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="会话 ID">
          <el-input v-model="searchForm.sessionId" placeholder="输入会话 ID" clearable />
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
            <template #loading><LoadingDots /></template>
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

    <el-card class="logs-list">
      <div class="list-header">
        <span>会话列表（共 {{ total }} 条）</span>
        <el-button type="danger" :disabled="selectedSessions.length === 0" @click="batchDelete">
          批量删除 ({{ selectedSessions.length }})
        </el-button>
      </div>
      <el-table :data="paginatedSessions" stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="session_id" label="会话 ID" width="280" show-overflow-tooltip />
        <el-table-column prop="message_count" label="消息数" width="90" sortable />
        <el-table-column prop="start_time" label="开始时间" width="180" sortable />
        <el-table-column prop="last_message" label="最后一条消息">
          <template #default="{ row }">
            <ContentTooltip :content="row.last_message || ''">
              <span class="cell-text">{{ row.last_message }}</span>
            </ContentTooltip>
          </template>
        </el-table-column>
        <el-table-column label="标记" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_flagged ? 'danger' : 'success'">{{ row.is_flagged ? '问题对话' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.session_id)">查看详情</el-button>
            <el-button :type="row.is_flagged ? 'warning' : 'danger'" size="small" @click="toggleFlag(row)">
              {{ row.is_flagged ? '取消标记' : '标记问题' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="currentPage = 1"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="会话详情" width="900px">
      <div class="chat-detail">
        <div v-for="(msg, index) in currentMessages" :key="index" class="message-item">
          <div class="message-header">
            <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small">
              {{ msg.role === 'user' ? '用户' : '客服' }}
            </el-tag>
            <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div v-if="msg.is_tool_call" class="tool-call-tag">
            <el-tag type="warning" size="small">工具调用</el-tag>
          </div>
        </div>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'
import LoadingDots from '@/components/shared/LoadingDots.vue'
import ContentTooltip from '@/components/shared/ContentTooltip.vue'

// ==================== 数据统计 ====================

const dateRange = ref([])

const stats = ref({
  totalConversations: 0,
  todayConversations: 0,
  conversationTrend: 0,
  totalActiveUsers: 0,
  todayActiveUsers: 0,
  userTrend: 0,
  knowledgeItems: 0,
  avgResponseTime: 0,
  responseTimeTrend: 0,
  satisfactionRate: 0,
  positiveFeedback: 0,
  negativeFeedback: 0
})
const topQuestions = ref([])
const unresolvedQuestions = ref([])

const toolCallChartRef = ref(null)
const categoryChartRef = ref(null)
const trendChartRef = ref(null)
const responseTimeChartRef = ref(null)

let toolCallChart = null
let categoryChart = null
let trendChart = null
let responseTimeChart = null

const getSatisfactionColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

const renderToolCallChart = (data) => {
  if (!toolCallChartRef.value) return
  if (!toolCallChart) toolCallChart = echarts.init(toolCallChartRef.value)
  toolCallChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.map(d => d.value), itemStyle: { color: '#409EFF' } }]
  })
}

const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.name, value: d.value })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  })
}

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.date), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      type: 'line', smooth: true, data: data.map(d => d.count),
      areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
      lineStyle: { color: '#409EFF' }, itemStyle: { color: '#409EFF' }
    }]
  })
}

const renderResponseTimeChart = (data) => {
  if (!responseTimeChartRef.value) return
  if (!responseTimeChart) responseTimeChart = echarts.init(responseTimeChartRef.value)
  responseTimeChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.range) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.map(d => d.count), itemStyle: { color: '#67c23a' } }]
  })
}

const fetchStatsData = async () => {
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.start_date = dateRange.value[0]
      params.end_date = dateRange.value[1]
    }
    const res = await request.get('/stats/overview', { params })
    if (res.data) {
      stats.value = { ...stats.value, ...res.data.stats }
      topQuestions.value = res.data.topQuestions || []
      unresolvedQuestions.value = res.data.unresolvedQuestions || []
      const c = res.data.charts || {}
      renderToolCallChart(c.toolCalls || defaultToolCallData)
      renderCategoryChart(c.categories || defaultCategoryData)
      renderTrendChart(c.trend || defaultTrendData)
      renderResponseTimeChart(c.responseTime || defaultResponseTimeData)
    }
  } catch {
    renderToolCallChart(defaultToolCallData)
    renderCategoryChart(defaultCategoryData)
    renderTrendChart(defaultTrendData)
    renderResponseTimeChart(defaultResponseTimeData)
  }
}

const defaultToolCallData = [
  { name: 'query_application_status', value: 156 },
  { name: 'calculate_repayment', value: 89 },
  { name: 'check_approval_progress', value: 67 },
  { name: 'modify_application', value: 34 }
]
const defaultCategoryData = [
  { name: '申请流程', value: 45 }, { name: '产品咨询', value: 30 },
  { name: '还款问题', value: 15 }, { name: '通用', value: 10 }
]
const defaultTrendData = [
  { date: '04-18', count: 50 }, { date: '04-19', count: 65 }, { date: '04-20', count: 78 },
  { date: '04-21', count: 92 }, { date: '04-22', count: 85 }, { date: '04-23', count: 95 },
  { date: '04-24', count: 102 }
]
const defaultResponseTimeData = [
  { range: '0-500ms', count: 120 }, { range: '500-1000ms', count: 85 },
  { range: '1000-2000ms', count: 45 }, { range: '2000ms+', count: 12 }
]

const addToKnowledge = (row) => {
  ElMessage.info(`将 "${row.question}" 添加到知识库功能开发中…`)
}

const exportReport = () => {
  ElMessage.info('导出报表功能开发中…')
}

// ==================== 对话日志 ====================

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

const paginatedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredSessions.value.slice(start, start + pageSize.value)
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await request.get('/logs')
    sessionsList.value = res.data || []
    filteredSessions.value = sessionsList.value
    total.value = filteredSessions.value.length
  } catch {
    ElMessage.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

const searchLogs = () => {
  const { sessionId, dateRange: dr, flagged } = searchForm.value
  filteredSessions.value = sessionsList.value.filter(session => {
    if (sessionId && !session.session_id.includes(sessionId)) return false
    if (flagged !== null && flagged !== undefined && session.is_flagged !== flagged) return false
    if (dr && dr.length === 2) {
      const sessionDate = new Date(session.start_time)
      const startDate = new Date(dr[0])
      const endDate = new Date(dr[1])
      endDate.setHours(23, 59, 59, 999)
      if (sessionDate < startDate || sessionDate > endDate) return false
    }
    return true
  })
  total.value = filteredSessions.value.length
  currentPage.value = 1
}

const resetSearch = () => {
  searchForm.value = { sessionId: '', dateRange: [], flagged: null }
  searchLogs()
}

const viewDetail = async (sessionId) => {
  try {
    const res = await request.get(`/logs/${sessionId}`)
    currentMessages.value = res.data.messages || []
    detailVisible.value = true
  } catch {
    ElMessage.error('获取会话详情失败')
  }
}

const toggleFlag = async (row) => {
  try {
    await request.post(`/logs/${row.session_id}/flag`, { is_flagged: !row.is_flagged })
    ElMessage.success(row.is_flagged ? '已取消标记' : '已标记为问题对话')
    fetchLogs()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleSelectionChange = (selection) => {
  selectedSessions.value = selection.map(s => s.session_id)
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedSessions.value.length} 条会话吗？`, '警告', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await request.post('/logs/batch-delete', { session_ids: selectedSessions.value })
    ElMessage.success('批量删除成功')
    selectedSessions.value = []
    fetchLogs()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString('zh-CN')
}

const exportLogs = () => {
  ElMessage.info('导出日志功能开发中…')
}

const handleResize = () => {
  ;[toolCallChart, categoryChart, trendChart, responseTimeChart].forEach(c => c?.resize())
}

onMounted(async () => {
  await fetchStatsData()
  await fetchLogs()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  ;[toolCallChart, categoryChart, trendChart, responseTimeChart].forEach(c => c?.dispose())
})
</script>

<style scoped>
.cs-manage-page {
  padding: 20px;
  padding-top: 0;
}
.filter-card {
  margin-bottom: 20px;
}
.metrics-row {
  margin-bottom: 20px;
}
.metric-card {
  text-align: center;
}
.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.metric-title {
  font-size: 14px;
  color: var(--color-info);
}
.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--text-color);
  margin-bottom: 10px;
}
.metric-footer {
  font-size: 12px;
  color: var(--color-info);
}
.metric-footer .up { color: var(--conversation-up-color); }
.metric-footer .down { color: var(--conversation-down-color); }
.metric-label { margin-left: 5px; }
.chart-card { min-height: 350px; }
.chart-container { height: 280px; }
.chart { width: 100%; height: 100%; }
.list-card { min-height: 350px; }
.satisfaction-container { text-align: center; padding: 20px; }
.satisfaction-detail { display: flex; justify-content: space-around; margin-top: 20px; }
.satisfaction-detail .label { display: block; font-size: 12px; color: var(--color-info); }
.satisfaction-detail .value { display: block; font-size: 20px; font-weight: bold; margin-top: 5px; }
.search-form { margin-bottom: 20px; }
.logs-list { margin-top: 10px; }
.list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
.chat-detail { max-height: 600px; overflow-y: auto; }
.message-item { padding: 15px; margin-bottom: 15px; background: var(--conversation-bg); border-radius: 12px; }
.message-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.message-time { font-size: 12px; color: var(--color-info); }
.message-content { font-size: 14px; line-height: 1.6; color: var(--text-color); white-space: pre-wrap; }
.tool-call-tag { margin-top: 8px; }

.cell-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>