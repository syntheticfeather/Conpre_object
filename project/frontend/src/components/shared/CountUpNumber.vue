<template>
  <span class="count-up-number">{{ display }}</span>
</template>

<script setup>
/**
 * 数字滚动组件：首屏 0 → 目标值 CountUp
 * 用法：<CountUpNumber :target="1234" group suffix="%" :decimals="1" />
 */
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  target: { type: Number, default: 0 },
  duration: { type: Number, default: 1200 }, // 动画时长 ms
  decimals: { type: Number, default: 0 },    // 保留小数位
  prefix: { type: String, default: '' },     // 前缀，如 ¥
  suffix: { type: String, default: '' },     // 后缀，如 % / 万
  group: { type: Boolean, default: false },  // 是否启用千位分隔符
  toWan: { type: Boolean, default: false },  // 按"万"缩写显示（target 为元）
  fallback: { type: String, default: '0' }   // target 为 0 时的兜底文案
})

const display = ref(props.prefix + '0' + props.suffix)
let rafId = null

const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3)

const formatValue = (value) => {
  if (props.toWan) {
    if (!value || value <= 0) return props.fallback
    return props.prefix + (value / 10000).toFixed(props.decimals || 2) + (props.suffix || ' 万')
  }
  let text = value.toFixed(props.decimals)
  if (props.group) {
    const [intPart, decimalPart] = text.split('.')
    text = Number(intPart).toLocaleString('zh-CN') + (decimalPart ? '.' + decimalPart : '')
  }
  return props.prefix + text + props.suffix
}

const runAnimation = () => {
  cancelAnimationFrame(rafId)
  const endValue = Number(props.target) || 0
  if (endValue <= 0) {
    display.value = props.toWan ? props.fallback : formatValue(0)
    return
  }
  const start = performance.now()
  const tick = (now) => {
    const progress = Math.min((now - start) / props.duration, 1)
    display.value = formatValue(endValue * easeOutCubic(progress))
    if (progress < 1) {
      rafId = requestAnimationFrame(tick)
    } else {
      display.value = formatValue(endValue)
    }
  }
  rafId = requestAnimationFrame(tick)
}

onMounted(runAnimation)
watch(() => props.target, runAnimation)
onBeforeUnmount(() => cancelAnimationFrame(rafId))
</script>
