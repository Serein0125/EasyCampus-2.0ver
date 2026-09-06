<template>
  <div class="register-page">
    <!-- 顶部导航 -->
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">注册账号</h1>
      <div style="width: 36px;"></div>
    </header>

    <!-- 注册表单 -->
    <main class="form-container">
      <form @submit.prevent="handleRegister" class="register-form">
        <div class="input-group">
          <div class="input-wrapper" :class="{ focused: focusState.username }">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <input
              type="text"
              v-model="form.username"
              placeholder="自定义账号（4-20位）"
              @focus="focusState.username = true"
              @blur="focusState.username = false"
            />
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ focused: focusState.nickname }">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
              <line x1="1" y1="1" x2="23" y2="23"/>
            </svg>
            <input
              type="text"
              v-model="form.nickname"
              placeholder="昵称（2-12字）"
              maxlength="12"
              @focus="focusState.nickname = true"
              @blur="focusState.nickname = false"
            />
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ focused: focusState.email }">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            <input
              type="email"
              v-model="form.email"
              placeholder="校园邮箱（必填）"
              @focus="focusState.email = true"
              @blur="focusState.email = false"
            />
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ focused: focusState.password }">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2"/>
              <path d="M7 11V7a5 5 0 1110 0v4"/>
            </svg>
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="form.password"
              placeholder="密码（至少6位）"
              @focus="focusState.password = true"
              @blur="focusState.password = false"
            />
            <button type="button" @click="showPassword = !showPassword" class="toggle-pwd">
              <svg v-if="!showPassword" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="input-group">
          <div class="input-wrapper" :class="{ focused: focusState.confirmPwd }">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <input
              :type="showConfirm ? 'text' : 'password'"
              v-model="form.confirmPassword"
              placeholder="确认密码"
              @focus="focusState.confirmPwd = true"
              @blur="focusState.confirmPwd = false"
            />
          </div>
        </div>

        <label class="agreement-check">
          <input type="checkbox" v-model="agreeTerms" />
          <span>我已阅读并同意</span>
          <a href="#" @click.prevent="showAgreement('user')">《用户协议》</a>
          <span>和</span>
          <a href="#" @click.prevent="showAgreement('privacy')">《隐私政策》</a>
        </label>

        <button
          type="submit"
          :disabled="loading || !isFormValid"
          class="submit-btn"
          :class="{ active: isFormValid }"
        >
          {{ loading ? '注册中...' : '注 册' }}
        </button>

        <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>

        <p class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </p>
      </form>
    </main>

    <transition name="modal-fade">
      <div v-if="agreementModal.visible" class="modal-overlay" @click.self="agreementModal.visible = false">
        <div class="modal-box">
          <h3 class="modal-title">{{ agreementModal.title }}</h3>
          <div class="modal-body">
            <template v-if="agreementModal.type === 'user'">
              <p>欢迎使用易校EasyCampus！在使用本平台前，请您仔细阅读以下条款：</p>
              <p>1. 用户应提供真实、准确的注册信息，并对账号安全负责。</p>
              <p>2. 用户发布的内容应合法合规，不得发布违禁品、虚假信息或侵犯他人权益的内容。</p>
              <p>3. 交易双方应诚信交易，平台仅提供信息发布服务，不参与实际交易。</p>
              <p>4. 用户不得利用平台进行任何违法违规活动。</p>
              <p>5. 平台有权对违规用户进行警告、限制功能或封禁账号等处理。</p>
              <p>6. 用户应妥善保管账号密码，因个人原因导致账号泄露的，平台不承担责任。</p>
              <p>7. 本协议的解释权归易校EasyCampus平台所有。</p>
            </template>
            <template v-else>
              <p>我们重视您的隐私保护，本隐私政策说明我们如何收集、使用和保护您的信息：</p>
              <p>1. 我们收集的信息包括：注册信息（学号、昵称）、交易记录、浏览行为等。</p>
              <p>2. 我们使用收集的信息用于：提供平台服务、改善用户体验、推送相关内容。</p>
              <p>3. 未经您的同意，我们不会向第三方披露您的个人信息，法律法规另有规定的除外。</p>
              <p>4. 我们采取合理的技术措施保护您的信息安全。</p>
              <p>5. 您有权查看、更正或删除您的个人信息。</p>
              <p>6. 本政策可能会不时更新，更新后将在平台公示。</p>
            </template>
          </div>
          <button class="modal-close-btn" @click="agreementModal.visible = false">我已知晓</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const agreementModal = reactive({
  visible: false,
  type: 'user',
  title: '用户协议'
})

function showAgreement(type) {
  agreementModal.type = type
  agreementModal.title = type === 'user' ? '用户协议' : '隐私政策'
  agreementModal.visible = true
}

const form = ref({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const agreeTerms = ref(false)

const focusState = ref({
  username: false,
  nickname: false,
  email: false,
  password: false,
  confirmPwd: false
})

// 邮箱格式简单校验：必须包含 @ 和 .
function isValidEmail(email) {
  return email && email.includes('@') && email.includes('.')
}

const isFormValid = computed(() => {
  return (
    form.value.username.trim().length >= 4 &&
    form.value.nickname.trim().length >= 2 &&
    isValidEmail(form.value.email) &&
    form.value.password.length >= 6 &&
    form.value.password === form.value.confirmPassword &&
    agreeTerms.value
  )
})

async function handleRegister() {
  if (!isFormValid.value || loading.value) return

  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    const result = await authStore.register({
      username: form.value.username,
      nickname: form.value.nickname,
      email: form.value.email,
      password: form.value.password
    })

    if (result.success) {
      router.push({
        path: '/login',
        query: { registered: 'true', username: form.value.username }
      })
    } else {
      errorMessage.value = result.message || '注册失败，请稍后重试'
    }
  } catch (error) {
    console.error('注册失败:', error)

    if (error.code === 'NETWORK_ERROR' || error.code === 'TIMEOUT') {
      errorMessage.value = error.message || '网络错误，请稍后重试'
    } else if (error.status === 400) {
      errorMessage.value = error.message || '注册信息有误，请检查输入'
    } else if (error.status === 409) {
      errorMessage.value = '该用户名已被注册'
    } else {
      errorMessage.value = error.message || '注册失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3_5) var(--space-4);
  background-color: var(--color-bg-primary);
  box-shadow: var(--shadow-sm);
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  border-radius: var(--radius-full);
  transition: all var(--duration-normal) var(--ease-out);
}

.back-btn:hover {
  background-color: var(--color-primary-50);
  color: var(--color-primary-600);
}

.back-btn svg {
  width: 22px;
  height: 22px;
}

.header-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.form-container {
  padding: var(--space-8) var(--space-6);
  max-width: 420px;
  margin: 0 auto;
}

.input-group {
  margin-bottom: var(--space-4_5);
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-secondary);
  border: 2px solid transparent;
  border-radius: var(--radius-xl);
  transition: all var(--duration-normal) var(--ease-out);
}

.input-wrapper.focused,
.input-wrapper:focus-within {
  background-color: var(--color-bg-primary);
  border-color: var(--color-primary-400);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.input-icon {
  width: 20px;
  height: 20px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
  transition: color var(--duration-normal) var(--ease-out);
}

.input-wrapper.focused .input-icon {
  color: var(--color-primary-500);
}

.input-wrapper input {
  flex: 1;
  border: none;
  background: none;
  font-size: var(--text-base);
  color: var(--color-text-primary);
  outline: none;
  font-family: var(--font-sans);
}

.input-wrapper input::placeholder {
  color: var(--color-text-tertiary);
}

.toggle-pwd {
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color var(--duration-normal) var(--ease-out);
}

.toggle-pwd:hover {
  color: var(--color-primary-500);
}

.agreement-check {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-5_5);
  cursor: pointer;
  line-height: var(--leading-relaxed);
}

.agreement-check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary-500);
  margin-top: 2px;
}

.agreement-check a {
  color: var(--color-primary-600);
  font-weight: var(--font-medium);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.agreement-check a:hover {
  color: var(--color-primary-700);
}

.submit-btn {
  width: 100%;
  padding: var(--space-3_5);
  background-color: var(--color-gray-200);
  color: var(--color-text-tertiary);
  border: none;
  border-radius: var(--radius-xl);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  cursor: not-allowed;
  transition: all var(--duration-slow) var(--ease-out);
  letter-spacing: 4px;
  font-family: var(--font-sans);
}

.submit-btn.active {
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  cursor: pointer;
  box-shadow: var(--shadow-green);
}

.submit-btn.active:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-lg);
}

.submit-btn.active:active {
  transform: translateY(0) scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.7;
}

.error-msg {
  margin-top: var(--space-3_5);
  padding: var(--space-2_5);
  background-color: var(--color-rose-50);
  border: 1px solid var(--color-rose-100);
  border-radius: var(--radius-md);
  color: var(--color-rose-500);
  font-size: var(--text-sm);
  text-align: center;
}

.login-link {
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.login-link a {
  color: var(--color-primary-600);
  font-weight: var(--font-semibold);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.login-link a:hover {
  color: var(--color-primary-700);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  padding: 16px;
}

.modal-box {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 380px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
  margin: 0 0 16px;
  text-align: center;
}

.modal-body {
  max-height: 50vh;
  overflow-y: auto;
  font-size: 14px;
  color: var(--color-text-secondary, #4b5563);
  line-height: 1.8;
  margin-bottom: 20px;
}

.modal-body p {
  margin: 0 0 8px;
}

.modal-close-btn {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-600, #059669));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s;
}

.modal-close-btn:active {
  transform: scale(0.96);
}

.modal-fade-enter-active {
  transition: opacity 0.2s ease;
}

.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
