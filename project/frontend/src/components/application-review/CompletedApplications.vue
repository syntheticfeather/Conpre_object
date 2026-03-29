<template>
  <BaseTable
    :data-source="paginatedApplications"
    :columns="columns"
    :current-page="currentPage"
    :total="applications.length"
    :page-size="pageSize"
    :row-key="'applicationId'"
    :show-row-selection="false"
    :show-batch-actions="false"
    :show-index="false"
    :show-action="false"
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
      <span :style="{ color: getStatusColor(record.status) }">
        {{ formatStatus(record.status) }}
      </span>
    </template>
  </BaseTable>
</template>

<script setup>
import { ref, computed } from 'vue'
import BaseTable from '@/components/shared/BaseTable.vue'

const props = defineProps({
  applications: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-detail'])

const currentPage = ref(1)
const pageSize = 5

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
  return props.applications.slice(start, start + pageSize)
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
  const colorMap = {
    '已通过': '#25ce25',
    '人工拒绝': 'red',
  }
  console.log(colorMap[status])
  
  return colorMap[status] || 'default'
}
</script>
