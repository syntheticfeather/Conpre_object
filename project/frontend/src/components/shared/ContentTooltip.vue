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
import { ref, onMounted, onUpdated, watch } from 'vue'

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

const checkOverflow = () => {
  if (!textRef.value) return
  const el = textRef.value
  isOverflow.value = el.scrollWidth > el.clientWidth
}

onMounted(() => {
  checkOverflow()
})

onUpdated(() => {
  checkOverflow()
})

watch(() => props.content, () => {
  checkOverflow()
})
</script>

<style scoped>
.tooltip-trigger {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
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
