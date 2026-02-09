<template>
  <div class="container" :style="{ backgroundImage: `url(${bgImage})`,backgroundSize: 'cover' }">
    <h1>借贷APP管理员控制系统</h1>
    <div class="main wrapper">
      <h2>———Login———</h2>

      <!-- 登录方式切换 -->
      <div class="login-type">
        <button
          type="button"
          id="change-passwordLogin-btn"
          :class="{ active: loginType === 'password' }"
          @click="loginType = 'password'"
        >
          密码登录
        </button>
        <button
          type="button"
          id="change-smsLogin-btn"
          :class="{ active: loginType === 'sms' }"
          @click="loginType = 'sms'"
        >
          验证码登录
        </button>
      </div>

      <!-- 密码登录表单 -->
      <form v-if="loginType === 'password'" @submit.prevent="handlePasswordLogin">
        <input
          v-model.trim="formData.phone"
          type="text"
          placeholder="请输入手机号码"
          autocomplete="off"
        />
        <div class="error-message" :style="{ opacity: errors.phone ? 1 : 0 }">
          {{ errors.phone }}
        </div>

        <div class="password-input">
          <input
            :type="showPassword ? 'text' : 'password'"
            v-model="formData.password"
            placeholder="请输入密码"
            autocomplete="off"
          />
          <div @click="togglePassword" id="showPassword-btn">
            <span v-if="showPassword" class="iconfont icon-browse"></span>
            <span v-else class="iconfont icon-hide"></span>
          </div>
        </div>
        <div class="error-message" :style="{ opacity: errors.password ? 1 : 0 }">
          {{ errors.password }}
        </div>

        <button type="submit" :disabled="loading" id="passwordLogin-submit-btn">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 验证码登录表单 -->
      <form v-else @submit.prevent="handleSmsLogin">
        <input
          v-model.trim="formData.phone"
          type="text"
          placeholder="请输入手机号码"
          autocomplete="off"
        />
        <div class="error-message" :style="{ opacity: errors.phone ? 1 : 0 }">
          {{ errors.phone }}
        </div>

        <div class="sms-code-containert">
          <input
            v-model="formData.smsCode"
            id="sms-code-input"
            type="text"
            placeholder="请输入验证码"
            autocomplete="off"
          />
          <button
            type="button"
            :disabled="isCounting || !canSendSms"
            @click="sendSmsCode"
            id="get-sms-btn"
          >
            {{ isCounting ? `${countDown}s 后重发` : '获取验证码' }}
          </button>
        </div>
        <div class="error-message" :style="{ opacity: errors.smsCode ? 1 : 0 }">
          {{ errors.smsCode }}
        </div>

        <button type="submit" :disabled="loading" id="smsLogin-submit-btn">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 协议与操作 -->
      <label class="agreement">
        <input type="checkbox" v-model="formData.agreed" />
        同意并接受
        <a href="#" target="_blank">《服务条款》</a> 和
        <a href="#" target="_blank">《隐私政策》</a>
      </label>
      <div class="error-message" :style="{ opacity: errors.agree ? 1 : 0 }">
        {{ errors.agree }}
      </div>

      <div class="other">
        <span>还没有账号？<router-link to="/register">注册</router-link></span>
        <a href="#" class="forget-password">忘记密码?</a>
      </div>

      <!-- 成功提示 -->
      <div v-if="success" class="success-message">
        登录成功！正在跳转...
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authAPI } from '@/api/index'
import bgImage from '@/assets/images/background.png'

const router = useRouter()
const authStore = useAuthStore()

// 登录类型：'password' | 'sms'
const loginType = ref('password')

// 表单数据
const formData = reactive({
  phone: '',
  password: '',
  smsCode: '',
  agreed: false
})

// 错误信息
const errors = reactive({
  phone: '',
  password: '',
  smsCode: '',
  agree: ''
})

// UI 状态
const loading = ref(false)
const success = ref(false)
const showPassword = ref(false)

// 验证码倒计时
const countDown = ref(60)
const isCounting = ref(false)

// 计算属性：是否可以发送验证码（手机号合法）
const canSendSms = computed(() => {
  return /^1[3-9]\d{9}$/.test(formData.phone)
})

// 切换密码可见性
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

// 发送验证码（模拟）
const sendSmsCode = () => {
  if (!canSendSms.value) {
    errors.phone = '请输入正确的手机号码'
    return
  }

  // 实际项目：调用 API 发送验证码
  console.log('发送验证码到:', formData.phone)
  isCounting.value = true
  countDown.value = 60

  const timer = setInterval(() => {
    if (countDown.value > 0) {
      countDown.value--
    } else {
      clearInterval(timer)
      isCounting.value = false
    }
  }, 1000)
}

// 表单验证
const validate = () => {
  errors.phone = errors.password = errors.smsCode = errors.agree = ''

  if (!formData.phone) {
    errors.phone = '请输入手机号码'
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    errors.phone = '手机号格式不正确'
    return false
  }

  if (loginType.value === 'password') {
    if (!formData.password) {
      errors.password = '请输入密码'
      return false
    }
  } else {
    if (!formData.smsCode) {
      errors.smsCode = '请输入验证码'
      return false
    }
  }

  if (!formData.agreed) {
    errors.agree = '请同意服务条款和隐私政策'
    return false
  }

  return true
}

// 密码登录
const handlePasswordLogin = async () => {
  if (!validate()) return

  loading.value = true
  try {
    // 调用 API 登录
    const res = await authAPI.loginByPassword(formData.phone, formData.password)
    
    if (res.data.code === 200) {
      // 保存认证信息
      authStore.setAuthInfo({
        token: res.data.data.token,
        user: res.data.data.user
      })
      
      success.value = true
      setTimeout(() => router.push('/dashboard'), 1500)
    } else {
      errors.password = res.data.message || '用户名或密码错误'
    }
  } catch (err) {
    console.error('登录失败:', err)
    errors.password = err.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 验证码登录
const handleSmsLogin = async () => {
  if (!validate()) return

  loading.value = true
  try {
    const res = await authAPI.loginBySms(formData.phone, formData.smsCode)
    if (res.data.code === 200) {
      authStore.setToken(res.data.data.token, formData.phone)
      success.value = true
      setTimeout(() => router.push('/dashboard'), 1500)
    } else {
      errors.smsCode = res.data.message || '验证码错误'
    }
  } catch (err) {
    console.error('登录失败:', err)
    errors.password = err.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 页面加载时检查是否已登录
onMounted(() => {
  if (authStore.isLoggedIn) {
    router.push('/dashboard')
  }
})
</script>

<style scoped>
@import '../assets/css/login.css';
</style>