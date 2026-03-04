// src/api/modules/loanApplication.js
import request from '@/utils/request'

const loanApplicationAPI = {
  // 获取任意用户的单个贷款申请详情
  getApplication: (applicationId) => request.get(`/api/loan-applications/${applicationId}`),
  // 获取指定用户的所有贷款申请
  getUserApplications: (userId) => request.get(`/api/loan-applications/user/${userId}`),
}

export default loanApplicationAPI