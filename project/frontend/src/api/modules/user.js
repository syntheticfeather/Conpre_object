// src/api/modules/user.js
import request from '@/utils/request'

const userAPI = {
  // 获取用户统计信息
  getUserStats: () => request.get('/users/stats'),
  // 获取用户详情
  getUserDetail: (id) => request.get(`/users/${id}/detail`),
  // 获取黑名单列表
  getBlacklist: () => request.get('/users/blacklist/list'),
  // 添加到黑名单
  addToBlacklist: (blacklistData) => request.post('/users/blacklist/add', blacklistData),
  // 从黑名单移除
  removeFromBlacklist: (userId) =>
    request.post('/users/blacklist/remove', `userId=${userId}`, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    }),
  // 根据信誉分从高到低查询用户
  searchUsersByCredit: () => request.get('/users/search-by-credit'),
}

export default userAPI
