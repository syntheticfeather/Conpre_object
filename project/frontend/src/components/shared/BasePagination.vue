<template>
  <div class="pagination">
    <button 
      :disabled="currentPage === 1"
      class="page-btn"
      @click="changePage(currentPage - 1)"
    >
      上一页
    </button>
    
    <span 
      v-for="page in visiblePages" 
      :key="page"
      :class="{ active: page === currentPage }"
      class="page-number"
      @click="changePage(page)"
    >
      {{ page }}
    </span>
    
    <button 
      :disabled="currentPage === totalPages"
      class="page-btn"
      @click="changePage(currentPage + 1)"
    >
      下一页
    </button>
    
    <span class="page-info">
      共 {{ total }} 条，{{ totalPages }} 页
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['page-change'])

const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

const visiblePages = computed(() => {
  const pages = []
  const maxVisible = 5
  let start = Math.max(1, props.currentPage - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages.value, start + maxVisible - 1)
  
  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  
  return pages
})

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    emit('page-change', page)
  }
}
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
}

.page-btn, .page-number {
  padding: 6px 12px;

  color: var(--pag-color);
  border: 1px solid var(--border-color-lighter);
  background: var(--pag-buttom-bg);
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s;
}

.page-btn:hover, .page-number:hover {
  background: var(--pag-hover-bg);
}

.page-number.active {
  background: var(--color-primary);
  color: var(--color-white);
  border-color: var(--color-primary);
}

.page-info {
  margin-left: 20px;
  color: var(--page-info-color);
  font-size: 14px;
}
</style>
