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
app.use(router)
app.use(ElementPlus)
app.use(Antd)

app.mount('#app')
