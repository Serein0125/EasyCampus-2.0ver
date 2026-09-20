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
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        size="large"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="自定义账号（4-20位）" maxlength="20" clearable>
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（2-12字）" maxlength="12" clearable>
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="校园邮箱（必填）" clearable>
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少6位）"
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
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            show-password
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="agreeTerms" class="agreement-item">
          <el-checkbox v-model="agreeTerms">
            <span class="agreement-text">我已阅读并同意</span>
            <a href="#" @click.prevent="showAgreement('user')">《用户协议》</a>
            <span class="agreement-text">和</span>
            <a href="#" @click.prevent="showAgreement('privacy')">《隐私政策》</a>
          </el-checkbox>
        </el-form-item>

        <el-button
          native-type="submit"
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
        >
          注 册
        </el-button>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="error-alert"
        />

        <p class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </p>
      </el-form>
    </main>

    <el-dialog
      v-model="agreementModal.visible"
      :title="agreementModal.title"
      width="420px"
      append-to-body
    >
      <div class="agreement-body">
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
      <template #footer>
        <el-button type="primary" @click="agreementModal.visible = false">我已知晓</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const agreementModal = reactive({
  visible: false,
  type: 'user',
  title: '用户协议'
})

function showAgreement(type: string) {
  agreementModal.type = type
  agreementModal.title = type === 'user' ? '用户协议' : '隐私政策'
  agreementModal.visible = true
}

const formRef = ref()
const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const errorMessage = ref('')
const agreeTerms = ref(false)

// 邮箱格式简单校验：必须包含 @ 和 .
function isValidEmail(email: string) {
  return email && email.includes('@') && email.includes('.')
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度4-20位', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 12, message: '昵称长度2-12字', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入校园邮箱', trigger: 'blur' },
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (!isValidEmail(value)) callback(new Error('邮箱格式不正确'))
      else callback()
    }, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (value !== form.password) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ],
  agreeTerms: [
    { validator: (_rule: unknown, _value: boolean, callback: (e?: Error) => void) => {
      if (!agreeTerms.value) callback(new Error('请阅读并同意用户协议和隐私政策'))
      else callback()
    }, trigger: 'change' }
  ]
}

async function handleRegister() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    loading.value = true
    errorMessage.value = ''

    const result = await authStore.register({
      username: form.username,
      nickname: form.nickname,
      email: form.email,
      password: form.password
    })

    if (result.success) {
      router.push({
        path: '/login',
        query: { registered: 'true', username: form.username }
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
  padding: var(--space-3) var(--space-4);
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
  border: none;
  background: none;
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
  padding: var(--space-10) var(--space-6) var(--space-8);
  max-width: 440px;
  margin: 0 auto;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.agreement-item {
  margin-bottom: var(--space-5);
}

.agreement-text {
  margin: 0 2px;
}

.agreement-item a {
  color: var(--color-primary-600);
  font-weight: var(--font-medium);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.agreement-item a:hover {
  color: var(--color-primary-700);
}

/* 未勾选协议时：勾选框描红，强化"需要操作"的视觉指向 */
.agreement-item.is-error :deep(.el-checkbox__inner) {
  border-color: var(--color-error);
}

/* 错误提示改为独立成行的告警条：占布局空间（不再与注册按钮重叠）且更醒目 */
.agreement-item :deep(.el-form-item__error) {
  position: static;
  flex: 0 0 100%;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  line-height: 1.5;
  color: var(--color-error);
  background: rgba(239, 68, 68, 0.08);
  border-left: 3px solid var(--color-error);
  border-radius: var(--radius-sm);
}

.agreement-item :deep(.el-form-item__error)::before {
  content: '';
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ef4444' stroke-width='2.5' stroke-linecap='round'%3E%3Ccircle cx='12' cy='12' r='10'/%3E%3Cline x1='12' y1='8' x2='12' y2='12'/%3E%3Cline x1='12' y1='16' x2='12.01' y2='16'/%3E%3C/svg%3E") center / contain no-repeat;
}

.submit-btn {
  width: 100%;
  font-weight: var(--font-bold);
  letter-spacing: 4px;
  border-radius: var(--radius-xl);
  font-size: var(--text-lg);
  min-height: 48px;
}

.error-alert {
  margin-top: var(--space-4);
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

.agreement-body {
  max-height: 50vh;
  overflow-y: auto;
  font-size: 14px;
  color: var(--color-text-secondary, #4b5563);
  line-height: 1.8;
}

.agreement-body p {
  margin: 0 0 8px;
}
</style>
