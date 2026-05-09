import { createRouter, createWebHistory } from 'vue-router'

const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')

const DashboardView = () => import('@/views/DashboardView.vue')

const PendingApplicationView = () => import('@/views/application/PendingApplicationView.vue')
const CompletedApplicationView = () => import('@/views/application/CompletedApplicationView.vue')

const ProductManageView = () => import('@/views/product/ProductManageView.vue')
const AddProductView = () => import('@/views/product/AddProductView.vue')

const UserManageView = () => import('@/views/user/UserManageView.vue')
const BlackUserManageView = () => import('@/views/user/BlackUserManageView.vue')

const RiskManageView = () => import('@/views/risk/RiskManageView.vue')
const DVScreenView = () => import('@/views/risk/DVScreenView.vue')
const CollectManagementView = () => import('@/views/risk/CollectManagementView.vue')

const KnowledgeManagementView = () => import('@/views/system/KnowledgeManagementView.vue')
const ConversationLogsView = () => import('@/views/system/ConversationLogsView.vue')
const AgentConfigView = () => import('@/views/system/AgentConfigView.vue')
const MCPToolsView = () => import('@/views/system/MCPToolsView.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
    meta: { requiresAuth: false }
  },
  {
    path: '/dashboard',
    component: DashboardView,
    meta: { requiresAuth: true },
    children: [
      { path: 'pending-applications', name: 'Applications', component: PendingApplicationView },
      { path: 'completed-applications', name: 'CompletedApplications', component: CompletedApplicationView },

      { path: 'products', name: 'Products', component: ProductManageView },
      { path: 'add-pro', name: 'AddProduct', component: AddProductView },

      { path: 'users', name: 'Users', component: UserManageView },
      { path: 'black-users', name: 'BlackUsers', component: BlackUserManageView },

      { path: 'risk', name: 'Risk', component: RiskManageView },
      { path: 'collect-management', name: 'CollectManagement', component: CollectManagementView },

      { path: 'knowledge', name: 'Knowledge', component: KnowledgeManagementView },
      { path: 'conversation-logs', name: 'ConversationLogs', component: ConversationLogsView },
      { path: 'agent-config', name: 'AgentConfig', component: AgentConfigView },
      { path: 'mcp-tools', name: 'MCPTools', component: MCPToolsView },
      { path: 'dashboard-stats', redirect: { name: 'ConversationLogs' } },
    ]
  },

    { path: '/dv-screen', name: 'DVScreen', component: DVScreenView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

import { isTokenExpired } from '@/utils/jwt'
import { authAPI } from '@/api'

function checkAuthFromStorage() {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return false
  try {
    const data = JSON.parse(stored)
    if (!data.isAuthenticated || !data.token) return false
    return !isTokenExpired(data.token)
  } catch {
    return false
  }
}

function getRefreshTokenFromStorage() {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return null
  try {
    const data = JSON.parse(stored)
    return data.refreshToken || null
  } catch {
    return null
  }
}

function updateStoredToken(token, refreshToken) {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return
  try {
    const data = JSON.parse(stored)
    data.token = token
    data.refreshToken = refreshToken
    localStorage.setItem('auth-store', JSON.stringify(data))
  } catch {
    // 忽略解析错误
  }
}

router.beforeEach(async (to) => {
  const isAuthenticated = checkAuthFromStorage()

  if (isAuthenticated) {
    if (to.name === 'Login' || to.name === 'Register') {
      return '/dashboard/pending-applications'
    }
    return
  }

  if (!to.meta.requiresAuth) {
    return
  }

  const refreshToken = getRefreshTokenFromStorage()
  if (refreshToken) {
    try {
      const res = await authAPI.refreshToken(refreshToken)
      if (res.code === 200) {
        const { token, refreshToken: newRefreshToken } = res.data
        updateStoredToken(token, newRefreshToken)
        return
      }
    } catch {
      // 刷新失败，继续跳转登录页
    }
  }

  return '/login'
})

export default router
