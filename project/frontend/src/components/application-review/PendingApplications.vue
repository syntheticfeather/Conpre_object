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
      :empty-title="activeChip === 'all' ? '暂无待审申请' : '没有符合筛选条件的申请'"
      :empty-description="activeChip === 'all' ? '用户提交贷款申请后将实时出现在这里' : '试试切换其他筛选条件'"
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
  </BaseTable>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import BaseTable from '@/components/shared/BaseTable.vue'
import FilterChips from '@/components/shared/FilterChips.vue'

const props = defineProps({
  applications: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-detail', 'refresh'])

const currentPage = ref(1)
const pageSize = 5

/* ===== chip 快捷筛选：基于后端实际返回字段做业务维度细分 =====
 */
const activeChip = ref('all')

// 业务阈值（占位）
const THRESHOLDS = {
  largeAmount: 100000, // 大额申请：≥10万
  longPeriod: 5,      // 长期申请：年限≥5
  longTerm: 36        // 多期申请：期数≥36
}

const chips = [
  { key: 'all', label: '全部', color: '#909399' },
  { key: 'large', label: '大额申请', color: 'var(--status-danger)' },
  { key: 'long', label: '长期申请', color: 'var(--status-warning)' },
  { key: 'week', label: '本周新增', color: 'var(--brand-500)' }
]

// 本周一 0 点（周一作为一周开始）
const weekStart = (() => {
  const d = new Date()
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  d.setHours(0, 0, 0, 0)
  return d.getTime()
})()

const filteredApplications = computed(() => {
  const list = props.applications // 已是待审核申请
  switch (activeChip.value) {
    case 'large':
      return list.filter(a => Number(a.loanAmount) >= THRESHOLDS.largeAmount)
    case 'long':
      return list.filter(a => Number(a.loanPeriod) >= THRESHOLDS.longPeriod)
    case 'week':
      return list.filter(a => a.applyTime && new Date(a.applyTime).getTime() >= weekStart)
    default:
      return list
  }
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
  }
]

const paginatedApplications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredApplications.value.slice(start, start + pageSize)
})

// 切换筛选条件时回到第一页
watch(activeChip, () => {
  currentPage.value = 1
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
</script>
