<template>
  <PageStage :active="showUserDetail">
    <template #list>
      <div class="black-user-manage-view">
        <div class="header">
          黑名单管理
        </div>

        <BlacklistTable
          @user-selected="handleUserSelected"
        />
      </div>
    </template>

    <!-- 用户详情：常驻挂载，显隐与滑入滑出由 PageStage 负责 -->
    <template #detail>
      <div class="user-detail-section">
        <UserDetailPanel
          :user-id="selectedUserId"
          :is-visible="showUserDetail"
          @close="closeUserDetail"
        />
      </div>
    </template>
  </PageStage>
</template>

<script setup>
import { ref } from 'vue'
import PageStage from '@/components/shared/PageStage.vue'
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
}

.title {
  font-size: 24px;
  color: var(--blacklist-text);
}
</style>