<template>
  <div class="container" :style="{ backgroundImage: `url(${bgImage})`,backgroundSize: 'cover' }">
    <div class="wrapper">
      <!-- 基础信息输入区 -->
      <div id="baseInformation" class="main" v-show="currentStep === 'base'">
        <h2>———Register———</h2>
        <div class="base-information">
          <div class="infor">
            <p>* 用户名(2-20位)</p>
            <span id="adminNameError" class="error-message"></span>
            <input
              type="text"
              id="adminName"
              v-model.trim="formData.adminName"
              @input="clearFieldError('adminName')"
            />
            <p>* 密码(8-20位，包含大小写字母、数字和特殊字符)</p>
            <span id="passwordError" class="error-message"></span>
            <div class="password-input">
              <input
                :type="showPassword ? 'text' : 'password'"
                id="password"
                v-model="formData.password"
                @input="clearFieldError('password')"
              />
              <div @click="togglePassword" class="showPassword-btn">
                <span v-if="showPassword" class="iconfont icon-browse"></span>
                <span v-else class="iconfont icon-hide"></span>
              </div>
            </div>

            <p>
                * 确认密码（请再次输入密码）
                <span id="confirmPasswordError" class="error-message"></span>
            </p>
            <div class="password-input">
                <input
                :type="showConfirmPassword ? 'text' : 'password'"
                id="confirmPassword"
                v-model="formData.confirmPassword"
                @input="clearFieldError('confirmPassword')"
                />
                <div @click="toggleConfirmPassword" class="showPassword-btn">
                    <span v-if="showConfirmPassword" class="iconfont icon-browse"></span>
                    <span v-else class="iconfont icon-hide"></span>
                </div>
            </div>
          </div>
        </div>
        <button type="button" class="confirm-btn btn" @click="handleConfirm">
          确认
        </button>
        <router-link to="/login" id="back-to-login">返回登录</router-link>
      </div>

      <!-- 验证信息输入区 -->
      <div id="authentication" class="main" v-show="currentStep === 'auth'">
        <h2>———Register———</h2>
        <div id="authentication-input">
          <p>* 手机号码</p>
          <input
            type="text"
            id="phone"
            v-model.trim="formData.phone"
            @input="clearFieldError('phone')"
          />
          <p id="phoneError" class="error-message"></p>

          <p>* 短信验证码</p>
          <div id="sms-code-input">
            <input
              type="text"
              id="smsCode"
              v-model="formData.smsCode"
              style="margin-left: 0;"
              @input="clearFieldError('smsCode')"
            />
            <button type="button" class="get-code-btn btn" @click="sendSmsCode">
              获取验证码
            </button>
          </div>
          <p id="smsCodeError" class="error-message"></p>

          <button type="button" class="register-btn" @click="handleSubmit" :disabled="loading">
              {{ loading ? '注册中...' : '注册' }}
          </button>
        </div>

        <div class="back-line">
            <span class="close-btn" @click="handleClose">返回</span>
        </div>

        <div id="loadingSpinner" class="loading" v-if="loading">注册中，请稍候...</div>
        <div id="successMessage"></div>
        <div id="networkError" style="color: red; text-align: center;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/index' 
import bgImage from '@/assets/images/background.png'

// 设置背景图（如果需要）
document.body.style.backgroundSize = 'cover'
document.body.style.margin = '0'

const router = useRouter()

// 步骤控制
const currentStep = ref('base') // 'base' | 'auth'

// 表单数据
const formData = reactive({
  adminName: '',
  password: '',
  confirmPassword: '',
  phone: '',
  smsCode: ''
})

// UI 状态
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// 切换步骤
const handleConfirm = () => {
  clearAllErrors()
  if (validateBaseForm()) {
    currentStep.value = 'auth'
  }
}

const handleClose = () => {
  currentStep.value = 'base'
}

// 切换密码可见性
const togglePassword = () => {
  showPassword.value = !showPassword.value
}
const toggleConfirmPassword = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

// 表单验证
const validateBaseForm = () => {
  let isValid = true

  // 用户名
  if (!formData.adminName) {
    showError('adminName', '请输入用户名')
    isValid = false
  } else if (formData.adminName.length < 2 || formData.adminName.length > 20) {
    showError('adminName', '用户名长度需为2-20位')
    isValid = false
  } else if (!/^[a-zA-Z0-9_一-龥]+$/.test(formData.adminName)) {
    showError('adminName', '用户名只能包含字母、数字、下划线和中文字符')
    isValid = false
  }

  // 密码
  if (!formData.password) {
    showError('password', '请输入密码')
    isValid = false
  } else if (formData.password.length < 8 || formData.password.length > 20) {
    showError('password', '密码长度需为8-20位')
    isValid = false
  } else if (!validatePasswordComplexity(formData.password)) {
    showError('password', '密码需包含大小写字母、数字和特殊字符')
    isValid = false
  }

  // 确认密码
  if (!formData.confirmPassword) {
    showError('confirmPassword', '请确认密码')
    isValid = false
  } else if (formData.password !== formData.confirmPassword) {
    showError('confirmPassword', '两次输入的密码不一致')
    isValid = false
  }

  return isValid
}
const validateAuthForm = () => {
  let isValid = true
  if (!formData.phone) {
    showError('phone', '请输入手机号码')
    isValid = false
  } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    showError('phone', '手机号码格式错误')
    isValid = false
  }
  if (!formData.smsCode) {
    showError('smsCode', '请输入短信验证码')
    isValid = false
  } else if (formData.smsCode.length !== 6) {
    showError('smsCode', '短信验证码格式错误')
    isValid = false
  }
  return isValid
}

// 密码复杂度验证
const validatePasswordComplexity = (password) => {
  const hasLower = /[a-z]/.test(password)
  const hasUpper = /[A-Z]/.test(password)
  const hasNumber = /\d/.test(password)
  // eslint-disable-next-line no-useless-escape
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"|,.<>?]/.test(password)
  return hasLower && hasUpper && hasNumber && hasSpecial
}

// 显示错误
const showError = (field, message) => {
  const el = document.getElementById(field + 'Error')
  if (el) {
    el.textContent = message
    el.style.opacity = '1'
  }
}

// 清除错误
const clearFieldError = (field) => {
  const el = document.getElementById(field + 'Error')
  if (el) {
    el.textContent = ''
    el.style.opacity = '0'
  }
}
const clearAllErrors = () => {
  ;['adminName', 'password', 'confirmPassword', 'phone', 'smsCode'].forEach(clearFieldError)
  const networkError = document.getElementById('networkError')
  if (networkError) networkError.style.display = 'none'
}

// 模拟发送验证码
const sendSmsCode = () => {
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    showError('phone', '手机号码格式错误')
    return
  }
  alert(`验证码已发送到 ${formData.phone}（模拟）`)
}

// 提交注册
const handleSubmit = async () => {
  clearAllErrors()
  if (!validateAuthForm()) return

  loading.value = true
  try {
    const result = await authAPI.register({
      name: formData.adminName,   // 对应后端要求的 "name"
      phone: formData.phone,
      password: formData.password
    })

    // 检查返回 code
    if (result.data.code !== 200) {
      throw new Error(result.data.message || '注册失败')
    }

    // 显示成功
    const successEl = document.getElementById('successMessage')
    if (successEl) {
      successEl.textContent = '注册成功！正在跳转到登录页...'
      successEl.style.opacity = '1'
    }

    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (error) {
    // 统一错误处理
    const msg = error.response?.data?.message ||
                error.message ||
                '网络错误，请稍后重试'

    const networkError = document.getElementById('networkError')
    if (networkError) {
      networkError.textContent = msg
      networkError.style.display = 'block'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
  @import '@/assets/css/registration.css';
</style>