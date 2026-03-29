import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import Antd from 'ant-design-vue'

import './assets/css/base.css'
import 'element-plus/dist/index.css'
import 'ant-design-vue/dist/reset.css'
import './assets/iconfont/iconfont.css'

// 导入认证相关工具和store
import { useAuthStore } from './stores/auth'
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
  if (!stored) return
  
  try {
    const data = JSON.parse(stored)
    if (data.token && data.refreshToken) {
      // 检查 token 是否过期或即将过期
      const tokenExpired = isTokenExpired(data.token)
      const tokenAboutToExpire = isTokenAboutToExpire(data.token)
      
      if (tokenExpired || tokenAboutToExpire) {
        console.log('Token 过期或即将过期，尝试刷新...')
        // 刷新 token
        const result = await authStore.refreshToken()
        if (result.success) {
          console.log('Token 刷新成功')
        } else {
          console.log('Token 刷新失败，需要重新登录')
          authStore.logout()
        }
      } else {
        // token 有效，恢复认证状态
        authStore.setAuthInfo({
          token: data.token,
          refreshToken: data.refreshToken,
          user: data.userInfo
        })
        console.log('Token 有效，恢复认证状态')
      }
    }
  } catch (error) {
    console.error('检查认证状态失败:', error)
    // 清除无效的认证信息
    localStorage.removeItem('auth-store')
  }
}

// 检查认证状态
checkAuthStatus()

app.use(router)
app.use(ElementPlus)
app.use(Antd)

app.mount('#app')
