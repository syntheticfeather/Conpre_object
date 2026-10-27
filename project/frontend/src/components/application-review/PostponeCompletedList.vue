<template>
  <div class="list-wrapper">
    <FilterChips v-model="activeChip" :chips="chips" />
    <BaseTable
      :data-source="paginatedPostponeRequests"
      :columns="columns"
      height="var(--table-fixed-height)"
      watermark
      :current-page="currentPage"
      :total="filteredPostponeRequests.length"
      :page-size="pageSize"
      :row-key="'id'"
      :show-row-selection="false"
      :show-batch-actions="false"
      :show-index="false"
      :show-action="false"
      :empty-title="activeChip === 'all' ? '暂无已完成的延期申请' : '没有符合筛选条件的申请'"
      :empty-description="activeChip === 'all' ? '完成审核的延期申请会归档在这里' : '试试切换其他筛选条件'"
      empty-cta="刷新数据"
      @empty-action="$emit('refresh')"
      @page-change="handlePageChange"
      @row-click="handleRowClick"
    >
    <template #createdAt="{ record }">
      {{ formatDate(record.createdAt) }}
    </template>
    <template #reviewedAt="{ record }">
      {{ formatDate(record.reviewedAt) }}
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
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import BaseTable from '@/components/shared/BaseTable.vue'
import FilterChips from '@/components/shared/FilterChips.vue'

const props = defineProps({
  postponeRequests: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-detail', 'refresh'])

const currentPage = ref(1)
const pageSize = 5

/* ===== chip 快捷筛选（延期已完成：全部/通过/人工拒绝/本周新增） ===== */
const activeChip = ref('all')
const chips = [
  { key: 'all', label: '全部', color: '#909399' },
  { key: 'success', label: '通过', color: 'var(--status-success)' },
  { key: 'danger', label: '人工拒绝', color: 'var(--status-danger)' },
  { key: 'week', label: '本周新增', color: 'var(--brand-500)' }
]

// 本周一 0 点
const weekStart = (() => {
  const d = new Date()
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  d.setHours(0, 0, 0, 0)
  return d.getTime()
})()

const filteredPostponeRequests = computed(() => {
  const list = props.postponeRequests
  switch (activeChip.value) {
    case 'success':
      return list.filter(r => r.status === '已通过')
    case 'danger':
      return list.filter(r => r.status === '已拒绝')
    case 'week':
      return list.filter(r => r.createdAt && new Date(r.createdAt).getTime() >= weekStart)
    default:
      return list
  }
})

// 切换筛选条件时回到第一页
watch(activeChip, () => {
  currentPage.value = 1
})

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
    title: '拒绝原因',
    dataIndex: 'rejectReason',
    key: 'rejectReason'
  },
  {
    title: '申请时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    slotName: 'createdAt'
  },
  {
    title: '审核时间',
    dataIndex: 'reviewedAt',
    key: 'reviewedAt',
    slotName: 'reviewedAt'
  }
]

const paginatedPostponeRequests = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredPostponeRequests.value.slice(start, start + pageSize)
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
  if (status === '已通过') {
    return {
      color: '#389e0d',
      background: '#f6ffed',
      border: '#b7eb8f'
    }
  }
  if (status === '已拒绝') {
    return {
      color: 'red',
      background: '#fff3f3',
      border: '#ffd6d6'
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
