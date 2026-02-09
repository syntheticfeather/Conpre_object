<template>
  <div class="black-user-manage-view">
    <div class="header">
      <h2 class="title">黑名单管理</h2>
    </div>
    
    <!-- 主内容区域 -->
    <div class="content-container">
      <!-- 黑名单列表区域 -->
      <div class="blacklist-table-section">
        <BlacklistTable 
          @user-selected="handleUserSelected"
        />
      </div>
      
      <!-- 用户详情区域 -->
      <div v-if="showUserDetail" class="user-detail-section">
        <UserDetailPanel 
          :user-id="selectedUserId"
          :is-visible="showUserDetail"
          @close="closeUserDetail"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BlacklistTable from '@/components/user/BlacklistTable.vue'
import UserDetailPanel from '@/components/user/UserDetailPanel.vue'

const selectedUserId = ref(null)
const showUserDetail = ref(false)

// 处理用户选择事件
const handleUserSelected = (userId) => {
  console.log('BlackUserManageView: 接收到用户选择事件，userId:', userId)
  selectedUserId.value = userId
  showUserDetail.value = true
}

// 关闭用户详情
const closeUserDetail = () => {
  console.log('BlackUserManageView: 关闭用户详情')
  showUserDetail.value = false
  selectedUserId.value = null
}
</script>

<style scoped>
.black-user-manage-view {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  margin-bottom: 20px;
}

.title {
  font-size: 24px;
  color: #333;
}

/* 内容容器 */
.content-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

/* 黑名单表格区域 */
.blacklist-table-section {
  flex-shrink: 0;
}

/* 用户详情区域 */
.user-detail-section {
  flex: 1;
  min-height: 400px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>