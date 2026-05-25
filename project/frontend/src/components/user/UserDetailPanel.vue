<template>
  <div v-if="visible" class="user-detail-container">
    <!-- 顶部标题栏 -->
    <div class="detail-header">
      <div class="header-left">
        <el-icon class="back-icon" @click="close"><ArrowLeft /></el-icon>
        <div class="title-group">
          <h2 class="detail-title">用户详情</h2>
          <span class="user-id">用户ID: {{ userDetail?.user?.id || '—' }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-button size="small" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button 
          v-if="userStatus === 'normal'"
          size="small" 
          type="danger" 
          @click="handleBlacklist"
        >
          <el-icon><CircleClose /></el-icon>
          列入黑名单
        </el-button>
        <el-button 
          v-else
          size="small" 
          type="danger" 
          disabled
        >
          <el-icon><CircleClose /></el-icon>
          黑名单等级：{{ blacklistLevel }}
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      加载失败: {{ error }}
    </div>

    <!-- 正常显示 -->
    <div v-else-if="userDetail" class="content-wrapper">
      <!-- 中间基本信息卡片 -->
      <div class="basic-info-card">
        <div class="info-main">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img :src="avatarUrl" alt="用户头像" class="avatar">
              <span class="online-dot" :class="{ online: userStatus === 'normal' }"></span>
            </div>
          </div>
          <div class="user-main-info">
            <div class="name-row">
              <span class="user-name">{{ userDetail.user?.userName || '—' }}</span>
              <el-tag v-if="userStatus === 'normal'" size="small" type="success" effect="light">正常</el-tag>
              <el-tag v-else size="small" type="danger" effect="light">黑名单</el-tag>
              <el-tag size="small" type="success" effect="light">低风险</el-tag>
            </div>
            <div class="contact-row">
              <span class="contact-item">
                <el-icon><Phone /></el-icon>
                {{ formatPhone(userDetail.user?.phone) }}
              </span>
              <span class="contact-item">
                <el-icon><Location /></el-icon>
                {{ userDetail.user?.area || '中国' }}
              </span>
              <span class="contact-item">
                <el-icon><User /></el-icon>
                {{ calculateAge() }} 岁 
              </span>
              <span class="contact-item">
                <el-icon><Clock /></el-icon>
                上次登录时间：{{ formatDateShort(userDetail.user?.updateTime) }}
              </span>
            </div>
          </div>
          <div class="credit-score-card">
            <div class="score-label">信用评分</div>
            <div class="score-value" :class="getScoreClass(userDetail.userCert?.creditScore)">
              {{ userDetail.userCert?.creditScore || 0 }}
            </div>
            <div class="score-level">
              <el-icon><StarFilled /></el-icon>
              {{ getScoreLevel(userDetail.userCert?.creditScore) }}
            </div>
          </div>
        </div>
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-value">{{ userDetail.order?.length || 0 }}</div>
            <div class="stat-label">贷款笔数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value amount">¥{{ formatNumber(calculateTotalLoanAmount()) }}</div>
            <div class="stat-label">累计贷款额</div>
          </div>
          <div class="stat-item">
            <div class="stat-value income">¥{{ formatNumber(calculateMonthlyIncome()) }}</div>
            <div class="stat-label">月均收入</div>
          </div>
          <div class="stat-item">
            <div class="stat-value rate">{{ calculateRepaymentRate() }}%</div>
            <div class="stat-label">还款率</div>
          </div>
          <div class="stat-item">
            <div class="stat-value date">{{ formatDateShort(userDetail.user?.createTime) }}</div>
            <div class="stat-label">注册时间</div>
          </div>
        </div>
      </div>

      <!-- Tab切换区域 -->
      <div class="tabs-section">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="基本信息" name="basic">
            <div class="tab-content basic-tab">
              <div class="left-panel">
                <div class="panel-title">
                  <el-icon><CreditCard /></el-icon>
                  认证信息
                </div>
                <div class="info-list">
                  <div class="info-row">
                    <span class="info-label">信誉分</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.creditScore != null ? '#67c23a' : '#f56c6c' }">
                      {{ userDetail.userCert?.creditScore != null ? userDetail.userCert.creditScore : '0' }}
                    </span>
                  </div>
                  <div 
                    class="info-row clickable"
                    :class="{ 'has-value': userDetail.userCert?.idCard != null }"
                    @click="handleMaterialClick('idCard', userDetail.userCert?.idCard)"
                  >
                    <span class="info-label">身份证</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.idCard != null ? '#67c23a' : '#f56c6c' }">
                      <template v-if="userDetail.userCert?.idCard != null">
                        <template v-if="showIdCardFull">
                          {{ userDetail.userCert.idCard }}
                        </template>
                        <template v-else>
                          {{ formatIdCard(userDetail.userCert.idCard) }}
                        </template>
                      </template>
                      <template v-else>
                        未上传
                      </template>
                    </span>
                  </div>
                  <div 
                    class="info-row clickable"
                    :class="{ 'has-value': userDetail.userCert?.bankCardId != null }"
                    @click="handleMaterialClick('bankCardId', userDetail.userCert?.bankCardId)"
                  >
                    <span class="info-label">银行卡</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.bankCardId != null ? '#67c23a' : '#f56c6c' }">
                      <template v-if="userDetail.userCert?.bankCardId != null">
                        <template v-if="showBankCardFull">
                          {{ userDetail.userCert.bankCardId }}
                        </template>
                        <template v-else>
                          {{ formatBankCard(userDetail.userCert.bankCardId) }}
                        </template>
                      </template>
                      <template v-else>
                        未上传
                      </template>
                    </span>
                  </div>
                  <div 
                    class="info-row clickable"
                    :class="{ 'has-value': userDetail.userCert?.workCertId != null }"
                    @click="handleMaterialClick('workCertId', userDetail.userCert?.workCertId)"
                  >
                    <span class="info-label">工作证明</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.workCertId != null ? '#67c23a' : '#f56c6c' }">
                      <template v-if="userDetail.userCert?.workCertId != null">
                        已上传 (点击查看)
                      </template>
                      <template v-else>
                        未上传
                      </template>
                    </span>
                  </div>
                  <div 
                    class="info-row clickable"
                    :class="{ 'has-value': userDetail.userCert?.immovableCertId != null }"
                    @click="handleMaterialClick('immovableCertId', userDetail.userCert?.immovableCertId)"
                  >
                    <span class="info-label">不动产证明</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.immovableCertId != null ? '#67c23a' : '#f56c6c' }">
                      <template v-if="userDetail.userCert?.immovableCertId != null">
                        已上传 (点击查看)
                      </template>
                      <template v-else>
                        未上传
                      </template>
                    </span>
                  </div>
                  <div 
                    class="info-row clickable"
                    :class="{ 'has-value': userDetail.userCert?.triCertId != null }"
                    @click="handleMaterialClick('triCertId', userDetail.userCert?.triCertId)"
                  >
                    <span class="info-label">第三方认证</span>
                    <span class="info-value" :style="{ color: userDetail.userCert?.triCertId != null ? '#67c23a' : '#f56c6c' }">
                      <template v-if="userDetail.userCert?.triCertId != null">
                        已上传 (点击查看)
                      </template>
                      <template v-else>
                        未上传
                      </template>
                    </span>
                  </div>
                </div>
              </div>
              <div class="right-panel">
                <div class="panel-title">
                  <el-icon><TrendCharts /></el-icon>
                  信用画像
                </div>
                <div ref="radarChartRef" class="radar-chart"></div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="贷款记录" name="loans">
            <div class="tab-content loans-tab">
              <div class="section-container">
                <h3 class="detail-subtitle">用户贷款订单列表 ({{ userDetail.order?.length || 0 }})</h3>
                <div class="table-wrapper">
                  <table class="data-table">
                    <thead>
                      <tr>
                        <th>序号</th>
                        <th>贷款产品</th>
                        <th>状态</th>
                        <th>已还金额</th>
                        <th>贷款金额</th>
                        <th>年利率</th>
                        <th>还款方式</th>
                        <th>年限</th>
                        <th>期数</th>
                        <th>已还期数</th>
                        <th>合同</th>
                        <th>逾期天数</th>
                        <th>起始时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(order, index) in userDetail.order || []" :key="index">
                        <td>{{ index + 1 }}</td>
                        <td>{{ productMap[order.productId] || order.productId || '—' }}</td>
                        <td>{{ order.status || '—' }}</td>
                        <td>{{ formatAmount(order.repaidAmount) }}</td>
                        <td>{{ formatAmount(order.loanAmount) }}</td>
                        <td>{{ formatRate(order.interestRate) }}</td>
                        <td>{{ order.repaidType || '—' }}</td>
                        <td>{{ order.loanPeriod || '—' }}</td>
                        <td>{{ order.term || '—' }}</td>
                        <td>{{ order.currentTerm || '—' }}</td>
                        <td>{{ order.contract || '—' }}</td>
                        <td>{{ order.overdueDays || '—' }}</td>
                        <td>{{ formatDate(order.startTime) }}</td>
                      </tr>
                      <tr v-if="!userDetail.order?.length">
                        <td colspan="13" class="no-data-cell">暂无贷款订单</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <div class="section-container">
                <h3 class="detail-subtitle">贷款申请列表 ({{ userDetail.loanApplication?.length || 0 }})</h3>
                <div class="table-wrapper">
                  <table class="data-table">
                    <thead>
                      <tr>
                        <th>序号</th>
                        <th>贷款产品</th>
                        <th>申请金额</th>
                        <th>期限</th>
                        <th>还款方式</th>
                        <th>年利率</th>
                        <th>申请时间</th>
                        <th>状态</th>
                        <th>拒绝原因</th>
                        <th>人工审核时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(app, index) in userDetail.loanApplication || []" :key="index">
                        <td>{{ index + 1 }}</td>
                        <td>{{ productMap[app.productId] || app.productId || '—' }}</td>
                        <td>{{ formatAmount(app.loanAmount) }}</td>
                        <td>{{ app.term || '—' }}</td>
                        <td>{{ app.repaidType || '—' }}</td>
                        <td>{{ formatRate(app.interestRate) }}</td>
                        <td>{{ formatDate(app.applyTime) }}</td>
                        <td>{{ app.status || '—' }}</td>
                        <td>{{ (app.rejectReason || '').trim() || '—' }}</td>
                        <td>{{ formatDate(app.reviewTime) }}</td>
                      </tr>
                      <tr v-if="!userDetail.loanApplication?.length">
                        <td colspan="10" class="no-data-cell">暂无贷款申请</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="风险评估" name="risk">
            <div class="tab-content risk-tab">
              <div class="left-panel">
                <div class="panel-title">
                  <el-icon><Lock /></el-icon>
                  风险评估指标
                </div>
                <div class="risk-metrics">
                  <div class="metric-row">
                    <span class="metric-label">综合风险等级</span>
                    <span class="metric-value low-risk">低风险</span>
                  </div>
                  <div class="metric-row">
                    <span class="metric-label">信用评分</span>
                    <span class="metric-value score">{{ userDetail.userCert?.creditScore || 0 }} 分</span>
                  </div>
                  <div class="metric-row">
                    <span class="metric-label">历史还款率</span>
                    <span class="metric-value rate">{{ calculateRepaymentRate() }}%</span>
                  </div>
                  <div class="metric-row">
                    <span class="metric-label">贷款笔数</span>
                    <span class="metric-value">{{ userDetail.order?.length || 0 }} 笔</span>
                  </div>
                  <div class="metric-row">
                    <span class="metric-label">累计贷款额度</span>
                    <span class="metric-value amount">¥{{ formatNumber(calculateTotalLoanAmount()) }}</span>
                  </div>
                </div>
              </div>
              <div class="right-panel">
                <div class="panel-title">
                  <el-icon><CircleCheck /></el-icon>
                  风控预警
                </div>
                <div class="risk-alert-box">
                  <el-icon class="alert-icon"><CircleCheckFilled /></el-icon>
                  <div class="alert-text">
                    <div class="alert-title">该用户无风险预警</div>
                    <div class="alert-desc">信用状况良好，历史还款记录正常</div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 无数据状态 -->
    <div v-else class="no-data">
      暂无用户数据
    </div>

    <!-- 图片预览组件 -->
    <ImagePreview
      v-model:visible="showImagePreview"
      :image-url="previewImageUrl"
      :title="previewTitle"
    />
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { authAPI } from '@/api'
import { loanAPI } from '@/api'
import ImagePreview from '@/components/shared/ImagePreview.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  ArrowLeft, Edit, Lock, CircleClose, Loading, Phone, Location,
  StarFilled, CreditCard, TrendCharts, CircleCheck, CircleCheckFilled, User, Clock
} from '@element-plus/icons-vue'

const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  },
  isVisible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])
const userStore = useUserStore()
const visible = ref(props.isVisible)
const loading = ref(false)
const error = ref(null)
const activeTab = ref('basic')
const radarChartRef = ref(null)
let radarChart = null

const showImagePreview = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')

const showIdCardFull = ref(false)
const showBankCardFull = ref(false)

const productList = ref([])
const productMap = computed(() => {
  const map = {}
  productList.value.forEach(product => {
    map[product.id] = product.productName
  })
  return map
})

const userStatus = computed(() => {
  if (!props.userId) return 'normal'
  const inBlacklist = userStore.blacklist.some(user => user.userId === props.userId)
  if (inBlacklist) return 'blacklist'
  return 'normal'
})

const blacklistLevel = computed(() => {
  if (!props.userId) return null
  const blacklistUser = userStore.blacklist.find(user => user.userId === props.userId)
  return blacklistUser?.blackLevel || null
})

const userDetail = computed(() => {
  return userStore.blacklistUserDetail || userStore.userDetail
})

const avatarUrl = computed(() => {
  const avatar = userDetail.value?.user?.avatar
  if (!avatar) return '/1.jpg'
  let processedUrl = avatar.replace(/[\\/]/g, '/')
  if (processedUrl.startsWith('http')) return processedUrl
  if (processedUrl.startsWith('/uploads/')) return processedUrl
  if (processedUrl.startsWith('avatars/')) return '/uploads/' + processedUrl
  if (!processedUrl.startsWith('/')) processedUrl = '/' + processedUrl
  return processedUrl
})

const materialMap = {
  idCard: '身份证',
  bankCardId: '银行卡',
  workCertId: '工作证明',
  triCertId: '第三方认证',
  immovableCertId: '不动产证明'
}

const certTypeMap = {
  workCertId: 'workCert',
  triCertId: 'triCert',
  immovableCertId: 'immovableCert'
}

const formatPhone = (phone) => {
  if (!phone) return '—'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const formatIdCard = (idCard) => {
  if (!idCard) return '—'
  return idCard.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
}

const formatBankCard = (bankCard) => {
  if (!bankCard) return '—'
  const cleaned = bankCard.replace(/\s/g, '')
  if (cleaned.length <= 8) return cleaned
  return cleaned.replace(/(\d{4})\d+(\d{4})/, '$1 **** **** $2')
}

const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatDateShort = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const formatAmount = (amount) => {
  if (amount == null) return '—'
  return `¥${Number(amount).toLocaleString('zh-CN')}`
}

const formatNumber = (num) => {
  if (num == null) return '0'
  return Number(num).toLocaleString('zh-CN')
}

const formatRate = (rate) => {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(2)}%`
}

const calculateAge = () => {
  const idCard = userDetail.value?.userCert?.idCard
  if (!idCard || idCard.length < 14) return '未知'
  const year = parseInt(idCard.substring(6, 10))
  const currentYear = new Date().getFullYear()
  return currentYear - year
}

const calculateTotalLoanAmount = () => {
  if (!userDetail.value?.order?.length) return 0
  return userDetail.value.order.reduce((sum, order) => sum + (order.loanAmount || 0), 0)
}

const calculateMonthlyIncome = () => {
  return 18000
}

const calculateRepaymentRate = () => {
  const orders = userDetail.value?.order || []
  if (!orders.length) return '0.0'
  const totalLoan = orders.reduce((sum, order) => sum + (order.loanAmount || 0), 0)
  const totalRepaid = orders.reduce((sum, order) => sum + (order.repaidAmount || 0), 0)
  if (totalLoan === 0) return '0.0'
  return ((totalRepaid / totalLoan) * 100).toFixed(1)
}

const getScoreClass = (score) => {
  if (!score) return 'score-low'
  if (score >= 700) return 'score-excellent'
  if (score >= 600) return 'score-good'
  if (score >= 500) return 'score-medium'
  return 'score-low'
}

const getScoreLevel = (score) => {
  if (!score) return '待评估'
  if (score >= 700) return '优质'
  if (score >= 600) return '良好'
  if (score >= 500) return '一般'
  return '较差'
}

const calculateRadarData = () => {
  const userCert = userDetail.value?.userCert || {}
  const orders = userDetail.value?.order || []
  const applications = userDetail.value?.loanApplication || []

  const creditScore = (userCert.creditScore || 0) / 800 * 100

  let repaymentCredit = 0
  if (orders.length > 0) {
    const noOverdueCount = orders.filter(order => !order.overdueDays || order.overdueDays === 0).length
    repaymentCredit = (noOverdueCount / orders.length) * 100
  }

  let certCompleteness = 0
  const certFields = ['idCard', 'bankCardId', 'workCertId', 'triCertId', 'immovableCertId']
  const uploadedCount = certFields.filter(field => userCert[field] != null).length
  certCompleteness = uploadedCount * 20

  const totalTransactions = orders.length + applications.length
  const activity = Math.min((totalTransactions / 50) * 100, 100)

  let assetAbility = 0
  if (orders.length > 0) {
    const totalLoanAmount = orders.reduce((sum, order) => sum + (order.loanAmount || 0), 0)
    const totalRepaidAmount = orders.reduce((sum, order) => sum + (order.repaidAmount || 0), 0)
    if (totalLoanAmount > 0) {
      assetAbility = (totalRepaidAmount / totalLoanAmount) * 100
    }
  }

  return [
    { name: '信誉分', value: Math.round(creditScore) },
    { name: '还款信用', value: Math.round(repaymentCredit) },
    { name: '认证完整度', value: Math.round(certCompleteness) },
    { name: '活跃度', value: Math.round(activity) },
    { name: '资产能力', value: Math.round(assetAbility) }
  ]
}

const initRadarChart = () => {
  if (!radarChartRef.value) return

  if (radarChart) {
    radarChart.dispose()
  }

  radarChart = echarts.init(radarChartRef.value)

  const radarData = calculateRadarData()

  const option = {
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: radarData.map(item => ({
        name: item.name,
        max: 100
      })),
      radius: '65%',
      axisName: {
        color: '#666',
        fontSize: 12
      },
      splitArea: {
        areaStyle: {
          color: ['#f8f9fa', '#fff']
        }
      },
      axisLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#e0e0e0'
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radarData.map(item => item.value),
            name: '用户画像',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.2)'
            },
            lineStyle: {
              color: '#409eff',
              width: 2
            },
            itemStyle: {
              color: '#409eff'
            }
          }
        ]
      }
    ]
  }

  radarChart.setOption(option)
}

const handleEdit = () => {
  ElMessage.info('编辑功能开发中')
}

const handleBlacklist = async () => {
  if (!props.userId) {
    ElMessage.warning('用户ID不存在')
    return
  }

  const userName = userDetail.value?.user?.userName || '该用户'
  const userId = props.userId

  if (userStatus.value === 'blacklist') {
    ElMessage.warning('该用户已在黑名单中')
    return
  }

  try {
    const { value: blackLevel } = await ElMessageBox.prompt(
      `请输入用户【${userName}】的黑名单等级（1-3）：`,
      '加入黑名单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[123]$/,
        inputErrorMessage: '黑名单等级只能为 1、2 或 3'
      }
    )

    const level = parseInt(blackLevel)
    if (level < 1 || level > 3) {
      ElMessage.error('黑名单等级只能为 1、2 或 3')
      return
    }

    await ElMessageBox.confirm(
      `确定将用户【${userName}】（ID: ${userId}）加入黑名单？等级：${level}`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await userStore.addToBlacklist(userId, level)
    if (result.success) {
      ElMessage.success('已成功加入黑名单')
      await userStore.fetchBlacklist()
      close()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败：' + (error.message || '未知错误'))
    }
  }
}

const fetchCertImage = async (certId, certType) => {
  if (!certId) {
    ElMessage.warning('该材料未上传')
    return
  }

  try {
    let response
    let imageUrl

    switch (certType) {
      case 'workCert':
        response = await authAPI.getWorkCert(certId)
        if (response.code === 200 && response.data) {
          imageUrl = response.data.employmentCertPath || response.data.salaryCertPath
        }
        break
      case 'triCert':
        response = await authAPI.getTriCert(certId)
        if (response.code === 200 && response.data) {
          imageUrl = response.data.socialSecurityPath || response.data.creditReportPath
        }
        break
      case 'immovableCert':
        response = await authAPI.getImmovablesCert(certId)
        if (response.code === 200 && response.data) {
          imageUrl = response.data.propertyCertPath || response.data.carCertPath
        }
        break
      default:
        ElMessage.warning('不支持的认证类型')
        return
    }

    if (imageUrl) {
      imageUrl = imageUrl.replace(/[\\/]/g, '/')
      if (!imageUrl.startsWith('/')) {
        imageUrl = '/' + imageUrl
      }
      previewImageUrl.value = imageUrl
      previewTitle.value = materialMap[certType] || '认证材料'
      showImagePreview.value = true
    } else {
      ElMessage.warning('未找到图片')
    }
  } catch (error) {
    console.error('获取认证材料失败:', error)
    ElMessage.error('获取认证材料失败')
  }
}

const handleMaterialClick = (key, certId) => {
  if (!certId) {
    ElMessage.warning('该材料未上传')
    return
  }
  
  if (key === 'idCard') {
    showIdCardFull.value = !showIdCardFull.value
    return
  }
  
  if (key === 'bankCardId') {
    showBankCardFull.value = !showBankCardFull.value
    return
  }
  
  const certType = certTypeMap[key]
  if (certType) {
    fetchCertImage(certId, certType)
  } else {
    ElMessage.warning('该材料类型不支持图片查看')
  }
}

const loadUserDetail = async () => {
  if (!props.userId) return

  loading.value = true
  error.value = null

  try {
    userStore.clearUserDetail()
    userStore.clearBlacklistUserDetail()

    if (userStore.blacklist.length === 0) {
      try {
        await userStore.fetchBlacklist()
      } catch (blacklistErr) {
        console.warn('加载黑名单失败:', blacklistErr)
      }
    }

    const userInBlacklist = userStore.blacklist.some(user => user.userId === props.userId)

    if (userInBlacklist) {
      await userStore.fetchBlacklistUserDetail(props.userId)
    } else {
      await userStore.fetchUserDetail(props.userId)
    }

    // 数据加载完成后，等待 DOM 更新再初始化雷达图
    await nextTick()
    setTimeout(() => {
      initRadarChart()
    }, 100)
  } catch (err) {
    console.error('加载用户详情失败:', err)
    error.value = err.message || '加载用户详情失败'
    ElMessage.error('加载用户详情失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const fetchProductList = async () => {
  try {
    const response = await loanAPI.getProducts()
    if (response.code === 200) {
      productList.value = response.data || []
    }
  } catch (error) {
    console.error('获取产品列表失败:', error)
  }
}

const close = () => {
  visible.value = false
  emit('close')
}

watch(() => props.isVisible, (val) => {
  visible.value = val
  if (val && props.userId) {
    loadUserDetail()
  }
}, { immediate: true })

watch(() => props.userId, (newUserId) => {
  if (newUserId && visible.value) {
    loadUserDetail()
  }
}, { immediate: true })

watch(activeTab, (newTab) => {
  if (newTab === 'basic') {
    nextTick(() => {
      initRadarChart()
    })
  }
})

onMounted(() => {
  fetchProductList()
  nextTick(() => {
    initRadarChart()
  })
})

onUnmounted(() => {
  if (radarChart) {
    radarChart.dispose()
  }
})
</script>

<style scoped>
@import '@/assets/css/user/userDetailPanel.css';
</style>