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
      
      <!-- 通知铃铛图标 -->
      <div class="notification-wrapper" @click="toggleNotificationPanel">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
          <BellOutlined class="bell-icon" />
        </el-badge>
        
        <!-- 通知下拉面板 -->
        <div v-show="showNotificationPanel" class="notification-panel" @mouseenter="keepNotificationVisible" @mouseleave="hideNotificationPanel">
          <div class="panel-header">
            <span>通知中心</span>
            <div class="header-actions">
              <el-button size="small" type="primary" @click="markAllRead" :disabled="unreadCount === 0">
                全部已读
              </el-button>
              <el-button size="small" type="danger" @click="clearAll" :disabled="notifications.length === 0">
                清空全部
              </el-button>
            </div>
          </div>
          
          <div class="notification-list" v-loading="loading">
            <div v-if="notifications.length === 0 && !loading" class="empty-notice">
              <el-empty description="暂无通知" :image-size="80" />
            </div>
            
            <div 
              v-for="notif in notifications" 
              :key="notif.id" 
              class="notification-item"
              :class="{ unread: !notif.readFlag }"
              @click="handleNotificationClick(notif)"
            >
              <div class="notif-left">
                <div class="notif-title">
                  {{ notif.title }}
                  <el-tag v-if="!notif.readFlag" size="small" type="danger" class="unread-tag">未读</el-tag>
                </div>
                <div class="notif-content">{{ notif.content }}</div>
                <div class="notif-footer">
                  <span class="notif-time">{{ formatTime(notif.createdAt) }}</span>
                  <el-tag v-if="getBusinessTypeLabel(notif.businessType)" size="small" class="type-tag">
                    {{ getBusinessTypeLabel(notif.businessType) }}
                  </el-tag>
                </div>
              </div>
              <div class="notif-actions">
                <el-button 
                  v-if="!notif.readFlag" 
                  type="primary" 
                  size="small" 
                  @click.stop="markAsRead(notif.id)"
                >
                  已读
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click.stop="deleteNotification(notif.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
          
          <div v-if="notifications.length > 0" class="panel-footer">
            <el-button size="small" @click="loadMore" :disabled="loading">加载更多</el-button>
          </div>
        </div>
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
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import notificationAPI from '@/api/modules/notification'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'NavbarMenu',
  components: {
    BellOutlined, Moon, Sunny
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    
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
      currentRouteName,
      router
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
      searchText: '',
      
      // 通知相关
      notifications: [],
      unreadCount: 0,
      showNotificationPanel: false,
      loading: false,
      notificationPanelTimer: null
    }
  },
  mounted() {
    // 从本地存储加载头像
    const savedAvatar = localStorage.getItem('adminAvatar')
    if (savedAvatar) {
      this.avatarUrl = savedAvatar
    }
    // 加载通知列表
    this.fetchNotifications()
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
    },
    
    // ========== 通知相关方法 ==========
    
    // 切换通知面板
    toggleNotificationPanel() {
      this.showNotificationPanel = !this.showNotificationPanel
      if (this.showNotificationPanel) {
        this.fetchNotifications()
      }
    },
    
    // 保持通知面板可见
    keepNotificationVisible() {
      if (this.notificationPanelTimer) {
        clearTimeout(this.notificationPanelTimer)
        this.notificationPanelTimer = null
      }
    },
    
    // 隐藏通知面板
    hideNotificationPanel() {
      this.notificationPanelTimer = setTimeout(() => {
        this.showNotificationPanel = false
        this.notificationPanelTimer = null
      }, 300)
    },
    
    // 获取通知列表
    async fetchNotifications() {
      this.loading = true
      try {
        const res = await notificationAPI.getAdminNotifications()
        if (res.code === 200) {
          this.notifications = res.data || []
          this.unreadCount = this.notifications.filter(n => !n.readFlag).length
        }
      } catch (error) {
        console.error('获取通知失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 标记为已读
    async markAsRead(id) {
      try {
        await notificationAPI.markAsRead(id)
        // 更新本地状态
        const notification = this.notifications.find(n => n.id === id)
        if (notification) {
          notification.readFlag = true
        }
        this.unreadCount = Math.max(0, this.unreadCount - 1)
        ElMessage.success('已标记为已读')
      } catch (error) {
        ElMessage.error('操作失败'+error)
      }
    },
    
    // 全部标记已读
    async markAllRead() {
      try {
        // 批量标记已读
        const unreadIds = this.notifications.filter(n => !n.readFlag).map(n => n.id)
        const promises = unreadIds.map(id => notificationAPI.markAsRead(id))
        await Promise.all(promises)
        
        // 更新本地状态
        this.notifications.forEach(n => n.readFlag = true)
        this.unreadCount = 0
        ElMessage.success('全部标记为已读')
      } catch (error) {
        ElMessage.error('操作失败'+error)
      }
    },
    
    // 删除通知
    async deleteNotification(id) {
      try {
        await ElMessageBox.confirm('确定要删除该通知吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await notificationAPI.deleteNotification(id)
        // 更新本地列表
        this.notifications = this.notifications.filter(n => n.id !== id)
        this.unreadCount = this.notifications.filter(n => !n.readFlag).length
        ElMessage.success('删除成功')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    },
    
    // 清空全部
    async clearAll() {
      try {
        await ElMessageBox.confirm('确定要清空所有通知吗？', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const allIds = this.notifications.map(n => n.id)
        await notificationAPI.batchDelete(allIds)
        
        this.notifications = []
        this.unreadCount = 0
        ElMessage.success('清空成功')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('清空失败')
        }
      }
    },
    
    // 加载更多（暂时只刷新）
    loadMore() {
      this.fetchNotifications()
    },
    
    // 点击通知处理
    handleNotificationClick(notif) {
      // 先标记为已读
      if (!notif.readFlag) {
        this.markAsRead(notif.id)
      }
      
      // 根据业务类型跳转
      const route = this.getBusinessRoute(notif.businessType, notif.businessId)
      if (route) {
        this.router.push(route)
        this.showNotificationPanel = false
      }
    },
    
    // 获取业务路由
    getBusinessRoute(businessType, businessId) {
      switch (businessType) {
        case 'LOAN_APPLICATION_APPROVE':
          return { path: '/dashboard/pending-applications', query: { applicationId: businessId } }
        default:
          return null
      }
    },
    
    // 获取业务类型标签
    getBusinessTypeLabel(type) {
      const labels = {
        'LOAN_APPLICATION_APPROVE': '贷款审核',
        'OVERDUE_WARNING': '逾期预警',
        'SYSTEM': '系统通知'
      }
      return labels[type] || ''
    },
    
    // 格式化时间
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      
      // 一小时内显示"刚刚"
      if (diff < 60 * 60 * 1000) {
        return '刚刚'
      }
      // 24 小时内显示"X 小时前"
      if (diff < 24 * 60 * 60 * 1000) {
        const hours = Math.floor(diff / (60 * 60 * 1000))
        return `${hours}小时前`
      }
      // 7 天内显示"X 天前"
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        const days = Math.floor(diff / (24 * 60 * 60 * 1000))
        return `${days}天前`
      }
      // 其他显示日期
      return date.toLocaleDateString('zh-CN')
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

/* 通知铃铛样式 */
.notification-wrapper {
  position: relative;
  margin-right: 10px;
  cursor: pointer;
}

.notification-badge {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell-icon {
  font-size: 22px;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.bell-icon:hover {
  color: #ffffff;
  transform: scale(1.1);
}

/* 通知面板样式 */
.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 10px;
  width: 400px;
  max-height: 500px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.panel-header span {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
}

.empty-notice {
  padding: 40px 20px;
  text-align: center;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
  border-left: 3px solid #409EFF;
}

.notif-left {
  flex: 1;
  margin-right: 15px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-tag {
  font-size: 10px;
  padding: 1px 6px;
}

.notif-content {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notif-time {
  font-size: 12px;
  color: #909399;
}

.type-tag {
  font-size: 10px;
  padding: 1px 6px;
}

.notif-actions {
  display: flex;
  flex-direction: column;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.notification-item:hover .notif-actions {
  opacity: 1;
}

.panel-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
  background-color: #fafafa;
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
