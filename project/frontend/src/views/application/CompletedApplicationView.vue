<template>
  <div class="header">
    已办审核
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
          disabled
          style="cursor: not-allowed; opacity: 0.5;"
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
        :is="CompletedApplications"
        :applications="currentLoanApplications"
        @show-detail="showDetail"
        @refresh="loadLoanApplications"
      />
    </template>

    <template v-if="reviewMode === 'postpone'">
      <component
        :is="PostponeCompletedList"
        :postpone-requests="currentPostponeRequests"
        @show-detail="showDetail"
        @refresh="loadPostponeRequests"
      />
    </template>

    <ApplicationDetailModal
      v-model="showDetailModal"
      :application-id="selectedRequestId"
      :review-type="reviewMode"
      modal
      @close="closeDetailModal"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useApplicationStore } from '@/stores/application'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

const applicationStore = useApplicationStore()
const reviewMode = ref('loan')
const showDetailModal = ref(false)
const selectedRequestId = ref(null)
const loading = ref(false)

const currentLoanApplications = computed(() => applicationStore.completedApplications)
const currentPostponeRequests = computed(() => applicationStore.completedPostponeRequests)

const switchMode = (mode) => {
  reviewMode.value = mode
  loadData()
}

const loadLoanApplications = async () => {
  loading.value = true
  await applicationStore.fetchCompletedApplications()
  loading.value = false
}

const loadPostponeRequests = async () => {
  loading.value = true
  await applicationStore.fetchCompletedPostponeRequests()
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

onMounted(() => {
  loadData()
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
