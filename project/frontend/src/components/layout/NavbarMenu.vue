<template>
  <div class="nav-bar">
    <div class="logo-container">
      <img src="@/assets/images/logo.jpg" alt="logo">
      <div class="search-container">
        <el-input
          v-model="searchText"
          placeholder="搜索..."
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><SearchIcon /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <div class="right-info">
      <el-icon class="message-icon"><ChatDotRound /></el-icon>
      <el-icon class="message-icon"><ChatLineSquare /></el-icon>
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
import { ChatDotRound, ChatLineSquare, Search as SearchIcon } from '@element-plus/icons-vue' 

export default {
  name: 'NavbarMenu',
  components: {
    ChatDotRound, ChatLineSquare, SearchIcon
  },
  data() {
    return {
      admin: '管理员',
      authStore: useAuthStore(),
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
    // 搜索处理
    handleSearch() {
      if (this.searchText.trim()) {
        console.log('搜索内容:', this.searchText)
        // 这里可以添加实际的搜索逻辑
        // 例如：跳转到搜索结果页面或调用搜索API
      }
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
  background-color: #2c3e50;
  color: white;
  align-items: center;
  box-shadow: 5px 5px 6px rgba(0, 0, 0, 0.3);
  z-index: 9990;
}

.logo-container {
  display: flex;
  align-items: center;
  margin-left: 17px;
  flex: 1;
}

.search-container {
  margin-left: 30px;
  flex: 1;
  max-width: 400px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.search-input :deep(.el-input__placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.search-input :deep(.el-input__inner) {
  color: white;
}

.search-input :deep(.el-icon) {
  color: rgba(255, 255, 255, 0.7);
}

.message-icon {
  font-size: 24px;
  margin-right: 5px;
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
  padding: 5px 10px;
  
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
  min-width: 120px;
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
