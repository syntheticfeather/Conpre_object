<template>
  <div class="detail-container" v-if="visible">
    <div class="detail-header">
      <h2 class="detail-title">用户详情</h2>
      <div class="header-actions">
        <!-- 如果是黑名单用户，显示黑名单标记 -->
        <span v-if="isBlacklistUser" class="blacklist-badge">
          ⚠️ 黑名单用户（等级: {{ blacklistLevel }}）
        </span>
        <button class="close-btn" @click="close">×</button>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      加载中...
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      加载失败: {{ error }}
    </div>
    
    <!-- 正常显示 -->
    <div v-else-if="userDetail" class="user-info-container">
      <div class="user-info">
        <div class="left">
          <h3 class="detail-subtitle">基本信息</h3>
          <div id="user-info-section1">
            <p><strong>用户ID：</strong><span>{{ userDetail.userId || '—' }}</span></p>
            <p><strong>用户名：</strong><span>{{ userDetail.user?.userName || '—' }}</span></p>
            <p><strong>手机号：</strong><span>{{ userDetail.user?.phone || '—' }}</span></p>
            <p><strong>注册时间：</strong><span>{{ formatDate(userDetail.user?.createTime) }}</span></p>
            <p><strong>最近上线时间：</strong><span>{{ formatDate(userDetail.user?.updateTime) }}</span></p>
            <!-- 黑名单信息 -->
            <p v-if="isBlacklistUser"><strong>黑名单等级：</strong><span class="blacklist-level">{{ blacklistLevel || '—' }}</span></p>
            <p v-if="isBlacklistUser && blacklistJoinTime"><strong>加入黑名单时间：</strong><span>{{ formatDate(blacklistJoinTime) }}</span></p>
          </div>
        </div>
        <div class="right">
          <h3 class="detail-subtitle">认证材料</h3>
          <div id="user-auth-section">
            <div class="material-item">
              <span>信誉分：</span>
              <span :style="{ color: userDetail.userCert?.creditScore != null ? '#27ae60' : '#e74c3c' }">
                {{ userDetail.userCert?.creditScore != null ? userDetail.userCert.creditScore : '—' }}
              </span>
            </div>
            <div 
              v-for="(label, key) in materialMap" 
              :key="key"
              class="material-item"
            >
              <span>{{ label }}：</span>
              <span :style="{ color: userDetail.userCert?.[key] != null ? '#27ae60' : '#e74c3c' }">
                {{ userDetail.userCert?.[key] != null ? '已上传' : '未上传' }}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="section-container">
        <h3 class="detail-subtitle">贷款申请列表</h3>
        <div id="applications">
          <table id="application-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>贷款产品</th>
                <th>申请金额（元）</th>
                <th>期限（月）</th>
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
                <td>{{ app.productId || '—' }}</td>
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
                <td colspan="10" style="text-align:center;">暂无贷款申请</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="section-container">
        <h3 class="detail-subtitle">贷款订单列表</h3>
        <div id="orders">
          <table id="order-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>贷款产品</th>
                <th>状态</th>
                <th>已还金额（元）</th>
                <th>贷款金额（元）</th>
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
                <td>{{ order.productId || '—' }}</td>
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
                <td colspan="13" style="text-align:center;">暂无贷款订单</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <!-- 无数据状态 -->
    <div v-else class="no-data">
      暂无用户数据
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch, computed } from 'vue'
import { useUserStore } from '@/stores/user' 

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

// 判断是否为黑名单用户
const isBlacklistUser = computed(() => {
  if (!userStore.blacklistUserDetail && !userStore.userDetail) return false
  
  // 检查当前用户是否在黑名单中
  const userId = userStore.blacklistUserDetail?.userId || userStore.userDetail?.userId
  if (!userId) return false
  
  return userStore.blacklist.some(user => user.userId === userId)
})

// 获取黑名单信息
const blacklistInfo = computed(() => {
  if (!props.userId) return null
  return userStore.blacklist.find(user => user.userId === props.userId)
})

const blacklistLevel = computed(() => blacklistInfo.value?.blackLevel || '—')
const blacklistJoinTime = computed(() => blacklistInfo.value?.createTime)

// 从 store 获取用户详情
const userDetail = computed(() => {
  // 优先使用黑名单用户详情，如果没有则使用普通用户详情
  return userStore.blacklistUserDetail || userStore.userDetail
})

const materialMap = {
  idCard: '身份证',
  bankCardId: '银行卡',
  workCertId: '工作证明',
  triCertId: '三证合一',
  immovableCertId: '不动产证明'
}

// 监听可见性变化
watch(() => props.isVisible, (val) => {
  console.log('UserDetailPanel isVisible changed:', val)
  visible.value = val
})

// 监听 userId 变化
watch(() => props.userId, (newUserId) => {
  console.log('UserDetailPanel userId changed:', newUserId)
})

// 方法
const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
}

const formatAmount = (amount) => {
  if (amount == null) return '—'
  return `¥${Number(amount).toLocaleString('zh-CN')}`
}

const formatRate = (rate) => {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(2)}%`
}

const close = () => {
  visible.value = false
  emit('close')
}
</script>

<style scoped>
/* 详情容器样式 */
.detail-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
  overflow: hidden;
}

/* 详情头部 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.detail-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.blacklist-badge {
  background: #f8d7da;
  color: #721c24;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  line-height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
  background-color: #e9ecef;
  border-radius: 4px;
}

/* 内容区域 */
.user-info-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.detail-subtitle {
  font-size: 16px;
  color: #555;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #dee2e6;
}

.user-info {
  display: flex;
  gap: 40px;
  margin-bottom: 24px;
}

.left, .right {
  flex: 1;
}

#user-info-section1 p {
  margin: 8px 0;
  line-height: 1.6;
}

.blacklist-level {
  color: #dc3545;
  font-weight: bold;
}

.material-item {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
  padding: 6px 0;
  border-bottom: 1px dashed #eee;
}

.material-item:last-child {
  border-bottom: none;
}

/* 表格容器 */
.section-container {
  margin-bottom: 24px;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0 0 0;
}

thead th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
  font-size: 14px;
  position: sticky;
  top: 0;
}

tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

tbody tr:hover {
  background: #f8f9fa;
}

/* 加载和错误状态 */
.loading-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error-state {
  text-align: center;
  padding: 20px;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  margin: 20px;
}

.no-data {
  text-align: center;
  padding: 60px;
  color: #999;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>