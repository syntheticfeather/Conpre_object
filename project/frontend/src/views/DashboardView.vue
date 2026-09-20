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
        <router-view v-slot="{ Component, route }">
          <Transition name="page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </Transition>
        </router-view>
      </div>

    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/layout/SidebarMenu.vue'
import Navbar from '@/components/layout/NavbarMenu.vue'
</script>

<style scoped>
@import '@/assets/css/share/dashboardView.css';
</style>