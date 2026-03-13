<template>
  <div class="user-table-container">
    <!-- 用户表格 -->
    <div class="user-content table-content">
      <table class="user-table data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>序号</th>
            <th>用户名</th>
            <th>信誉分</th>
            <th>目前借贷情况</th>
            <th>总交易笔数</th>
            <th>总贷款金额</th>
            <th>快捷操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(user, index) in paginatedUsers"
            :key="user.userId"
            :class="{ 'selected-row': selectedUserId === user.userId }"
            @click="selectUser(user)"
          >
            <td>{{ user.userId }}</td>
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td>{{ user.userName || '—' }}</td>
            <td>{{ user.creditScore || '—' }}</td>
            <td>{{ user.loanStatus || '—' }}</td>
            <td>{{ user.totalTransactionCount || 0 }}</td>
            <td>¥{{ formatAmount(user.totalLoanAmount) }}</td>
            <td>
              <button
                class="black-btn"
                @click.stop="addToBlacklist(user)"
                :disabled="isUserInBlacklist(user.userId)"
              >
                {{ isUserInBlacklist(user.userId) ? '已加入黑名单' : '加入黑名单' }}
              </button>
            </td>
          </tr>
          <tr v-if="userStore.users.length === 0">
            <td colspan="8" style="text-align: center;">暂无用户</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <BasePagination
        :current-page="currentPage"
        :total="userStore.users.length"
        :page-size="pageSize"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, defineEmits } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import BasePagination from '@/components/shared/BasePagination.vue'

// 定义 emits
const emit = defineEmits(['user-selected'])

const userStore = useUserStore()

// 分页控制（由组件管理）
const currentPage = ref(1)
const pageSize = 5
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return userStore.users.slice(start, start + pageSize)
})

// 分页变化处理
const handlePageChange = (page) => {
  currentPage.value = page
}

// 检查用户是否在黑名单中
const isUserInBlacklist = (userId) => {
  return userStore.blacklist.some(item => item.userId === userId)
}

// 选中用户（仅用于高亮显示）
const selectedUserId = ref(null)

// 生命周期：加载用户列表和黑名单列表
onMounted(async () => {
  await userStore.fetchUserStats()
  await userStore.fetchBlacklist()
})

// 选择用户（用于显示详情）
const selectUser = async (user) => {
  console.log('UserTable: 点击用户:', user.userId, user.userName)
  
  if (selectedUserId.value === user.userId) {
    // 如果点击已选中的用户，清除选中状态
    selectedUserId.value = null
  } else {
    selectedUserId.value = user.userId
    
    try {
      // 先获取用户详情数据
      console.log('UserTable: 开始获取用户详情...')
      await userStore.fetchUserDetail(user.userId)
      console.log('UserTable: 获取用户详情成功，store.userDetail:', userStore.userDetail)
      
      // 发射事件到父组件，传递用户ID
      emit('user-selected', user.userId)
    } catch (error) {
      console.error('UserTable: 获取用户详情失败:', error)
      ElMessage.error('获取用户详情失败')
      selectedUserId.value = null
    }
  }
}

// 加入黑名单
const addToBlacklist = async (user) => {
  try {
    // 使用 Element Plus 弹出输入框
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

    // 确认操作
    await ElMessageBox.confirm(
      `确定将用户【${user.userName}】（ID: ${user.userId}）加入黑名单？等级：${blackLevel}`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用 store 方法
    await userStore.addToBlacklist(user.userId, blackLevel)
    ElMessage.success('已成功加入黑名单')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败：' + (error.message || '未知错误'))
    }
  }
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount && amount !== 0) return '0'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}
</script>

<style scoped>
.user-table-container {
  width: 100%;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

thead th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  font-weight: 600;
}

tbody td {
  padding: 12px;
  border-bottom: 1px solid #dee2e6;
}

/* 行点击效果 */
.user-table tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-table tbody tr:hover {
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

/* 按钮样式 */
.black-btn {
  padding: 4px 8px;
  margin: 0 2px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.black-btn:hover {
  background-color: #dd4b4b;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
}

.page-btn {
  padding: 6px 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>