<template>
  <div class="page-stage" :class="{ 'stage--detail': showDetailModal }">
    <!-- 列表面板：页头 + 欢迎条 + 列表，整页左滑出场 -->
    <div class="stage-pane pane-list">
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

      <div class="welcome-section">
        <div class="welcome-info">
          <h1 class="welcome-title">欢迎回来，管理员</h1>
          <div class="date-info">
            <span>今日日期：{{ currentDate }}</span>
            <span class="divider">|</span>
            <span>数据实时更新</span>
          </div>
        </div>
        <div class="action-buttons">
          <el-button class="screen-btn" @click="goToDVScreen" type="primary" size="default">
            <el-icon><Monitor /></el-icon>
            数据大屏
          </el-button>
        </div>
      </div>

      <div class="apply-dashboard" ref="dashboardRef">
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
      </div>
    </div>

    <!-- 详情面板：从右侧整页滑入占满 -->
    <div class="stage-pane pane-detail">
      <ApplicationDetailModal
        ref="detailModalRef"
        v-model="showDetailModal"
        :application-id="selectedRequestId"
        :review-type="reviewMode"
        :is-pending="activeTab === 'pending'"
        embedded
        @close="closeDetailModal"
        @submit="handleReviewSubmit"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Monitor } from '@element-plus/icons-vue'
import { useApplicationStore } from '@/stores/application'
import PendingApplications from '@/components/application-review/PendingApplications.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponePendingList from '@/components/application-review/PostponePendingList.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

const applicationStore = useApplicationStore()
const router = useRouter()
const activeTab = ref('pending')
const reviewMode = ref('loan')
const showDetailModal = ref(false)
const selectedRequestId = ref(null)
const loading = ref(false)

const getCurrentDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  return `${year}年${month}月${day}日`
}
const currentDate = getCurrentDate()

const goToDVScreen = () => {
  router.push('/dv-screen')
}

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

const dashboardRef = ref(null)
const detailModalRef = ref(null)
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
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
    margin: 16px 20px 6px;
  padding: 20px;
  background: var(--app-welcome-gradient);
  border-radius: 12px;
  box-shadow: 0 4px 12px var(--app-welcome-shadow);
}

.welcome-info {
  display: flex;
  flex-direction: column;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--app-welcome-text);
  margin: 0 0 8px 0;
}

.date-info {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-info span {
  color: var(--app-welcome-text-secondary);
}

.divider {
  color: var(--app-welcome-text-tertiary);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.screen-btn {
  background: var(--app-screen-btn-gradient) !important;
  border: none !important;
  font-weight: 500;
  box-shadow: 0 2px 8px var(--app-screen-btn-shadow);
  transition: all 0.3s ease;
}

.screen-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px var(--app-screen-btn-shadow-hover);
}

.apply-dashboard {
  padding: 20px;
}

/* ===== Push 转场舞台：列表整页左滑消失、详情整页右滑占满 ===== */
/* 几何公理：列表静止位 X=0，详情静止位 X=+100%（屏外右侧），两页等宽 */
/* 节奏统一引用 base.css 全局 token：--page-stage-enter / -leave / -ease */
.page-stage {
  position: relative;
  overflow: hidden;
}

.stage-pane {
  width: 100%;
  will-change: transform;
}

/* 激活面板 relative 占流（决定舞台高度）且层级在上；非激活面板 absolute 叠放 */
.pane-list {
  position: relative;
  z-index: 2;
  transform: translateX(0);
  visibility: visible;
  transition: transform var(--page-stage-enter) var(--page-stage-ease),
              visibility 0s;
}

.pane-detail {
  position: absolute;
  inset: 0;
  z-index: 1;
  padding: 0 20px 20px;
  background: var(--html-bg, #f5f7fa);
  transform: translateX(100%);
  visibility: hidden;
  overflow-y: auto;
  transition: transform var(--page-stage-leave) var(--page-stage-ease),
              visibility 0s linear var(--page-stage-leave);
}

/* 打开详情：列表脱离文档流滑向屏外左侧，详情接管并从右侧滑入 */
.stage--detail .pane-list {
  position: absolute;
  inset: 0;
  z-index: 1;
  transform: translateX(-100%);
  visibility: hidden;
  pointer-events: none;
  transition: transform var(--page-stage-leave) var(--page-stage-ease),
              visibility 0s linear var(--page-stage-leave);
}

.stage--detail .pane-detail {
  position: relative;
  z-index: 2;
  transform: translateX(0);
  visibility: visible;
  transition: transform var(--page-stage-enter) var(--page-stage-ease),
              visibility 0s;
}

.stage--detail :deep(.inline-detail-panel) {
  margin-top: 0;
}

@media (prefers-reduced-motion: reduce) {
  .pane-list,
  .pane-detail {
    transition-duration: 1ms !important;
  }
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
  border-radius: 12px;
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

.toggle-group button.active {
  background: #409eff;
  color: #fff;
  border-radius: 12px;
}


.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
</style>
