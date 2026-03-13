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
    height: 55px;
  }

  .dashboard {
    display: flex;
  }

  .sidebar-container {
    color: aqua;
  }

  .main-content {
    flex: 1;
  }
</style>