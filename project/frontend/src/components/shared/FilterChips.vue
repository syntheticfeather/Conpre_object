<template>
  <div class="filter-chips">
    <button
      v-for="chip in chips"
      :key="chip.key"
      type="button"
      class="filter-chip"
      :class="[{ 'is-active': modelValue === chip.key }]"
      @click="$emit('update:modelValue', chip.key)"
    >
      <span v-if="chip.color" class="filter-chip__dot" :style="{ background: chip.color }"></span>
      {{ chip.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  // [{ key, label, color?: string }]  color 可为任意 CSS 颜色值或 CSS 变量
  chips: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: String,
    default: 'all'
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;

  font-size: var(--text-sm);
  color: var(--text-secondary);

  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-full);
  cursor: pointer;

  transition: all var(--duration-fast) var(--ease-out-expo);
}

.filter-chip:hover {
  color: var(--brand-600);
  border-color: var(--brand-500);
  transform: translateY(-1px);
}

.filter-chip.is-active {
  color: #fff;
  background: var(--brand-500);
  border-color: var(--brand-500);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.35);
}

.filter-chip__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--border-color-base);
}

/* 激活态下圆点反白，保持对比度（!important 覆盖 inline style） */
.filter-chip.is-active .filter-chip__dot {
  background: rgba(255, 255, 255, 0.9) !important;
}
</style>
