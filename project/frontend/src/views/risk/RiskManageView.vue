<template>
  <div class="risk-dashboard">
    <!-- 抬头区域 -->
    <div class="welcome-section">
      <div class="welcome-info">
        <h1 class="welcome-title">风险监控中心</h1>
        <div class="date-info">
          <span>今日日期：{{ currentDate }}</span>
          <span class="divider">|</span>
          <span>系统运行正常</span>
          <span class="divider">|</span>
          <span class="status-badge" :class="riskStatusClass">{{ riskStatusText }}</span>
        </div>
      </div>
      <div class="action-buttons">
        <el-button class="screen-btn" @click="goToDVScreen" type="primary" size="default">
          <el-icon><Monitor /></el-icon>
          数据大屏
        </el-button>
      </div>
    </div>

    <!-- 四个数据卡片 -->
    <div class="stats-cards">
      <!-- 用户数卡片 -->
      <div class="stat-card">
        <div class="stat-main">
          <div class="text-container">
            <div class="stat-title">注册用户总数</div>
            <div class="stat-value">{{ stats.totalUsers.toLocaleString() }}</div>
            <div class="stat-subtitle">活跃用户 {{ stats.activeUsers.toLocaleString() }}</div>
          </div>
          <div class="stat-icon user-icon">
            <el-icon><User /></el-icon>
          </div>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          {{ stats.userGrowth }}% 较上月
        </div>
      </div>

      <!-- 申请数卡片 -->
      <div class="stat-card">
        <div class="stat-main">
          <div class="text-container">
            <div class="stat-title">本月申请数</div>
            <div class="stat-value">{{ stats.monthlyApplications.toLocaleString() }}</div>
            <div class="stat-subtitle">待处理 {{ stats.pendingApplications }} 笔</div>
          </div>
          <div class="stat-icon application-icon">
            <el-icon><Document /></el-icon>
          </div>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          {{ stats.applicationGrowth }}% 较上月
        </div>
      </div>

      <!-- 放款数卡片 -->
      <div class="stat-card">
        <div class="stat-main">
          <div class="text-container">
            <div class="stat-title">累计放款金额</div>
            <div class="stat-value">¥{{ stats.totalLoanAmount }}</div>
            <div class="stat-subtitle">本月放款 ¥{{ stats.monthlyLoanAmount }}</div>
          </div>
          <div class="stat-icon loan-icon">
            <el-icon><Money /></el-icon>
          </div>
        </div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon>
          {{ stats.loanGrowth }}% 较上月
        </div>
      </div>

      <!-- 逾期数卡片 -->
      <div class="stat-card">
        <div class="stat-main">
          <div class="text-container">
            <div class="stat-title">逾期率</div>
            <div class="stat-value">{{ stats.overdueRate }}%</div>
            <div class="stat-subtitle">较健康水平</div>
          </div>
          <div class="stat-icon overdue-icon">
            <el-icon><Warning /></el-icon>
          </div>
        </div>
        <div class="stat-trend down">
          <el-icon><Bottom /></el-icon>
          {{ stats.overdueChange }}% 较上月
        </div>
      </div>
    </div>

    <!-- 中间区域：最新动态 + 月度审批统计 -->
    <div class="center-section">
      <!-- 最新动态 -->
      <div class="chart-card">
        <div class="chart-header">
          <div class="title-section">
            <h3 class="chart-title">最新动态</h3>
            <div class="connection-status" :class="{ connected: isStreamConnected, disconnected: !isStreamConnected }">
              <el-icon v-if="isStreamConnected"><CircleCheck /></el-icon>
              <el-icon v-else><Warning /></el-icon>
              <span>{{ isStreamConnected ? '实时连接中' : '连接断开' }}</span>
              <el-button v-if="!isStreamConnected" size="small" @click="reconnectStream" type="primary" plain>
                重新连接
              </el-button>
            </div>
          </div>
          <a href="javascript:void(0)" class="view-all" @click="toggleViewAll">
            {{ showAllActivities ? '收起' : '查看详细信息' }}
            <el-icon><ArrowRight /></el-icon>
          </a>
        </div>
        <div class="activity-list">
          <div v-for="(activity, index) in displayActivities" :key="index" class="activity-item" :class="{ 'new-item': activity.isNew, 'clickable': hasBusinessRoute(activity) }" @mouseenter="activity.isNew = false" @click="handleActivityClick(activity)">
            <span class="activity-dot" :style="{ backgroundColor: activity.color }"></span>
            <div class="activity-content">
              <div class="activity-text">{{ activity.text }}</div>
              <div class="activity-time">{{ activity.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 月度审批统计 -->
      <div class="chart-card">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">月度审批统计</h3>
            <p class="chart-subtitle">通过 / 拒绝 对比</p>
          </div>
        </div>
        <div ref="monthlyApprovalRef" class="monthly-chart"></div>
        <div class="approval-stats">
          <div class="approval-stat approved">
            <el-icon><CircleCheck /></el-icon>
            <div class="stat-label">通过率</div>
            <div class="stat-value">{{ approvalStats.rate }}%</div>
          </div>
          <div class="approval-stat rejected">
            <el-icon><Warning /></el-icon>
            <div class="stat-label">拒绝率</div>
            <div class="stat-value">{{ approvalStats.rejectionRate }}%</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 左侧：申请趋势图 -->
      <div class="chart-card trend-chart left-chart">
        <div class="chart-header">
          <div>
            <h3 class="chart-title">申请趋势</h3>
            <p class="chart-subtitle">近 8 个月申请 / 审批 数据</p>
          </div>
          <div class="chart-legend">
            <span class="legend-item">
              <span class="legend-dot blue"></span>
              申请量
            </span>
            <span class="legend-item">
              <span class="legend-dot green"></span>
              通过量
            </span>
          </div>
        </div>
        <div ref="applicationTrendRef" class="chart-body"></div>
      </div>

      <!-- 右侧：风险分布 + 贷款用途分布 -->
      <div class="right-charts">
        <!-- 风险分布图 -->
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">风险分布</h3>
              <p class="chart-subtitle">当前用户风险等级占比</p>
            </div>
          </div>
          <div class="risk-distribution">
            <div ref="riskDistributionRef" class="risk-chart"></div>
            <div class="risk-legend">
              <div class="legend-row">
                <span class="legend-dot green"></span>
                <span>无风险</span>
                <span class="legend-value">{{ (riskStats.totalUsers - riskStats.lowRisk - riskStats.mediumRisk - riskStats.highRisk).toLocaleString() }}</span>
              </div>
              <div class="legend-row">
                <span class="legend-dot light-green"></span>
                <span>低风险</span>
                <span class="legend-value">{{ riskStats.lowRisk.toLocaleString() }}</span>
              </div>
              <div class="legend-row">
                <span class="legend-dot orange"></span>
                <span>中风险</span>
                <span class="legend-value">{{ riskStats.mediumRisk.toLocaleString() }}</span>
              </div>
              <div class="legend-row">
                <span class="legend-dot red"></span>
                <span>高风险</span>
                <span class="legend-value">{{ riskStats.highRisk.toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 贷款用途分布 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">贷款用途分布</h3>
          </div>
          <div class="purpose-list">
            <div v-for="item in purposeDistribution" :key="item.name" class="purpose-item">
              <span class="purpose-name">{{ item.name }}</span>
              <div class="purpose-bar">
                <div 
                  class="purpose-fill" 
                  :style="{ width: item.percentage + '%', backgroundColor: item.color }"
                ></div>
              </div>
              <span class="purpose-percentage">{{ item.percentage }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { userAPI, loanApplicationAPI, notificationAPI } from '@/api'
import * as echarts from 'echarts'
import { 
  User, 
  Document, 
  Money, 
  Warning,
  Top, 
  Bottom, 
  ArrowRight,
  CircleCheck,
  Monitor
} from '@element-plus/icons-vue'
import { useNotificationStream } from '@/composables/useNotificationStream'

const router = useRouter()
const route = useRoute()

// 当前日期
const getCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  return `${year}年${month}月${day}日`
}
const currentDate = getCurrentDate()

// 跳转到数据大屏
const goToDVScreen = () => {
  router.push('/dv-screen')
}

// 风控状态
const riskStatusText = computed(() => {
  if (stats.overdueRate > 10) return '⚠ 逾期率偏高'
  if (stats.overdueRate > 5) return '⚡ 需关注风险'
  return '✅ 风险可控'
})
const riskStatusClass = computed(() => {
  if (stats.overdueRate > 10) return 'status-danger'
  if (stats.overdueRate > 5) return 'status-warning'
  return 'status-safe'
})

// 统计数据
const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  userGrowth: 0,
  monthlyApplications: 0,
  pendingApplications: 0,
  applicationGrowth: 0,
  totalLoanAmount: '0',
  monthlyLoanAmount: '0',
  loanGrowth: 0,
  overdueRate: 0,
  overdueChange: 0
})

// 风险统计数据
const riskStats = reactive({
  lowRisk: 0,
  mediumRisk: 0,
  highRisk: 0,
  totalUsers: 0
})

// 贷款用途分布
const purposeDistribution = ref([])

// 最新动态
const activities = ref([])
const showAllActivities = ref(false)

const {
  isStreamConnected,
  reconnectStream,
  initNotificationStreamWithFetch,
  closeNotificationStream,
  requestNotificationPermission
} = useNotificationStream({
  onNotification: (notification) => {
    // 检查重复消息
    const isDuplicate = activities.value.some(
      activity => activity.rawNotification?.id === notification.id
    )
    if (isDuplicate) return

    // 根据是否已读确定颜色
    const color = notification.readFlag ? '#5AD8A6' : '#F87474'

    // 格式化时间
    let timeStr = notification.createdAt
    if (timeStr && timeStr.includes(' ')) {
      timeStr = timeStr.split(' ')[1].substring(0, 5)
    }

    // 构建显示文本
    const userIdText = notification.userId ? `用户${notification.userId}` : '系统'
    const text = `${userIdText}的${notification.content}`

    // 创建新活动项
    const newActivity = {
      text,
      time: timeStr,
      color,
      rawNotification: notification,
      isNew: true
    }

    activities.value.unshift(newActivity)

    // 如果开启了显示全部，限制显示数量
    if (!showAllActivities.value && activities.value.length > 5) {
      activities.value = activities.value.slice(0, 5)
    }

    // 显示桌面通知
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification('风控系统新通知', {
          body: notification.content,
          icon: '/favicon.ico',
          tag: `notification-${notification.id}`
        })
      } catch (error) {
        console.error('显示桌面通知失败:', error)
      }
    }
  },
  notificationTitle: '风控系统新通知'
})

// 加载历史通知作为初始活动数据
const fetchInitialActivities = async () => {
  try {
    const res = await notificationAPI.getAdminNotifications()
    if (res.code === 200 && res.data?.length) {
      activities.value = res.data.map(notification => {
        const color = notification.readFlag ? '#5AD8A6' : '#F87474'
        let timeStr = notification.createdAt
        if (timeStr && timeStr.includes(' ')) {
          timeStr = timeStr.split(' ')[1].substring(0, 5)
        }
        return {
          text: `用户${notification.userId}的${notification.content}`,
          time: timeStr,
          color,
          rawNotification: notification,
          isNew: false
        }
      })
    }
  } catch (error) {
    console.error('加载历史通知失败:', error)
  }
}

// 显示的活动列表（默认只显示最新 5 条）
const displayActivities = computed(() => {
  if (showAllActivities.value) {
    return activities.value
  }
  return activities.value.slice(0, 5)
})

// 切换显示详细信息/收起
const toggleViewAll = () => {
  showAllActivities.value = !showAllActivities.value
}

// 判断活动项是否有可跳转的业务路由
const hasBusinessRoute = (activity) => {
  const notif = activity.rawNotification
  if (!notif) return false
  // 这些业务类型支持跳转到待办审核页
  return ['LOAN_APPLICATION_APPROVE', 'LOAN_APPLICATION_REJECT'].includes(notif.businessType)
}

// 活动项点击跳转
const handleActivityClick = (activity) => {
  const notif = activity.rawNotification
  if (!notif) return

  switch (notif.businessType) {
    case 'LOAN_APPLICATION_APPROVE':
    case 'LOAN_APPLICATION_REJECT':
      router.push({ path: '/dashboard/pending-applications', query: { applicationId: notif.businessId } })
      break
  }
}

// 审批统计
const approvalStats = reactive({
  rate: 0,
  rejectionRate: 0
})

// 月度审批统计数据（细分）
const monthlyApprovalData = ref({
  aiApproved: [],  // AI 通过
  aiRejected: [],  // AI 拒绝
  manualApproved: [],  // 人工通过
  manualRejected: []  // 人工拒绝
})

// 图表引用
const applicationTrendRef = ref(null)
const riskDistributionRef = ref(null)
const monthlyApprovalRef = ref(null)

// 图表实例
let applicationTrendChart = null
let riskDistributionChart = null
let monthlyApprovalChart = null

// 窗口大小变化处理
const handleResize = () => {
  ;[applicationTrendChart, riskDistributionChart, monthlyApprovalChart].forEach(chart => {
    if (chart) {
      chart.resize()
    }
  })
}

// 金额格式化辅助函数
const formatToWan = (amount, defaultValue = '4.56 万') => {
  if (!amount || amount <= 0) return defaultValue
  return (amount / 10000).toFixed(2) + ' 万'
}

const formatToYuan = (amount, defaultValue = '3,200') => {
  if (!amount || amount <= 0) return defaultValue
  // 添加千位分隔符
  return amount.toLocaleString('zh-CN') + ' 元'
}

// 获取用户数据
const fetchUserData = async () => {
  try {
    const response = await userAPI.getUserStats()
    if (response.code === 200) {
      const users = response.data
      
      // 处理用户统计数据
      const loanStatusCount = {
        '正常': 0,
        '无借贷': 0,
        '逾期': 0
      }
      
      let totalLoanAmount = 0
      let monthlyLoanAmount = 0
      const overdueCount = users.filter(u => u.loanStatus === '逾期').length
      
      // 按月份统计用户增长
      const userGrowthByMonth = {}
      
      users.forEach(user => {
        if (user.loanStatus) {
          loanStatusCount[user.loanStatus] = (loanStatusCount[user.loanStatus] || 0) + 1
        }
        
        // 统计贷款金额（模拟数据，因为 API 可能不返回）
        if (user.totalLoanAmount) {
          totalLoanAmount += user.totalLoanAmount
        }
        
        // 统计用户注册月份
        if (user.createTime) {
          const month = user.createTime.substring(0, 7) // YYYY-MM
          userGrowthByMonth[month] = (userGrowthByMonth[month] || 0) + 1
        }
      })
      
      // 更新统计数据
      stats.totalUsers = users.length
      stats.activeUsers = users.filter(u => u.loanStatus === '正常').length
      
      // 计算用户增长率（与上月相比）
      const months = Object.keys(userGrowthByMonth).sort()
      if (months.length >= 2) {
        const lastMonthCount = userGrowthByMonth[months[months.length - 1]]
        const prevMonthCount = userGrowthByMonth[months[months.length - 2]]
        if (prevMonthCount > 0) {
          stats.userGrowth = ((lastMonthCount - prevMonthCount) / prevMonthCount * 100).toFixed(1) 
        }
      }
      
      // 模拟贷款金额数据（如果 API 没有返回）
      // TODO: 当后端提供贷款金额统计 API 时，替换此处
      stats.totalLoanAmount = formatToWan(totalLoanAmount)
      stats.monthlyLoanAmount = formatToYuan(monthlyLoanAmount)
      stats.loanGrowth = 18.7 // TODO: 从 API 获取
      
      // 计算逾期率
      if (users.length > 0) {
        stats.overdueRate = ((overdueCount / users.length) * 100).toFixed(1)
        stats.overdueChange = -0.4 // TODO: 计算与上月的变化
      }
    }
  } catch (error) {
    console.error('获取用户数据失败:', error)
  }
}

// 获取贷款申请数据
const fetchLoanApplicationData = async () => {
  try {
    const [pendingResponse, completedResponse] = await Promise.all([
      loanApplicationAPI.getPendingApprovals(),
      loanApplicationAPI.getCompletedApprovals()
    ])
    
    const statusCount = {
      '审核中': 0,
      '已通过': 0,
      'AI 拒绝': 0,
      '人工拒绝': 0,
      '已取消': 0
    }
    
    const applyTimeData = {}
    
    // 月度审批细分数据
    const monthlyStats = {
      aiApproved: {},    // AI 通过
      aiRejected: {},    // AI 拒绝
      manualApproved: {}, // 人工通过
      manualRejected: {}  // 人工拒绝
    }
    
    // 当前月份
    const currentMonth = new Date().toISOString().slice(0, 7) // YYYY-MM
    
    // 用于存储本月申请数
    let pendingThisMonth = 0
    let completedThisMonth = 0
    
    // 待审核列表 = AI 拒绝
    if (pendingResponse.code === 200 && pendingResponse.data) {
      stats.pendingApplications = pendingResponse.data.length
      
      // 计算本月申请数（待审核的）
      pendingThisMonth = pendingResponse.data.filter(app => {
        if (app.applyTime) {
          let dateStr = app.applyTime
          if (dateStr.includes('T')) {
            dateStr = dateStr.split('T')[0]
          } else if (dateStr.includes(' ')) {
            dateStr = dateStr.split(' ')[0]
          }
          return dateStr.startsWith(currentMonth)
        }
        return false
      }).length
      
      pendingResponse.data.forEach(app => {
        statusCount['AI 拒绝']++
        if (app.applyTime) {
          let dateStr = app.applyTime
          if (dateStr.includes('T')) {
            dateStr = dateStr.split('T')[0]
          } else if (dateStr.includes(' ')) {
            dateStr = dateStr.split(' ')[0]
          }
          applyTimeData[dateStr] = (applyTimeData[dateStr] || 0) + 1
          
          // 统计 AI 拒绝的月度数据
          const month = dateStr.substring(0, 7)
          monthlyStats.aiRejected[month] = (monthlyStats.aiRejected[month] || 0) + 1
        }
      })
      
      // 更新待处理数量 - 已在上面直接赋值
    }
    
    // 已完成审批列表 = 已通过（人工通过）+ 人工拒绝
    if (completedResponse.code === 200 && completedResponse.data) {
      // 计算本月已完成申请数
      completedThisMonth = completedResponse.data.filter(app => {
        if (app.applyTime) {
          let dateStr = app.applyTime
          if (dateStr.includes('T')) {
            dateStr = dateStr.split('T')[0]
          } else if (dateStr.includes(' ')) {
            dateStr = dateStr.split(' ')[0]
          }
          return dateStr.startsWith(currentMonth)
        }
        return false
      }).length
      
      // 本月总申请数 = 待审核的 + 已完成的
      stats.monthlyApplications = pendingThisMonth + completedThisMonth
      
      completedResponse.data.forEach(app => {
        if (app.status === '已通过') {
          statusCount['已通过']++
          // 已通过的申请都是人工审核通过的
          if (app.applyTime) {
            let dateStr = app.applyTime
            if (dateStr.includes('T')) {
              dateStr = dateStr.split('T')[0]
            } else if (dateStr.includes(' ')) {
              dateStr = dateStr.split(' ')[0]
            }
            const month = dateStr.substring(0, 7)
            monthlyStats.manualApproved[month] = (monthlyStats.manualApproved[month] || 0) + 1
          }
        } else if (app.status === '人工拒绝') {
          statusCount['人工拒绝']++
          if (app.applyTime) {
            let dateStr = app.applyTime
            if (dateStr.includes('T')) {
              dateStr = dateStr.split('T')[0]
            } else if (dateStr.includes(' ')) {
              dateStr = dateStr.split(' ')[0]
            }
            const month = dateStr.substring(0, 7)
            monthlyStats.manualRejected[month] = (monthlyStats.manualRejected[month] || 0) + 1
          }
        }
        if (app.applyTime) {
          let dateStr = app.applyTime
          if (dateStr.includes('T')) {
            dateStr = dateStr.split('T')[0]
          } else if (dateStr.includes(' ')) {
            dateStr = dateStr.split(' ')[0]
          }
          applyTimeData[dateStr] = (applyTimeData[dateStr] || 0) + 1
        }
      })
    }
    
    // 更新月度审批统计数据
    monthlyApprovalData.value = monthlyStats

    // 申请趋势图表
    const monthlyData = {}
    Object.entries(applyTimeData).forEach(([date, count]) => {
      const month = date.substring(0, 7)
      monthlyData[month] = (monthlyData[month] || 0) + count
    })
    
    const sortedMonths = Object.keys(monthlyData).sort().slice(-8)
    const applicationCounts = sortedMonths.map(month => monthlyData[month])
    const approvedCounts = sortedMonths.map(month => Math.floor(monthlyData[month] * 0.68))
    
    if (applicationTrendRef.value) {
      applicationTrendChart = echarts.init(applicationTrendRef.value)
      applicationTrendChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: sortedMonths,
          axisLabel: {
            fontSize: 10,
            rotate: 0
          }
        },
        yAxis: {
          type: 'value',
          splitLine: {
            lineStyle: {
              type: 'dashed'
            }
          }
        },
        series: [
          {
            name: '申请量',
            type: 'line',
            smooth: true,
            data: applicationCounts,
            itemStyle: {
              color: '#5B8FF9'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(91, 143, 249, 0.3)' },
                { offset: 1, color: 'rgba(91, 143, 249, 0.05)' }
              ])
            }
          },
          {
            name: '通过量',
            type: 'line',
            smooth: true,
            data: approvedCounts,
            itemStyle: {
              color: '#5AD8A6'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(90, 216, 166, 0.3)' },
                { offset: 1, color: 'rgba(90, 216, 166, 0.05)' }
              ])
            }
          }
        ]
      })
    }
    
    // 月度审批统计图表（AI 拒绝、人工拒绝、通过）
    if (monthlyApprovalRef.value) {
      const allMonths = new Set([
        ...Object.keys(monthlyStats.aiRejected),
        ...Object.keys(monthlyStats.manualApproved),
        ...Object.keys(monthlyStats.manualRejected)
      ])
      const sortedMonths = Array.from(allMonths).sort()
      const recentMonths = sortedMonths.slice(-4) // 最近 4 个月
      
      // 添加 2026-2 和 2026-3 到横轴
      const fixedMonths = ['2026-02', '2026-03']
      const displayMonths = [...fixedMonths, ...recentMonths]
      
      // 填充每个月的各类数据
      const aiRejectedData = displayMonths.map(month => monthlyStats.aiRejected[month] || 0)
      const manualRejectedData = displayMonths.map(month => monthlyStats.manualRejected[month] || 0)
      // 通过 = 人工通过 + AI 通过
      const approvedData = displayMonths.map(month => {
        const manualApproved = monthlyStats.manualApproved[month] || 0
        const aiApproved = 0 // TODO: 当后端提供 AI 通过数据时，替换此处
        return manualApproved + aiApproved
      })
      
      monthlyApprovalChart = echarts.init(monthlyApprovalRef.value)
      monthlyApprovalChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: function(params) {
            let result = params[0].name + '<br/>'
            params.forEach(param => {
              result += `${param.marker} ${param.seriesName}: ${param.value}<br/>`
            })
            return result
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: displayMonths,
          axisLabel: {
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          splitLine: {
            lineStyle: {
              type: 'dashed'
            }
          }
        },
        series: [
          {
            name: '通过',
            type: 'bar',
            data: approvedData,
            barWidth: 20, // 缩小柱状体宽度
            label: {
              show: true,
              position: 'top',
              formatter: '{c}'
            },
            itemStyle: {
              color: '#5AD8A6',
              borderRadius: [4, 4, 0, 0]
            }
          },
          {
            name: 'AI 拒绝',
            type: 'bar',
            data: aiRejectedData,
            barWidth: 20, // 缩小柱状体宽度
            label: {
              show: true,
              position: 'top',
              formatter: '{c}'
            },
            itemStyle: {
              color: '#F6BD16',
              borderRadius: [4, 4, 0, 0]
            }
          },
          {
            name: '人工拒绝',
            type: 'bar',
            data: manualRejectedData,
            barWidth: 20, // 缩小柱状体宽度
            label: {
              show: true,
              position: 'top',
              formatter: '{c}'
            },
            itemStyle: {
              color: '#F87474',
              borderRadius: [4, 4, 0, 0]
            }
          }
        ]
      })
      
      // 计算通过率和拒绝率
      const totalApplications = Object.values(statusCount).reduce((sum, value) => sum + value, 0)
      const approvedCount = statusCount['已通过'] || 0
      const rejectedCount = (statusCount['AI 拒绝'] || 0) + (statusCount['人工拒绝'] || 0)
      
      if (totalApplications > 0) {
        approvalStats.rate = ((approvedCount / totalApplications) * 100).toFixed(1)
        approvalStats.rejectionRate = ((rejectedCount / totalApplications) * 100).toFixed(1)
      }
    }
    
    return monthlyData
  } catch (error) {
    console.error('获取贷款申请数据失败:', error)
    return {}
  }
}

// 获取风险等级数据
const fetchRiskLevelData = async () => {
  try {
    const response = await userAPI.getBlacklist()
    if (response.code === 200) {
      const blacklist = response.data
      
      const riskLevelCount = {
        '0': 0,
        '1': 0,
        '2': 0,
        '3': 0
      }
      
      blacklist.forEach(item => {
        if (item.blackLevel) {
          riskLevelCount[item.blackLevel] = (riskLevelCount[item.blackLevel] || 0) + 1
        }
      })
      
      const totalUsersResponse = await userAPI.searchUsersByCredit('>0')
      const totalUsers = Array.isArray(totalUsersResponse) ? totalUsersResponse.length : 0
      riskLevelCount['0'] = totalUsers - blacklist.length
      
      // 更新风险统计数据
      riskStats.lowRisk = riskLevelCount['1']
      riskStats.mediumRisk = riskLevelCount['2']
      riskStats.highRisk = riskLevelCount['3']
      riskStats.totalUsers = totalUsers
      
      // 风险分布环形图
      if (riskDistributionRef.value) {
        riskDistributionChart = echarts.init(riskDistributionRef.value)
        riskDistributionChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: function(params) {
              return `${params.name}: ${params.value} 人<br/>占比：${((params.value / riskStats.totalUsers) * 100).toFixed(1)}%`
            },
            position: function(point, params, dom, rect, size) {
              // point: 鼠标位置
              // size: 图表尺寸
              const chartCenterX = size.viewSize[0] / 2;
              const chartCenterY = size.viewSize[1] / 2;
              // 计算 tooltip 的宽高
              const tooltipWidth = dom.offsetWidth;
              const tooltipHeight = dom.offsetHeight;
              
              // 根据鼠标位置判断象限
              if (point[0] < chartCenterX && point[1] < chartCenterY) {
                // 左上象限 - tooltip 显示在鼠标左上
                return [point[0] - tooltipWidth - 10, point[1] - tooltipHeight - 10];
              } else if (point[0] < chartCenterX && point[1] >= chartCenterY) {
                // 左下象限 - tooltip 显示在鼠标左下
                return [point[0] - tooltipWidth - 10, point[1] + 10];
              } else if (point[0] >= chartCenterX && point[1] < chartCenterY) {
                // 右上象限 - tooltip 显示在鼠标右上
                return [point[0] + 10, point[1] - tooltipHeight - 10];
              } else {
                // 右下象限 - tooltip 显示在鼠标右下
                return [point[0] + 10, point[1] + 10];
              }
            }
          },
          series: [
            {
              name: '风险等级',
              type: 'pie',
              radius: ['50%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
              },
              label: {
                show: false,
                position: 'center'
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 16,
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: [
                { value: riskLevelCount['0'], name: '无风险', itemStyle: { color: '#C6E5FF' } },
                { value: riskStats.lowRisk, name: '低风险', itemStyle: { color: '#5AD8A6' } },
                { value: riskStats.mediumRisk, name: '中风险', itemStyle: { color: '#F6BD16' } },
                { value: riskStats.highRisk, name: '高风险', itemStyle: { color: '#F87474' } }
              ]
            }
          ]
        })
      }
    }
  } catch (error) {
    console.error('获取风险等级数据失败:', error)
  }
}

// 获取贷款用途分布数据
const fetchPurposeDistribution = async () => {
  try {
    // TODO: 当后端提供贷款用途统计 API 时，替换此处模拟数据
    // 目前使用模拟数据
    purposeDistribution.value = [
      { name: '消费', percentage: 35, color: '#5B8FF9' },
      { name: '经营', percentage: 22, color: '#5AD8A6' },
      { name: '装修', percentage: 18, color: '#F6BD16' },
      { name: '医疗', percentage: 12, color: '#7262FD' },
      { name: '教育', percentage: 8, color: '#F87474' },
      { name: '其他', percentage: 5, color: '#48C6EF' }
    ]
  } catch (error) {
    console.error('获取贷款用途分布数据失败:', error)
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  await fetchUserData()
  await fetchLoanApplicationData()
  await fetchRiskLevelData()
  await fetchPurposeDistribution()

  await fetchInitialActivities()
  requestNotificationPermission()
  initNotificationStreamWithFetch()

  window.addEventListener('resize', handleResize)

  // 检查是否需要滚动到最新动态区域
  if (route.query.scrollTo === 'activities') {
    await nextTick()
    setTimeout(() => {
      const el = document.querySelector('.center-section')
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }, 500)
  }
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  closeNotificationStream()

  ;[applicationTrendChart, riskDistributionChart, monthlyApprovalChart].forEach(chart => {
    if (chart) {
      chart.dispose()
    }
  })
})
</script>

<style scoped>
  @import '@/assets/css/risk/riskManageView.css';
</style>