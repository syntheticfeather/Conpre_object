<template>
  <div class="risk-manage-view">
    <div class="header">
      <h2 class="title">风险管理</h2>
    </div>
    
    <!-- 数据统计图表 -->
    <div class="chart-section">
      <h3 class="section-title">数据统计</h3>
      <div class="chart-grid">
        <!-- 用户统计图表 -->
        <div class="chart-item">
          <h4 class="chart-title">用户贷款状态分布</h4>
          <el-card shadow="hover">
            <div ref="userChartRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
        
        <!-- 贷款申请统计图表 -->
        <div class="chart-item">
          <h4 class="chart-title">贷款申请状态分布</h4>
          <el-card shadow="hover">
            <div ref="loanApplicationChartRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
        
        <!-- 申请通过率 -->
        <div class="chart-item">
          <h4 class="chart-title">申请通过率</h4>
          <el-card shadow="hover">
            <div ref="approvalRateRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 风控数据支持 -->
    <div class="chart-section">
      <h3 class="section-title">风控数据</h3>
      <div class="chart-grid">
        <!-- 用户信用分分布 -->
        <div class="chart-item">
          <h4 class="chart-title">用户信用分统计</h4>
          <el-card shadow="hover">
            <div ref="creditScoreDistributionRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
        
        <!-- 风险等级统计 -->
        <div class="chart-item">
          <h4 class="chart-title">风险（黑名单）等级统计</h4>
          <el-card shadow="hover">
            <div ref="riskLevelRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 数据趋势图表 -->
    <div class="chart-section">
      <h3 class="section-title">数据趋势</h3>
      <div class="chart-grid">
        <!-- 贷款申请趋势 -->
        <div class="chart-item">
          <h4 class="chart-title">贷款申请趋势</h4>
          <el-card shadow="hover">
            <div ref="loanApplicationTrendRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
        
        <!-- 用户注册趋势 -->
        <div class="chart-item">
          <h4 class="chart-title">用户注册趋势</h4>
          <el-card shadow="hover">
            <div ref="userRegistrationTrendRef" class="chart-container" style="height: 200px;"></div>
          </el-card>
        </div>
      </div>
    </div>
    
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { userAPI, loanApplicationAPI } from '@/api'
import * as echarts from 'echarts'

// 图表引用
const userChartRef = ref(null)
const loanApplicationChartRef = ref(null)
const userRegistrationTrendRef = ref(null)
const loanApplicationTrendRef = ref(null)
const creditScoreDistributionRef = ref(null)
const approvalRateRef = ref(null)
const riskLevelRef = ref(null)

// 图表实例
let userChart = null
let loanApplicationChart = null
let userRegistrationTrendChart = null
let loanApplicationTrendChart = null
let creditScoreDistributionChart = null
let approvalRateChart = null
let riskLevelChart = null

// 获取用户数据
const fetchUserData = async () => {
  try {
    const response = await userAPI.getUserStats()
    if (response.code === 200) {
      const users = response.data
      
      // 处理用户统计数据
      const loanStatusCount = {
        正常: 0,
        无借贷: 0,
        逾期: 0
      }
      
      const creditScores = []
      
      users.forEach(user => {
        if (user.loanStatus) {
          loanStatusCount[user.loanStatus] = (loanStatusCount[user.loanStatus] || 0) + 1
        }
        if (user.creditScore) {
          creditScores.push(user.creditScore)
        }
      })
      
      // 用户统计图表
      if (userChartRef.value) {
        const totalUsers = Object.values(loanStatusCount).reduce((sum, value) => sum + value, 0);
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
              // 计算tooltip的宽高
              const tooltipWidth = dom.offsetWidth;
              const tooltipHeight = dom.offsetHeight;
              
              // 根据鼠标位置判断象限
              if (point[0] < chartCenterX && point[1] < chartCenterY) {
                // 左上象限 - tooltip显示在鼠标左上
                return [point[0] - tooltipWidth - 10, point[1] - tooltipHeight - 10];
              } else if (point[0] < chartCenterX && point[1] >= chartCenterY) {
                // 左下象限 - tooltip显示在鼠标左下
                return [point[0] - tooltipWidth - 10, point[1] + 10];
              } else if (point[0] >= chartCenterX && point[1] < chartCenterY) {
                // 右上象限 - tooltip显示在鼠标右上
                return [point[0] + 10, point[1] - tooltipHeight - 10];
              } else {
                // 右下象限 - tooltip显示在鼠标右下
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
    // 获取待审核列表和已审核列表
    const [pendingResponse, completedResponse] = await Promise.all([
      loanApplicationAPI.getPendingApprovals(),
      loanApplicationAPI.getCompletedApprovals()
    ])
    
    // 处理贷款申请统计数据
    const statusCount = {
      '审核中': 0,
      '已通过': 0,
      'AI拒绝': 0,
      '人工拒绝': 0,
      '已取消': 0
    }
    
    const applyTimeData = {}
    
    // 处理待审批列表（AI拒绝）
    if (pendingResponse.code === 200 && pendingResponse.data) {
      pendingResponse.data.forEach(app => {
        statusCount['AI拒绝']++
        if (app.applyTime) {
          // 处理不同格式的日期
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
    
    // 处理已完成审批列表（已通过、人工拒绝）
    if (completedResponse.code === 200 && completedResponse.data) {
      completedResponse.data.forEach(app => {
        let status = app.status
        if (status === '已通过') {
          statusCount['已通过']++
        } else if (status === '人工拒绝') {
          statusCount['人工拒绝']++
        }
        if (app.applyTime) {
          // 处理不同格式的日期
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
    
    console.log('Final status count:', statusCount)
      
      // 贷款申请统计图表
      if (loanApplicationChartRef.value) {
        const totalApplications = Object.values(statusCount).reduce((sum, value) => sum + value, 0);
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
              // 计算tooltip的宽高
              const tooltipWidth = dom.offsetWidth;
              const tooltipHeight = dom.offsetHeight;
              
              // 根据鼠标位置判断象限
              if (point[0] < chartCenterX && point[1] < chartCenterY) {
                // 左上象限 - tooltip显示在鼠标左上
                return [point[0] - tooltipWidth - 10, point[1] - tooltipHeight - 10];
              } else if (point[0] < chartCenterX && point[1] >= chartCenterY) {
                // 左下象限 - tooltip显示在鼠标左下
                return [point[0] - tooltipWidth - 10, point[1] + 10];
              } else if (point[0] >= chartCenterX && point[1] < chartCenterY) {
                // 右上象限 - tooltip显示在鼠标右上
                return [point[0] + 10, point[1] - tooltipHeight - 10];
              } else {
                // 右下象限 - tooltip显示在鼠标右下
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
      
      // 贷款申请趋势图表 - 按月份统计
      const monthlyData = {}
      Object.entries(applyTimeData).forEach(([date, count]) => {
        const month = date.substring(0, 7) // 提取年月，格式为 YYYY-MM
        monthlyData[month] = (monthlyData[month] || 0) + count
      })
      
      // 申请通过率图表
      const totalApplications = Object.values(statusCount).reduce((sum, value) => sum + value, 0)
      const approvedCount = statusCount['已通过'] || 0
      const approvalRate = totalApplications > 0 ? (approvedCount / totalApplications) * 100 : 0
      
      if (approvalRateRef.value) {
        approvalRateChart = echarts.init(approvalRateRef.value)
        approvalRateChart.setOption({
          tooltip: {
            trigger: 'item',
            position: function(point, params, dom, rect, size) {
              // point: 鼠标位置
              // size: 图表尺寸
              const chartCenterX = size.viewSize[0] / 2;
              const chartCenterY = size.viewSize[1] / 2;
              // 计算tooltip的宽高
              const tooltipWidth = dom.offsetWidth;
              const tooltipHeight = dom.offsetHeight;
              
              // 根据鼠标位置判断象限
              if (point[0] < chartCenterX && point[1] < chartCenterY) {
                // 左上象限 - tooltip显示在鼠标左上
                return [point[0] - tooltipWidth - 10, point[1] - tooltipHeight - 10];
              } else if (point[0] < chartCenterX && point[1] >= chartCenterY) {
                // 左下象限 - tooltip显示在鼠标左下
                return [point[0] - tooltipWidth - 10, point[1] + 10];
              } else if (point[0] >= chartCenterX && point[1] < chartCenterY) {
                // 右上象限 - tooltip显示在鼠标右上
                return [point[0] + 10, point[1] - tooltipHeight - 10];
              } else {
                // 右下象限 - tooltip显示在鼠标右下
                return [point[0] + 10, point[1] + 10];
              }
            }
          },
          series: [
            {
              name: '申请通过率',
              type: 'gauge',
              startAngle: 180,
              endAngle: 0,
              min: 0,
              max: 100,
              splitNumber: 10,
              axisLine: {
                lineStyle: {
                  width: 6,
                  color: [
                    [0.3, '#67C23A'],
                    [0.7, '#E6A23C'],
                    [1, '#F56C6C']
                  ]
                }
              },
              pointer: {
                icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
                length: '12%',
                width: 20,
                offsetCenter: [0, '-60%'],
                itemStyle: {
                  color: 'inherit'
                }
              },
              axisTick: {
                length: 12,
                lineStyle: {
                  color: 'inherit',
                  width: 2
                }
              },
              splitLine: {
                length: 20,
                lineStyle: {
                  color: 'inherit',
                  width: 5
                }
              },
              axisLabel: {
                color: '#464646',
                fontSize: 16,
                distance: -60,
                formatter: function (value) {
                  if (value === 0 || value === 100) {
                    return value + '%';
                  } else {
                    return '';
                  }
                }
              },
              title: {
                offsetCenter: [0, '-10%'],
                fontSize: 20
              },
              detail: {
                fontSize: 30,
                offsetCenter: [0, '-35%'],
                valueAnimation: true,
                formatter: function (value) {
                  return Math.round(value) + '%';
                },
                color: 'inherit'
              },
              data: [
                {
                  value: approvalRate,
                  name: '通过率'
                }
              ]
            }
          ]
        })
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
      
      // 处理风险等级数据
      const riskLevelCount = {
        '0': 0, // 无风险
        '1': 0,
        '2': 0,
        '3': 0
      }
      
      blacklist.forEach(item => {
        if (item.blackLevel) {
          riskLevelCount[item.blackLevel] = (riskLevelCount[item.blackLevel] || 0) + 1
        }
      })
      
      // 计算无风险用户数
      // 先获取总用户数
      const totalUsersResponse = await userAPI.searchUsersByCredit('>0');
      const totalUsers = Array.isArray(totalUsersResponse) ? totalUsersResponse.length : 0;
      // 无风险用户数 = 总用户数 - 黑名单用户数
      riskLevelCount['0'] = totalUsers - blacklist.length;
      
      // 风险等级统计图表
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
              }
            }
          ]
        })
      }
    }
  } catch (error) {
    console.error('获取风险等级数据失败:', error)
  }
}

// 获取用户注册趋势数据
const fetchUserRegistrationTrend = async () => {
  try {
    const response = await userAPI.searchUsersByCredit('>0')
    console.log('searchUsersByCredit response:', response)
    let users = []
    
    // 根据接口文档，直接返回用户数组
    if (Array.isArray(response)) {
      users = response
    }
    
    if (users.length > 0) {
      // 处理注册时间数据
      const registrationTimeData = {}
      
      users.forEach(user => {
        if (user.createTime) {
          // 处理不同格式的日期
          let dateStr = user.createTime
          if (dateStr.includes('T')) {
            dateStr = dateStr.split('T')[0]
          } else if (dateStr.includes(' ')) {
            dateStr = dateStr.split(' ')[0]
          }
          registrationTimeData[dateStr] = (registrationTimeData[dateStr] || 0) + 1
        }
      })
      
      // 按月份统计
      const monthlyRegistrationData = {}
      Object.entries(registrationTimeData).forEach(([date, count]) => {
        const month = date.substring(0, 7) // 提取年月，格式为 YYYY-MM
        monthlyRegistrationData[month] = (monthlyRegistrationData[month] || 0) + count
      })
      
      console.log('monthlyRegistrationData:', monthlyRegistrationData)
      return monthlyRegistrationData
    }
    return {}
  } catch (error) {
    console.error('获取用户注册趋势数据失败:', error)
    return {}
  }
}

// 页面加载时获取数据
onMounted(async () => {
  await fetchUserData()
  const monthlyLoanData = await fetchLoanApplicationData()
  await fetchRiskLevelData()
  const monthlyRegistrationData = await fetchUserRegistrationTrend()
  console.log('monthlyRegistrationData:', monthlyRegistrationData)
  
  // 处理贷款申请趋势数据
  const loanMonths = new Set(Object.keys(monthlyLoanData))
  const currentYear = new Date().getFullYear()
  for (let i = 1; i <= 12; i++) {
    const monthStr = `${currentYear}-${i.toString().padStart(2, '0')}`
    loanMonths.add(monthStr)
  }
  const sortedLoanMonths = Array.from(loanMonths).sort((a, b) => a.localeCompare(b))
  const formattedLoanMonths = sortedLoanMonths.map(month => {
    const parts = month.split('-')
    return parts[1] + '月'
  })
  const loanCounts = sortedLoanMonths.map(month => monthlyLoanData[month] || 0)
  
  // 处理用户注册趋势数据
  const registrationMonths = new Set(Object.keys(monthlyRegistrationData))
  for (let i = 1; i <= 12; i++) {
    const monthStr = `${currentYear}-${i.toString().padStart(2, '0')}`
    registrationMonths.add(monthStr)
  }
  const sortedRegistrationMonths = Array.from(registrationMonths).sort((a, b) => a.localeCompare(b))
  const formattedRegistrationMonths = sortedRegistrationMonths.map(month => {
    const parts = month.split('-')
    return parts[1] + '月'
  })
  const registrationCounts = sortedRegistrationMonths.map(month => monthlyRegistrationData[month] || 0)
  console.log('registrationCounts:', registrationCounts)
  
  // 贷款申请趋势图表
  if (loanApplicationTrendRef.value) {
    loanApplicationTrendChart = echarts.init(loanApplicationTrendRef.value)
    loanApplicationTrendChart.setOption({
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
        data: formattedLoanMonths,
        axisLine: {
          onZero: false
        },
        axisLabel: {
          interval: 0,
          fontSize: 10,
          rotate: 30
        }
      },
      yAxis: {
        type: 'value',
        splitLine: {
          show: true,
          lineStyle: {
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '申请数',
          type: 'line',
          stack: 'Total',
          data: loanCounts,
          smooth: true,
          lineStyle: {
            width: 2
          }
        }
      ]
    })
  }
  
  // 用户注册趋势图表
  if (userRegistrationTrendRef.value) {
    userRegistrationTrendChart = echarts.init(userRegistrationTrendRef.value)
    userRegistrationTrendChart.setOption({
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
        data: formattedRegistrationMonths,
        axisLine: {
          onZero: false
        },
        axisLabel: {
          interval: 0,
          fontSize: 10,
          rotate: 30
        }
      },
      yAxis: {
        type: 'value',
        splitLine: {
          show: true,
          lineStyle: {
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '注册数',
          type: 'line',
          stack: 'Total',
          data: registrationCounts,
          smooth: true,
          lineStyle: {
            width: 2
          }
        }
      ]
    })
  }
  
  // 添加响应式处理
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onUnmounted(() => {
  // 销毁图表实例
  if (userChart) userChart.dispose()
  if (loanApplicationChart) loanApplicationChart.dispose()
  if (userRegistrationTrendChart) userRegistrationTrendChart.dispose()
  if (loanApplicationTrendChart) loanApplicationTrendChart.dispose()
  if (creditScoreDistributionChart) creditScoreDistributionChart.dispose()
  if (approvalRateChart) approvalRateChart.dispose()
  if (riskLevelChart) riskLevelChart.dispose()
  
  // 移除事件监听器
  window.removeEventListener('resize', handleResize)
})

// 处理窗口大小变化
const handleResize = () => {
  if (userChart) userChart.resize()
  if (loanApplicationChart) loanApplicationChart.resize()
  if (userRegistrationTrendChart) userRegistrationTrendChart.resize()
  if (loanApplicationTrendChart) loanApplicationTrendChart.resize()
  if (creditScoreDistributionChart) creditScoreDistributionChart.resize()
  if (approvalRateChart) approvalRateChart.resize()
  if (riskLevelChart) riskLevelChart.resize()
}
</script>

<style scoped>
.risk-manage-view {
  padding: 20px;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  flex-wrap: wrap;
  gap: 16px;
}

.title {
  margin: 0;

  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.chart-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.chart-item {
  display: flex;
  flex-direction: column;
}

.chart-item.full-width {
  grid-column: 1 / -1;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.el-card {
  flex: 1;
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .search-section {
    width: 100%;
    max-width: none;
  }
  
  .chart-item.full-width {
    grid-column: 1;
  }
}
</style>