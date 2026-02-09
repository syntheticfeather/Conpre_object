<template>
  <div class="nav-bar">
    <!-- logo -->
     <div>
       <img src="@/assets/images/logo.jpg" alt="logo">
     </div>

     <!-- 右侧基础信息展示 -->
     <div class="right-info">
      <span>信息字体图标</span>
      <div class="admin-info">
        <img src="@/assets/images/admin.png" alt="admin" class="avatar">
        <!-- <span>{{ user.username }}</span> -->
        <span>xxx</span>
      </div>

      <!-- 退出登录按钮 -->
      <button @click="logout" id="logout-btn">退出登录</button>
     </div>
  </div>
</template>

<script>
  import { authAPI } from '@/api'
  export default {
    name: 'NavbarMenu',
    methods: {
      logout() {
        authAPI.logout().then(() => {
          this.$router.push('/login')
        })
      }
    }
  }
</script>

<style scoped>
/* 顶部导航栏全区 */
.nav-bar {
position: fixed;
  display: flex;
  justify-content: space-between;

  width: 100%;
  height: 55px;
  background-color: #2c3e50;
  color: white;
  align-items: center;

  box-shadow: 5px 5px 6px rgba(0, 0, 0, 0.3);
}

/*logo*/
img[alt="logo"] {
  margin-left: 17px;
  height: 55px;
}

/* 右侧基础信息*/
.right-info {
  display: flex;
  justify-content: space-around;
  align-items: center;

  margin-right: 40px;

  width: 255px;
}
/* 管理员信息 */
.admin-info {
  display: flex;
  align-items: center;
}
/* 头像 */
img[alt="admin"] {
  margin-right: 5px;

  width: 35px;
  height: 35px;
}
.avatar {
  border-radius: 50%;  /* 将元素变为圆形 */
  object-fit: cover;   /* 保持图片比例并裁剪填充 */
}

/* 退出登录按钮 */
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