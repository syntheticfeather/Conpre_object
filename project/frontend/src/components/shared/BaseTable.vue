<template>
  <div class="base-table">
    <!-- 表格 -->
    <table class="data-table">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key">
            {{ column.label }}
          </th>
          <th v-if="actions.length > 0">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr 
          v-for="(item, index) in paginatedData" 
          :key="item[primaryKey]"
          :class="{ 'selected-row': selectedId === item[primaryKey] }"
          @click="handleRowClick(item)"
        >
          <td v-for="column in columns" :key="column.key">
            {{ column.formatter ? column.formatter(item[column.key], item, index) : item[column.key] }}
          </td>
          <td v-if="actions.length > 0">
            <button 
              v-for="action in actions" 
              :key="action.key"
              :class="action.className"
              @click.stop="action.handler(item)"
            >
              {{ typeof action.label === 'function' ? action.label(item) : action.label }}
            </button>
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length + (actions.length > 0 ? 1 : 0)" style="text-align: center;">
            {{ emptyText }}
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 分页 -->
    <BasePagination 
      :current-page="currentPage"
      :total="data.length"
      :page-size="pageSize"
      @page-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import BasePagination from './BasePagination.vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  actions: {
    type: Array,
    default: () => []
  },
  primaryKey: {
    type: String,
    default: 'id'
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  pageSize: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits(['row-click', 'selection-change'])

// 分页控制
const currentPage = ref(1)
const selectedId = ref(null)

// 分页数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  return props.data.slice(start, start + props.pageSize)
})

// 处理分页变化
const handlePageChange = (page) => {
  currentPage.value = page
}

// 处理行点击
const handleRowClick = (item) => {
  const itemId = item[props.primaryKey]
  if (selectedId.value === itemId) {
    selectedId.value = null
  } else {
    selectedId.value = itemId
  }
  emit('row-click', item)
  emit('selection-change', selectedId.value)
}

// 暴露方法和属性
const resetSelection = () => {
  selectedId.value = null
}

const setCurrentPage = (page) => {
  currentPage.value = page
}

defineExpose({
  resetSelection,
  setCurrentPage,
  currentPage
})
</script>

<style scoped>
.base-table {
  width: 100%;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
}

.data-table td {
  padding: 12px;
  border-bottom: 1px solid #dee2e6;
}

/* 行点击效果 */
.data-table tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.data-table tbody tr:hover {
  background-color: #f5f5f5;
}

/* 选中行样式 */
.selected-row {
  background-color: #f0f9ff;
  border-left: 3px solid #409eff;
}

.selected-row:hover {
  background-color: #e6f7ff;
}

/* 操作按钮通用样式 */
.data-table tbody button {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}
</style>