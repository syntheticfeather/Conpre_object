<template>
  <!-- 顶部导航栏 -->
  <div class="navbar-container">
    <Navbar />
  </div>

  <div class="dashboard">
    <!-- 侧边导航栏 -->
    <div class="sidebar-container">
      <Sidebar />
    </div>
    
    <!-- 主体区域 -->
    <div class="main-content">
      <router-view />
    </div>

  </div>
</template>

<script setup>
import Sidebar from '@/components/layout/SidebarMenu.vue'
import Navbar from '@/components/layout/NavbarMenu.vue'
</script>

<style scoped>
  .navbar-container {
    height: 75px;
  }

  .dashboard {
    display: flex;
  }

  .sidebar-container {
    position: sticky;
    top: 75px;
    z-index: 100;

    height: calc(100vh - 75px);
    background-color: var(--sidebar-color);
    transition: width 0.3s ease;
  } 

  .main-content {
    flex: 1; 
  }
</style>