<template>
  <div class="blacklist-table-container">
    <!-- 黑名单表格 -->
    <div class="blacklist-content table-content">
      <table class="blacklist-table data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>序号</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>黑名单等级</th>
            <th>加入时间</th>
            <th>更新时间</th>
            <th>解除时间</th>
            <th>快捷操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(user, index) in paginatedBlacklist"
            :key="user.userId"
            :class="{ 'selected-row': selectedUserId === user.userId }"
            @click="selectUser(user)"
          >
            <td>{{ user.userId }}</td>
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td>{{ user.userName || '—' }}</td>
            <td>{{ user.phone || '—' }}</td>
            <td>{{ user.blackLevel || '—' }}</td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td>{{ formatDate(user.updateTime) }}</td>
            <td>{{ formatDate(user.removeTime) }}</td>
            <td>
              <button
                class="remove-btn"
                @click.stop="removeFromBlacklist(user)"
                :disabled="user.removeTime !== null"
              >
                {{ user.removeTime !== null ? '已解除' : '解除黑名单' }}
              </button>
            </td>
          </tr>
          <tr v-if="userStore.blacklist.length === 0">
            <td colspan="9" style="text-align: center;">暂无黑名单用户</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <BasePagination
        :current-page="currentPage"
        :total="userStore.blacklist.length"
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
const paginatedBlacklist = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return userStore.blacklist.slice(start, start + pageSize)
})

// 分页变化处理
const handlePageChange = (page) => {
  currentPage.value = page
}

// 选中用户（仅用于高亮显示）
const selectedUserId = ref(null)

// 生命周期：加载黑名单列表
onMounted(async () => {
  await userStore.fetchBlacklist()
})

// 选择用户（用于显示详情）
const selectUser = async (user) => {
  console.log('BlacklistTable: 点击用户:', user.userId, user.userName)
  
  if (selectedUserId.value === user.userId) {
    // 如果点击已选中的用户，清除选中状态
    selectedUserId.value = null
  } else {
    selectedUserId.value = user.userId
    
    try {
      // 先获取用户详情数据
      console.log('BlacklistTable: 开始获取用户详情...')
      await userStore.fetchBlacklistUserDetail(user.userId)
      console.log('BlacklistTable: 获取用户详情成功，store.blacklistUserDetail:', userStore.blacklistUserDetail)
      
      // 发射事件到父组件，传递用户ID
      emit('user-selected', user.userId)
    } catch (error) {
      console.error('BlacklistTable: 获取用户详情失败:', error)
      ElMessage.error('获取用户详情失败')
      selectedUserId.value = null
    }
  }
}

// 从黑名单移除用户
const removeFromBlacklist = async (user) => {
  try {
    // 使用 Element Plus 确认框
    await ElMessageBox.confirm(
      `确定解除用户【${user.userName}】的黑名单吗？`,
      '解除黑名单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用 store 方法
    await userStore.removeFromBlacklist(user.userId)
    ElMessage.success('已成功解除黑名单')
    
    // 刷新列表
    await userStore.fetchBlacklist()
    
    // 如果当前选中了这个用户，清除选中状态
    if (selectedUserId.value === user.userId) {
      selectedUserId.value = null
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败：' + (error.message || '未知错误'))
    }
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
}
</script>

<style scoped>
.blacklist-table-container {
  width: 100%;
}

.blacklist-table {
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
.blacklist-table tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.blacklist-table tbody tr:hover {
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
.remove-btn {
  padding: 4px 8px;
  margin: 0 2px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.remove-btn:hover {
  background-color: #218838;
}

.remove-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
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