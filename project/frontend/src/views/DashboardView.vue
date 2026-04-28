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
  box-shadow: var(--sidebar-shadow-color);
}

.navbar-container {
  position: sticky;
  top: 0;
  z-index: 9990;
}

.main-content {
  flex: 1;
}
</style>