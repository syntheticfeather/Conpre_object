<template>
  <div class="black-user-manage-view" v-show="showBlacklistTable">
    <div class="header">
      黑名单管理
    </div>
    
    <BlacklistTable 
      @user-selected="handleUserSelected"
    />
  </div>
  
  <div class="user-detail-section" v-show="showUserDetail">
    <UserDetailPanel 
      :user-id="selectedUserId"
      :is-visible="showUserDetail"
      @close="closeUserDetail"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BlacklistTable from '@/components/user/BlacklistTable.vue'
import UserDetailPanel from '@/components/user/UserDetailPanel.vue'

const selectedUserId = ref(null)
const showUserDetail = ref(false)
const showBlacklistTable = ref(true)

// 处理用户选择事件
const handleUserSelected = (userId) => {
  console.log('BlackUserManageView: 接收到用户选择事件，userId:', userId)
  selectedUserId.value = userId
  showUserDetail.value = true
  showBlacklistTable.value = false
}

// 关闭用户详情
const closeUserDetail = () => {
  console.log('BlackUserManageView: 关闭用户详情')
  showUserDetail.value = false
  selectedUserId.value = null
  showBlacklistTable.value = true
}
</script>

<style scoped>
.black-user-manage-view {
  padding: 20px;
}

.title {
  font-size: 24px;
  color: var(--blacklist-text);
}

.user-detail-section {
  min-height: 400px;
}
</style>