<template>
  <div class="page-stage" :class="{ 'stage--detail': showDetailModal }">
    <!-- 列表面板：页头 + 列表，整页左滑出场 -->
    <div class="stage-pane pane-list">
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
      </div>
    </div>

    <!-- 详情面板：从右侧整页滑入占满 -->
    <div class="stage-pane pane-detail">
      <ApplicationDetailModal
        v-model="showDetailModal"
        :application-id="selectedRequestId"
        :review-type="reviewMode"
        embedded
        @close="closeDetailModal"
      />
    </div>
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
  border: 1px solid var(--border-color-base);
  border-radius: 12px;
  overflow: hidden;
}

.toggle-group button {
  padding: 6px 16px;
  border: none;
  background: var(--color-white);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}


.toggle-group button.active {
  background: var(--color-primary);
  color: var(--color-white);
  border-radius: 12px;
}


.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-color-caption);
}
</style>
