<!-- src/App.vue -->
<template>
  <div id="app">
    <!-- 路由出口：匹配的组件会在这里渲染 -->
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
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