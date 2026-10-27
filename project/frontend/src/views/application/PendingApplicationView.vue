<template>
  <div class="page-stage" :class="{ 'stage--detail': showDetailModal }">
    <!-- 列表面板：页头 + 欢迎条 + 列表，整页左滑出场 -->
    <div class="stage-pane pane-list">
      <div class="header">
        待办审核
        <div class="header-right">
          <SegmentedControl
            :model-value="reviewMode"
            :options="reviewOptions"
            label="审核类型"
            @update:model-value="switchMode"
          />
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
        <!-- 两份列表常驻同一网格单元，由 CSS 自动撑出行高；切换只做透明度/位移的合成层动画 -->
        <div class="list-stack">
          <div
            class="list-pane"
            :class="{ 'is-hidden': reviewMode !== 'loan' }"
            :inert="reviewMode !== 'loan' || null"
          >
            <component
              :is="currentLoanComponent"
              :applications="currentLoanApplications"
              @show-detail="showDetail"
              @refresh="loadLoanApplications"
            />
          </div>

          <div
            class="list-pane"
            :class="{ 'is-hidden': reviewMode !== 'postpone' }"
            :inert="reviewMode !== 'postpone' || null"
          >
            <component
              :is="currentPostponeComponent"
              :postpone-requests="currentPostponeRequests"
              @show-detail="showDetail"
              @refresh="loadPostponeRequests"
            />
          </div>
        </div>

        <div v-if="loading" class="list-loading-bar" aria-hidden="true"></div>
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
import SegmentedControl from '@/components/shared/SegmentedControl.vue'
import PendingApplications from '@/components/application-review/PendingApplications.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponePendingList from '@/components/application-review/PostponePendingList.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

const applicationStore = useApplicationStore()
const router = useRouter()
const activeTab = ref('pending')
const REVIEW_MODE_KEY = 'review-mode:pending'
const reviewMode = ref(
  localStorage.getItem(REVIEW_MODE_KEY) === 'postpone' ? 'postpone' : 'loan'
)
const reviewOptions = [
  { key: 'loan', label: '贷款申请' },
  { key: 'postpone', label: '延期申请' }
]
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
  if (reviewMode.value === mode) return
  reviewMode.value = mode
  localStorage.setItem(REVIEW_MODE_KEY, mode)
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
  position: relative;
  padding: 20px;
}

/* ===== 列表切换：同一格叠放 + 交叉淡化 + 同向位移 ===== */
.list-stack {
  display: grid;
}

.list-pane {
  grid-area: 1 / 1;

  opacity: 1;
  transform: translateX(0);
  transition:
    opacity var(--duration-fast) var(--ease-out-expo),
    transform var(--duration-fast) var(--ease-out-expo);
}

.list-pane.is-hidden {
  opacity: 0;
  pointer-events: none;
}

/* 左格退场向左、右格退场向右，方向与分段控件滑块一致 */
.list-pane:nth-child(1).is-hidden {
  transform: translateX(-12px);
}

.list-pane:nth-child(2).is-hidden {
  transform: translateX(12px);
}

/* 切换请求期间用顶部细进度条替代"加载中"占位，不改变文档流高度 */
.list-loading-bar {
  position: absolute;
  top: 6px;
  left: 20px;
  right: 20px;
  height: 3px;
  border-radius: var(--radius-full);
  overflow: hidden;
  pointer-events: none;
}

.list-loading-bar::after {
  content: '';
  display: block;
  width: 40%;
  height: 100%;
  background: var(--brand-500);
  border-radius: inherit;
  animation: list-loading-slide 900ms ease-in-out infinite;
}

@keyframes list-loading-slide {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(350%);
  }
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
  .pane-detail,
  .list-pane {
    transition-duration: 1ms !important;
  }

  .list-loading-bar::after {
    animation: none;
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin:0 20px;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>
