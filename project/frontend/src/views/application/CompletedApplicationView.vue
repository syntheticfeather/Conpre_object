<template>
  <div class="page-stage" :class="{ 'stage--detail': showDetailModal }">
    <!-- 列表面板：页头 + 列表，整页左滑出场 -->
    <div class="stage-pane pane-list">
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
import SegmentedControl from '@/components/shared/SegmentedControl.vue'
import CompletedApplications from '@/components/application-review/CompletedApplications.vue'
import PostponeCompletedList from '@/components/application-review/PostponeCompletedList.vue'
import ApplicationDetailModal from '@/components/application-review/ApplicationDetailModal.vue'

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
  margin:0px 20px;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>
