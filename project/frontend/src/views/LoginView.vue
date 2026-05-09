<template>
  <div class="container" :style="{ backgroundImage: `url(${bgImage})`,backgroundSize: 'cover' }">
    <h1>管理员控制系统</h1>
    <div class="main wrapper">
      <h2>———Login———</h2>

      <!-- 登录方式切换 -->
      <div class="login-type">
        <button
          id="change-passwordLogin-btn"
          type="button"
          :class="{ active: loginType === 'password' }"
          @click="loginType = 'password'"
        >
          密码登录
        </button>
        <button
          id="change-smsLogin-btn"
          type="button"
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

        <div class="password-input">
          <input
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            autocomplete="off"
          />
          <div id="showPassword-btn" @click="togglePassword">
            <span v-if="showPassword" class="iconfont icon-browse"></span>
            <span v-else class="iconfont icon-hide"></span>
          </div>
        </div>
        <button id="passwordLogin-submit-btn" type="submit" :disabled="loading">
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
          :style="{margin:'12px'}"
        />

        <div class="sms-code-container">
          <input
            id="sms-code-input"
            v-model="formData.smsCode"
            type="text"
            placeholder="请输入验证码"
            autocomplete="off"
          />
          <button
            id="get-sms-btn"
            type="button"
            :disabled="isCounting || !canSendSms"
            @click="sendSmsCode"
          >
            {{ isCounting ? `${countDown}s 后重发` : '获取验证码' }}
          </button>
        </div>

        <button id="smsLogin-submit-btn" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 协议与操作 -->
      <label class="agreement">
        <input v-model="formData.agreed" type="checkbox" />
        同意并接受
        <a href="#" target="_blank">《服务条款》</a> 和
        <a href="#" target="_blank">《隐私政策》</a>
      </label>

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
import { ElMessage } from 'element-plus'
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
  if (!formData.phone) {
    ElMessage.warning('请输入手机号码')
    errors.phone = '请输入手机号码'
    return
  }
  if (!canSendSms.value) {
    ElMessage.warning('请输入正确的手机号码')
    errors.phone = '请输入正确的手机号码'
    return
  }

  ElMessage.success(`验证码已发送到 ${formData.phone}`)
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
  const errorMessages = []

  if (!formData.phone) {
    errors.phone = '请输入手机号码'
    errorMessages.push('请输入手机号码')
  } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    errors.phone = '手机号格式不正确，请输入11位有效手机号'
    errorMessages.push('手机号格式不正确，请输入11位有效手机号')
  }

  if (loginType.value === 'password') {
    if (!formData.password) {
      errors.password = '请输入密码'
      errorMessages.push('请输入密码')
    } else if (formData.password.length < 6) {
      errors.password = '密码长度不能少于6位'
      errorMessages.push('密码长度不能少于6位')
    }
  } else {
    if (!formData.smsCode) {
      errors.smsCode = '请输入验证码'
      errorMessages.push('请输入验证码')
    } else if (formData.smsCode.length !== 6) {
      errors.smsCode = '验证码应为6位数字'
      errorMessages.push('验证码应为6位数字')
    }
  }

  if (!formData.agreed) {
    errors.agree = '请同意服务条款和隐私政策'
    errorMessages.push('请先同意服务条款和隐私政策')
  }

  if (errorMessages.length > 0) {
    ElMessage({
      type: 'error',
      message: errorMessages.join('；'),
      duration: 3000,
      showClose: true
    })
    return false
  }

  return true
}

// 密码登录
const handlePasswordLogin = async () => {
  if (!validate()) return

  loading.value = true
  try {
    const res = await authAPI.loginByPassword(formData.phone, formData.password)
    
    if (res.code === 200) {
      authStore.setAuthInfo({
        token: res.data.token,
        refreshToken: res.data.refreshToken,
      })
      
      ElMessage.success('登录成功！正在跳转...')
      success.value = true
      setTimeout(() => router.push('/dashboard/pending-applications'), 1500)
    } else {
      ElMessage.error(res.message || '用户名或密码错误')
      errors.password = res.message || '用户名或密码错误'
    }
  } catch (err) {
    console.error('登录失败:', err)
    const errorMsg = err.response?.data?.message || '网络错误，请稍后重试'
    ElMessage.error(errorMsg)
    errors.password = errorMsg
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
    if (res.code === 200) {
      authStore.setAuthInfo({
        token: res.data.token,
        refreshToken: res.data.refreshToken,
      })
      ElMessage.success('登录成功！正在跳转...')
      success.value = true
      setTimeout(() => router.push('/dashboard/pending-applications'), 1500)
    } else {
      ElMessage.error(res.message || '验证码错误')
      errors.smsCode = res.message || '验证码错误'
    }
  } catch (err) {
    console.error('登录失败:', err)
    const errorMsg = err.message || '网络错误，请稍后重试'
    ElMessage.error(errorMsg)
    errors.password = errorMsg
  } finally {
    loading.value = false
  }
}

// 页面加载时检查是否已登录
onMounted(() => {
  if (authStore.isLoggedIn) {
    router.push('/dashboard/pending-applications')
  }
})
</script>

<style scoped>
@import '../assets/css/login.css';
</style>