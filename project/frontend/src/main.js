import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import Antd from 'ant-design-vue'
import DataVVue3 from '@kjgl77/datav-vue3'

import './assets/css/base.css'
import 'element-plus/dist/index.css'
import 'ant-design-vue/dist/reset.css'
import './assets/iconfont/iconfont.css'

// 导入认证相关工具和 store
import { useAuthStore } from './stores/auth'
import { useAppStore } from './stores/app'
import { isTokenExpired, isTokenAboutToExpire } from './utils/jwt'

const app = createApp(App)
const pinia = createPinia()

pinia.use(piniaPluginPersistedstate)

pinia.use(({ store }) => {
  store.$onAction(({ name, args, after, onError }) => {
    const startTime = Date.now()
    console.log(`[Store Action] ${store.$id}/${name}`, args)

    after((result) => {
      const duration = Date.now() - startTime
      console.log(`[Store Action Success] ${store.$id}/${name} (${duration}ms)`, result)
    })

    onError((error) => {
      const duration = Date.now() - startTime
      console.error(`[Store Action Error] ${store.$id}/${name} (${duration}ms)`, error)
    })
  })

  store.$subscribe((mutation) => {
    console.log(`[Store State Change] ${store.$id}`, {
      type: mutation.type,
      storeId: mutation.storeId,
      payload: mutation.payload
    })
  })
})

app.use(pinia)

// 应用启动时检查认证状态
async function checkAuthStatus() {
  const authStore = useAuthStore()

  // 从 localStorage 中获取存储的认证信息
  const stored = localStorage.getItem('auth-store')
  if (!stored) return false

  try {
    const data = JSON.parse(stored)
    if (data.token && data.refreshToken) {
      // 检查 token 是否过期或即将过期
      const tokenExpired = isTokenExpired(data.token)
      const tokenAboutToExpire = isTokenAboutToExpire(data.token)

      if (tokenExpired || tokenAboutToExpire) {
        console.log('Token 过期或即将过期，尝试刷新...')
        // 先恢复store状态，确保refreshToken可用
        authStore.setAuthInfo({
          token: data.token,
          refreshToken: data.refreshToken,
          user: data.userInfo
        })
        // 刷新 token
        const result = await authStore.refreshToken()
        if (result.success) {
          console.log('Token 刷新成功')
          return true
        } else {
          console.log('Token 刷新失败，需要重新登录')
          authStore.logout()
          return false
        }
      } else {
        // token 有效，恢复认证状态
        authStore.setAuthInfo({
          token: data.token,
          refreshToken: data.refreshToken,
          user: data.userInfo
        })
        console.log('Token 有效，恢复认证状态')
        return true
      }
    }
    return false
  } catch (error) {
    console.error('检查认证状态失败:', error)
    // 清除无效的认证信息
    localStorage.removeItem('auth-store')
    return false
  }
}

// 等待认证检查完成后再挂载应用
async function initApp() {
  await checkAuthStatus()

  // 初始化主题
  const appStore = useAppStore()
  appStore.initTheme()

  app.use(router)
  app.use(ElementPlus)
  app.use(Antd)
  app.use(DataVVue3)

  app.mount('#app')
}

// 启动应用
initApp()
