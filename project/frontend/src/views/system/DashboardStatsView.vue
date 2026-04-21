<template>
  <div class="dashboard">
    <h2>智能客服数据统计</h2>

    <!-- 时间选择器 -->
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
            @change="fetchAllData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchAllData">查询</el-button>
          <el-button @click="exportReport">导出报表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核心指标卡片 -->
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

    <!-- 工具调用统计 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>工具调用次数</span>
          </template>
          <div class="chart-container">
            <div ref="toolCallChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>问题分类分布</span>
          </template>
          <div class="chart-container">
            <div ref="categoryChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>对话趋势（7 天）</span>
          </template>
          <div class="chart-container">
            <div ref="trendChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门问题和未解决问题 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span>热门问题 TOP 10</span>
              <el-button type="primary" size="small">查看更多</el-button>
            </div>
          </template>
          <el-table :data="topQuestions" stripe :show-header="false">
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="question" label="问题" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="80" sortable>
              <template #default="{ row }">
                <el-tag type="success">{{ row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span>未解决问题 TOP 10</span>
              <el-button type="danger" size="small">处理</el-button>
            </div>
          </template>
          <el-table :data="unresolvedQuestions" stripe :show-header="false">
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="question" label="问题" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" width="80" sortable>
              <template #default="{ row }">
                <el-tag type="danger">{{ row.count }}</el-tag>
              </template>
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

    <!-- 用户满意度 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>用户满意度</span>
          </template>
          <div class="satisfaction-container">
            <el-progress 
              type="dashboard" 
              :percentage="stats.satisfactionRate" 
              :color="getSatisfactionColor(stats.satisfactionRate)"
            />
            <div class="satisfaction-detail">
              <div>
                <span class="label">好评</span>
                <span class="value">{{ stats.positiveFeedback }}</span>
              </div>
              <div>
                <span class="label">差评</span>
                <span class="value">{{ stats.negativeFeedback }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>响应时间分布</span>
          </template>
          <div class="chart-container">
            <div ref="responseTimeChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/stats'

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

const toolCallChart = ref(null)
const categoryChart = ref(null)
const trendChart = ref(null)
const responseTimeChart = ref(null)

const fetchAllData = async () => {
  try {
    const params = new URLSearchParams()
    if (dateRange.value && dateRange.value.length === 2) {
      params.append('start_date', dateRange.value[0])
      params.append('end_date', dateRange.value[1])
    }
    
    const res = await axios.get(`${API_BASE}/overview?${params}`)
    if (res.data) {
      stats.value = res.data.stats
      topQuestions.value = res.data.topQuestions || []
      unresolvedQuestions.value = res.data.unresolvedQuestions || []
      
      renderCharts(res.data.charts)
    }
  } catch (error) {
    console.error('Error fetching stats:', error)
  }
}

const renderCharts = (chartsData) => {
  // 这里使用 ECharts 或 Chart.js 渲染图表
  // 由于是示例，只展示数据结构
  
  // 工具调用次数图表数据
  const toolCallData = chartsData?.toolCalls || [
    { name: 'query_application_status', value: 156 },
    { name: 'calculate_repayment', value: 89 }
  ]
  
  // 分类分布图表数据
  const categoryData = chartsData?.categories || [
    { name: '申请流程', value: 45 },
    { name: '产品咨询', value: 30 },
    { name: '还款问题', value: 15 },
    { name: '通用', value: 10 }
  ]
  
  // 对话趋势图表数据
  const trendData = chartsData?.trend || [
    { date: '2026-04-13', count: 50 },
    { date: '2026-04-14', count: 65 },
    { date: '2026-04-15', count: 78 },
    { date: '2026-04-16', count: 92 },
    { date: '2026-04-17', count: 85 },
    { date: '2026-04-18', count: 95 },
    { date: '2026-04-19', count: 102 }
  ]
  
  // 响应时间分布图表数据
  const responseTimeData = chartsData?.responseTime || [
    { range: '0-500ms', count: 120 },
    { range: '500-1000ms', count: 85 },
    { range: '1000-2000ms', count: 45 },
    { range: '2000ms+', count: 12 }
  ]
  
  console.log('Charts data:', { toolCallData, categoryData, trendData, responseTimeData })
}

const addToKnowledge = (row) => {
  ElMessage.info(`将 "${row.question}" 添加到知识库功能开发中...`)
}

const exportReport = () => {
  ElMessage.info('导出报表功能开发中...')
}

const getSatisfactionColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
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
  color: #909399;
}
.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}
.metric-footer {
  font-size: 12px;
  color: #909399;
}
.metric-footer .up {
  color: #67c23a;
}
.metric-footer .down {
  color: #f56c6c;
}
.metric-label {
  margin-left: 5px;
}
.chart-card {
  height: 350px;
}
.chart-container {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart {
  width: 100%;
  height: 100%;
}
.list-card {
  min-height: 350px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.satisfaction-container {
  text-align: center;
  padding: 20px;
}
.satisfaction-detail {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}
.satisfaction-detail .label {
  display: block;
  font-size: 12px;
  color: #909399;
}
.satisfaction-detail .value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  margin-top: 5px;
}
</style>
