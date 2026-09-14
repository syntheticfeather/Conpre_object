<template>
  <div class="auth-page">
    <!-- 背景装饰光晕 -->
    <!-- <div class="bg-glow bg-glow--1"></div>
    <div class="bg-glow bg-glow--2"></div> -->

    <div class="auth-card" :class="{ 'is-register': isRegister }">
      <!-- 表单层：登录(左) + 注册(右) -->
      <div class="forms-wrapper">
        <!-- 登录表单 -->
        <div class="form-block form-block--login">
          <div class="form-inner">
            <div class="form-header">
              <h2 class="form-title">账号登录</h2>
              <p class="form-subtitle">欢迎回来，请输入您的账号信息</p>
            </div>

            <!-- 登录方式切换 -->
            <div class="tab-switch">
              <button
                type="button"
                :class="{ active: loginType === 'password' }"
                @click="loginType = 'password'"
              >密码登录</button>
              <button
                type="button"
                :class="{ active: loginType === 'sms' }"
                @click="loginType = 'sms'"
              >验证码登录</button>
            </div>

            <!-- 密码登录 -->
            <form v-if="loginType === 'password'" class="auth-form" @submit.prevent="handlePasswordLogin">
              <div class="field">
                <input
                  v-model.trim="formData.phone"
                  type="text"
                  placeholder="请输入手机号码"
                  autocomplete="off"
                />
              </div>
              <div class="field field--with-suffix">
                <input
                  v-model="formData.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入密码"
                  autocomplete="off"
                />
                <span class="field-suffix" @click="showPassword = !showPassword">
                  <span class="iconfont" :class="showPassword ? 'icon-browse' : 'icon-hide'"></span>
                </span>
              </div>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{ loading ? '登录中...' : '登 录' }}
              </button>
            </form>

            <!-- 验证码登录 -->
            <form v-else class="auth-form" @submit.prevent="handleSmsLogin">
              <div class="field">
                <input
                  v-model.trim="formData.phone"
                  type="text"
                  placeholder="请输入手机号码"
                  autocomplete="off"
                />
              </div>
              <div class="field field--split">
                <input
                  v-model="formData.smsCode"
                  type="text"
                  placeholder="请输入验证码"
                  autocomplete="off"
                />
                <button
                  type="button"
                  class="btn-code"
                  :disabled="isCounting || !canSendSms"
                  @click="sendSmsCode"
                >{{ isCounting ? `${countDown}s` : '获取验证码' }}</button>
              </div>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{ loading ? '登录中...' : '登 录' }}
              </button>
            </form>

            <label class="agreement">
              <input v-model="formData.agreed" type="checkbox" />
              <span>同意并接受 <a href="#">《服务条款》</a> 和 <a href="#">《隐私政策》</a></span>
            </label>

            <div class="form-footer">
              <a href="#" class="link-muted">忘记密码？</a>
            </div>
          </div>
        </div>

        <!-- 注册表单 -->
        <div class="form-block form-block--register">
          <div class="form-inner">
            <div class="form-header">
              <h2 class="form-title">创建账号</h2>
              <p class="form-subtitle">
                {{ currentStep === 'base' ? '填写基础信息，开启您的旅程' : '完成手机验证即可注册' }}
              </p>
            </div>

            <!-- 第一步：基础信息 -->
            <form v-if="currentStep === 'base'" class="auth-form" @submit.prevent="handleConfirm">
              <div class="field">
                <input
                  v-model.trim="regForm.adminName"
                  type="text"
                  placeholder="用户名（2-20位）"
                  autocomplete="off"
                />
              </div>
              <div class="field field--with-suffix">
                <input
                  v-model="regForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="密码（8-20位，含大小写字母、数字、特殊字符）"
                  autocomplete="off"
                />
                <span class="field-suffix" @click="showPassword = !showPassword">
                  <span class="iconfont" :class="showPassword ? 'icon-browse' : 'icon-hide'"></span>
                </span>
              </div>
              <div class="field field--with-suffix">
                <input
                  v-model="regForm.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  placeholder="请再次输入密码"
                  autocomplete="off"
                />
                <span class="field-suffix" @click="showConfirmPassword = !showConfirmPassword">
                  <span class="iconfont" :class="showConfirmPassword ? 'icon-browse' : 'icon-hide'"></span>
                </span>
              </div>
              <button type="submit" class="btn-primary" :disabled="loading">下一步</button>
            </form>

            <!-- 第二步：手机验证 -->
            <form v-else class="auth-form" @submit.prevent="handleSubmit">
              <div class="field">
                <input
                  v-model.trim="regForm.phone"
                  type="text"
                  placeholder="请输入手机号码"
                  autocomplete="off"
                />
              </div>
              <div class="field field--split">
                <input
                  v-model="regForm.smsCode"
                  type="text"
                  placeholder="请输入验证码"
                  autocomplete="off"
                />
                <button
                  type="button"
                  class="btn-code"
                  :disabled="isCounting || !/^1[3-9]\d{9}$/.test(regForm.phone)"
                  @click="sendSmsCode"
                >{{ isCounting ? `${countDown}s` : '获取验证码' }}</button>
              </div>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{ loading ? '注册中...' : '注 册' }}
              </button>
              <button type="button" class="btn-ghost" @click="currentStep = 'base'">返回上一步</button>
            </form>
          </div>
        </div>
      </div>

      <!-- 滑动品牌遮罩层 -->
      <div class="overlay">
        <!-- 登录态展示（遮罩在右侧时） -->
        <div class="overlay-panel overlay-panel--login">
          <div class="brand-logo">
            <img src="@/assets/images/logo.png" alt="XIN Finance" class="brand-logo-img" />
          </div>
          <h1 class="brand-name">XIN Finance</h1>
          <p class="brand-desc">智能金融风控管理平台</p>
          <p class="brand-intro">已有账号？立即登录，<br/>体验高效安全的金融管理服务。</p>
          <button type="button" class="btn-outline" @click="goRegister">去注册</button>
        </div>

        <!-- 注册态展示（遮罩滑到左侧时） -->
        <div class="overlay-panel overlay-panel--register">
          <div class="brand-logo">
            <img src="@/assets/images/logo.png" alt="XIN Finance" class="brand-logo-img" />
          </div>
          <h1 class="brand-name">XIN Finance</h1>
          <p class="brand-desc">智能金融风控管理平台</p>
          <p class="brand-intro">还没有账号？<br/>加入我们，开启智能金融之旅。</p>
          <button type="button" class="btn-outline" @click="goLogin">去登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authAPI } from '@/api'
import { isTokenExpired } from '@/utils/jwt'

const router = useRouter()
const authStore = useAuthStore()

// 登录/注册 切换
const isRegister = ref(false)
const goRegister = () => { isRegister.value = true }
const goLogin = () => { isRegister.value = false }

// ===== 登录 =====
const loginType = ref('password')
const formData = reactive({ phone: '', password: '', smsCode: '', agreed: false })
const loading = ref(false)
const success = ref(false)
const showPassword = ref(false)
const countDown = ref(60)
const isCounting = ref(false)

const canSendSms = computed(() => /^1[3-9]\d{9}$/.test(formData.phone))

const startCountdown = () => {
  isCounting.value = true
  countDown.value = 60
  const timer = setInterval(() => {
    countDown.value--
    if (countDown.value <= 0) {
      clearInterval(timer)
      isCounting.value = false
    }
  }, 1000)
}

const sendSmsCode = () => {
  if (!canSendSms.value) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  ElMessage.success(`验证码已发送到 ${formData.phone}`)
  startCountdown()
}

const validateLogin = () => {
  if (!formData.phone) { ElMessage.error('请输入手机号码'); return false }
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) { ElMessage.error('手机号格式不正确'); return false }
  if (loginType.value === 'password') {
    if (!formData.password) { ElMessage.error('请输入密码'); return false }
    if (formData.password.length < 6) { ElMessage.error('密码长度不能少于6位'); return false }
  } else {
    if (!formData.smsCode) { ElMessage.error('请输入验证码'); return false }
    if (formData.smsCode.length !== 6) { ElMessage.error('验证码应为6位数字'); return false }
  }
  if (!formData.agreed) { ElMessage.error('请先同意服务条款和隐私政策'); return false }
  return true
}

const handlePasswordLogin = async () => {
  if (!validateLogin()) return
  loading.value = true
  try {
    const res = await authStore.loginByPassword(formData.phone, formData.password)
    if (res.success) {
      ElMessage.success('登录成功！正在跳转...')
      success.value = true
      setTimeout(() => router.push('/dashboard/pending-applications'), 1200)
    } else {
      ElMessage.error(res.message || '用户名或密码错误')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSmsLogin = async () => {
  if (!validateLogin()) return
  loading.value = true
  try {
    const res = await authStore.loginBySms(formData.phone, formData.smsCode)
    if (res.success) {
      ElMessage.success('登录成功！正在跳转...')
      success.value = true
      setTimeout(() => router.push('/dashboard/pending-applications'), 1200)
    } else {
      ElMessage.error(res.message || '验证码错误')
    }
  } catch (err) {
    ElMessage.error(err.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ===== 注册 =====
const currentStep = ref('base')
const regForm = reactive({ adminName: '', password: '', confirmPassword: '', phone: '', smsCode: '' })
const showConfirmPassword = ref(false)

const validatePasswordComplexity = (pwd) => {
  return /[a-z]/.test(pwd) && /[A-Z]/.test(pwd) && /\d/.test(pwd) && /[!@#$%^&*()_+\-=[\]{};':"|,.<>?]/.test(pwd)
}

const handleConfirm = () => {
  if (!regForm.adminName) { ElMessage.error('请输入用户名'); return }
  if (regForm.adminName.length < 2 || regForm.adminName.length > 20) { ElMessage.error('用户名长度需为2-20位'); return }
  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(regForm.adminName)) { ElMessage.error('用户名只能包含字母、数字、下划线和中文字符'); return }
  if (!regForm.password) { ElMessage.error('请输入密码'); return }
  if (regForm.password.length < 8 || regForm.password.length > 20) { ElMessage.error('密码长度需为8-20位'); return }
  if (!validatePasswordComplexity(regForm.password)) { ElMessage.error('密码需包含大小写字母、数字和特殊字符'); return }
  if (regForm.password !== regForm.confirmPassword) { ElMessage.error('两次输入的密码不一致'); return }
  currentStep.value = 'auth'
}

const handleSubmit = async () => {
  if (!/^1[3-9]\d{9}$/.test(regForm.phone)) { ElMessage.error('手机号码格式错误'); return }
  if (!regForm.smsCode || regForm.smsCode.length !== 6) { ElMessage.error('短信验证码应为6位数字'); return }
  loading.value = true
  try {
    const result = await authAPI.register({
      name: regForm.adminName,
      phone: regForm.phone,
      password: regForm.password
    })
    if (result.code !== 200) throw new Error(result.message || '注册失败')
    ElMessage.success('注册成功！正在跳转到登录页...')
    setTimeout(() => { isRegister.value = false; currentStep.value = 'base' }, 1500)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 满足自动获取登录 token 条件时，自动勾选条款并直接登录
  const stored = localStorage.getItem('auth-store')
  if (!stored) return

  let data
  try { data = JSON.parse(stored) } catch { return }

  // 情况1：token 仍有效 → 自动勾选条款，直接跳转
  if (data.token && !isTokenExpired(data.token)) {
    formData.agreed = true
    router.push('/dashboard/pending-applications')
    return
  }

  // 情况2：token 过期但 refreshToken 存在 → 自动勾选条款，尝试刷新后跳转
  if (data.refreshToken) {
    formData.agreed = true
    loading.value = true
    try {
      // 先恢复 store 状态，确保 refreshToken 可用
      authStore.setAuthInfo({
        token: data.token,
        refreshToken: data.refreshToken,
        user: data.userInfo
      })
      const result = await authStore.refreshToken()
      if (result.success) {
        ElMessage.success('登录已恢复，正在跳转...')
        await authStore.loadUserInfo()
        router.push('/dashboard/pending-applications')
      }
      // 刷新失败则留在登录页，authStore.logout() 已在 refreshToken 内处理
    } catch {
      // 静默处理，留在登录页
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.auth-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #1a1a3e 0%, #2D1B69 35%, #2563EB 75%, #25E0BF 120%);
}

/* 背景光晕装饰 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
}
.bg-glow--1 {
  width: 420px; height: 420px;
  background: #3B82F6;
  top: -120px; left: -100px;
}
.bg-glow--2 {
  width: 380px; height: 380px;
  background: #25E0BF;
  bottom: -100px; right: -80px;
}

.auth-card {
  position: relative;
  width: 960px;
  max-width: 50vw;
  height: 580px;
  max-height: 60vh;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.35),
              0 8px 24px rgba(15, 23, 42, 0.2);
  overflow: hidden;
}

/* ===== 表单层 ===== */
.forms-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
}

.form-block {
  flex: 0 0 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  box-sizing: border-box;
}

.form-inner {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 8px;
}
.form-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.5px;
}
.form-subtitle {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

/* 登录方式 Tab */
.tab-switch {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  border-bottom: 1px solid #e2e8f0;
}
.tab-switch button {
  position: relative;
  padding: 8px 2px;
  background: none;
  border: none;
  font-size: 15px;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.2s;
}
.tab-switch button.active {
  color: #2563EB;
  font-weight: 600;
}
.tab-switch button.active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -1px;
  height: 2px;
  background: #2563EB;
  border-radius: 2px;
}

/* 表单字段 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  position: relative;
}
.field input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  box-sizing: border-box;
  font-size: 14px;
  color: #0f172a;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.field input:focus {
  border-color: #3B82F6;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}
.field input::placeholder {
  color: #94a3b8;
}
.field--with-suffix .field-suffix {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  cursor: pointer;
  font-size: 18px;
  user-select: none;
}
.field--split {
  display: flex;
  gap: 10px;
}
.field--split input {
  flex: 1;
}

/* 按钮 */
.btn-primary {
  height: 48px;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, opacity 0.2s;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.4);
}
.btn-primary:active:not(:disabled) { transform: translateY(0); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-code {
  flex-shrink: 0;
  padding: 0 16px;
  height: 48px;
  font-size: 13px;
  color: #2563EB;
  background: #EFF6FF;
  border: 1.5px solid #BFDBFE;
  border-radius: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.btn-code:hover:not(:disabled) { background: #DBEAFE; }
.btn-code:disabled { color: #94a3b8; background: #f1f5f9; border-color: #e2e8f0; cursor: not-allowed; }

.btn-ghost {
  height: 44px;
  font-size: 14px;
  color: #64748b;
  background: transparent;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover { border-color: #cbd5e1; background: #f8fafc; }

/* 协议 & 底部链接 */
.agreement {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.agreement input {
  accent-color: #3B82F6;
  margin: 0;
}
.agreement a {
  color: #3B82F6;
  text-decoration: none;
}
.form-footer {
  margin-top: 16px;
  text-align: right;
}
.link-muted {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: none;
}
.link-muted:hover { color: #3B82F6; }

/* ===== 滑动遮罩层 ===== */
.overlay {
  position: absolute;
  top: 0;
  left: 50%;
  width: 50%;
  height: 100%;
  background: linear-gradient(135deg, #2D1B69 0%, #2563EB 60%, #3B82F6 100%);
  overflow: hidden;
  transition: transform 0.6s cubic-bezier(0.77, 0, 0.175, 1);
  z-index: 10;
}

/* 切换到注册态：遮罩滑到左侧 */
.auth-card.is-register .overlay {
  transform: translateX(-102%);
}

.overlay-panel {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  box-sizing: border-box;
  text-align: center;
  color: #fff;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.4s ease, visibility 0.4s ease;
}

/* 登录态：遮罩在右侧，展示登录品牌面板 */
.overlay-panel--login {
  opacity: 1;
  visibility: visible;
}
.auth-card.is-register .overlay-panel--login {
  opacity: 0;
  visibility: hidden;
}
/* 注册态：遮罩在左侧，展示注册品牌面板 */
.overlay-panel--register {
  opacity: 0;
  visibility: hidden;
}
.auth-card.is-register .overlay-panel--register {
  opacity: 1;
  visibility: visible;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  margin-bottom: 24px;
  border-radius: 24px;
  background: #1557b09e;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}
.brand-logo-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
}

.brand-name {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #fff 0%, #bfdbfe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 1px;
}

.brand-intro {
  margin: 0 0 36px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.btn-outline {
  padding: 12px 40px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: transparent;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s;
  letter-spacing: 2px;
}
.btn-outline:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: #fff;
  transform: translateY(-2px);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .auth-card { height: auto; min-height: 600px; }
  .overlay { display: none; }
  .form-block { flex: 0 0 100%; padding: 32px 24px; }
  .form-block--register { display: none; }
  .auth-card.is-register .form-block--login { display: none; }
  .auth-card.is-register .form-block--register { display: flex; }
}
</style>
