<template>
  <div class="user-table-wrapper">
    <BaseTable
      ref="tableRef"
      :data-source="paginatedUsers"
      :columns="columns"
      :current-page="currentPage"
      :total="userStore.users.length"
      :page-size="pageSize"
      :row-key="'userId'"
      :show-row-selection="false"
      :show-batch-actions="false"
      :show-index="true"
      :show-action="true"
      :row-clickable="true"
      :selected-row-class="selectedUserId"
      @page-change="handlePageChange"
      @row-click="selectUser"
      @table-change="handleTableChange"
    >
      <!-- 信誉分列 -->
      <template #creditScore="{ record }">
        <span :style="{ color: getCreditScoreColor(record.creditScore) }">
          {{ record.creditScore || '—' }}
        </span>
      </template>
      
      <!-- 自定义信誉分列头 -->
      <template #headerCell="{ column }">
        <template v-if="column.key === 'creditScore'">
          <div @click="handleCreditScoreSort" style="cursor: pointer; display: flex; align-items: center; justify-content: center;">
            <span>信誉分</span>
            <span v-if="sortInfo.field === 'creditScore'" style="margin-left: 4px;">
              {{ sortInfo.order === 'ascend' ? '↑' : '↓' }}
            </span>
          </div>
        </template>
      </template>

      <!-- 总贷款金额列 -->
      <template #totalLoanAmount="{ record }">
        ¥{{ formatAmount(record.totalLoanAmount) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ record }">
        <a-button
          class="blacklist-btn"
          :disabled="isUserInBlacklist(record.userId)"
          @click.stop="addToBlacklist(record)"
        >
          {{ isUserInBlacklist(record.userId) ? '已加入黑名单' : '加入黑名单' }}
        </a-button>
      </template>
    </BaseTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseTable from '@/components/shared/BaseTable.vue'

const emit = defineEmits(['user-selected'])

const userStore = useUserStore()
const tableRef = ref(null)

const currentPage = ref(1)
const pageSize = 5
const selectedUserId = ref(null)
const sortInfo = ref({ field: null, order: null })

const columns = [
  {
    title: 'ID',
    dataIndex: 'userId',
    key: 'userId',
    width: 60
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName'
  },
  {
    title: '信誉分',
    dataIndex: 'creditScore',
    key: 'creditScore',
    slotName: 'creditScore'
  },
  {
    title: '目前借贷情况',
    dataIndex: 'loanStatus',
    key: 'loanStatus'
  },
  {
    title: '总交易笔数',
    dataIndex: 'totalTransactionCount',
    key: 'totalTransactionCount'
  },
  {
    title: '总贷款金额',
    dataIndex: 'totalLoanAmount',
    key: 'totalLoanAmount',
    slotName: 'totalLoanAmount'
  }
]

const paginatedUsers = computed(() => {
  const users = [...userStore.users]
  
  if (sortInfo.value.field && sortInfo.value.order) {
    const order = sortInfo.value.order === 'ascend' ? 1 : -1
    users.sort((a, b) => {
      const aVal = a[sortInfo.value.field] || 0
      const bVal = b[sortInfo.value.field] || 0
      return (aVal - bVal) * order
    })
  }
  
  const start = (currentPage.value - 1) * pageSize
  return users.slice(start, start + pageSize)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleTableChange = ({ sorter }) => {
  if (sorter && sorter.field) {
    sortInfo.value = {
      field: sorter.field,
      order: sorter.order
    }
  } else {
    sortInfo.value = { field: null, order: null }
  }
}

// 处理信誉分列头点击排序
const handleCreditScoreSort = () => {
  if (sortInfo.value.field === 'creditScore') {
    // 切换排序方向
    sortInfo.value = {
      field: 'creditScore',
      order: sortInfo.value.order === 'ascend' ? 'descend' : 'ascend'
    }
  } else {
    // 首次点击，默认降序
    sortInfo.value = {
      field: 'creditScore',
      order: 'descend'
    }
  }
}

const isUserInBlacklist = (userId) => {
  return userStore.blacklist.some(item => item.userId === userId)
}

onMounted(async () => {
  await userStore.fetchUserStats()
  await userStore.fetchBlacklist()
})

/**
 * 处理用户点击事件，切换选中状态并获取用户详情
 * @param {Object} record - 点击的用户记录
 */
const selectUser = async (record) => {
  if (selectedUserId.value === record.userId) {
    selectedUserId.value = null
  } else {
    selectedUserId.value = record.userId
    
    try {
      await userStore.fetchUserDetail(record.userId)
      emit('user-selected', record.userId)
    } catch (error) {
      console.error('UserTable: 获取用户详情失败:', error)
      ElMessage.error('获取用户详情失败')
      selectedUserId.value = null
    }
  }
}

/**
 * 处理将用户加入黑名单的操作
 * @param {Object} user - 要加入黑名单的用户记录
 */
const addToBlacklist = async (user) => {
  try {
    const { value: blackLevel } = await ElMessageBox.prompt(
      `请输入用户【${user.userName}】的黑名单等级：`,
      '加入黑名单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d+$/,
        inputErrorMessage: '请输入有效数字'
      }
    )

    await ElMessageBox.confirm(
      `确定将用户【${user.userName}】（ID: ${user.userId}）加入黑名单？等级：${blackLevel}`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await userStore.addToBlacklist(user.userId, blackLevel)
    ElMessage.success('已成功加入黑名单')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败：' + (error.message || '未知错误'))
    }
  }
}

/**
 * 格式化金额，保留两位小数
 * @param {number} amount - 要格式化的金额
 * @returns {string} - 格式化后的金额字符串
 */
const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 根据信誉分获取信誉分颜色
 * @param {number} score - 信誉分
 * @returns {string} - 信誉分颜色
 */
const getCreditScoreColor = (score) => {
  if (!score) return '#666'
  if (score >= 700) return '#52c41a'
  if (score >= 600) return '#1890ff'
  if (score >= 500) return '#faad14'
  return '#ff4d4f'
}
</script>

<style scoped>
.user-table-wrapper {
  width: 100%;
}

.blacklist-btn {
  background-color: #ec6062;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 2px 5px;
}

.blacklist-btn:hover {
  background-color: #c64441;
}

.blacklist-btn:disabled {
  background-color: #d9d9d9;
  color: rgba(0, 0, 0, 0.25);
}


</style>
