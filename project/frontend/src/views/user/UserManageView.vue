<template>
  <div class="user-manage-view">
    <div class="header">
      <h2 class="title">用户管理</h2>
      
      <!-- 搜索框 根据信誉分范围搜索用户并从高到底排序展示 -->
      <div class="search-section">
        <el-input
          v-model="searchText"
          placeholder="请输入信誉分范围，例如<100"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch" id="btn-search">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <!-- 用户列表 -->
    <UserTable 
      @user-selected="handleUserSelected"
    />
    
    <!-- 用户详情模态框 -->
    <div class="user-detail-section">
      <UserDetailPanel 
        v-if="showUserDetail"
        :user-id="selectedUserId"
        :is-visible="showUserDetail"
        @close="closeUserDetail"
      />  
      </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import UserTable from '@/components/user/UserTable.vue'
import UserDetailPanel from '@/components/user/UserDetailPanel.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const selectedUserId = ref(null)
const showUserDetail = ref(false)
const searchText = ref('')

// 处理用户选择事件
const handleUserSelected = (userId) => {
  console.log('UserManageView: 接收到用户选择事件，userId:', userId)
  selectedUserId.value = userId
  showUserDetail.value = true
}

// 关闭用户详情
const closeUserDetail = () => {
  console.log('UserManageView: 关闭用户详情')
  showUserDetail.value = false
  selectedUserId.value = null
}

// 处理搜索
const handleSearch = async () => {
  if (!searchText.value) {
    ElMessage.warning('请输入搜索条件')
    return
  }
  
  try {
    await userStore.searchUsersByCredit(searchText.value)
    ElMessage.success('搜索成功')
  } catch (error) {
    ElMessage.error('搜索失败：' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.user-manage-view {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 20px;
}

.title {
  margin: 0;
  
  width: auto;
  color: #4A5A6B;

  font-size: 25px;
  font-weight: 600;
  font-family: 方正小标宋，楷体，微软雅黑;
}

.search-section {
  margin-right: 20px;

  width: 300px;
}

#btn-search {
  background-color: #1890ff;
  color: #fff;
}
</style>