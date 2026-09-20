<template>
  <div class="login-page">
    <!-- 左侧品牌区：极光 + 噪点 + 集市芯片层 + 粒子场 -->
    <LoginBrandPanel />

    <!-- 右侧表单区：作为卡片柔光的指针监听区域 -->
    <main ref="formSideRef" class="login-form-side">
      <!-- .form-stage 是 CSS 变量的写入目标，同时也是坐标换算基准 -->
      <div ref="stageRef" class="form-stage">
        <!-- 环外环境光：pre-rendered 渐变层，仅靠 translate3d 位移，走合成器不触发重绘 -->
        <div class="card-glow" aria-hidden="true"></div>

        <div class="form-card">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="login-form"
            size="large"
            @submit.prevent="handleLogin"
          >
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-desc">登录你的账号，继续探索校园好物</p>

            <el-form-item prop="username" class="form-field">
              <el-input
                v-model="form.username"
                placeholder="学号/手机号/邮箱"
                clearable
                autocomplete="username"
              >
                <template #prefix>
                  <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password" class="form-field">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                autocomplete="current-password"
              >
                <template #prefix>
                  <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2"/>
                    <path d="M7 11V7a5 5 0 1110 0v4"/>
                  </svg>
                </template>
              </el-input>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <router-link to="/forgot-password" class="forgot-link">忘记密码?</router-link>
            </div>

            <el-button
              native-type="submit"
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
            >
              登 录
            </el-button>

            <el-alert
              v-if="errorMessage"
              :title="errorMessage"
              type="error"
              :closable="false"
              show-icon
              class="error-alert"
            />
          </el-form>
        </div>
      </div>

      <p class="register-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import LoginBrandPanel from '../components/LoginBrandPanel.vue'
import { useEdgeGlow } from '../use/useEdgeGlow'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref()
const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref('')
const rememberMe = ref(false)

// 右侧表单区：作为卡片柔光的指针监听区域
const formSideRef = ref<HTMLElement | null>(null)
// .form-stage：柔光 CSS 变量的写入目标与坐标换算基准
const stageRef = ref<HTMLElement | null>(null)

useEdgeGlow({ surfaceRef: formSideRef, stageRef })

const rules = {
  username: [{ required: true, message: '请输入学号/手机号/邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    loading.value = true
    errorMessage.value = ''

    const result = await authStore.login(
      form.username,
      form.password
    )

    if (result.success) {
      const redirectPath = String(route.query.redirect || '/')
      router.push(redirectPath)
    } else {
      errorMessage.value = result.message || '登录失败，请检查账号密码'
    }
  } catch (error) {
    console.error('登录失败:', error)

    if (error.code === 'NETWORK_ERROR' || error.code === 'TIMEOUT') {
      errorMessage.value = error.message || '网络错误，请稍后重试'
    } else if (error.status === 401) {
      errorMessage.value = '账号或密码错误'
    } else {
      errorMessage.value = error.message || '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

/* ---------- 右侧表单区 ---------- */
.login-form-side {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-8);
  min-height: 100vh;
  min-height: 100dvh;
  /* 极淡的祖母绿 wash：让玻璃卡与环境光可被感知（不用 backdrop-filter，浅色底上无收益却每帧重新取景） */
  background: radial-gradient(120% 80% at 62% 0%, #f0fdf4 0%, #ffffff 52%, #ecfdf5 100%);
}

/* .form-stage 尺寸即卡片尺寸，作为柔光坐标的基准 */
.form-stage {
  position: relative;
  width: 100%;
  max-width: 460px;
}

/* 环外环境光：只做 translate3d 位移，不触发重绘 */
.card-glow {
  position: absolute;
  inset: -34px;
  z-index: 0;
  border-radius: 56px;
  pointer-events: none;
  background: radial-gradient(closest-side, rgba(16, 185, 129, 0.24), rgba(16, 185, 129, 0) 72%);
  transform: translate3d(var(--glow-dx, 0px), var(--glow-dy, 0px), 0);
}

/* 仅在运动期间挂 will-change，空闲时摘掉，避免常驻占用显存 */
.form-stage.is-glowing .card-glow {
  will-change: transform;
}

.form-card {
  position: relative;
  z-index: 1;
  padding: var(--space-10) var(--space-8) var(--space-8);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    var(--shadow-xl),
    0 18px 46px rgba(5, 150, 105, 0.14);
}

/* 描边光环：1px 渐变环，受光点跟随光标。变量变化只重绘这 1px 细环，开销有界 */
.form-card::before {
  content: '';
  position: absolute;
  inset: 0;
  padding: 1px;
  border-radius: inherit;
  pointer-events: none;
  background: radial-gradient(
    400px circle at var(--glow-x, 50%) var(--glow-y, 50%),
    rgba(16, 185, 129, 0.55),
    rgba(16, 185, 129, 0.07)
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  mask-composite: exclude;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.form-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-extrabold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1) 0;
}

.form-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-6) 0;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: var(--space-1) 0 var(--space-6);
}

.forgot-link {
  font-size: var(--text-sm);
  color: var(--color-primary-500);
  font-weight: var(--font-medium);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.forgot-link:hover {
  color: var(--color-primary-600);
}

/* ---------- 按钮 ---------- */
.login-form .submit-btn {
  width: 100%;
  font-weight: var(--font-bold);
  letter-spacing: 4px;
  border-radius: var(--radius-xl);
  font-size: var(--text-lg);
  --el-button-size: 48px;
}

.error-alert {
  margin-top: var(--space-4);
}

.register-link {
  text-align: center;
  margin-top: var(--space-8);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.register-link a {
  color: var(--color-primary-500);
  font-weight: var(--font-semibold);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.register-link a:hover {
  color: var(--color-primary-600);
}

/* ---------- 入场编排：仅在「允许动效」时启用，基础样式保持完全可见 ---------- */
@media (prefers-reduced-motion: no-preference) {
  /* 逐级入场：标题 → 说明 → 账号 → 密码 → 选项 → 登录按钮 */
  .login-form > * {
    animation: card-in 460ms var(--ease-out) both;
  }

  .login-form > *:nth-child(1) {
    animation-delay: 60ms;
  }

  .login-form > *:nth-child(2) {
    animation-delay: 110ms;
  }

  .login-form > *:nth-child(3) {
    animation-delay: 160ms;
  }

  .login-form > *:nth-child(4) {
    animation-delay: 210ms;
  }

  .login-form > *:nth-child(5) {
    animation-delay: 260ms;
  }

  .login-form > *:nth-child(6) {
    animation-delay: 310ms;
  }
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- 窄屏：上下堆叠，表单卡上压品牌区 ---------- */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-form-side {
    flex: 1;
    min-height: 0;
    padding: var(--space-8) var(--space-5) var(--space-10);
    border-radius: var(--radius-2xl) var(--radius-2xl) 0 0;
    margin-top: calc(-1 * var(--space-6));
    z-index: 1;
  }

  .form-card {
    padding: var(--space-8) var(--space-6) var(--space-6);
  }

  .form-title {
    font-size: var(--text-2xl);
  }

  .card-glow {
    inset: -20px;
    border-radius: 40px;
  }
}
</style>