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

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleSendCode"
        >
          <el-form-item prop="account">
            <el-input v-model="form.account" placeholder="请输入绑定的邮箱地址" clearable>
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-button native-type="submit" type="primary" size="large" class="submit-btn" :loading="loading">
            {{ loading ? '发送中...' : '发送验证码' }}
          </el-button>
        </el-form>

        <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon class="msg-alert" />
        <el-alert v-if="successMessage" :title="successMessage" type="success" :closable="false" show-icon class="msg-alert" />

        <div class="back-link">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>

      <!-- 步骤2：输入验证码和新密码 -->
      <div v-if="step === 2" class="step-content">
        <div class="step-icon">🔐</div>
        <h3 class="step-title">重置密码</h3>
        <p class="step-desc">验证码已发送至 {{ maskedAccount }}</p>

        <el-form
          ref="resetFormRef"
          :model="resetForm"
          :rules="resetRules"
          size="large"
          @submit.prevent="handleResetPassword"
        >
          <el-form-item prop="verifyCode">
            <el-input v-model="resetForm.verifyCode" placeholder="请输入验证码" maxlength="6">
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/>
                  <path d="M7 11V7a5 5 0 1110 0v4"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="newPassword">
            <el-input
              v-model="resetForm.newPassword"
              type="password"
              placeholder="请输入新密码（至少6位）"
              show-password
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/>
                  <path d="M7 11V7a5 5 0 1110 0v4"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              show-password
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2"/>
                  <path d="M7 11V7a5 5 0 1110 0v4"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-button native-type="submit" type="primary" size="large" class="submit-btn" :loading="loading">
            {{ loading ? '重置中...' : '确认重置' }}
          </el-button>
        </el-form>

        <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon class="msg-alert" />

        <div class="resend-row">
          <el-button link type="primary" :disabled="countdown > 0" @click="handleResendCode">
            {{ countdown > 0 ? `${countdown}s后重新发送` : '重新发送验证码' }}
          </el-button>
        </div>
      </div>

      <!-- 步骤3：重置成功 -->
      <div v-if="step === 3" class="step-content success-step">
        <el-result icon="success" title="密码重置成功" sub-title="请使用新密码重新登录">
          <template #extra>
            <el-button type="primary" size="large" class="submit-btn" @click="goToLogin">去登录</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../services/api'

const router = useRouter()

const step = ref(1)
const account = ref('')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const countdown = ref(0)

let countdownTimer: ReturnType<typeof setInterval> | null = null

const formRef = ref()
const resetFormRef = ref()

const form = reactive({ account: account })
const resetForm = reactive({
  verifyCode: '',
  newPassword: '',
  confirmPassword: ''
})

const maskedAccount = computed(() => {
  if (!account.value) return ''
  if (account.value.includes('@')) {
    const [name, domain] = account.value.split('@')
    return name.slice(0, 2) + '***@' + domain
  }
  return account.value.slice(0, 3) + '****' + account.value.slice(-2)
})

const rules = {
  account: [
    { required: true, message: '请输入绑定的邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const resetRules = {
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (value !== resetForm.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

function goBack() {
  router.push('/login')
}

function goToLogin() {
  router.push('/login')
}

async function handleSendCode() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

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
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleResetPassword() {
  if (loading.value) return
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    loading.value = true
    errorMessage.value = ''

    await userApi.verifyAndResetPassword(account.value, resetForm.verifyCode, resetForm.newPassword)

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

.input-icon {
  width: 18px;
  height: 18px;
  color: #bbb;
  flex-shrink: 0;
}

.submit-btn {
  width: 100%;
  font-weight: 700;
  letter-spacing: 4px;
  border-radius: 14px;
  margin-top: 8px;
  --el-button-size: 48px;
}

.msg-alert {
  margin-top: 16px;
}

.back-link {
  margin-top: 28px;
  text-align: center;
}

.back-link a {
  color: var(--color-primary-500, #10b981);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}

.resend-row {
  margin-top: 20px;
  text-align: center;
}

.success-step {
  padding-top: 24px;
}
</style>
