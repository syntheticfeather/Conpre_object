<template>
  <div class="nav-bar">
      <div class="search-container">
        <el-input
          v-model="searchText"
          placeholder="搜索..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><SearchIcon /></el-icon>
          </template>
        </el-input>
      </div>

    <div class="right-info">
      <!-- 主题切换图标 -->
      <div class="theme-icons">
        <!-- 太阳图标：亮色模式下隐藏 -->
        <el-icon 
          v-show="appStore.theme !== 'dark'"
          class="theme-icon moon" 
          title="切换到暗色模式"
          @click="setTheme('dark')"
        >
          
          <Moon />
        </el-icon>
        <!-- 月亮图标：暗色模式下隐藏 -->
        <el-icon 
          v-show="appStore.theme !== 'light'"
          class="theme-icon sunny" 
          title="切换到亮色模式"
          @click="setTheme('light')"
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
        <Transition name="pop">
        <div v-show="showNotificationPanel" class="notification-panel" @mouseenter="keepNotificationVisible" @mouseleave="hideNotificationPanel">
          <div class="panel-header">
            <div class="header-left">
              <span>通知中心</span>
              <span class="connection-dot" :class="{ connected: isStreamConnected, disconnected: !isStreamConnected }" :title="isStreamConnected ? '实时连接中' : '连接断开'"></span>
            </div>
            <div class="header-actions">
              <el-button v-if="notifications.length > 0" size="small" type="primary" @click="loadMore" :disabled="loading">
                加载更多
              </el-button>
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
          
        </div>
        </Transition>
      </div>

      <div class="admin-info" @mouseenter="showAdminTable" @mouseleave="hideAdminTable">
        <div class="avatar-container">
          <img :src="avatarUrl" alt="admin" class="avatar">
        </div>
        <span>{{ admin }}</span>
        
        <Transition name="pop">
        <div id="admin-table" ref="adminTable" v-show="showAdminPanel" @mouseenter="keepAdminTableVisible" @mouseleave="hideAdminTable">
          <div class="admin-menu-item" @click="triggerFileInput">
            <span class="admin-menu-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </span>
            <span class="admin-menu-text">修改头像</span>
          </div>
          <div class="admin-menu-item admin-menu-item--danger" @click="handleLogout">
            <span class="admin-menu-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </span>
            <span class="admin-menu-text">退出登录</span>
          </div>
          <input type="file" ref="fileInput" accept="image/*" @change="handleFileChange" style="display: none;">
        </div>
        </Transition>
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
import { Moon, Sunny, Search as SearchIcon } from '@element-plus/icons-vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import notificationAPI from '@/api/modules/notification'
import { useNotificationStream } from '@/composables/useNotificationStream'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'NavbarMenu',
  components: {
    BellOutlined, Moon, Sunny, SearchIcon
  },
  setup() {
    const router = useRouter()
    const instance = getCurrentInstance()

    const {
      isStreamConnected,
      initNotificationStreamWithFetch,
      closeNotificationStream,
      reconnectStream,
      requestNotificationPermission
    } = useNotificationStream({
      onNotification: (notification) => {
        const vm = instance?.proxy
        if (!vm) return
        const exists = vm.notifications.some(n => n.id === notification.id)
        if (exists) return
        vm.notifications.unshift(notification)
        if ('Notification' in window && Notification.permission === 'granted') {
          try {
            new Notification('系统新通知', {
              body: notification.content || notification.title,
              icon: '/favicon.ico',
              tag: `notification-${notification.id}`
            })
          } catch (error) {
            console.error('显示桌面通知失败:', error)
          }
        }
      },
      notificationTitle: '系统新通知'
    })

    return {
      router,
      isStreamConnected,
      initNotificationStreamWithFetch,
      closeNotificationStream,
      reconnectStream,
      requestNotificationPermission
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
      showNotificationPanel: false,
      showAdminPanel: false,
      loading: false,
      notificationPanelTimer: null
    }
  },
  computed: {
    unreadCount() {
      return this.notifications.filter(n => !n.readFlag).length
    }
  },
  mounted() {
    const savedAvatar = localStorage.getItem('adminAvatar')
    if (savedAvatar) {
      this.avatarUrl = savedAvatar
    }
    this.fetchNotifications()
    this.requestNotificationPermission()
    this.initNotificationStreamWithFetch()
  },

  beforeUnmount() {
    // 关闭 SSE 实时通知流连接
    this.closeNotificationStream()
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
      this.showAdminPanel = true
    },
    hideAdminTable() {
      // 设置 500ms 后消失的定时器
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
      }
      this.hideTimer = setTimeout(() => {
        this.showAdminPanel = false
        this.hideTimer = null
      }, 500)
    },
    keepAdminTableVisible() {
      // 清除定时器
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
        this.hideTimer = null
      }
      this.showAdminPanel = true
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
    
    // 获取通知列表（与已有通知合并，保留 SSE 实时推送的数据）
    async fetchNotifications() {
      this.loading = true
      try {
        const res = await notificationAPI.getAdminNotifications()
        if (res.code === 200) {
          const apiNotifications = res.data || []
          // 合并：API 数据覆盖已有，新增的追加到尾部
          const merged = [...this.notifications]
          apiNotifications.forEach(apiNotif => {
            const index = merged.findIndex(n => n.id === apiNotif.id)
            if (index !== -1) {
              merged[index] = apiNotif
            } else {
              merged.push(apiNotif)
            }
          })
          // 按 createdAt 降序排列
          merged.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
          this.notifications = merged
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
        this.notifications = this.notifications.filter(n => n.id !== id)
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
        ElMessage.success('清空成功')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('清空失败')
        }
      }
    },
    
    // 加载更多：跳转到风险管理页面并定位到消息列表
    loadMore() {
      this.showNotificationPanel = false
      this.router.push({ path: '/dashboard/risk', query: { scrollTo: 'activities' } })
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
    },

  }
}
</script>

<style scoped>
.nav-bar {
  display: flex;
  justify-content: space-between;
  width: 100%;
  height: 75px;
  background: transparent;                    /* 露出 DashboardView 共享画布渐变 */
  color: var(--navbar-text-color);
  align-items: center;
}

.search-container {
  display: flex;
  align-items: center;
  margin-left: 24px;
}
.search-input {
  width: 320px;
}
.search-input :deep(.el-input__wrapper) {
  background: var(--navbar-search-bg);
  border: none;
  box-shadow: none;
  border-radius: 20px;
}
.search-input :deep(.el-input__inner) {
  color: var(--navbar-text-color);
}
.search-input :deep(.el-input__inner::placeholder) {
  color: var(--navbar-text-secondary);
}
.search-input :deep(.el-input__prefix) {
  color: var(--navbar-text-secondary);
}
.search-input :deep(.el-input__clear) {
  color: var(--navbar-text-secondary);
}
.search-input :deep(.el-input__clear:hover) {
  color: var(--navbar-text-color);
}

/* 右侧信息区域样式 */
.right-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-right: 40px;
  width: 220px;
}

/* 通知铃铛样式 */
.BellOutlined {
  font-size: 20px;
  margin-right: 5px;
}
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
  color: var(--navbar-bell-text);
  transition: all 0.3s ease;
}
.bell-icon:hover {
  color: var(--navbar-bell-hover-text);
  transform: scale(1.1);
}

/* 通知面板样式 — 毛玻璃化 */
.notification-panel {
  display: flex;
  flex-direction: column;

  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 10px;
  padding-bottom: 10px;

  width: 400px;
  max-height: 500px;

  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-lg, 20px);
  box-shadow: 0 20px 60px rgba(45, 27, 105, 0.25);

  z-index: 10000;
  overflow: hidden;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  padding: 11px 9px;

  border-bottom: 1px solid var(--notif-header-border);
  background-color: var(--notif-header-bg);
}
.panel-header span {
  font-size: 16px;
  font-weight: 600;
  color: var(--notif-header-text);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.connection-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  transition: all 0.3s ease;
}
.connection-dot.connected {
  background-color: var(--notif-dot-connected);
  box-shadow: 0 0 6px var(--notif-dot-connected-shadow);
}
.connection-dot.disconnected {
  background-color: var(--notif-dot-disconnected);
  box-shadow: 0 0 6px var(--notif-dot-disconnected-shadow);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.header-actions {
  display: flex;
  gap: 8px;
}
/* 头部按钮改为 link 风格：无边框 */
.header-actions :deep(.el-button) {
  background: transparent !important;
  border: none !important;
  padding: 2px 4px !important;
  font-size: 13px;
  color: var(--brand-500, #3B82F6);
  transition: padding 0.2s var(--ease-out-expo, ease), color 0.2s;
}
.header-actions :deep(.el-button:hover) {
  padding-left: 8px !important;
  color: var(--brand-accent, #25E0BF);
  background: transparent !important;
}
.header-actions :deep(.el-button.is-disabled) {
  color: var(--text-color-caption, #94a3b8) !important;
  background: transparent !important;
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
  padding: 5px 15px;
  border-bottom: 1px solid var(--notif-item-border);
  cursor: pointer;
  transition: all 0.3s ease;
}
.notification-item:hover {
  background-color: var(--notif-item-hover-bg);
}
.notification-item.unread {
  background: linear-gradient(90deg, rgba(37, 224, 191, 0.12), transparent 60%);
  border-left: 3px solid var(--brand-accent, #25E0BF);
}
.notif-left {
  flex: 1;
  margin-right: 15px;
}
.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--notif-title-color);
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
  color: var(--notif-content-color);
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
  color: var(--notif-time-color);
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
.theme-toggle {
  margin-right: 10px;
  border: none;
  background-color: var(--navbar-icon-hover-bg);
  color: var(--color-white);
}
.theme-toggle:hover {
  background-color: var(--navbar-icon-hover-bg-strong);
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
  color: var(--navbar-bell-text);
  transition: all 0.3s ease;
  padding: 4px;
  border-radius: 12px;
}
.theme-icon:hover {
  color: var(--navbar-bell-hover-text);
  background-color: var(--navbar-icon-hover-bg);
  transform: scale(1.15);
  box-shadow: 0 0 10px var(--navbar-icon-hover-shadow);
}
/* 太阳图标特殊样式 */
.theme-icon.sunny {
  font-size: 30px;
  color: var(--navbar-text-tertiary);
}
.theme-icon.sunny:hover {
  color: var(--navbar-bell-hover-text);
  background-color: var(--navbar-sunny-hover-bg);
  box-shadow: 0 0 12px var(--navbar-sunny-hover-shadow);
}
/* 月亮图标特殊样式 */
.theme-icon.moon {
  font-size: 30px;
  color: var(--navbar-text-tertiary);
}
.theme-icon.moon:hover {
  color: var(--navbar-moon-hover-text);
  background-color: var(--navbar-moon-hover-bg);
  box-shadow: 0 0 12px var(--navbar-moon-hover-shadow);
}

.breadcrumb-container {
  margin-left: 30px;
  display: flex;
  align-items: center;
}

.admin-info {
  display: flex;
  align-items: center;
  padding: 5px 10px;  
  justify-content: space-around;

  width: 148px;
  background-color: var(--navbar-admin-bg);
  border-radius: 12px;
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
  border: 2px solid var(--border-color-light);
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

/* 管理员菜单 — 毛玻璃 + 条目风 */
#admin-table {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 5px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  padding: var(--space-xs, 4px);
  border-radius: var(--radius-md, 12px);
  box-shadow: 0 20px 60px rgba(45, 27, 105, 0.25);
  min-width: 180px;
  z-index: 1000;
}

/* 条目菜单项 */
.admin-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: var(--radius-sm, 6px);
  cursor: pointer;
  color: var(--navbar-admin-text, #0f172a);
  font-size: 14px;
  transition: background 0.2s var(--ease-out-expo, ease), padding 0.2s var(--ease-out-expo, ease), color 0.2s;
}

.admin-menu-item:hover {
  background: rgba(59, 130, 246, 0.1);
  padding-left: 20px;
  color: var(--brand-500, #3B82F6);
}

.admin-menu-item--danger:hover {
  background: rgba(239, 68, 68, 0.08);
  color: var(--status-danger, #EF4444);
}

.admin-menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* Transition 进出场动画 */
.pop-enter-active,
.pop-leave-active {
  transition: opacity 0.2s var(--ease-out-expo, ease), transform 0.2s var(--ease-out-expo, ease);
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
