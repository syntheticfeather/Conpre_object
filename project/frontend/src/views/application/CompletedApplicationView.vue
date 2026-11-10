<template>
  <PageStage :active="showDetailModal">
    <!-- 列表面板：页头 + 列表，整页左滑出场 -->
    <template #list>
      <div class="header">
        已办审核
        <div class="header-right">
          <SegmentedControl
            :model-value="reviewMode"
            :options="reviewOptions"
            label="审核类型"
            @update:model-value="switchMode"
          />
        </div>
      </div>

      <div class="apply-dashboard">
        <!-- 两份列表常驻同一网格单元，由 CSS 自动撑出行高；切换只做透明度/位移的合成层动画 -->
        <div class="list-stack">
          <div
            class="list-pane"
            :class="{ 'is-hidden': reviewMode !== 'loan' }"
            :inert="reviewMode !== 'loan' || null"
          >
            <component
              :is="CompletedApplications"
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
              :is="PostponeCompletedList"
              :postpone-requests="currentPostponeRequests"
              @show-detail="showDetail"
              @refresh="loadPostponeRequests"
            />
          </div>
        </div>

        <div v-if="loading" class="list-loading-bar" aria-hidden="true"></div>
      </div>
    </template>

    <!-- 详情面板：从右侧整页滑入占满 -->
    <template #detail>
      <div class="application-detail-inner">
        <ApplicationDetailModal
          v-model="showDetailModal"
          :application-id="selectedRequestId"
          :review-type="reviewMode"
          embedded
          @close="closeDetailModal"
        />
      </div>
    </template>
  </PageStage>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useApplicationStore } from '@/stores/application'
import SegmentedControl from '@/components/shared/SegmentedControl.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'
import PageStage from '@/components/shared/PageStage.vue'

const applicationStore = useApplicationStore()
const reviewMode = ref('loan')
const reviewOptions = [
  { key: 'loan', label: '贷款申请' },
  { key: 'postpone', label: '延期申请', disabled: true }
]
const showDetailModal = ref(false)
const selectedRequestId = ref(null)
const loading = ref(false)

const currentLoanApplications = computed(() => applicationStore.completedApplications)
const currentPostponeRequests = computed(() => applicationStore.completedPostponeRequests)

const switchMode = (mode) => {
  if (reviewMode.value === mode) return
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
  position: relative;
  padding: 20px;
}

/* ===== 列表切换：同一格叠放 + 交叉淡化 + 同向位移（方案 1+2） ===== */
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

/* ===== Push 转场舞台：结构与转场节奏均由 PageStage 组件统一提供 ===== */
/* 详情面板内层：不设背景色 */
.application-detail-inner {
  padding: 0 20px 20px;
}

.application-detail-inner :deep(.inline-detail-panel) {
  margin-top: 0;
}

@media (prefers-reduced-motion: reduce) {
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
  margin:0px 20px;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>
