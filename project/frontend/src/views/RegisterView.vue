<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 注册功能已合并到登录页（带左右滑动切换），此处直接重定向
onMounted(() => {
  router.replace('/login')
})
</script>

<template>
  <div></div>
</template>
