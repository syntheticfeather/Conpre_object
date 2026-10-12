<template>
  <component
    :is="detailTransition"
    v-bind="embedded ? { name: 'detail-slide' } : {}"
    @after-enter="onTransitionComplete"
  >
  <div v-show="shouldShow" class="inline-detail-panel" :class="{ 'is-embedded': embedded }">
    <div class="detail-header">
      <h3>{{ reviewType === 'postpone' ? '延期申请详情' : '申请详情' }}</h3>
      <button class="back-btn" @click="handleClose">← 返回列表</button>
    </div>

    <div v-loading="loading">
      <!-- ========== 顶栏：loan=贷款信息 / postpone=项目信息，右侧按钮或状态 pill ========== -->
      <div class="loan-topbar">
        <span class="loan-topbar__title">{{ reviewType === 'postpone' ? '项目信息' : '贷款信息' }}</span>
        <div class="loan-topbar__actions">
          <template v-if="isPending">
            <button class="btn-reject" @click="handleSubmit(false)">不通过</button>
            <button class="btn-pass" @click="handleSubmit(true)">通过</button>
          </template>
          <template v-else>
            <span class="status-pill" :class="detailStatusMeta.cls">
              <i class="status-pill__dot" />
              {{ detailStatusMeta.text }}
            </span>
          </template>
        </div>
      </div>

      <!-- ========== loan 分支：贷款申请 3×2 九宫格 ========== -->
      <section v-if="reviewType !== 'postpone'" class="loan-grid">
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-chanpin"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">贷款项目</div>
              <div class="loan-tile__value">{{ loanApplication?.productName || ('#' + (loanApplication?.productId || '—')) }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-jine"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">申请金额</div>
              <div class="loan-tile__value">{{ formatCurrency(loanApplication?.loanAmount) }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-jiekuanqishu"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">总期数</div>
              <div class="loan-tile__value">{{ loanApplication?.term || '—' }} 期</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-nianxian"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">贷款年限</div>
              <div class="loan-tile__value">{{ loanApplication?.loanPeriod || '—' }} 年</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-nianhualishuai"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">年利率</div>
              <div class="loan-tile__value">{{ formatRate(loanApplication?.interestRate) }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-huankuan"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">还款方式</div>
              <div class="loan-tile__value">{{ loanApplication?.repaidType || '—' }}</div>
            </div>
          </div>
        </section>

        <!-- ========== postpone 分支：项目信息（订单视角 3×2 九宫格，数据取自关联订单） ========== -->
        <section v-else class="loan-grid">
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-chanpin"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">贷款项目</div>
              <div class="loan-tile__value">{{ orderProductName }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-jine"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">贷款金额</div>
              <div class="loan-tile__value">{{ formatCurrency(orderInfo?.loanAmount) }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-riqiqishu"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">当前期数</div>
              <div class="loan-tile__value">{{ orderInfo?.currentTerm ?? '—' }} 期</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-jiekuanqishu"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">总期数</div>
              <div class="loan-tile__value">{{ orderInfo?.term ?? '—' }} 期</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-huankuan"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">已还金额</div>
              <div class="loan-tile__value">{{ formatCurrency(orderInfo?.repaidAmount) }}</div>
            </div>
          </div>
          <div class="loan-tile">
            <div class="loan-tile__icon"><span class="iconfont icon-complete"></span></div>
            <div class="loan-tile__body">
              <div class="loan-tile__label">订单状态</div>
              <div class="loan-tile__value" :class="orderStatusClass">{{ orderInfo?.status || '—' }}</div>
            </div>
          </div>
        </section>

        <!-- 延期申请元信息条：申请ID / 申请延期期数 / 申请时间 / 审核时间 -->
        <div v-if="reviewType === 'postpone'" class="loan-postpone-meta">
          <div class="loan-postpone-meta__item">
            <span class="loan-postpone-meta__label">申请ID</span>
            <span class="loan-postpone-meta__value">{{ postponeDetail?.id || '—' }}</span>
          </div>
          <div class="loan-postpone-meta__item">
            <span class="loan-postpone-meta__label">申请延期期数</span>
            <span class="loan-postpone-meta__value">第 {{ postponeDetail?.currentTerm ?? '—' }} 期</span>
          </div>
          <div class="loan-postpone-meta__item">
            <span class="loan-postpone-meta__label">申请时间</span>
            <span class="loan-postpone-meta__value">{{ formatDate(postponeDetail?.createdAt) }}</span>
          </div>
          <div class="loan-postpone-meta__item">
            <span class="loan-postpone-meta__label">审核时间</span>
            <span class="loan-postpone-meta__value">
              {{ postponeDetail?.reviewedAt ? formatDate(postponeDetail.reviewedAt) : '待审核' }}
            </span>
          </div>
        </div>

        <!-- 拒绝原因：AI拒绝（仅左）/ 人工拒绝（左AI + 右人工 两端） -->
        <div
          v-if="reviewType !== 'postpone' && loanApplication?.rejectReason && loanApplication.rejectReason !== '无'"
          class="reject-banner"
        >
          <template v-if="isManualReject">
            <div class="reject-banner__left">
              <span class="reject-banner__label">AI拒绝原因：</span>
              <span class="reject-banner__value">{{ aiRejectReason || '—' }}</span>
            </div>
            <div class="reject-banner__right">
              <span class="reject-banner__label">人工审核未通过：</span>
              <span class="reject-banner__value">{{ manualRejectReason || '未填写原因' }}</span>
            </div>
          </template>
          <template v-else>
            <div class="reject-banner__left">
              <span class="reject-banner__label">AI拒绝原因：</span>
              <span class="reject-banner__value">{{ loanApplication?.rejectReason }}</span>
            </div>
          </template>
        </div>

        <!-- postpone 分支拒绝原因：纯文本，单侧展示 -->
        <div
          v-else-if="reviewType === 'postpone' && postponeDetail?.rejectReason"
          class="reject-banner"
        >
          <div class="reject-banner__left">
            <span class="reject-banner__label">拒绝原因：</span>
            <span class="reject-banner__value">{{ postponeDetail.rejectReason }}</span>
          </div>
        </div>

        <!-- 下方：左栏（申请人）1 : 右栏（状态+认证）2，两个分支共享 -->
        <section class="loan-bottom-cols">
          <!-- 左栏：申请人信息（头像居中在上 → 下方竖排所有字段） -->
          <div class="loan-col loan-col--applicant">
            <div class="loan-col__title">申请人信息</div>
            <div class="loan-applicant">
              <div class="loan-applicant__avatar-wrap">
                <img :src="avatarUrl" alt="用户头像" class="loan-applicant__avatar">
              </div>
              <div class="loan-applicant__info">
                <div class="loan-applicant__name">{{ userInfo?.userName || '—' }}</div>
                <div class="loan-applicant__uid">用户ID: {{ userInfo?.id || '—' }}</div>
                <div class="loan-applicant__meta">手机: {{ userInfo?.phone || '—' }}</div>
                <div class="loan-applicant__meta">注册时间: {{ formatDate(userInfo?.createTime) || '—' }}</div>
                <div class="loan-applicant__credit">
                  信誉分:
                  <span :style="{ color: getCreditColor(userCertInfo?.creditScore) }">
                    {{ userCertInfo?.creditScore || '0' }}
                  </span>
                  <i class="loan-applicant__score-level">{{ getCreditLevelLabel(userCertInfo?.creditScore) }}</i>
                </div>
              </div>
            </div>
          </div>

          <!-- 右栏：上半部横排申请状态 + 下半部认证材料 -->
          <div class="loan-col loan-col--right">
            <!-- 上半部：横排申请状态 -->
            <div class="loan-col__title">申请状态概览</div>
            <div class="loan-stat-row-grid">
              <div class="loan-stat-cell">
                <div class="loan-stat-cell__icon is-pass"><span class="iconfont icon-user"></span></div>
                <div class="loan-stat-cell__value">{{ userStats.totalSubmitted }}</div>
                <div class="loan-stat-cell__label">用户提交申请</div>
              </div>
              <div class="loan-stat-cell">
                <div class="loan-stat-cell__icon is-pass"><span class="iconfont icon-complete"></span></div>
                <div class="loan-stat-cell__value">{{ userStats.passed }}</div>
                <div class="loan-stat-cell__label">已通过申请</div>
              </div>
              <div class="loan-stat-cell">
                <div class="loan-stat-cell__icon is-warn"><span class="iconfont icon-time"></span></div>
                <div class="loan-stat-cell__value">{{ userStats.postponeCount }}</div>
                <div class="loan-stat-cell__label">延期申请</div>
              </div>
              <div class="loan-stat-cell">
                <div class="loan-stat-cell__icon is-warn"><span class="iconfont icon-huankuan"></span></div>
                <div class="loan-stat-cell__value">{{ userStats.orderCount }}</div>
                <div class="loan-stat-cell__label">在贷订单</div>
              </div>
            </div>

            <!-- 分隔线 -->
            <div class="loan-col__divider" />

            <!-- 下半部：认证材料 -->
            <div class="loan-col__title">认证信息</div>
            <div class="loan-cert-grid">
              <div
                v-for="(label, key) in materialMap"
                :key="key"
                class="loan-cert-item"
                :class="{
                  'is-uploaded': isMaterialUploaded(key),
                  'is-locked': !isMaterialUploaded(key),
                  'is-expanded': showCertDetails[key]
                }"
                @click="handleCertTileClick(key)"
              >
                <div class="loan-cert-item__icon"><span class="iconfont" :class="certIcon(key)"></span></div>
                <div class="loan-cert-item__body">
                  <div class="loan-cert-item__label">{{ label }}</div>
                  <div
                    class="loan-cert-item__click"
                    :class="isMaterialUploaded(key) ? 'is-uploaded' : 'is-locked'"
                  >
                    {{ isMaterialUploaded(key) ? '已认证' : '未认证' }}
                  </div>
                </div>
                <!-- 右侧状态位：默认「查看详情」；点击身份证/银行卡后就地替换为号码；未认证显示 — -->
                <div
                  class="loan-cert-item__state"
                  :class="{
                    'is-locked': !isMaterialUploaded(key),
                    'is-number': showCertDetails[key] && (key === 'idCard' || key === 'bankCardId')
                  }"
                >
                  <template v-if="!isMaterialUploaded(key)">—</template>
                  <template v-else-if="showCertDetails[key] && (key === 'idCard' || key === 'bankCardId')">
                    {{ userCertInfo?.[key] }}
                  </template>
                  <template v-else>查看详情</template>
                </div>
              </div>
            </div>
          </div>
        </section>
    </div>

    <ImagePreview
      v-model:visible="showImagePreview"
      :images="previewImages"
      :title="previewTitle"
    />
  </div>
</component>
</template>

<script setup>
import { ref, watch, computed, Transition } from 'vue';
import { ElCollapseTransition } from 'element-plus';
import { applicationAPI, authAPI, userAPI } from '@/api';
import { useLoanStore } from '@/stores/loan';
import ImagePreview from '@/components/shared/ImagePreview.vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  applicationId: {
    type: [String, Number, null],
    required: true
  },
  modal: {
    type: Boolean,
    default: false
  },
  // 内嵌双栏模式：用横向滑入过渡替代默认的纵向折叠展开
  embedded: {
    type: Boolean,
    default: false
  },
  isPending: {
    type: Boolean,
    default: false
  },
  reviewType: {
    type: String,
    default: 'loan'
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'submit', 'transition-complete']);

// 产品列表全局缓存（应用生命周期内只拉一次），供延期分支把 productId 映射为产品名
const loanStore = useLoanStore();

const applicationDetail = ref(null);
const postponeDetail = ref(null);
const userDetail = ref(null);
const loading = ref(false);

const showImagePreview = ref(false)
const previewImages = ref([])
const previewTitle = ref('')
// 身份证/银行卡号码就地展开状态：点击后右侧「查看详情」替换为号码，再点收起
const showCertDetails = ref({})

const certTypeMap = {
  workCertId: 'workCert',
  triCertId: 'triCert',
  immovableCertId: 'immovableCert'
}

const materialMap = {
  idCard: '身份证',
  bankCardId: '银行卡',
  workCertId: '工作证明',
  triCertId: '第三方认证',
  immovableCertId: '不动产证明'
}

const userInfo = computed(() => {
  if (props.reviewType === 'postpone') {
    return userDetail.value?.user || userDetail.value
  }
  return userDetail.value?.user || applicationDetail.value?.data?.user
})

const userCertInfo = computed(() => {
  if (props.reviewType === 'postpone') {
    return userDetail.value?.userCert || null
  }
  return userDetail.value?.userCert || applicationDetail.value?.userCert
})

const loanApplication = computed(() => applicationDetail.value?.data?.application)

/* ========== postpone 分支：项目信息数据组装 ==========
   延期详情接口只返回 orderId，而用户详情接口（本分支必调）已内嵌完整 order[]，
   直接按 orderId 在前端匹配即可，无需再发订单请求
   （用户端 /api/orders/{id} 有订单归属校验，管理员 token 调用会 403）。 */
const orderInfo = computed(() => {
  const orderId = postponeDetail.value?.orderId
  if (orderId == null) return null
  const list = userDetail.value?.order || []
  return list.find(o => String(o.id) === String(orderId)) || null
})

// 产品名：用 loanStore 内的产品列表做 productId → productName 映射，取不到时回退显示 #id
const orderProductName = computed(() => {
  const productId = orderInfo.value?.productId
  if (productId == null) return '—'
  const product = loanStore.products.find(p => String(p.id) === String(productId))
  return product?.productName || ('#' + productId)
})

// 订单状态语义色：正常沿用品牌蓝 / 已逾期红 / 已完成绿
const ORDER_STATUS_CLASS_MAP = {
  '正常': 'is-status-normal',
  '已逾期': 'is-status-overdue',
  '已完成': 'is-status-done'
}
const orderStatusClass = computed(() => ORDER_STATUS_CLASS_MAP[orderInfo.value?.status] || '')

const avatarUrl = computed(() => {
  const avatar = userDetail.value?.user?.avatar || applicationDetail.value?.data?.user?.avatar
  if (!avatar) return '/1.jpg'

  let processedUrl = avatar.replace(/[\\/]/g, '/')

  if (processedUrl.startsWith('http')) {
    return processedUrl
  }

  if (processedUrl.startsWith('/uploads/')) {
    return processedUrl
  }

  if (processedUrl.startsWith('avatars/')) {
    return '/uploads/' + processedUrl
  }

  if (!processedUrl.startsWith('/')) {
    processedUrl = '/' + processedUrl
  }

  return processedUrl
})

const shouldShow = computed(() => props.modelValue && !!props.applicationId)

// 双栏内嵌用 Vue Transition 横向滑入；默认用 Element 折叠过渡
const detailTransition = computed(() => (props.embedded ? Transition : ElCollapseTransition))

watch(() => props.applicationId, async (newId) => {
  if (!newId) {
    applicationDetail.value = null
    postponeDetail.value = null
    return
  }
  await fetchDetail(newId)
}, { immediate: true })

watch(() => props.reviewType, () => {
  if (props.applicationId) {
    fetchDetail(props.applicationId)
  }
})

watch(() => props.modelValue, (newValue) => {
  if (!newValue) {
    applicationDetail.value = null
    postponeDetail.value = null
    userDetail.value = null
  }
})

const fetchDetail = async (id) => {
  loading.value = true
  try {
    if (props.reviewType === 'postpone') {
      await fetchPostponeDetail(id)
    } else {
      await fetchApplicationDetail(id)
    }
  } catch (error) {
    console.error('Failed to fetch detail:', error)
  } finally {
    loading.value = false
  }
}

const fetchApplicationDetail = async (id) => {
  try {
    const response = await applicationAPI.getApplicationDetail(id)
    applicationDetail.value = response

    const userId = applicationDetail.value?.data?.user?.id
    if (userId) {
      try {
        const userResponse = await userAPI.getUserDetail(userId)
        if (userResponse.code === 200) {
          userDetail.value = userResponse.data
        }
      } catch (userError) {
        console.error('获取用户详情失败:', userError)
      }
    }
  } catch (error) {
    console.error('Failed to fetch application detail:', error)
  }
}

const fetchPostponeDetail = async (id) => {
  try {
    const response = await applicationAPI.getPostponeDetail(id)
    if (response.code === 200) {
      postponeDetail.value = response.data

      // 项目信息的产品名依赖产品列表；store 已缓存则跳过，未缓存则异步拉取，
      // 不阻塞详情渲染——products 到达后 orderProductName 计算属性会自动更新
      if (!loanStore.hasProducts) {
        loanStore.fetchProducts()
      }

      const userId = response.data?.userId
      if (userId) {
        try {
          const userResponse = await userAPI.getUserDetail(userId)
          if (userResponse.code === 200) {
            userDetail.value = userResponse.data
          }
        } catch (userError) {
          console.error('获取用户详情失败:', userError)
        }
      }
    }
  } catch (error) {
    console.error('Failed to fetch postpone detail:', error)
  }
}

const getCreditColor = (credit) => {
  if (credit >= 600) {
    return '#25ce25'
  } else if (credit > 100 && credit < 600) {
    return 'brown'
  } else if (credit <= 100) {
    return 'red'
  }
  return 'default'
}

// 与点击/号码展示统一使用 userCertInfo（用户详情接口优先，申请详情接口兜底），
// 避免用户详情接口失败时出现「显示未上传但点击能查看材料」的矛盾
const isMaterialUploaded = (key) => {
  return userCertInfo.value?.[key] != null
}

const processImagePath = (url) => {
  let processed = url.replace(/[\\/]/g, '/')

  if (processed.startsWith('/uploads/')) {
    if (!processed.startsWith('/')) {
      processed = '/' + processed
    }
  } else {
    if (processed.includes('project/frontend/public/')) {
      processed = processed.replace('project/frontend/public/', '')
    }
    if (processed.includes('public/')) {
      processed = processed.replace('public/', '')
    }

    if (processed === 'thr_c.jpg') {
      processed = 'thir_c.jpg'
    }

    if (!processed.startsWith('/')) {
      processed = '/' + processed
    }
  }

  return processed
}

const fetchCertImage = async (certId, certType, materialKey) => {
  if (!certId) {
    ElMessage.warning('该材料未上传')
    return
  }

  try {
    let response
    const images = []

    switch (certType) {
      case 'workCert':
        response = await authAPI.getWorkCert(certId)
        if (response.code === 200 && response.data) {
          if (response.data.employmentCertPath) images.push(response.data.employmentCertPath)
          if (response.data.salaryCertPath) images.push(response.data.salaryCertPath)
        }
        break
      case 'triCert':
        response = await authAPI.getTriCert(certId)
        if (response.code === 200 && response.data) {
          if (response.data.socialSecurityPath) images.push(response.data.socialSecurityPath)
          if (response.data.creditReportPath) images.push(response.data.creditReportPath)
        }
        break
      case 'immovableCert':
        response = await authAPI.getImmovablesCert(certId)
        if (response.code === 200 && response.data) {
          if (response.data.propertyCertPath) images.push(response.data.propertyCertPath)
          if (response.data.carCertPath) images.push(response.data.carCertPath)
        }
        break
      default:
        ElMessage.warning('不支持的认证类型')
        return
    }

    if (images.length > 0) {
      previewImages.value = images.map(processImagePath)
      previewTitle.value = materialMap[materialKey] || '认证材料'
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

  if (key === 'idCard' || key === 'bankCardId') {
    return
  }

  const certType = certTypeMap[key]
  if (certType) {
    fetchCertImage(certId, certType, key)
  } else {
    ElMessage.warning('该材料类型不支持图片查看')
  }
}

const formatCurrency = (amount) => {
  if (amount == null) return '0'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  }).format(amount)
}

const formatRate = (rate) => {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(2)}%`
}

const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

// 状态元数据：统一供 Hero 状态 pill 和拒绝原因语义条使用，cls 直接映射 CSS 类
const STATUS_META_MAP = {
  '审核中':   { text: '待审核',        cls: 'is-pending',      actionLabel: '当前等待处理' },
  '已通过':   { text: '已通过',        cls: 'is-pass',         actionLabel: '审核通过' },
  'AI拒绝':   { text: 'AI拒绝·待复审', cls: 'is-ai-reject',    actionLabel: 'AI 初审拒绝，需人工复审' },
  '人工拒绝': { text: '人工拒绝',      cls: 'is-manual-reject',actionLabel: '人工审核未通过' },
  '已取消':   { text: '已取消',        cls: 'is-cancel',       actionLabel: '用户已撤回申请' }
}
const statusMeta = computed(() => {
  const raw = loanApplication.value?.status
  return STATUS_META_MAP[raw] || { text: raw || '—', cls: '', actionLabel: '状态未知' }
})

// 延期申请状态：后端为纯中文字符串 待审核 / 已通过 / 已拒绝
const POSTPONE_STATUS_META_MAP = {
  '待审核': { text: '待审核', cls: 'is-pending' },
  '已通过': { text: '已通过', cls: 'is-pass' },
  '已拒绝': { text: '已拒绝', cls: 'is-manual-reject' }
}

// 顶栏状态 pill 统一入口：postpone 走延期状态表，其余复用贷款状态表
const detailStatusMeta = computed(() => {
  if (props.reviewType === 'postpone') {
    const raw = postponeDetail.value?.status
    return POSTPONE_STATUS_META_MAP[raw] || { text: raw || '—', cls: '' }
  }
  return statusMeta.value
})

// 人工拒绝判定：后端 status === '人工拒绝'
const isManualReject = computed(() => loanApplication.value?.status === '人工拒绝')

// 拆分 rejectReason：后端格式为 "AI原因人工审核未通过: 人工原因"
// 分隔标记来自 ManualApproveServiceImpl 行 129-132 的拼接逻辑
const AI_REJECT_SEP = '人工审核未通过: '
const aiRejectReason = computed(() => {
  const reason = loanApplication.value?.rejectReason
  if (!reason || reason === '无') return ''
  const idx = reason.indexOf(AI_REJECT_SEP)
  return idx >= 0 ? reason.slice(0, idx).trim() : reason.trim()
})
const manualRejectReason = computed(() => {
  const reason = loanApplication.value?.rejectReason
  if (!reason || reason === '无') return ''
  const idx = reason.indexOf(AI_REJECT_SEP)
  return idx >= 0 ? reason.slice(idx + AI_REJECT_SEP.length).trim() : ''
})

// 申请人历史统计：从用户详情接口的 loanApplication[] / order[] 聚合
const userStats = computed(() => {
  const apps = userDetail.value?.loanApplication || []
  const orders = userDetail.value?.order || []
  return {
    totalSubmitted: apps.length,
    passed: apps.filter(a => a.status === '已通过' || a.status === 'AI通过' || a.status === '人工通过').length,
    postponeCount: 0, // 延期请求在 postpone_request 表，用户详情接口暂未返回；后续接延期列表接口补齐
    orderCount: orders.length
  }
})

// 认证材料 iconfont 类名（iconfont 线上项目 5038192，全局引入于 main.js）
const CERT_ICON_MAP = {
  idCard:         'icon-shenfenzheng',
  bankCardId:     'icon-yinxingqia',
  workCertId:     'icon-gongzuozhengming',
  triCertId:      'icon-disanfangpinggu',
  immovableCertId:'icon-budongchanrenzheng'
}
const certIcon = (key) => CERT_ICON_MAP[key] || 'icon-biaoji'

// 统一的认证 tile 点击处理：
// - 身份证/银行卡：右侧「查看详情」与号码就地切换（toggle），不新增 DOM 行，不影响布局
// - 未认证材料给出提示
// - 图片类材料调认证接口取图预览
const handleCertTileClick = (key) => {
  if (!isMaterialUploaded(key)) {
    ElMessage.warning('该材料未上传')
    return
  }
  if (key === 'idCard' || key === 'bankCardId') {
    showCertDetails.value[key] = !showCertDetails.value[key]
    return
  }
  const certId = userCertInfo.value?.[key]
  if (certId) {
    handleMaterialClick(key, certId)
  }
}

// 信誉分等级文案（与 getCreditColor 的阈值对齐：≥600 优秀 / 300-600 良好 / 100-300 一般 / ≤100 较差）
const getCreditLevelLabel = (score) => {
  const s = Number(score) || 0
  if (s >= 600) return '优秀'
  if (s >= 300) return '良好'
  if (s > 100)  return '一般'
  return '较差'
}

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

const handleSubmit = (approved) => {
  emit('submit', props.applicationId, approved)
}

const onTransitionComplete = () => {
  emit('transition-complete')
}
</script>

<style scoped>
@import '@/assets/css/application/applicationDetailModal.css';

/* ===== 内嵌 Push 模式：横向整页位移由外层 stage 负责，这里只做内容淡入淡出 ===== */
/* 时长/缓动引用 base.css 全局 token，与 PageStage 横移同时起止 */
.detail-slide-enter-active {
  transition: opacity var(--page-stage-enter) var(--page-stage-ease);
}
.detail-slide-leave-active {
  transition: opacity var(--page-stage-leave) var(--page-stage-ease);
}
.detail-slide-enter-from,
.detail-slide-leave-to {
  opacity: 0;
}
</style>
