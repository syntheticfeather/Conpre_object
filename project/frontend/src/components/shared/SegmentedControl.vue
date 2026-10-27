<template>
  <div
    class="segmented"
    role="radiogroup"
    :aria-label="label"
    :style="{ '--seg-count': options.length, '--seg-index': activeIndex }"
  >
    <span class="segmented__thumb" aria-hidden="true"></span>
    <button
      v-for="(option, index) in options"
      :key="option.key"
      :ref="(el) => setButtonRef(el, index)"
      type="button"
      role="radio"
      class="segmented__btn"
      :class="{ 'is-checked': option.key === modelValue }"
      :aria-checked="option.key === modelValue"
      :disabled="option.disabled"
      :tabindex="option.key === modelValue ? 0 : -1"
      @click="select(option)"
      @keydown.left.prevent="move(-1)"
      @keydown.right.prevent="move(1)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    required: true
  },
  // [{ key, label, disabled? }]
  options: {
    type: Array,
    required: true
  },
  label: {
    type: String,
    default: '选项切换'
  }
})

const emit = defineEmits(['update:modelValue'])

const buttonRefs = ref([])
const setButtonRef = (el, index) => {
  buttonRefs.value[index] = el
}

// 当前选中项的下标，驱动滑块位移
const activeIndex = computed(() => {
  const index = props.options.findIndex((option) => option.key === props.modelValue)
  return index < 0 ? 0 : index
})

const select = (option) => {
  // 点击已选中项不重复派发，避免上层重复请求
  if (option.disabled || option.key === props.modelValue) return
  emit('update:modelValue', option.key)
}

// ←/→ 在可用选项间漫游，并同步焦点（roving tabindex）
const move = (step) => {
  const total = props.options.length
  let index = activeIndex.value
  for (let i = 0; i < total; i++) {
    index = (index + step + total) % total
    const option = props.options[index]
    if (option.disabled) continue
    select(option)
    nextTick(() => buttonRefs.value[index]?.focus())
    return
  }
}
</script>

<style scoped>
.segmented {
  --seg-pad: 3px;

  position: relative;
  display: inline-grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  vertical-align: middle;

  padding: var(--seg-pad);
  background: var(--fill-color-light);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-full);
}

/* 滑块只负责"选中块外观"，尺寸由 --seg-count 均分，位移由 --seg-index 驱动 */
.segmented__thumb {
  position: absolute;
  top: var(--seg-pad);
  left: var(--seg-pad);
  width: calc((100% - var(--seg-pad) * 2) / var(--seg-count, 2));
  height: calc(100% - var(--seg-pad) * 2);

  background: var(--brand-600);
  border-radius: var(--radius-full);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.28);

  transform: translateX(calc(var(--seg-index, 0) * 100%));
  transition: transform var(--duration-normal) var(--ease-out-expo);
  pointer-events: none;
}

.segmented__btn {
  position: relative;
  z-index: 1;

  min-width: 96px;
  padding: 6px 16px;

  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);

  font: inherit;
  font-size: var(--text-sm);
  line-height: 1.4;

  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;

  transition:
    background-color var(--duration-fast) var(--ease-out-expo),
    color var(--duration-fast) var(--ease-out-expo);
}

/* 未选中项悬停：只给极淡品牌底，文字保持角色令牌，暗色下自动成立 */
.segmented__btn:hover:not(.is-checked):not(:disabled) {
  background: var(--brand-50);
  color: var(--text-color);
}

.segmented__btn:focus-visible {
  outline: 2px solid var(--brand-500);
  outline-offset: 2px;
}

.segmented__btn.is-checked {
  color: #fff;
  font-weight: 500;
}

.segmented__btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

@media (prefers-reduced-motion: reduce) {
  .segmented__thumb {
    transition-duration: 1ms;
  }
}
</style>
