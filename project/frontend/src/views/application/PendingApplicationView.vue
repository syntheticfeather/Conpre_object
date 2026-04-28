<template>
  <div class="header">
    待办审核
    <div class="header-right">
      <div class="toggle-group">
        <button
          :class="{ active: reviewMode === 'loan' }"
          @click="switchMode('loan')"
        >
          贷款申请
        </button>
        <button
          :class="{ active: reviewMode === 'postpone' }"
          @click="switchMode('postpone')"
        >
          延期申请
        </button>
      </div>
    </div>
  </div>

  <div class="apply-dashboard">
    <div v-if="loading" class="loading">
      加载中...
    </div>

    <template v-if="reviewMode === 'loan'">
      <component
        :is="currentLoanComponent"
        :applications="currentLoanApplications"
        @show-detail="showDetail"
        @refresh="loadLoanApplications"
      />
    </template>

    <template v-if="reviewMode === 'postpone'">
      <component
        :is="currentPostponeComponent"
        :postpone-requests="currentPostponeRequests"
        @show-detail="showDetail"
        @refresh="loadPostponeRequests"
      />
    </template>

    <ApplicationDetailModal
      v-model="showDetailModal"
      :application-id="selectedRequestId"
      :review-type="reviewMode"
      :is-pending="activeTab === 'pending'"
      modal
      @close="closeDetailModal"
      @submit="handleReviewSubmit"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useApplicationStore } from '@/stores/application'
import PendingApplications from '@/components/application-review/PendingApplications.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponePendingList from '@/components/application-review/PostponePendingList.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

const applicationStore = useApplicationStore()
const activeTab = ref('pending')
const reviewMode = ref('loan')
const showDetailModal = ref(false)
const selectedRequestId = ref(null)
const loading = ref(false)

const currentLoanComponent = computed(() => {
  return activeTab.value === 'pending' ? PendingApplications : CompletedApplications
})

const currentPostponeComponent = computed(() => {
  return activeTab.value === 'pending' ? PostponePendingList : PostponeCompletedList
})

const currentLoanApplications = computed(() => {
  return activeTab.value === 'pending'
    ? applicationStore.pendingApplications
    : applicationStore.completedApplications
})

const currentPostponeRequests = computed(() => {
  return activeTab.value === 'pending'
    ? applicationStore.pendingPostponeRequests
    : applicationStore.completedPostponeRequests
})

const switchMode = (mode) => {
  reviewMode.value = mode
  activeTab.value = 'pending'
  loadData()
}

const loadLoanApplications = async () => {
  loading.value = true
  if (activeTab.value === 'pending') {
    await applicationStore.fetchPendingApplications()
  } else {
    await applicationStore.fetchCompletedApplications()
  }
  loading.value = false
}

const loadPostponeRequests = async () => {
  loading.value = true
  if (activeTab.value === 'pending') {
    await applicationStore.fetchPendingPostponeRequests()
  } else {
    await applicationStore.fetchCompletedPostponeRequests()
  }
  loading.value = false
}

const loadData = async () => {
  if (reviewMode.value === 'loan') {
    await loadLoanApplications()
  } else {
    await loadPostponeRequests()
  }
}

const showDetail = (id) => {
  selectedRequestId.value = id
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedRequestId.value = null
}

const handleReviewSubmit = async (id, approved) => {
  if (reviewMode.value === 'postpone') {
    if (approved) {
      const result = await applicationStore.approvePostpone(id)
      if (result.success) {
        ElMessage.success('延期申请已通过')
        closeDetailModal()
      } else {
        ElMessage.error(result.message || '审核失败')
      }
    } else {
      try {
        const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝延期申请', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /.+/,
          inputErrorMessage: '拒绝原因不能为空'
        })
        const result = await applicationStore.rejectPostpone(id, { rejectReason: value })
        if (result.success) {
          ElMessage.success('已拒绝延期申请')
          closeDetailModal()
        } else {
          ElMessage.error(result.message || '审核失败')
        }
      } catch {
        // 用户取消
      }
    }
  } else {
    try {
      const reviewData = {
        loanApplicationId: parseInt(id),
        approved: approved ? 'true' : 'false'
      }
      const result = await applicationStore.submitReview(reviewData)
      if (result.success) {
        closeDetailModal()
        if (activeTab.value === 'pending') {
          await applicationStore.fetchPendingApplications()
        }
      } else {
        ElMessage.error('审核提交失败，请重试')
      }
    } catch (error) {
      console.error('审核提交出错:', error)
      ElMessage.error('审核提交出错，请重试')
    }
  }
}

onMounted(async () => {
  await loadData()
})
</script>

<style scoped>
.apply-dashboard {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin:20px 20px 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.toggle-group {
  display: flex;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.toggle-group button {
  padding: 6px 16px;
  border: none;
  background: #fff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.toggle-group button:not(:last-child) {
  border-right: 1px solid #dcdfe6;
}

.toggle-group button.active {
  background: #409eff;
  color: #fff;
}

.toggle-group button:hover:not(.active) {
  background: #ecf5ff;
  color: #409eff;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
</style>
