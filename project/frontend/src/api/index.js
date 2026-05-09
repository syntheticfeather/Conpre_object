// src/api/index.js
// API 模块导出入口
import authAPI from './modules/auth'
import loanAPI from './modules/loan'
import applicationAPI from './modules/application'
import userAPI from './modules/user'
import loanApplicationAPI from './modules/loanApplication'
import notificationAPI from './modules/notification'
import promptsAPI from './modules/prompts'

export { authAPI, loanAPI, applicationAPI, userAPI, loanApplicationAPI, notificationAPI, promptsAPI }
