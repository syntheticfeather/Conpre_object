<template>
  <div class="nav-bar">
    <div class="logo-container">
      <img src="@/assets/images/logo.jpg" alt="logo">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-container">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>管理平台</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <div class="right-info">
      <BellOutlined class="BellOutlined" />
      <!-- 主题切换图标 -->
      <div class="theme-icons">
        <!-- 太阳图标：亮色模式下隐藏 -->
        <el-icon 
          v-show="appStore.theme !== 'dark'"
          class="theme-icon moon" 
          @click="setTheme('dark')"
          title="切换到暗色模式"
        >
          <Moon />
        </el-icon>
        <!-- 月亮图标：暗色模式下隐藏 -->
        <el-icon 
          v-show="appStore.theme !== 'light'"
          class="theme-icon sunny" 
          @click="setTheme('light')"
          title="切换到亮色模式"
        >
          <Sunny />
        </el-icon>
      </div>
      <div class="admin-info" @mouseenter="showAdminTable" @mouseleave="hideAdminTable">
        <div class="avatar-container">
          <img :src="avatarUrl" alt="admin" class="avatar">
        </div>
        <span>{{ admin }}</span>
        
        <div id="admin-table" ref="adminTable" @mouseenter="keepAdminTableVisible" @mouseleave="hideAdminTable">
          <button class="admin-btn" @click="triggerFileInput">修改头像</button>
          <button id="logout-btn" @click="handleLogout">退出登录</button>
          <input type="file" ref="fileInput" accept="image/*" @change="handleFileChange" style="display: none;">
        </div>
      </div>
    </div>
    
    <!-- 头像上传预览对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="头像上传"
      width="300px"
    >
      <div class="avatar-upload-container">
        <img :src="previewUrl" alt="预览" class="preview-avatar">
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="uploadAvatar">确定上传</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script>
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

export default {
  name: 'NavbarMenu',
  components: {
    BellOutlined, Moon, Sunny
  },
  setup() {
    const route = useRoute()
    
    // 当前路由名称
    const currentRouteName = computed(() => {
      const nameMap = {
        'Applications': '待审核申请',
        'CompletedApplications': '已完成申请',
        'Products': '产品管理',
        'AddProduct': '添加产品',
        'Users': '用户管理',
        'BlackUsers': '黑名单管理',
        'Risk': '风险管理',
        'CollectManagement': '催收管理'
      }
      return nameMap[route.name] || route.name || ''
    })
    
    return {
      currentRouteName
    }
  },
  data() {
    return {
      admin: '管理员',
      authStore: useAuthStore(),
      appStore: useAppStore(),
      avatarUrl: '@/assets/images/admin.png',
      previewUrl: '',
      dialogVisible: false,
      selectedFile: null,
      hideTimer: null,
      searchText: ''
    }
  },
  mounted() {
    // 从本地存储加载头像
    const savedAvatar = localStorage.getItem('adminAvatar')
    if (savedAvatar) {
      this.avatarUrl = savedAvatar
    }
  },
  methods: {
    handleLogout() {
      const authStore = useAuthStore()
      authStore.logout()
      this.$router.push('/login')
    },
    showAdminTable() {
      // 清除之前的定时器
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
        this.hideTimer = null
      }
      this.$refs.adminTable.classList.add('show')
    },
    hideAdminTable() {
      // 设置三秒后消失的定时器
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
      }
      this.hideTimer = setTimeout(() => {
        this.$refs.adminTable.classList.remove('show')
        this.hideTimer = null
      }, 500)
    },
    keepAdminTableVisible() {
      // 清除定时器
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
        this.hideTimer = null
      }
      this.$refs.adminTable.classList.add('show')
    },
    // 触发文件选择
    triggerFileInput() {
      this.$refs.fileInput.click()
    },
    // 处理文件选择
    handleFileChange(event) {
      const file = event.target.files[0]
      if (file) {
        this.selectedFile = file
        // 创建预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.previewUrl = e.target.result
          this.dialogVisible = true
        }
        reader.readAsDataURL(file)
      }
    },
    // 上传头像
    uploadAvatar() {
      if (this.selectedFile) {
        // 这里可以添加实际的上传逻辑
        // 暂时使用本地存储模拟
        localStorage.setItem('adminAvatar', this.previewUrl)
        this.avatarUrl = this.previewUrl
        this.dialogVisible = false
        this.selectedFile = null
        this.previewUrl = ''
        // 显示成功提示
        this.$message.success('头像上传成功')
      }
    },
    // 切换主题
    setTheme(theme) {
      this.appStore.setTheme(theme)
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
  height: 75px;
  background-color: var(--nabar-color);
  color: white;
  align-items: center;
  box-shadow: var(--nabar-shadow-color);
  z-index: 9990;
}

.logo-container {
  display: flex;
  align-items: center;
  margin-left: 17px;
  flex: 1;
}

.BellOutlined {
  font-size: 20px;
  margin-right: 5px;
}
.theme-toggle {
  margin-right: 10px;
  border: none;
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}
.theme-toggle:hover {
  background-color: rgba(255, 255, 255, 0.3);
}
/* 主题切换图标容器 */
.theme-icons {
  display: flex;
  align-items: center;
  gap: 8px;
}
/* 主题图标基础样式 */
.theme-icon {
  font-size: 25px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
  padding: 4px;
  border-radius: 4px;
}
.theme-icon:hover {
  color: rgba(255, 255, 255, 1);
  background-color: rgba(255, 255, 255, 0.15);
  transform: scale(1.15);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.3);
}
/* 太阳图标特殊样式 */
.theme-icon.sunny {
  font-size: 30px;
  color: rgba(255, 255, 255, 0.8);
}
.theme-icon.sunny:hover {
  color: #ffffff;
  background-color: rgba(131, 179, 198, 0.2);
  box-shadow: 0 0 12px rgba(201, 203, 204, 0.5);
}
/* 月亮图标特殊样式 */
.theme-icon.moon {
  font-size: 30px;
  color: rgba(255, 255, 255, 0.8);
}
.theme-icon.moon:hover {
  color: #a0c4ff;
  background-color: rgba(160, 196, 255, 0.2);
  box-shadow: 0 0 12px rgba(198, 208, 223, 0.5);
}

img[alt="logo"] {
  margin-left: 17px;
  height: 55px;
}

.breadcrumb-container {
  margin-left: 30px;
  display: flex;
  align-items: center;
}

/* 面包屑导航文字颜色 */
.breadcrumb-container :deep(.el-breadcrumb__inner) {
  color: white !important;
}
.breadcrumb-container :deep(.el-breadcrumb__separator) {
  color: white !important;
}

.right-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-right: 40px;
  width: 220px;
}

.admin-info {
  display: flex;
  align-items: center;
  padding: 5px 10px;  
  justify-content: space-around;

  width: 148px;
  background-color: #055986;
  border-radius: 5px;
  position: relative;  
}

.admin-info span {
  font-size: 20px;
  font-family: 'fangsong','Times New Roman', Times, serif;
}

img[alt="admin"] {
  margin-right: 5px;
  width: 35px;
  height: 35px;
}
/*管理员头像*/
.avatar {
  border-radius: 50%;
  object-fit: cover;
}

.avatar-container {
  position: relative;
}

/* 头像上传预览样式 */
.avatar-upload-container {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.preview-avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ddd;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

#admin-table {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 5px;
  background-color: #fff;
  padding: 10px;
  border-radius: 3px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  display: none;
  min-width: 150px;
  z-index: 1000;
}

#admin-table.show {
  display: block;
}

/*管理员按钮样式*/ 
.admin-btn, #logout-btn {
  display: block;
  width: 100%;
  margin: 5px 0;
  padding: 2px 11px;
  background-color: transparent;
  color: #3498db;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.admin-btn:hover, #logout-btn:hover {
  background-color: #3498db;
  color: white;
}
</style>
