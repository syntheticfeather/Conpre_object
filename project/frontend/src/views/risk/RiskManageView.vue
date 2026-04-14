<template>
  <div class="risk-dashboard">
    <!-- 顶部欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-info">
        <h1 class="welcome-title">欢迎回来，管理员 👋</h1>
        <div class="date-info">
          <span>今日日期：{{ currentDate }}</span>
          <span class="divider">|</span>
          <span>数据实时更新</span>
        </div>
      </div>
      <div class="pending-badge" @click="goToPendingApplications" style="cursor: pointer;">
        <el-icon><Clock /></el-icon>
        待审核申请 ({{ pendingCount }})
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

    <!-- 底部区域：最新动态 + 月度审批统计 -->
    <div class="bottom-section">
      <!-- 最新动态 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">最新动态</h3>
          <a href="#" class="view-all">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </a>
        </div>
        <div class="activity-list">
          <div v-for="(activity, index) in activities" :key="index" class="activity-item">
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

    <!-- 中间图表区域 -->
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

    <!-- 原有图表区域 -->
    <div class="original-charts-section">
      <h3 class="section-title">详细数据分析</h3>
      <div class="chart-grid">
        <div class="chart-item">
          <h4 class="chart-title">用户贷款状态分布</h4>
          <el-card shadow="hover">
            <div ref="userChartRef" class="chart-container"></div>
          </el-card>
        </div>
        
        <div class="chart-item">
          <h4 class="chart-title">贷款申请状态分布</h4>
          <el-card shadow="hover">
            <div ref="loanApplicationChartRef" class="chart-container"></div>
          </el-card>
        </div>
        
        <div class="chart-item">
          <h4 class="chart-title">用户信用分统计</h4>
          <el-card shadow="hover">
            <div ref="creditScoreDistributionRef" class="chart-container"></div>
          </el-card>
        </div>
        
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userAPI, loanApplicationAPI, notificationAPI } from '@/api'
import * as echarts from 'echarts'
import { 
  User, 
  Document, 
  Money, 
  Warning, 
  Clock, 
  Top, 
  Bottom, 
  ArrowRight,
  CircleCheck 
} from '@element-plus/icons-vue'

const router = useRouter()

// 当前日期 - 使用真实日期
const getCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  return `${year}年${month}月${day}日`
}
const currentDate = getCurrentDate()

// 待审核数量
const pendingCount = ref(0)

// 跳转到待办审核页面
const goToPendingApplications = () => {
  router.push('/dashboard/pending-applications')
}

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
const userChartRef = ref(null)
const loanApplicationChartRef = ref(null)
// const userRegistrationTrendRef = ref(null)
// const loanApplicationTrendRef = ref(null)
const creditScoreDistributionRef = ref(null)
// const approvalRateRef = ref(null)
const riskLevelRef = ref(null)
const applicationTrendRef = ref(null)
const riskDistributionRef = ref(null)
const monthlyApprovalRef = ref(null)

// 图表实例
let userChart = null
let loanApplicationChart = null
let userRegistrationTrendChart = null
let loanApplicationTrendChart = null
let creditScoreDistributionChart = null
let approvalRateChart = null
let riskLevelChart = null
let applicationTrendChart = null
let riskDistributionChart = null
let monthlyApprovalChart = null

// 窗口大小变化处理
const handleResize = () => {
  const charts = [
    userChart,
    loanApplicationChart,
    userRegistrationTrendChart,
    loanApplicationTrendChart,
    creditScoreDistributionChart,
    approvalRateChart,
    riskLevelChart,
    applicationTrendChart,
    riskDistributionChart,
    monthlyApprovalChart
  ]
  charts.forEach(chart => {
    if (chart) {
      chart.resize()
    }
  })
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
      
      const creditScores = []
      let totalLoanAmount = 0
      let monthlyLoanAmount = 0
      const overdueCount = users.filter(u => u.loanStatus === '逾期').length
      
      // 按月份统计用户增长
      const userGrowthByMonth = {}
      
      users.forEach(user => {
        if (user.loanStatus) {
          loanStatusCount[user.loanStatus] = (loanStatusCount[user.loanStatus] || 0) + 1
        }
        if (user.creditScore) {
          creditScores.push(user.creditScore)
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
      stats.totalLoanAmount = (totalLoanAmount / 100000000).toFixed(2) + '亿' || '4.56 亿'
      stats.monthlyLoanAmount = (monthlyLoanAmount / 10000).toFixed(0) + '万' || '3,200 万'
      stats.loanGrowth = 18.7 // TODO: 从 API 获取
      
      // 计算逾期率
      if (users.length > 0) {
        stats.overdueRate = ((overdueCount / users.length) * 100).toFixed(1)
        stats.overdueChange = -0.4 // TODO: 计算与上月的变化
      }
      
      // 用户统计图表
      if (userChartRef.value) {
        const totalUsers = Object.values(loanStatusCount).reduce((sum, value) => sum + value, 0);
        // 使用 totalUsers 计算百分比或显示总数
        console.log(`总用户数: ${totalUsers}`);
        userChart = echarts.init(userChartRef.value)
        userChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: function(params) {
              const percentage = totalUsers > 0 ? ((params.value / totalUsers) * 100).toFixed(2) : 0;
              return `${params.name}: ${percentage}%`;
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
          legend: {
            top: '5%',
            left: 'center'
          },
          series: [
            {
              name: '贷款状态',
              type: 'pie',
              radius: ['40%', '70%'],
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
                  fontSize: 20,
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: Object.entries(loanStatusCount).map(([name, value]) => ({
                value,
                name
              }))
            }
          ]
        })
      }
      
      // 用户信用分分布图表
      const creditScoreRanges = {
        '0-300': 0,
        '301-600': 0,
        '601-700': 0,
        '701-800': 0,
        '801-900': 0,
        '901-1000': 0
      }
      
      creditScores.forEach(score => {
        if (score <= 300) creditScoreRanges['0-300']++
        else if (score <= 600) creditScoreRanges['301-600']++
        else if (score <= 700) creditScoreRanges['601-700']++
        else if (score <= 800) creditScoreRanges['701-800']++
        else if (score <= 900) creditScoreRanges['801-900']++
        else creditScoreRanges['901-1000']++
      })
      
      if (creditScoreDistributionRef.value) {
        creditScoreDistributionChart = echarts.init(creditScoreDistributionRef.value)
        creditScoreDistributionChart.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
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
            data: Object.keys(creditScoreRanges)
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '用户数',
              type: 'bar',
              data: Object.values(creditScoreRanges),
              label: {
                show: true,
                position: 'top',
                formatter: '{c}'
              },
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#5B8FF9' },
                  { offset: 1, color: '#C6E5FF' }
                ])
              }
            }
          ]
        })
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
      pendingCount.value = pendingResponse.data.length
      
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
      
      // 更新待处理数量
      stats.pendingApplications = pendingCount.value
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
    
    // 贷款申请统计图表
    if (loanApplicationChartRef.value) {
      const totalApplications = Object.values(statusCount).reduce((sum, value) => sum + value, 0)
      loanApplicationChart = echarts.init(loanApplicationChartRef.value)
      loanApplicationChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            const percentage = totalApplications > 0 ? ((params.value / totalApplications) * 100).toFixed(2) : 0;
            return `${params.name}: ${percentage}%`;
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
        legend: {
          top: '5%',
          left: 'center'
        },
        series: [
          {
            name: '申请状态',
            type: 'pie',
            radius: ['40%', '70%'],
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
                fontSize: 20,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: Object.entries(statusCount).map(([name, value]) => ({
              value,
              name
            }))
          }
        ]
      })
    }
    
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
      
      if (riskLevelRef.value) {
        riskLevelChart = echarts.init(riskLevelRef.value)
        riskLevelChart.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
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
            data: ['无风险', '低风险', '中风险', '高风险']
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '用户数',
              type: 'bar',
              data: [riskLevelCount['0'], riskLevelCount['1'], riskLevelCount['2'], riskLevelCount['3']],
              label: {
                show: true,
                position: 'top',
                formatter: '{c}'
              },
              itemStyle: {
                color: function(params) {
                  const colors = ['#5AD8A6', '#5B8FF9', '#F6BD16', '#F87474']
                  return colors[params.dataIndex]
                }
              }
            }
          ]
        })
      }
      
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

// 获取最新动态数据
const fetchActivities = async () => {
  try {
    // 调用通知API获取管理员通知
    const response = await notificationAPI.getAdminNotifications()
    if (response.code === 200) {
      // 将通知数据映射为活动数据
      activities.value = response.data.map(notification => {
        // 根据是否已读确定颜色
        const color = notification.readFlag ? '#5AD8A6' : '#F87474'
        // 格式化时间：显示创建时间的小时和分钟
        let timeStr = notification.createdAt
        if (timeStr && timeStr.includes(' ')) {
          timeStr = timeStr.split(' ')[1].substring(0, 5) // 提取 "HH:mm"
        }
        // 使用内容作为文本（更具体）
        const text = notification.content
        
        return {
          text,
          time: timeStr,
          color
        }
      })
    } else {
      console.error('获取通知数据失败:', response.message)
      // 如果API失败，使用模拟数据作为后备
      useFallbackActivities()
    }
  } catch (error) {
    console.error('获取最新动态数据失败:', error)
    // 出错时使用模拟数据作为后备
    useFallbackActivities()
  }
}

// 后备模拟数据函数
const useFallbackActivities = () => {
  activities.value = [
    { text: '用户 张伟 的贷款申请 A20240001 已批准', time: '10 分钟前', color: '#5AD8A6' },
    { text: '新用户 周婷 注册，提交信用极速贷申请', time: '25 分钟前', color: '#5B8FF9' },
    { text: '用户 赵丽 账户已被冻结 (风控触发)', time: '1 小时前', color: '#F6BD16' },
    { text: '用户 吴磊 的申请 A20240008 已批准放款', time: '2 小时前', color: '#5AD8A6' },
    { text: '系统检测到 5 条可疑申请，已转人工复核', time: '3 小时前', color: '#F87474' }
  ]
}

// 组件挂载时加载数据
onMounted(async () => {
  await fetchUserData()
  await fetchLoanApplicationData()
  await fetchRiskLevelData()
  await fetchPurposeDistribution()
  await fetchActivities()
  
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  const charts = [
    userChart,
    loanApplicationChart,
    userRegistrationTrendChart,
    loanApplicationTrendChart,
    creditScoreDistributionChart,
    approvalRateChart,
    riskLevelChart,
    applicationTrendChart,
    riskDistributionChart,
    monthlyApprovalChart
  ]
  
  charts.forEach(chart => {
    if (chart) {
      chart.dispose()
    }
  })
})
</script>

<style scoped>
.risk-dashboard {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ed 100%);
  min-height: 100vh;
}

/* 欢迎区域 */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.date-info {
  font-size: 14px;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.divider {
  color: #d9d9d9;
}

.pending-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: pointer;
}

.pending-badge:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 1400px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  padding: 24px;
  min-height: 166px;
  height: auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.stat-main{
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.text-container {
  display: flex;
  flex-direction: column;
}

.stat-title {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 500;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.application-icon {
  background: linear-gradient(135deg, #5AD8A6 0%, #38b28a 100%);
}

.loan-icon {
  background: linear-gradient(135deg, #f6bd16 0%, #d9a012 100%);
}

.overdue-icon {
  background: linear-gradient(135deg, #f87474 0%, #e04a4a 100%);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.stat-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.stat-trend.up {
  color: #5AD8A6;
}

.stat-trend.down {
  color: #f87474;
}

/* 中间图表区域 */
.charts-section {
  display: flex;
  justify-content: space-between;

  gap: 2%;
  margin-bottom: 24px;
}
@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}
.trend-chart {
  flex: 0 0 60%; 
}

.right-charts {
  display: flex;
  flex-direction: column;
  gap: 20px;

  flex: 0 0 38%; 
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.chart-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.chart-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.blue {
  background-color: #5B8FF9;
}

.legend-dot.green {
  background-color: #5AD8A6;
}

.chart-body {
  height: 320px;
}

/* 风险分布 */
.risk-distribution {
  display: flex;
  align-items: center;
  gap: 16px;
}

.risk-chart {
  width: 100%;
  height: 220px;
}

.risk-legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #595959;
}

.legend-dot.green {
  background-color: #5AD8A6;
}

.legend-dot.orange {
  background-color: #F6BD16;
}

.legend-dot.red {
  background-color: #F87474;
}

.legend-value {
  margin-left: auto;
  font-weight: 600;
  color: #1a1a1a;
}

/* 贷款用途分布 */
.purpose-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.purpose-item {
  display: grid;
  grid-template-columns: 50px 1fr 50px;
  align-items: center;
  gap: 12px;
}

.purpose-name {
  font-size: 14px;
  color: #595959;
}

.purpose-bar {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.purpose-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.purpose-percentage {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  text-align: right;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  margin-bottom: 24px;
}

.view-all {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
}

.view-all:hover {
  text-decoration: underline;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  gap: 12px;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 14px;
  color: #262626;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: #8c8c8c;
}

/* 月度审批统计 */
.monthly-chart {
  height: 200px;
  margin-bottom: 16px;
}

.approval-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.approval-stat {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.approval-stat.approved {
  background: linear-gradient(135deg, #f0f9f5 0%, #d4edda 100%);
}

.approval-stat.rejected {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe5e5 100%);
}

.approval-stat .el-icon {
  font-size: 20px;
  margin-bottom: 8px;
}

.approval-stat.approved .el-icon {
  color: #5AD8A6;
}

.approval-stat.rejected .el-icon {
  color: #F87474;
}

.stat-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 4px;
}

.approval-stat .stat-value {
  font-size: 24px;
  color: #1a1a1a;
  margin: 0;
}

.approval-stat.approved .stat-value {
  color: #5AD8A6;
}

.approval-stat.rejected .stat-value {
  color: #F87474;
}

/* 原有图表区域 */
.original-charts-section {
  margin-top: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 20px 0;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.chart-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px 0;
}

.chart-container {
  height: 280px;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .welcome-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

/* 深色模式 */
html.dark .risk-dashboard {
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 100%);
}

html.dark .welcome-title {
  color: #e0e0e0;
}

html.dark .date-info {
  color: #b0b0b0;
}

html.dark .divider {
  color: #444;
}

html.dark .stat-card {
  background: var(--card-bg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

html.dark .stat-title {
  color: var(--text-secondary);
}

html.dark .stat-value {
  color: var(--text-color);
}

html.dark .stat-subtitle {
  color: var(--text-secondary);
}

html.dark .chart-card {
  background: var(--card-bg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

html.dark .chart-header h4 {
  color: var(--text-color);
}

html.dark .chart-title {
  color: var(--text-color);
}

html.dark .section-title {
  color: var(--text-color);
}

html.dark .chart-item {
  background: var(--card-bg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

html.dark .legend-row {
  color: var(--text-secondary);
}

html.dark .legend-value {
  color: var(--text-color);
}

html.dark .purpose-name {
  color: var(--text-secondary);
}

html.dark .purpose-value {
  color: var(--text-color);
}

html.dark .approval-stat .stat-label {
  color: var(--text-secondary);
}

html.dark .approval-stat .stat-value {
  color: var(--text-color);
}
</style>
