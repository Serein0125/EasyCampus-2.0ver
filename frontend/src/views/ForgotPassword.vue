<template>
  <div class="forgot-password-page">
    <div class="back-header">
      <button @click="goBack" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h2 class="page-title">找回密码</h2>
    </div>

    <div class="content-container">
      <!-- 步骤1：输入绑定邮箱 -->
      <div v-if="step === 1" class="step-content">
        <div class="step-icon">📧</div>
        <h3 class="step-title">验证身份</h3>
        <p class="step-desc">请输入注册时绑定的邮箱地址</p>

        <form @submit.prevent="handleSendCode" class="form-group">
          <div class="input-group">
            <div class="input-wrapper" :class="{ focused: inputFocused, filled: account }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                type="text"
                v-model="account"
                placeholder="请输入绑定的邮箱地址"
                @focus="inputFocused = true"
                @blur="inputFocused = false"
              />
            </div>
          </div>

          <button
            type="submit"
            :disabled="loading || !account"
            class="submit-btn"
            :class="{ active: account }"
          >
            {{ loading ? '发送中...' : '发送验证码' }}
          </button>
        </form>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>

        <div class="back-link">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>

      <!-- 步骤2：输入验证码和新密码 -->
      <div v-if="step === 2" class="step-content">
        <div class="step-icon">🔐</div>
        <h3 class="step-title">重置密码</h3>
        <p class="step-desc">验证码已发送至 {{ maskedAccount }}</p>

        <form @submit.prevent="handleResetPassword" class="form-group">
          <div class="input-group">
            <div class="input-wrapper" :class="{ focused: codeFocused, filled: verifyCode }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 1110 0v4"/>
              </svg>
              <input
                type="text"
                v-model="verifyCode"
                placeholder="请输入验证码"
                maxlength="6"
                @focus="codeFocused = true"
                @blur="codeFocused = false"
              />
            </div>
          </div>

          <div class="input-group">
            <div class="input-wrapper" :class="{ focused: newPwdFocused, filled: newPassword }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 1110 0v4"/>
              </svg>
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="newPassword"
                placeholder="请输入新密码（至少6位）"
                @focus="newPwdFocused = true"
                @blur="newPwdFocused = false"
              />
            </div>
          </div>

          <div class="input-group">
            <div class="input-wrapper" :class="{ focused: confirmFocused, filled: confirmPassword }">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 1110 0v4"/>
              </svg>
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="confirmPassword"
                placeholder="请确认新密码"
                @focus="confirmFocused = true"
                @blur="confirmFocused = false"
              />
            </div>
          </div>

          <button
            type="submit"
            :disabled="loading || !canSubmit"
            class="submit-btn"
            :class="{ active: canSubmit }"
          >
            {{ loading ? '重置中...' : '确认重置' }}
          </button>
        </form>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <div class="resend-row">
          <button @click="handleResendCode" class="resend-btn" :disabled="countdown > 0">
            {{ countdown > 0 ? `${countdown}s后重新发送` : '重新发送验证码' }}
          </button>
        </div>
      </div>

      <!-- 步骤3：重置成功 -->
      <div v-if="step === 3" class="step-content success-step">
        <div class="step-icon success-icon">✅</div>
        <h3 class="step-title">密码重置成功</h3>
        <p class="step-desc">请使用新密码重新登录</p>

        <button @click="goToLogin" class="submit-btn active">
          去登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../services/api'

const router = useRouter()

const step = ref(1)
const account = ref('')
const verifyCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showPassword = ref(false)
const countdown = ref(0)

let countdownTimer = null

const inputFocused = ref(false)
const codeFocused = ref(false)
const newPwdFocused = ref(false)
const confirmFocused = ref(false)

const maskedAccount = computed(() => {
  if (!account.value) return ''
  if (account.value.includes('@')) {
    const [name, domain] = account.value.split('@')
    return name.slice(0, 2) + '***@' + domain
  }
  return account.value.slice(0, 3) + '****' + account.value.slice(-2)
})

const canSubmit = computed(() => {
  return verifyCode.value.length === 6 &&
         newPassword.value.length >= 6 &&
         newPassword.value === confirmPassword.value
})

function goBack() {
  router.push('/login')
}

function goToLogin() {
  router.push('/login')
}

async function handleSendCode() {
  if (!account.value || loading.value) return

  try {
    loading.value = true
    errorMessage.value = ''
    successMessage.value = ''

    await userApi.sendResetCode(account.value)

    step.value = 2
    successMessage.value = '验证码已发送'
    startCountdown()
  } catch (error) {
    console.error('发送验证码失败:', error)
    errorMessage.value = error.message || '发送失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleResendCode() {
  if (countdown.value > 0) return

  try {
    loading.value = true
    errorMessage.value = ''

    await userApi.sendResetCode(account.value)
    successMessage.value = '验证码已重新发送'
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message || '发送失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleResetPassword() {
  if (!canSubmit.value || loading.value) return

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    await userApi.verifyAndResetPassword(account.value, verifyCode.value, newPassword.value)

    step.value = 3
  } catch (error) {
    console.error('重置密码失败:', error)
    errorMessage.value = error.message || '重置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.forgot-password-page {
  min-height: 100vh;
  background: linear-gradient(180deg, var(--color-primary-500, #10b981) 0%, var(--color-primary-400, #34d399) 35%, #f5f5f5 35%);
}

.back-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  color: white;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.back-btn:active {
  background-color: rgba(255, 255, 255, 0.15);
}

.back-btn svg {
  width: 22px;
  height: 22px;
}

.page-title {
  flex: 1;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  margin-right: 36px;
}

.content-container {
  background-color: #fff;
  border-radius: 28px 28px 0 0;
  padding: 40px 28px;
  min-height: calc(100vh - 68px);
}

.step-content {
  max-width: 400px;
  margin: 0 auto;
  text-align: center;
}

.step-icon {
  font-size: 56px;
  margin-bottom: 20px;
}

.success-icon {
  animation: popIn 0.4s ease;
}

@keyframes popIn {
  0% { transform: scale(0); }
  60% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.step-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px;
}

.step-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 32px;
}

.form-group {
  text-align: left;
}

.input-group {
  margin-bottom: 18px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background-color: #fafafa;
  border: 2px solid transparent;
  border-radius: 14px;
  transition: all 0.25s ease;
}

.input-wrapper.focused {
  background-color: #fff;
  border-color: var(--color-primary-500, #10b981);
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.08);
}

.input-icon {
  width: 22px;
  height: 22px;
  color: #bbb;
  flex-shrink: 0;
}

.input-wrapper input {
  flex: 1;
  border: none;
  background: none;
  font-size: 16px;
  color: #333;
  outline: none;
}

.input-wrapper input::placeholder {
  color: #ccc;
}

.submit-btn {
  width: 100%;
  padding: 15px;
  background-color: #e0e0e0;
  color: #999;
  border: none;
  border-radius: 14px;
  font-size: 17px;
  font-weight: 700;
  cursor: not-allowed;
  transition: all 0.3s ease;
  letter-spacing: 4px;
  margin-top: 8px;
}

.submit-btn.active {
  background: linear-gradient(135deg, var(--color-primary-500, #10b981) 0%, var(--color-primary-400, #34d399) 100%);
  color: white;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
}

.submit-btn.active:active {
  transform: scale(0.98);
}

.error-message {
  margin-top: 16px;
  padding: 12px;
  background-color: #FFF1F0;
  border: 1px solid #FFCCC7;
  border-radius: 10px;
  color: #FF4D4F;
  font-size: 13px;
  text-align: center;
}

.success-message {
  margin-top: 16px;
  padding: 12px;
  background-color: #F6FFED;
  border: 1px solid #B7EB8F;
  border-radius: 10px;
  color: #52C41A;
  font-size: 13px;
  text-align: center;
}

.back-link {
  margin-top: 28px;
  text-align: center;
}

.back-link a {
  color: var(--color-primary-500, #10b981);
  font-size: 14px;
  font-weight: 500;
}

.resend-row {
  margin-top: 20px;
  text-align: center;
}

.resend-btn {
  background: none;
  border: none;
  color: var(--color-primary-500, #10b981);
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
}

.resend-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.success-step .step-desc {
  margin-bottom: 40px;
}
</style>
