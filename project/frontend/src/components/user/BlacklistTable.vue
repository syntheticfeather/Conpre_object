<template>
  <div class="blacklist-table-wrapper">
    <BaseTable
      ref="tableRef"
      :data-source="paginatedBlacklist"
      :columns="columns"
      :current-page="currentPage"
      :total="userStore.blacklist.length"
      :page-size="pageSize"
      :row-key="'userId'"
      :show-row-selection="false"
      :show-batch-actions="false"
      :show-index="true"
      :show-action="true"
      :row-clickable="true"
      @page-change="handlePageChange"
      @row-click="selectUser"
    >
      <!-- 黑名单等级列 -->
      <template #blackLevel="{ record }">
        <span>
          {{ record.blackLevel || '—' }}
        </span>
      </template>

      <!-- 加入时间列 -->
      <template #createTime="{ record }">
        {{ formatDate(record.createTime) }}
      </template>

      <!-- 更新时间列 -->
      <template #updateTime="{ record }">
        {{ formatDate(record.updateTime) }}
      </template>

      <!-- 解除时间列 -->
      <template #removeTime="{ record }">
        <span :style="{ color: record.removeTime ? '#52c41a' : '#999' }">
          {{ formatDate(record.removeTime) }}
        </span>
      </template>

      <!-- 操作列 -->
      <template #action="{ record }">
        <a-button
          class="remove-blacklist-btn"
          :disabled="record.removeTime !== null"
          @click.stop="removeFromBlacklist(record)"
        >
          {{ record.removeTime !== null ? '已解除' : '解除黑名单' }}
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
    title: '手机号',
    dataIndex: 'phone',
    key: 'phone'
  },
  {
    title: '黑名单等级',
    dataIndex: 'blackLevel',
    key: 'blackLevel',
    slotName: 'blackLevel'
  },
  {
    title: '加入时间',
    dataIndex: 'createTime',
    key: 'createTime',
    slotName: 'createTime'
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    slotName: 'updateTime'
  },
  {
    title: '解除时间',
    dataIndex: 'removeTime',
    key: 'removeTime',
    slotName: 'removeTime'
  }
]

const paginatedBlacklist = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return userStore.blacklist.slice(start, start + pageSize)
})

const handlePageChange = (page) => {
  currentPage.value = page
}

onMounted(async () => {
  await userStore.fetchBlacklist()
})

const selectUser = async (record) => {
  if (selectedUserId.value === record.userId) {
    selectedUserId.value = null
  } else {
    selectedUserId.value = record.userId
    
    try {
      await userStore.fetchBlacklistUserDetail(record.userId)
      emit('user-selected', record.userId)
    } catch (error) {
      console.error('BlacklistTable: 获取用户详情失败:', error)
      ElMessage.error('获取用户详情失败')
      selectedUserId.value = null
    }
  }
}

const removeFromBlacklist = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定解除用户【${user.userName}】的黑名单吗？`,
      '解除黑名单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await userStore.removeFromBlacklist(user.userId)
    ElMessage.success('已成功解除黑名单')
    
    await userStore.fetchBlacklist()
    
    if (selectedUserId.value === user.userId) {
      selectedUserId.value = null
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败：' + (error.message || '未知错误'))
    }
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
}

</script>

<style scoped>
.blacklist-table-wrapper {
  width: 100%;
}

.remove-blacklist-btn {
  padding: 0px 12px;

  background-color: #52c41a;

  font-size: 14px;
  color: white;
  border: none;
  border-radius: 12px;
}

.remove-blacklist-btn:hover {
  background-color: #73d13d;
}

.remove-blacklist-btn:disabled {
  background-color: #d9d9d9;
  color: rgba(0, 0, 0, 0.25);
}
</style>
