<template>
  <div class="table-content">
    <div class="data-table">
      <table>
        <thead>
          <tr>
            <th>贷款人姓名</th>
            <th>贷款项目</th>
            <th>贷款金额</th>
            <th>贷款年限</th>
            <th>贷款期数</th>
            <th>申请时间</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="app in paginatedApplications" 
            :key="app.applicationId"
            @click="$emit('show-detail', app.applicationId)"
            style="cursor: pointer;"
          >
            <td>{{ app.userName || '—' }}</td>
            <td>{{ app.productName || '—' }}</td>
            <td>{{ formatCurrency(app.loanAmount) }}</td>
            <td>{{ app.loanPeriod || 0 }} 年</td>
            <td>{{ app.term || 0 }} 期</td>
            <td>{{ formatDate(app.applyTime) }}</td>
          </tr>
          <tr v-if="applications.length === 0">
            <td colspan="6" style="text-align: center;">暂无待审核申请</td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pagination 
      :current-page="currentPage"
      :total="applications.length"
      :page-size="pageSize"
      @page-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Pagination from '@/components/shared/BasePagination.vue'

const props = defineProps({
  applications: {
    type: Array,
    default: () => []
  }
})

const currentPage = ref(1)
const pageSize = 5

const paginatedApplications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return props.applications.slice(start, start + pageSize)
})

const handlePageChange = (page) => {
  currentPage.value = page
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