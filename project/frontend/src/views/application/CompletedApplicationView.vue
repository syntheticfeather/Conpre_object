<template>
  <div class="apply-dashboard">
    <div class="header">
      <h2>已办审核</h2>
    </div>

    <component 
      :is="currentComponent" 
      :applications="currentApplications"
      @show-detail="showApplicationDetail"
      @refresh="loadApplications"
    />

    <ApplicationDetailModal 
      v-model="showDetailModal"
      :application-id="selectedApplicationId"
      @close="closeDetailModal"
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
const activeTab = ref('completed')
const showDetailModal = ref(false)
const selectedApplicationId = ref(null)

const currentComponent = computed(() => {
  return activeTab.value === 'pending' ? PendingApplications : CompletedApplications
})

const currentApplications = computed(() => {
  return activeTab.value === 'pending' 
    ? applicationStore.pendingApplications 
    : applicationStore.completedApplications
})

const loadApplications = async () => {
  if (activeTab.value === 'pending') {
    await applicationStore.fetchPendingApplications()
  } else {
    await applicationStore.fetchCompletedApplications()
  }
}

const showApplicationDetail = (applicationId) => {
  selectedApplicationId.value = applicationId
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedApplicationId.value = null
  applicationStore.clearCurrentApplication()
}

onMounted(() => {
  loadApplications()
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
</style>