<template>
  <div class="base-container">
    <!-- 侧边导航栏 -->
    <div class="sidebar-container">
      <Sidebar />
    </div>

    <div class="dashboard">
      <!-- 顶部导航栏 -->
      <div class="navbar-container">
        <Navbar />
      </div>

      <!-- 主体区域 -->
      <div class="main-content">
        <router-view />
      </div>

    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/layout/SidebarMenu.vue'
import Navbar from '@/components/layout/NavbarMenu.vue'
</script>

<style scoped>
.base-container {
  display: flex;
  min-height: 100vh;
}

.dashboard {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100vh;
  background-color: var(--dashboard-card-title);
}

.sidebar-container {
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;

  height: 100vh;
  overflow: hidden;
  background-color: var(--sidebar-color);
  transition: width 0.3s ease;
}

.navbar-container {
  position: sticky;
  top: 0;
  z-index: 101;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  position: relative;
  z-index: 100;
  border-top-left-radius: 12px;
  box-shadow: -3px 2px 31px var(--dashboard-stats-shadow);
}

/* main-content的内部滚动条样式  */
/* 整个滚动条 */
.main-content::-webkit-scrollbar {
  width: 6px;                /* 纵向滚动条宽度 */
}
/* 滚动条轨道(背景) */
.main-content::-webkit-scrollbar-track {
  background: transparent;   /* 透明轨道 */
}
/* 滚动条滑块(拖动手柄) */
.main-content::-webkit-scrollbar-thumb {
  background: var(--text-color-caption); /* 滑块颜色 */
  border-radius: 3px;
}
/* 滑块悬停效果 */
.main-content::-webkit-scrollbar-thumb:hover {
  background: var(--color-info);
}
/* Firefox 兼容 */
.main-content {
  scrollbar-width: thin;                /* 细滚动条 */
  scrollbar-color: var(--text-color-caption) transparent; /* 滑块颜色 轨道颜色 */
}
</style>