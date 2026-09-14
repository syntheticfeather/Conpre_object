<template>
  <div class="list-wrapper">
    <FilterChips v-model="activeChip" :chips="chips" />
    <BaseTable
      :data-source="paginatedApplications"
      :columns="columns"
      :current-page="currentPage"
      :total="filteredApplications.length"
      :page-size="pageSize"
      :row-key="'applicationId'"
      :show-row-selection="false"
      :show-batch-actions="false"
      :show-index="false"
      :show-action="false"
      :empty-title="activeChip === 'all' ? '暂无已完成的申请' : '没有符合筛选条件的申请'"
      :empty-description="activeChip === 'all' ? '完成审核的申请会归档在这里' : '试试切换其他筛选条件'"
      empty-cta="刷新数据"
      @empty-action="$emit('refresh')"
      @page-change="handlePageChange"
      @row-click="handleRowClick"
    >
    <!-- 贷款金额格式化 -->
    <template #loanAmount="{ record }">
      {{ formatCurrency(record.loanAmount) }}
    </template>

    <!-- 贷款年限格式化 -->
    <template #loanPeriod="{ record }">
      {{ record.loanPeriod || 0 }} 年
    </template>

    <!-- 贷款期数格式化 -->
    <template #term="{ record }">
      {{ record.term || 0 }} 期
    </template>

    <!-- 申请时间格式化 -->
    <template #applyTime="{ record }">
      {{ formatDate(record.applyTime) }}
    </template>

    <!-- 审核状态格式化 -->
    <template #status="{ record }" >
      <span
      class="status-tag"
      :style="{
        '--status-color': getStatusColor(record.status).color,
      '--status-bg': getStatusColor(record.status).background,
      '--status-border': getStatusColor(record.status).border
      }">
        {{ formatStatus(record.status) }}
      </span>
    </template>
  </BaseTable>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import BaseTable from '@/components/shared/BaseTable.vue'
import FilterChips from '@/components/shared/FilterChips.vue'
// import { color } from 'echarts'

const props = defineProps({
  applications: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-detail', 'refresh'])

const currentPage = ref(1)
const pageSize = 5

/* ===== chip 快捷筛选 ===== */
const activeChip = ref('all')
const chips = [
  { key: 'all', label: '全部', color: '#909399' },
  { key: 'success', label: '通过', color: 'var(--status-success)' },
  { key: 'danger', label: '人工拒绝', color: 'var(--status-danger)' },
  { key: 'week', label: '本周新增', color: 'var(--brand-500)' },
  { key: 'priority', label: '高优先级', color: '#8B5CF6' }
]

// 本周一 0 点（周一作为一周开始）
const weekStart = (() => {
  const d = new Date()
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  d.setHours(0, 0, 0, 0)
  return d.getTime()
})()

const filteredApplications = computed(() => {
  const list = props.applications
  switch (activeChip.value) {
    case 'success':
      return list.filter(a => a.status === 'APPROVED' || a.status === '已通过')
    case 'warning':
      return list.filter(a => a.status === 'PENDING' || a.status === '待审核')
    case 'danger':
      return list.filter(a => a.status === 'MANUAL_REJECTED' || a.status === '人工拒绝' || String(a.status || '').includes('REJECTED'))
    case 'week':
      return list.filter(a => a.applyTime && new Date(a.applyTime).getTime() >= weekStart)
    case 'priority': // 高优先级：大额申请（≥10万）
      return list.filter(a => Number(a.loanAmount) >= 100000)
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
    title: '贷款人姓名',
    dataIndex: 'userName',
    key: 'userName'
  },
  {
    title: '贷款项目',
    dataIndex: 'productName',
    key: 'productName'
  },
  {
    title: '贷款金额',
    dataIndex: 'loanAmount',
    key: 'loanAmount',
    slotName: 'loanAmount'
  },
  {
    title: '贷款年限',
    dataIndex: 'loanPeriod',
    key: 'loanPeriod',
    slotName: 'loanPeriod'
  },
  {
    title: '贷款期数',
    dataIndex: 'term',
    key: 'term',
    slotName: 'term'
  },
  {
    title: '申请时间',
    dataIndex: 'applyTime',
    key: 'applyTime',
    slotName: 'applyTime'
  },
  {
    title: '审核状态',
    dataIndex: 'status',
    key: 'status',
    slotName: 'status'
  }
]

const paginatedApplications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredApplications.value.slice(start, start + pageSize)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleRowClick = (record) => {
  emit('show-detail', record.applicationId)
}

const formatCurrency = (amount) => {
  if (!amount) return '—'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  }).format(amount)
}

const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleString('zh-CN')
}

const formatStatus = (status) => {
  if (!status) return '—'
  const statusMap = {
    'AI_REJECTED': 'AI拒绝',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'MANUAL_REJECTED': '人工拒绝',
    '已通过': '已通过',
    '人工拒绝': '人工拒绝'
  }
  return statusMap[status] || status
}

const getStatusColor = (status) => {
  const colorMap_pass = {
    color: '#389e0d',
    background: '#f6ffed',
    border: '#b7eb8f'
  }
  const colorMap_reject = {
    color: 'red',
    background: '#fff3f3',
    border: '#ffd6d6'
  }

  if(status === 'AI通过' || status === '人工通过') {
    return colorMap_pass
  }
  else if(status === '人工拒绝') {
    return colorMap_reject
  }
  else {
    return {
      color: '#999',
      background: '#f5f5f5',
      border: '#d9d9d9'
    }
  }
}

</script>

<style scoped>
  .status-tag {
    padding: 2px 4px;

    background-color: var(--status-bg);

    border-radius: 4px;
    color: var(--status-color);
    border:1px solid var(--status-border);
  }
</style>
