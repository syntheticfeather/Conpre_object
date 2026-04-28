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
  // 获取待审核延期申请
  getPendingPostponeRequests: () => request.get('/approval/postpone/pending'),
  // 获取已审核延期申请
  getCompletedPostponeRequests: () => request.get('/approval/postpone/completed'),
  // 获取延期申请详情
  getPostponeDetail: (requestId) => request.get(`/approval/postpone/${requestId}`),
  // 审核通过延期申请
  approvePostpone: (requestId) => request.post(`/approval/postpone/${requestId}/approve`),
  // 审核拒绝延期申请
  rejectPostpone: (requestId, reason) => request.post(`/approval/postpone/${requestId}/reject`, reason),
}

export default applicationAPI