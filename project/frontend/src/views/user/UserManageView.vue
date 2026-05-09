<template>
  <div class="user-manage-view" v-show="showUserTable">
    <div class="header">
      用户管理
    </div>

    <div class="banner">
      <!-- 分类卡片 -->
      <div class="card-container">
        <button 
        class="card all" 
        :class="{ active: filterType === 'all' }" 
        @click="handleAll"
        style="font:600 18px '微软雅黑';"
        >
          <h3>{{ userAmount ||'0'}}</h3>
          全部用户
        </button>
        <button class="card normal" :class="{ active: filterType === 'normal' }" @click="handleNormal">
          <h3>{{ normalUserAmount||'0' }}</h3>
          正常用户
        </button>
        <button class="card abnormal" :class="{ active: filterType === 'abnormal' }" @click="handleAbnormal">
          <h3>{{ abnormalUserAmount ||'0'}}</h3>
          状态异常用户
        </button>
        <button class="card black" :class="{ active: filterType === 'blacklist' }" @click="handleBlack">
          <h3>{{ blackUserAmount||'0' }}</h3>
          黑名单用户
        </button> 
      </div>

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
      ref="tableRef"
      @user-selected="handleUserSelected"
    />
    
  </div>
  <!-- 用户详情 -->
  <div class="user-detail-section" v-show="showUserDetail">
    <UserDetailPanel 
      :user-id="selectedUserId"
      :is-visible="showUserDetail"
      @close="closeUserDetail"
    />  
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UserTable from '@/components/user/UserTable.vue'
import UserDetailPanel from '@/components/user/UserDetailPanel.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const selectedUserId = ref(null)
const showUserDetail = ref(false)
const searchText = ref('')
const tableRef = ref(null)
const showUserTable = ref(true)

// 筛选状态：'all' | 'normal' | 'abnormal' | 'blacklist'
const filterType = ref('all')

// 用户数量统计
const userAmount = ref(0)
const normalUserAmount = ref(0)
const abnormalUserAmount = ref(0)
const blackUserAmount = ref(0)

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    // 获取所有用户
    await userStore.fetchUserStats()
    const allUsers = userStore.users
    
    // 统计各类用户数量
    userAmount.value = allUsers.length
    blackUserAmount.value = userStore.blacklist.length
    
    // 正常用户：信誉分 >= 0 且不在黑名单
    normalUserAmount.value = allUsers.filter(u => 
      u.creditScore >= 0 && !userStore.blacklist.find(b => b.userId === u.userId)
    ).length
    
    // 异常用户：信誉分 < 0
    abnormalUserAmount.value = allUsers.filter(u => 
      u.creditScore < 0 
    ).length
  } catch (error) {
    console.error('加载用户统计失败:', error)
  }
}

// 处理用户选择事件
const handleUserSelected = (userId) => {
  console.log('UserManageView: 接收到用户选择事件，userId:', userId)
  selectedUserId.value = userId
  showUserDetail.value = true
  showUserTable.value = false
}

// 关闭用户详情
const closeUserDetail = () => {
  console.log('UserManageView: 关闭用户详情')
  showUserDetail.value = false
  selectedUserId.value = null
  showUserTable.value = true
}

// 处理'全部用户'点击事件
const handleAll = () => {
  filterType.value = 'all'
  // 重置表格筛选
  if (tableRef.value) {
    tableRef.value.resetFilter()
  }
}

// 处理'正常用户'点击事件
const handleNormal = () => {
  filterType.value = 'normal'
  // 筛选信誉分 >= 0 且不在黑名单的用户
  if (tableRef.value) {
    tableRef.value.setFilter({
      creditScore: '>=0',
      notInBlacklist: true
    })
  }
}

// 处理'状态异常用户'点击事件
const handleAbnormal = () => {
  filterType.value = 'abnormal'
  // 筛选信誉分 < 0 
  if (tableRef.value) {
    tableRef.value.setFilter({
      creditScore: '<0',
    })
  }
}

// 处理'黑名单用户'点击事件
const handleBlack = () => {
  filterType.value = 'blacklist'
  // 筛选黑名单用户
  if (tableRef.value) {
    tableRef.value.setFilter({
      inBlacklist: true
    })
  }
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

// 生命周期
onMounted(() => {
  loadUserStats()
})
</script>

<style scoped>
.user-manage-view {
  padding: 20px;
}

.banner {
  display: flex;  
  justify-content: space-between;

  align-items: center;
}

.card-container {
  display: flex;
  justify-content: space-between;
  align-items: center;

  width: 70%;
  height: 80px;
}

.card {
  padding:5px 15px;
  margin-right: 20px;
  
  width: 25%;
  height: 60px;

  text-align: left;
  background-color: var(--user-card-bg);
  border-radius: 12px;
  border: 1px solid var(--user-card-border);
  
  cursor: pointer;
  transition: all 0.3s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--user-card-shadow);
}

.card.active {
  background-color: var(--user-btn-bg);
  border-color: var(--user-btn-border);
}

.card.active h3,
.card.active p {
  color: var(--user-btn-text);
}

.card h3 {
  margin: 0;
  width: auto;
  color: var(--user-stat-text);

  font-size: 25px;
  line-height: 25px;
  font-weight: 600;
  font-family: 方正小标宋，楷体，微软雅黑;
}

.search-section {
  margin-right: 20px;

  width: 300px;
}

#btn-search {
  background-color: var(--user-btn-bg);
  color: var(--user-btn-text);
}
</style>