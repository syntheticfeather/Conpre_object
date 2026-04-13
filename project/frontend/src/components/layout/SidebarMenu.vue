<template>
  <div class="sidebar">
    <!-- 可折叠菜单 -->
    <el-menu
      :default-active="activeIndex"
      :collapse="isCollapse"
      :collapse-transition="true"
      background-color="--sidebar-color"
      text-color="#fff"
      active-text-color="#25e0bf"
      class="el-menu-vertical"
      router
      :unique-opened="true"
    >
      <!-- 待办审核 -->
      <el-sub-menu index="/dashboard/pending-applications">
        <template #title>
          <el-icon><List /></el-icon>
          <span>申请管理</span>
        </template>
        <el-menu-item index="/dashboard/pending-applications">待办审核</el-menu-item>
        <el-menu-item index="/dashboard/completed-applications">已办审核</el-menu-item>
      </el-sub-menu>

      <!-- 贷款管理 -->
      <el-sub-menu index="/dashboard/products">
        <template #title>
          <el-icon><Histogram /></el-icon>
          <span>贷款管理</span>
        </template>
        <el-menu-item index="/dashboard/products">已有项目</el-menu-item>
        <el-menu-item index="/dashboard/add-pro">添加项目</el-menu-item>
      </el-sub-menu>

      <!-- 用户管理 -->
      <el-sub-menu index="/dashboard/users">
        <template #title>
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </template>
        <el-menu-item index="/dashboard/users">用户列表</el-menu-item>
        <el-menu-item index="/dashboard/black-users">黑名单用户</el-menu-item>
      </el-sub-menu>

      <!-- 风控管理 -->
      <el-sub-menu index="4">
        <template #title>
          <el-icon><Warning /></el-icon>
          <span>风控管理</span>
        </template>
        <el-menu-item index="/dv-screen">数据大屏</el-menu-item>      
        <el-menu-item index="/dashboard/risk">风险管理</el-menu-item>  
        <el-menu-item index="/dashboard/collect-management">催收设置</el-menu-item>
      </el-sub-menu>

      <!-- 系统管理 -->
      <el-sub-menu index="5">
        <template #title>
        <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="5-2">系统设置</el-menu-item>
        <el-menu-item index="/dashboard/knowledge">知识库管理</el-menu-item>
      </el-sub-menu>
    </el-menu>

    <!-- 切换按钮 -->
    <el-button circle class="collapse-btn" @click="toggleCollapse">
      <el-icon>
        <ArrowRightBold v-if="isCollapse" />
        <ArrowLeftBold v-else />
      </el-icon>
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeftBold, ArrowRightBold, User, Histogram, List, Warning, Setting } from '@element-plus/icons-vue'

const isCollapse = ref(false)
const activeIndex = ref('/dashboard/pending-applications') // 默认激活子项
const route = useRoute()

// 监听路由变化，更新激活状态
watch(
  () => route.path,
  (newPath) => {
    activeIndex.value = newPath
  },
  { immediate: true }
)

// 组件挂载时初始化激活状态
onMounted(() => {
  activeIndex.value = route.path
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style scoped>
.sidebar {
  display: flex;
  
  align-items: center;
  justify-content: center;
  
  padding-top: 10px;

  width: auto;
  height: 100%;
  background-color: var(--sidebar-color);
  box-shadow: var(--sidebar-shadow-color);
  transition: all 0.3s ease;
}

.collapse-btn {
  margin-bottom: 10px;
  background-color: var(--sidebar-color);
  border-color: var(--sidebar-color);  
  color: #fff;
  transition: all 0.3s ease;
}

.collapse-btn:hover {
  background-color: #003461;
}

/* 菜单宽度 */
.el-menu-vertical:not(.el-menu--collapse) {
  width:150px;
}

/* 菜单项悬停颜色 */
.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-sub-menu__title:hover) {
  --el-menu-hover-bg-color: rgb(72, 118, 203);
}
/* 二级菜单项激活状态样式 */
.sidebar :deep(.el-sub-menu .el-menu-item.is-active) {
  color: #25e0bf; /* 激活状态的字体颜色 */
  border-left: 3px solid #25e0bf; /* 左侧边框 */
  --el-menu-hover-bg-color: rgb(72, 118, 203); /* 保持与悬停相同的背景色 */
}

/* 折叠后弹出菜单样式 */
:global(.el-menu--popup) {
  background-color: var(--sidebar-color) !important;
  border: none !important;
}
:global(.el-menu--popup .el-menu-item) {
  color: #fff !important;
}
:global(.el-menu--popup .el-menu-item:hover) {
  --el-menu-hover-bg-color: rgb(72, 118, 203) !important;
}
:global(.el-menu--popup .el-menu-item.is-active) {
  color: #25e0bf !important;
  --el-menu-hover-bg-color: rgb(72, 118, 203) !important;
}

/* 折叠时隐藏文字 */
.el-menu--collapse .el-sub-menu .el-sub-menu__title span,
.el-menu--collapse .el-menu-item span {
  display: none;
}

/* 折叠后图标居中 */
.el-menu--collapse .el-menu-item,
.el-menu--collapse .el-sub-menu .el-sub-menu__title {
  justify-content: center;
}
.el-icon {
  font-size: 16px;
}
/* 滚动条优化（如果内容超出） */
.el-menu {
  flex: 1;
  overflow-y: auto;
  width: 100%;
}

/* 滚动条样式（可选） */
.el-menu::-webkit-scrollbar {
  width: 6px;
}
.el-menu::-webkit-scrollbar-thumb {
  background: var(--sidebar-color);
  border-radius: 3px;
}
</style>