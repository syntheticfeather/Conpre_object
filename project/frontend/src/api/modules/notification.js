// src/api/modules/notification.js
import request from '@/utils/request'

const notificationAPI = {
  // 管理员获取所有通知
  getAdminNotifications: () => request.get('/notifications/admin'),
  
  // 标记通知为已读
  markAsRead: (id) => request.patch(`/notifications/${id}/read`),
  
  // 删除单个通知
  deleteNotification: (id) => request.delete(`/notifications/${id}`),
  
  // 批量删除通知
  batchDelete: (ids) => request.delete('/notifications/batch', { data: ids }),
  
  // 全部标记已读（可选，如果后端支持）
  markAllRead: () => request.patch('/notifications/admin/read-all'),
}

export default notificationAPI