<template>
  <BaseTable
    :data-source="paginatedPostponeRequests"
    :columns="columns"
    :current-page="currentPage"
    :total="postponeRequests.length"
    :page-size="pageSize"
    :row-key="'id'"
    :show-row-selection="false"
    :show-batch-actions="false"
    :show-index="false"
    :show-action="false"
    @page-change="handlePageChange"
    @row-click="handleRowClick"
  >
    <template #createdAt="{ record }">
      {{ formatDate(record.createdAt) }}
    </template>
    <template #status="{ record }">
      <span
        class="status-tag"
        :style="{
          '--status-color': getStatusColor(record.status).color,
          '--status-bg': getStatusColor(record.status).background,
          '--status-border': getStatusColor(record.status).border
        }"
      >
        {{ record.status }}
      </span>
    </template>
  </BaseTable>
</template>

<script setup>
import { ref, computed } from 'vue'
import BaseTable from '@/components/shared/BaseTable.vue'

const props = defineProps({
  postponeRequests: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-detail'])

const currentPage = ref(1)
const pageSize = 5

const columns = [
  {
    title: '申请ID',
    dataIndex: 'id',
    key: 'id'
  },
  {
    title: '订单ID',
    dataIndex: 'orderId',
    key: 'orderId'
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    key: 'userId'
  },
  {
    title: '当前期数',
    dataIndex: 'currentTerm',
    key: 'currentTerm'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    slotName: 'status'
  },
  {
    title: '申请时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    slotName: 'createdAt'
  }
]

const paginatedPostponeRequests = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return props.postponeRequests.slice(start, start + pageSize)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleRowClick = (record) => {
  emit('show-detail', record.id)
}

const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusColor = (status) => {
  if (status === '待审核') {
    return {
      color: '#faad14',
      background: '#fffbe6',
      border: '#ffe58f'
    }
  }
  return {
    color: '#999',
    background: '#f5f5f5',
    border: '#d9d9d9'
  }
}
</script>

<style scoped>
.status-tag {
  padding: 2px 4px;
  background-color: var(--status-bg);
  border-radius: 4px;
  color: var(--status-color);
  border: 1px solid var(--status-border);
}
</style>
