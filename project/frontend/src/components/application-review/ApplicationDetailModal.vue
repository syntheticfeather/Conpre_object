<template>
  <!-- 如果是弹窗模式，则包裹一层遮罩和定位 -->
  <div v-if="shouldShow" class="inline-detail-panel">
    <div class="detail-header">
      <h3>申请详情</h3>
      <button class="close-btn" @click="handleClose">&times;</button>
    </div>

    <!-- 用户基本信息 -->
    <div class="user-info">
      <div class="left">
        <h3 class="detail-subtitle">基本信息</h3>
        <div id="user-info-section1">
          <div style="display: flex; align-items: flex-start; margin-bottom: 16px;">
            <img :src="applicationDetail?.data?.user?.avatar" alt="用户头像" class="avatar" style="margin-right: 16px;">
            <div style="display: flex; flex-direction: column;">
              <p style="margin: 0 0 4px 0;"><span>ID: {{ applicationDetail?.data?.user?.id || '—' }}</span></p>
              <p style="margin: 0;"><span>{{ applicationDetail?.data?.user?.userName || '—' }}</span></p>
            </div>
          </div>
          <p><strong>手机号：</strong><span>{{ applicationDetail?.data?.user?.phone || '—' }}</span></p>
          <p><strong>注册时间：</strong><span>{{ formatDate(applicationDetail?.data?.user?.createTime) || '—' }}</span></p>
          <p><strong>信誉分：</strong><span :style="{ color: getCreditColor(applicationDetail?.userCert?.creditScore) }">
            {{ applicationDetail?.userCert?.creditScore || '0' }}
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
            @click="handleMaterialClick(key, applicationDetail?.userCert?.[key])"
          >
            <span>{{ label }}：</span>
            <span :style="{ color: isMaterialUploaded(key) ? '#27ae60' : '#e74c3c' }">
              {{ isMaterialUploaded(key) ? '已上传 (点击查看)' : '未上传' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 贷款申请信息 -->
    <div class="section">
      <h4>贷款申请信息</h4>
      <div class="detail-grid">
        <div class="detail-item">
          <span>贷款项目：</span>
          <span>{{ applicationDetail?.data?.application?.productName || '—' }}</span>
        </div>
        <div class="detail-item">
          <span>申请金额：</span>
          <span>{{ formatCurrency(applicationDetail?.data?.application?.loanAmount) || '—' }}</span>
        </div>
        <div class="detail-item">
          <span>总期数：</span>
          <span>{{ applicationDetail?.data?.application?.term || '—' }} 期</span>
        </div>
        <div class="detail-item">
          <span>贷款年限：</span>
          <span>{{ applicationDetail?.data?.application?.loanPeriod || '—' }} 年</span>
        </div>
        <div class="detail-item">
          <span>年利率：</span>
          <span>{{ formatRate(applicationDetail?.data?.application?.interestRate) || '—' }}</span>
        </div>
        <div class="detail-item">
          <span>还款方式：</span>
          <span>{{ applicationDetail?.data?.application?.repaidType || '—' }}</span>
        </div>
      </div>

      <!-- 拒绝理由 -->
      <div v-if="applicationDetail?.data?.application?.rejectReason" class="rejectReasons">
        <span>拒绝原因：</span>
        <span>{{ applicationDetail.data.application.rejectReason }}</span>
      </div>
    </div>

    <!-- 审核操作按钮 (仅在待办审核状态下显示) -->
    <div v-if="isPending" class="action-buttons">
      <button class="btn-pass" @click="handleSubmit(true)">通过</button>
      <button class="btn-reject" @click="handleSubmit(false)">不通过</button>
    </div>
    <div v-else class="status-display">
      <span>状态: {{ formatStatus(applicationDetail?.data?.application?.status) || '—' }}</span>
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
import { ref, watch, computed } from 'vue';
import { applicationAPI, authAPI } from '@/api';
import ImagePreview from '@/components/shared/ImagePreview.vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  // 控制显示/隐藏 (用于 v-model)
  modelValue: {
    type: Boolean,
    default: false
  },
  // 申请 ID
  applicationId: {
    type: [String, Number, null],
    required: true
  },
  // 是否为弹窗模式
  modal: {
    type: Boolean,
    default: false
  },
  // 是否为待办审核状态
  isPending: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'submit']);

// 内部状态
const applicationDetail = ref(null);
const loading = ref(false);

const showImagePreview = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')

const certTypeMap = {
  workCertId: 'workCert',
  triCertId: 'triCert',
  immovableCertId: 'immovableCert'
}

// 材料映射
const materialMap = {
  idCard: '身份证',
  bankCardId: '银行卡',
  workCertId: '工作证明',
  triCertId: '第三方认证',
  immovableCertId: '不动产证明'
};

// 计算属性：是否应该显示
const shouldShow = computed(() => props.modelValue && !!props.applicationId);

// 监听 applicationId 变化，获取详情
watch(() => props.applicationId, async (newId) => {
  if (!newId) {
    applicationDetail.value = null;
    return;
  }
  await fetchApplicationDetail(newId);
}, { immediate: true });

// 监听 modelValue 变化，用于外部控制
watch(() => props.modelValue, (newValue) => {
  if (!newValue) {
    // 关闭时清空数据
    applicationDetail.value = null;
  }
});

// 获取申请详情
const fetchApplicationDetail = async (id) => {
  loading.value = true
  try {
    const response = await applicationAPI.getApplicationDetail(id)
    // 直接使用响应，因为response已经是response.data
    applicationDetail.value = response
  } catch (error) {
    console.error('Failed to fetch application detail:', error)
    // 错误处理逻辑
  } finally {
    loading.value = false
  }
}

// 根式化信誉分颜色
const getCreditColor = (credit) => {
  if (credit >= 600) {
    return '#25ce25'
  } else if (credit > 100 && credit < 600) {
    return 'brown'
  }else if (credit <= 100) {
    return 'red'
  }
  return 'default'
}

// 检查材料是否已上传
const isMaterialUploaded = (key) => {
  return applicationDetail.value?.userCert?.[key] != null
}

const fetchCertImage = async (certId, certType) => {
  if (!certId) {
    ElMessage.warning('该材料未上传')
    return
  }

  try {
    let response
    switch (certType) {
      case 'workCert':
        response = await authAPI.getWorkCert(certId)
        break
      case 'triCert':
        response = await authAPI.getTriCert(certId)
        break
      case 'immovableCert':
        response = await authAPI.getImmovablesCert(certId)
        break
      default:
        ElMessage.warning('不支持的认证类型')
        return
    }

    if (response.code === 200 && response.data) {
      const imageUrl = response.data.fileUrl || response.data.imageUrl
      if (imageUrl) {
        previewImageUrl.value = imageUrl
        previewTitle.value = materialMap[certType] || '认证材料'
        showImagePreview.value = true
      } else {
        ElMessage.warning('未找到图片')
      }
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

  const certType = certTypeMap[key]
  if (certType) {
    fetchCertImage(certId, certType)
  } else {
    ElMessage.warning('该材料类型不支持图片查看')
  }
}

// 格式化货币
const formatCurrency = (amount) => {
  if (amount == null) return '0'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  }).format(amount)
}

// 格式化利率 (假设后端返回的是小数，如 0.05)
const formatRate = (rate) => {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(2)}%`
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

// 格式化状态
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

// 处理关闭
const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

// 处理审核提交
const handleSubmit = (approved) => {
  emit('submit', props.applicationId, approved)
  // 父组件处理 submit 事件后应负责关闭此面板
}
</script>

<style scoped>
@import '@/assets/css/applicationDetailModal.css'
</style>