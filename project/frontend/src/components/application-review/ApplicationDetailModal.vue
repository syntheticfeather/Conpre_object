<template>
  <el-collapse-transition>
  <div v-show="shouldShow" class="inline-detail-panel">
    <div class="detail-header">
      <h3>{{ reviewType === 'postpone' ? '延期申请详情' : '申请详情' }}</h3>
      <button class="close-btn" @click="handleClose">&times;</button>
    </div>

    <div v-loading="loading">
      <!-- 用户基本信息 -->
      <div class="user-info">
        <div class="left">
          <h3 class="detail-subtitle">基本信息</h3>
          <div id="user-info-section1">
            <div style="display: flex; align-items: flex-start; margin-bottom: 16px;">
              <img :src="avatarUrl" alt="用户头像" class="avatar" style="margin-right: 16px;">
                <div style="display: flex; flex-direction: column;">
                  <p style="margin: 0 0 4px 0;"><span>ID: {{ userInfo?.id || '—' }}</span></p>
                  <p style="margin: 0;"><span>{{ userInfo?.userName || '—' }}</span></p>
                </div>
            </div>
            <p><strong>手机号：</strong><span>{{ userInfo?.phone || '—' }}</span></p>
            <p><strong>注册时间：</strong><span>{{ formatDate(userInfo?.createTime) || '—' }}</span></p>
            <p><strong>信誉分：</strong><span :style="{ color: getCreditColor(userCertInfo?.creditScore) }">
              {{ userCertInfo?.creditScore || '0' }}
            </span></p>
          </div>
        </div>
        <div class="right">
          <h3 class="detail-subtitle">认证材料</h3>
          <div id="user-auth-section">
            <div
              v-for="(label, key) in materialMap"
              :key="key"
              class="material-item clickable"
              :class="{ 'has-image': isMaterialUploaded(key) }"
              @click="key === 'idCard' || key === 'bankCardId' ? (showCertDetails[key] = !showCertDetails[key]) : handleMaterialClick(key, userCertInfo?.[key])"
            >
              <span>{{ label }}：</span>
              <span :style="{ color: isMaterialUploaded(key) ? '#27ae60' : '#e74c3c' }">
                <template v-if="isMaterialUploaded(key)">
                  <template v-if="(key === 'idCard' || key === 'bankCardId') && showCertDetails[key]">
                    {{ userCertInfo?.[key] }}
                  </template>
                  <template v-else>
                    {{ key === 'idCard' || key === 'bankCardId' ? '已上传 (点击查看)' : '已上传 (点击查看)' }}
                  </template>
                </template>
                <template v-else>
                  未上传
                </template>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 申请信息 -->
      <template v-if="reviewType === 'postpone'">
        <div class="section">
          <h4>延期申请信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span>申请ID：</span>
              <span>{{ postponeDetail?.id || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>订单ID：</span>
              <span>{{ postponeDetail?.orderId || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>用户ID：</span>
              <span>{{ postponeDetail?.userId || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>当前期数：</span>
              <span>{{ postponeDetail?.currentTerm || '—' }} 期</span>
            </div>
            <div class="detail-item">
              <span>状态：</span>
              <span>{{ postponeDetail?.status || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>申请时间：</span>
              <span>{{ formatDate(postponeDetail?.createdAt) || '—' }}</span>
            </div>
            <div v-if="postponeDetail?.reviewedAt" class="detail-item">
              <span>审核时间：</span>
              <span>{{ formatDate(postponeDetail?.reviewedAt) || '—' }}</span>
            </div>
          </div>
          <div v-if="postponeDetail?.rejectReason" class="rejectReasons">
            <span>拒绝原因：</span>
            <span>{{ postponeDetail?.rejectReason || '—' }}</span>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="section">
          <h4>贷款申请信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span>贷款项目：</span>
              <span>{{ loanApplication?.productName || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>申请金额：</span>
              <span>{{ formatCurrency(loanApplication?.loanAmount) || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>总期数：</span>
              <span>{{ loanApplication?.term || '—' }} 期</span>
            </div>
            <div class="detail-item">
              <span>贷款年限：</span>
              <span>{{ loanApplication?.loanPeriod || '—' }} 年</span>
            </div>
            <div class="detail-item">
              <span>年利率：</span>
              <span>{{ formatRate(loanApplication?.interestRate) || '—' }}</span>
            </div>
            <div class="detail-item">
              <span>还款方式：</span>
              <span>{{ loanApplication?.repaidType || '—' }}</span>
            </div>
          </div>
          <div v-if="loanApplication?.rejectReason" class="rejectReasons">
            <span>拒绝原因：</span>
            <span>{{ loanApplication?.rejectReason || '—' }}</span>
          </div>
        </div>
      </template>

      <!-- 审核操作按钮 -->
      <div v-if="isPending" class="action-buttons">
        <button class="btn-pass" @click="handleSubmit(true)">通过</button>
        <button class="btn-reject" @click="handleSubmit(false)">不通过</button>
      </div>
      <div v-else class="status-display">
        <span>状态: {{ statusText || '—' }}</span>
      </div>
    </div>

    <ImagePreview
      v-model:visible="showImagePreview"
      :image-url="previewImageUrl"
      :title="previewTitle"
    />
  </div>
</el-collapse-transition>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { applicationAPI, authAPI, userAPI } from '@/api';
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
  isPending: {
    type: Boolean,
    default: false
  },
  reviewType: {
    type: String,
    default: 'loan'
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'submit']);

const applicationDetail = ref(null);
const postponeDetail = ref(null);
const userDetail = ref(null);
const loading = ref(false);

const showImagePreview = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')
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

const statusText = computed(() => {
  if (props.reviewType === 'postpone') {
    return postponeDetail.value?.status || '—'
  }
  return formatStatus(loanApplication.value?.status)
})

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

const isMaterialUploaded = (key) => {
  return userDetail.value?.userCert?.[key] != null
}

const fetchCertImage = async (certId, certType, materialKey) => {
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

      if (imageUrl.startsWith('/uploads/')) {
        if (!imageUrl.startsWith('/')) {
          imageUrl = '/' + imageUrl
        }
      } else {
        if (imageUrl.includes('project/frontend/public/')) {
          imageUrl = imageUrl.replace('project/frontend/public/', '')
        }
        if (imageUrl.includes('public/')) {
          imageUrl = imageUrl.replace('public/', '')
        }

        if (imageUrl === 'thr_c.jpg') {
          imageUrl = 'thir_c.jpg'
        }

        if (!imageUrl.startsWith('/')) {
          imageUrl = '/' + imageUrl
        }
      }

      previewImageUrl.value = imageUrl
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

const formatStatus = (status) => {
  if (!status) return '—'
  const statusMap = {
    'AI_REJECTED': 'AI拒绝',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'MANUAL_REJECTED': '人工拒绝'
  }
  return statusMap[status] || status
}

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

const handleSubmit = (approved) => {
  emit('submit', props.applicationId, approved)
}
</script>

<style scoped>
@import '@/assets/css/applicationDetailModal.css'
</style>
