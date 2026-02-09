import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 视图组件
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

      { path: 'risk', name: 'Risk', component: RiskManageView }
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth !== false) {
    // 需要认证的路由
    if (!authStore.isAuthenticated) {
      next('/login')
    } else {
      next()
    }
  } else {
    // 不需要认证的路由
    if (authStore.isAuthenticated && to.name === 'Login') {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
