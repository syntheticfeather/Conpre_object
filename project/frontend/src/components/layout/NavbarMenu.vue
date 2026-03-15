<template>
  <div class="nav-bar">
    <div>
      <img src="@/assets/images/logo.jpg" alt="logo">
    </div>

    <div class="right-info">
      <span>信息字体图标</span>
      <div class="admin-info">
        <img src="@/assets/images/admin.png" alt="admin" class="avatar">
        <span>xxx</span>
      </div>

      <button id="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script>
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'NavbarMenu',
  methods: {
    async handleLogout() {
      const authStore = useAuthStore()
      await authStore.logout()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.nav-bar {
  position: fixed;
  display: flex;
  justify-content: space-between;
  width: 100%;
  height: 100px;
  background-color: #2c3e50;
  color: white;
  align-items: center;
  box-shadow: 5px 5px 6px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}

img[alt="logo"] {
  margin-left: 17px;
  height: 55px;
}

.right-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-right: 40px;
  width: 255px;
}

.admin-info {
  display: flex;
  align-items: center;
}

img[alt="admin"] {
  margin-right: 5px;
  width: 35px;
  height: 35px;
}

.avatar {
  border-radius: 50%;
  object-fit: cover;
}

#logout-btn {
  margin-left: 5px;
  padding: 2px 11px;
  background-color: transparent;
  color: #3498db;
  font-size: 20px;
  border: 1px solid #3498db;
  border-radius: 8px;
}

#logout-btn:hover {
  background-color: #3498db;
  color: white;
}
</style>
