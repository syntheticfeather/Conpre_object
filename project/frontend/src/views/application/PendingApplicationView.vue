[file name]: PendingApplicationView.vue
[file content begin]
<template>
  <div class="apply-dashboard">
    <div class="header">
      <h2>待办审核</h2>
    </div>

    <div v-if="loading" class="loading">
      加载中...
    </div>

    <component 
      :is="currentComponent"
      v-else 
      :applications="currentApplications"
      @show-detail="showApplicationDetail"
      @refresh="loadApplications"
    />

    <ApplicationDetailModal 
      v-model="showDetailModal"
      :application-id="selectedApplicationId"
      :is-pending="activeTab === 'pending'"
      modal
      @close="closeDetailModal"
      @submit="handleReviewSubmit"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useApplicationStore } from '@/stores/application'
import PendingApplications from '@/components/application-review/PendingApplications.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

const applicationStore = useApplicationStore()
const activeTab = ref('pending')
const showDetailModal = ref(false)
const selectedApplicationId = ref(null)
const loading = ref(false)

const currentComponent = computed(() => {
  return activeTab.value === 'pending' ? PendingApplications : CompletedApplications
})

const currentApplications = computed(() => {
  return activeTab.value === 'pending' 
    ? applicationStore.pendingApplications 
    : applicationStore.completedApplications
})

const loadApplications = async () => {
  loading.value = true
  if (activeTab.value === 'pending') {
    await applicationStore.fetchPendingApplications()
  } else {
    await applicationStore.fetchCompletedApplications()
  }
  loading.value = false
}

const showApplicationDetail = (applicationId) => {
  selectedApplicationId.value = applicationId
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedApplicationId.value = null
}

const handleReviewSubmit = async (applicationId, approved) => {
  try {
    const reviewData = {
      loanApplicationId: parseInt(applicationId),
      approved: approved ? "true" : "false"
    }
    
    const result = await applicationStore.submitReview(reviewData)
    
    if (result.success) {
      // 关闭详情弹窗
      closeDetailModal()
      
      // 如果当前在待办审核页面，刷新列表
      if (activeTab.value === 'pending') {
        await applicationStore.fetchPendingApplications()
      }
    } else {
      alert('审核提交失败，请重试')
    }
  } catch (error) {
    console.error('审核提交出错:', error)
    alert('审核提交出错，请重试')
  }
}

onMounted(async () => {
  await loadApplications()
})
</script>

<style scoped>
.apply-dashboard {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tabs button {
  padding: 8px 16px;
  margin-left: 10px;
  background: #f5f7fa;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.tabs button:hover {
  background: #e6f7ff;
}

.tabs button.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
</style>
