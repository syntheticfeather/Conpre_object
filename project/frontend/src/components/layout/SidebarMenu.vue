<template>
  <div class="sidebar-wrapper">
    <div class="logo-container">
      <img src="@/assets/images/logo.png" alt="logo">
      <span v-show="showLogoText">XIN FINANCE</span>
    </div>

    <div class="sidebar">
      <div class="menu-wrapper">
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
          ref="menuRef"
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
            <el-menu-item index="/dashboard/knowledge">知识库管理</el-menu-item>
            <el-menu-item index="/dashboard/agent-config">Agent 配置</el-menu-item>
            <el-menu-item index="/dashboard/mcp-tools">MCP 工具管理</el-menu-item>
            <el-menu-item index="/dashboard/conversation-logs">对话统计</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <div circle class="collapse-btn" @click="toggleCollapse">
          <el-icon>
            <ArrowRightBold v-if="isCollapse" />
            <ArrowLeftBold v-else />
          </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeftBold, ArrowRightBold, User, Histogram, List, Warning, Setting } from '@element-plus/icons-vue'

const isCollapse = ref(false)
const showLogoText = ref(true)
const activeIndex = ref('/dashboard/pending-applications') // 默认激活子项
const route = useRoute()
const menuRef = ref(null)

// 所有子菜单的 index
const subMenuIndexes = [
  '/dashboard/pending-applications',
  '/dashboard/products',
  '/dashboard/users',
  '4',
  '5'
]

// 监听路由变化，更新激活状态
watch(
  () => route.path,
  (newPath) => {
    activeIndex.value = newPath
  },
  { immediate: true }
)

// 侧栏展开时延时显示 logo 文字，收起时立即隐藏
watch(isCollapse, (val) => {
  if (val) {
    showLogoText.value = false
  } else {
    setTimeout(() => {
      showLogoText.value = true
    }, 250)
  }
})

// 组件挂载时初始化激活状态
onMounted(() => {
  activeIndex.value = route.path
})

const toggleCollapse = () => {
  // 如果当前是展开状态，准备收起，先关闭所有子菜单
  if (!isCollapse.value) {
    // 关闭所有展开的子菜单
    subMenuIndexes.forEach(index => {
      if (menuRef.value) {
        menuRef.value.close(index)
      }
    })
  }
  
  // 延迟一点时间让子菜单收起动画完成，再收起侧栏
  setTimeout(() => {
    isCollapse.value = !isCollapse.value
  }, 200)
}
</script>

<style scoped>
.sidebar-wrapper {
  display: flex;
  flex-direction: column;

  height: 100%;
  background-image: var(--sidebar-image);
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 75px;
  flex-shrink: 0;
  border-bottom: 1px solid #fff;
}
.logo-container span {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
}
img[alt="logo"] {
  margin: 7px;
  height: 60px;
  flex-shrink: 0;
}

/* 侧栏菜单部分 */
.sidebar {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.menu-wrapper {
  display: flex;
  flex-direction: column;
}
.el-menu-vertical {
  margin-top: auto;
  margin-bottom: auto;
  width: 100%;
}
.el-menu-vertical:not(.el-menu--collapse) {
  width: 150px;
}

/* 收起/展开按钮 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  height: 30px;
  width: 30px;
  color: #fff;
  transition: all 0.3s ease;
  border-radius: 20px;
  cursor: pointer;
}
.collapse-btn:hover {
  background-color: #003461;
}
.collapse-btn .el-icon {
  font-size: 20px;
  line-height: 1;
}

.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-sub-menu__title:hover) {
  --el-menu-hover-bg-color: #2d9d88c5;
}
.sidebar :deep(.el-sub-menu .el-menu-item.is-active) {
  color: #25e0bf;
  border-left: 3px solid #25e0bf;
  --el-menu-hover-bg-color: #2d9d88c5;
}

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

.el-menu--collapse .el-menu-item,
.el-menu--collapse .el-sub-menu .el-sub-menu__title {
  justify-content: center;
}
.el-icon {
  font-size: 16px;
}
</style>