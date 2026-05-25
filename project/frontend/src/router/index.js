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
  const stored = localStorage.getItem('auth-store')
  if (!stored) {
    // 没有存储的认证信息
    if (to.meta.requiresAuth) {
      console.log('[Router Guard] 无认证信息，需要登录')
      return '/login'
    }
    return
  }

  let authData
  try {
    authData = JSON.parse(stored)
  } catch (e) {
    console.error('[Router Guard] 解析认证信息失败:', e)
    localStorage.removeItem('auth-store')
    if (to.meta.requiresAuth) return '/login'
    return
  }

  // 如果token有效，直接放行
  if (authData.token && !isTokenExpired(authData.token)) {
    console.log('[Router Guard] Token有效，放行访问')
    if (to.name === 'Login' || to.name === 'Register') {
      return '/dashboard/pending-applications'
    }
    return
  }

  // token过期或无效，但目标页面不需要认证，直接放行
  if (!to.meta.requiresAuth) {
    return
  }

  // 需要认证但token已过期，尝试使用refreshToken刷新
  const refreshToken = authData.refreshToken
  if (!refreshToken) {
    console.log('[Router Guard] 无refreshToken，跳转登录页')
    localStorage.removeItem('auth-store')
    return '/login'
  }

  console.log('[Router Guard] Token已过期，尝试刷新...')
  try {
    const res = await authAPI.refreshToken(refreshToken)
    if (res.code === 200) {
      const { token, refreshToken: newRefreshToken } = res.data
      updateStoredToken(token, newRefreshToken)
      console.log('[Router Guard] Token刷新成功')
      // 刷新成功，放行访问
      return
    } else {
      console.warn('[Router Guard] Token刷新失败:', res.message)
      localStorage.removeItem('auth-store')
      return '/login'
    }
  } catch (error) {
    console.error('[Router Guard] Token刷新异常:', error)
    localStorage.removeItem('auth-store')
    return '/login'
  }
})

export default router
