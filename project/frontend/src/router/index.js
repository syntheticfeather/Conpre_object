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
const CollectManagementView = () => import('@/views/risk/CollectManagementView.vue')

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
      { path: 'collect-management', name: 'CollectManagement', component: CollectManagementView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function checkAuthFromStorage() {
  const stored = localStorage.getItem('auth-store')
  if (!stored) return false
  try {
    const data = JSON.parse(stored)
    return !!(data.isAuthenticated && data.token)
  } catch {
    return false
  }
}

router.beforeEach((to) => {
  const isAuthenticated = checkAuthFromStorage()

  if (to.meta.requiresAuth && !isAuthenticated) {
    return '/login'
  }

  if ((to.name === 'Login' || to.name === 'Register') && isAuthenticated) {
    return '/dashboard/pending-applications'
  }
})

export default router
