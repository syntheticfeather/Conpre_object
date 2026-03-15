<template>
  <div class="sidebar">
    <!-- 可折叠菜单 -->
    <el-menu
      :default-active="activeIndex"
      :collapse="isCollapse"
      :collapse-transition="true"
      background-color="#545c64"
      text-color="#fff"
      active-text-color="#ffd04b"
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
        <el-menu-item index="4-1">风险管理</el-menu-item>
        <el-menu-item index="4-2">催收设置</el-menu-item>
      </el-sub-menu>

      <!-- 系统管理 -->
      <el-sub-menu index="5">
        <template #title>
        <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="5-1">数据统计</el-menu-item>
        <el-menu-item index="5-2">系统设置</el-menu-item>
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
import { ref } from 'vue'
import { ArrowLeftBold, ArrowRightBold, User, Histogram, List, Warning, Setting } from '@element-plus/icons-vue'

const isCollapse = ref(false)
const activeIndex = ref('/dashboard/applications') // 默认激活子项

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
  min-height: calc(100vh - 55px);
  height: 100%;
  background-color: #545c64;
  box-shadow: 2px 0 8px rgb(5 29 64);
}

.collapse-btn {
  margin-bottom: 10px;
  background-color: #47515a;
  border-color: #47515a;
  color: #fff;
}

.collapse-btn:hover {
  background-color: #3a4249;
}

/* 菜单宽度 */
.el-menu-vertical:not(.el-menu--collapse) {
  width: 200px;
}

/* 折叠时隐藏文字 */
.el-menu--collapse .el-sub-menu .el-sub-menu__title span,
.el-menu--collapse .el-menu-item span {
  display: none;
}

/* 修复折叠后图标居中问题 */
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
  background: #47515a;
  border-radius: 3px;
}
</style>