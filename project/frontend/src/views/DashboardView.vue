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
/* ===== 共享画布：紫→蓝→青对角渐变，侧栏与顶栏透明露出，天然衔接无缝 ===== */
.base-container {
  display: flex;
  min-height: 100vh;
  background: var(--navbar-bg-gradient);
}

.dashboard {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100vh;
  background: transparent;
}

.sidebar-container {
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;

  height: 100vh;
  overflow: hidden;
  background: transparent;                    /* 露出共享画布，不再自画渐变 */
  transition: width var(--duration-normal) var(--ease-out-expo);
}

.navbar-container {
  position: sticky;
  top: 0;
  z-index: 101;
  flex-shrink: 0;
}

/* ===== 主卡片：顶部贴平顶栏（无圆角），左右下浮起露出画布 ===== */
.main-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  position: relative;
  z-index: 100;

  padding: var(--space-md) var(--space-lg);
  background: var(--html-bg);

  border-radius: var(--radius-lg) 0 0 0; /* 仅左上/右上圆角，顶部平 */
  border-left: 1px solid var(--float-border);
  border-right: 1px solid var(--float-border);
  border-bottom: 1px solid var(--float-border);
  box-shadow: var(--float-shadow);            /* 浮层阴影 + 底部内阴影（机制3） */

  transition: flex var(--duration-normal) var(--ease-out-expo);
}

/* ===== 机制 1：顶部内容渐隐带 =====
   sticky 吸附滚动容器顶部，负 margin 不占布局，
   内容滚入时在 40px 内淡入卡片底色，不再"撞硬边" */
.main-content::before {
  content: "";
  position: sticky;
  top: -16px;
  display: block;
  height: 20px;
  margin-bottom: -20px;
  z-index: 20;
  pointer-events: none;
  background: linear-gradient(180deg,
    var(--html-bg) 0%,
    var(--html-bg) 30%,
    transparent 100%);
}

/* ===== 机制 2：底部内容渐隐带 =====
   sticky 吸附底部，内容滚出前在 48px 内淡出到卡片底色 */
.main-content::after {
  content: "";
  position: sticky;
  bottom: -16px;
  display: block;
  height: 20px;
  margin-top: -24px;
  z-index: 20;
  pointer-events: none;
  background: linear-gradient(0deg,
    var(--html-bg) 0%,
    var(--html-bg) 40%,
    transparent 100%);
}

/* ===== 机制 4：滚动条主题化（细、品牌蓝、透明轨道）===== */
.main-content::-webkit-scrollbar {
  width: 6px;
}
.main-content::-webkit-scrollbar-track {
  background: transparent;
}
.main-content::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.3);
  border-radius: 3px;
}
.main-content::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.5);
}
/* Firefox 兼容 */
.main-content {
  scrollbar-width: thin;
  scrollbar-color: rgba(59, 130, 246, 0.3) transparent;
}
</style>