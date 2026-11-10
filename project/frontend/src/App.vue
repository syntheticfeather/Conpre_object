<!-- src/App.vue -->
<template>
  <div id="app">
    <!-- Ant Design Vue 走 CSS-in-JS，必须靠 algorithm 切换暗色 -->
    <a-config-provider :theme="antdThemeConfig">
      <!-- 路由出口：匹配的组件会在这里渲染 -->
      <router-view />
    </a-config-provider>
  </div>
</template>

<script>
import { computed } from 'vue'
import { theme as antdTheme } from 'ant-design-vue'
import { useAppStore } from './stores/app'

export default {
  name: 'App',
  setup() {
    const appStore = useAppStore()

    const antdThemeConfig = computed(() => ({
      algorithm:
        appStore.theme === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm
    }))

    return { antdThemeConfig }
  }
}
</script>

<style>
/* 全局重置样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  height: 100%;
  font-family: Arial, sans-serif;
}
</style>