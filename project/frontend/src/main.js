// src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
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

app.use(pinia)
app.use(router)
app.use(ElementPlus) // 全局注册 ElementPlus 组件
app.use(Antd) // 全局注册 Ant Design Vue 组件

app.mount('#app')
