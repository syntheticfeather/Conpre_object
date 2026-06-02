<template>
  <el-tooltip
    :content="content"
    :placement="placement"
    :disabled="!isOverflow"
    popper-class="content-tooltip-popper"
    :show-after="200"
    :hide-after="100"
    :offset="8"
  >
    <span ref="textRef" class="tooltip-trigger">
      <slot />
    </span>
  </el-tooltip>
</template>

<script setup>
import { ref, onMounted, onUpdated, watch, nextTick, onUnmounted } from 'vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  placement: {
    type: String,
    default: 'top'
  }
})

const textRef = ref(null)
const isOverflow = ref(false)

const checkOverflow = async () => {
  await nextTick()
  if (!textRef.value) return
  
  const el = textRef.value
  // 使用更严格的检测方式，处理表格单元格中的特殊情况
  const range = document.createRange()
  range.selectNodeContents(el)
  const contentWidth = range.getBoundingClientRect().width
  const containerWidth = el.clientWidth
  
  // 两种方式都检测，确保可靠性
  const scrollOverflow = el.scrollWidth > el.clientWidth + 1 // +1 避免浮点误差
  const rangeOverflow = contentWidth > containerWidth + 1
  
  isOverflow.value = scrollOverflow || rangeOverflow
}

onMounted(() => {
  checkOverflow()
  window.addEventListener('resize', checkOverflow)
})

onUpdated(() => {
  checkOverflow()
})

watch(() => props.content, () => {
  checkOverflow()
}, { immediate: true, deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', checkOverflow)
})
</script>

<style scoped>
.tooltip-trigger {
  display: inline-block;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>

<style>
.content-tooltip-popper {
  max-width: 420px !important;
  padding: 10px 14px !important;
  background: var(--color-white) !important;
  border: 1px solid var(--border-color-base) !important;
  border-left: 3px solid var(--color-primary) !important;
  border-radius: 12px !important;
  box-shadow: 0 6px 20px var(--tooltip-shadow) !important;
  font-size: 13px !important;
  line-height: 1.6 !important;
  color: var(--text-color) !important;
  word-break: break-word !important;
  white-space: pre-wrap !important;
}

.content-tooltip-popper .popper__arrow {
  display: none !important;
}

.content-tooltip-popper[data-popper-placement^='top'] {
  margin-bottom: 6px !important;
}
</style>
