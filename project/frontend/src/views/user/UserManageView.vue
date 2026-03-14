<template>
  <div class="user-manage-view">
    <div class="header">
      <h2 class="title">用户管理</h2>
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

const selectedUserId = ref(null)
const showUserDetail = ref(false)

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
</script>

<style scoped>
.user-manage-view {
  padding: 20px;
}

.title {
  font-size: 24px;
  color: #333;
}
</style>