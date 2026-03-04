// src/api/modules/application.js
import request from '@/utils/request'

const applicationAPI = {
  // 获取待审核申请
  getPendingApplications: () => request.get('/approval/pending'),
  // 获取已完成审核申请
  getCompletedApplications: () => request.get('/approval/completed'),
  // 获取申请详情
  getApplicationDetail: (id) => request.get(`/approval/detail/${id}`),
  // 提交审核结果
  submitReview: (reviewData) => request.post('/approval/check', reviewData),
}

export default applicationAPI