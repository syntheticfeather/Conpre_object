// src/api/modules/notification.js
import request from '@/utils/request'

const notificationAPI = {
  // 管理员获取所有通知
  getAdminNotifications: () => request.get('/notifications/admin'),
}

export default notificationAPI