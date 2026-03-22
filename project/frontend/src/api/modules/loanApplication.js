// src/api/modules/loanApplication.js
import request from '@/utils/request'

const loanApplicationAPI = {
  // 获取任意用户的单个贷款申请详情
  getApplication: (applicationId) => request.get(`/loan-applications/${applicationId}`),
  // 获取指定用户的所有贷款申请
  getUserApplications: (userId) => request.get(`/loan-applications/user/${userId}`),
  // 获取当前用户的所有贷款申请
  getMyApplications: () => request.get('/loan-applications/my'),
  // 获取待审批列表（AI拒绝）
  getPendingApprovals: () => request.get('/approval/pending'),
  // 获取已完成审批列表（已通过、人工拒绝）
  getCompletedApprovals: () => request.get('/approval/completed'),
}

export default loanApplicationAPI