import { ref, computed } from 'vue'
import { defineStore, storeToRefs } from 'pinia'
import { userApi } from '../services/api'

// 用户实体（仅声明后端实际返回并使用到的字段）
export interface AuthUser {
  id: number | string
  username: string
  nickname?: string
  avatar?: string
  school?: string
  campus?: string
  role?: string
  tokenExpiry?: string
  [key: string]: unknown
}

// 认证操作的统一返回结构
interface AuthResult {
  success: boolean
  message?: string
  data?: unknown
}

/**
 * 自定义持久化 storage：
 * 把 Pinia state 拆成 localStorage 中独立的 'token' / 'user' 两个 key，
 * 与 axios 请求拦截器读取 localStorage.getItem('token') 的既有约定保持兼容，
 * 这样 API 层无需反向依赖 store（避免 services 与 store 的循环依赖）。
 */
const splitStorage = {
  getItem(_key: string): string | null {
    const token = localStorage.getItem('token')
    const userRaw = localStorage.getItem('user')
    if (!token && !userRaw) return null
    let user: AuthUser | null = null
    try {
      user = userRaw ? (JSON.parse(userRaw) as AuthUser) : null
    } catch {
      // 解析失败时保持 null
    }
    return JSON.stringify({ token: token || '', user })
  },
  setItem(_key: string, value: string) {
    try {
      const state = JSON.parse(value) as { token?: string; user?: AuthUser | null }
      if (state.token) {
        localStorage.setItem('token', state.token)
      } else {
        localStorage.removeItem('token')
      }
      if (state.user) {
        localStorage.setItem('user', JSON.stringify(state.user))
      } else {
        localStorage.removeItem('user')
      }
    } catch (e) {
      console.error('持久化认证状态失败:', e)
    }
  },
  removeItem(_key: string) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
}

/**
 * 认证 Store（Pinia setup store）
 * - state: token / user
 * - getter: isAuthenticated / currentUser
 * - 持久化: pinia-plugin-persistedstate + 自定义 splitStorage
 */
export const useAuthPiniaStore = defineStore(
  'auth',
  () => {
    const token = ref('')
    const user = ref<AuthUser | null>(null)

    const isAuthenticated = computed(() => !!token.value)
    const currentUser = computed(() => user.value)

    /** 写入认证状态（由持久化插件自动同步到 localStorage） */
    function setAuth(newToken: string, userData: AuthUser) {
      token.value = newToken
      user.value = userData
    }

    /** 清除认证状态 */
    function clearAuthData() {
      token.value = ''
      user.value = null
    }

    /**
     * 启动时校验已持久化的登录态：
     * 拒绝演示模式假 token（demo_token_*）、数据不完整、已过期的 token
     */
    function initializeAuth() {
      if (!token.value) return

      if (token.value.startsWith('demo_token_') || token.value.includes('demo')) {
        console.warn('检测到无效的演示模式token，已自动清除')
        clearAuthData()
        return
      }

      if (!user.value?.id || !user.value?.username) {
        console.warn('用户数据不完整，已自动清除')
        clearAuthData()
        return
      }

      const expiry = user.value.tokenExpiry
      if (expiry && new Date() > new Date(expiry)) {
        console.warn('Token已过期，请重新登录')
        clearAuthData()
      }
    }

    async function login(username: string, password: string): Promise<AuthResult> {
      try {
        const response = await userApi.login({ username, password })

        if (response.code === 200 && response.data) {
          const { token: newToken, user: userData } = response.data
          setAuth(newToken, userData)
          return { success: true, data: response.data }
        }
        return { success: false, message: response.message || '登录失败' }
      } catch (error) {
        console.error('登录错误:', error)
        return { success: false, message: (error as Error).message || '登录失败' }
      }
    }

    async function register(userData: Record<string, unknown>): Promise<AuthResult> {
      try {
        const response = await userApi.register(userData)

        if (response.code === 200 && response.data) {
          const { token: newToken, user: registeredUser } = response.data
          setAuth(newToken, registeredUser)
          return { success: true, data: response.data }
        }
        return { success: false, message: response.message || '注册失败' }
      } catch (error) {
        console.error('注册错误:', error)
        return { success: false, message: (error as Error).message || '注册失败' }
      }
    }

    function logout() {
      clearAuthData()
      // 跳转到首页
      window.location.href = '/'
    }

    async function fetchUserInfo(): Promise<AuthResult> {
      try {
        const response = await userApi.getUserInfo()

        if (response.code === 200 && response.data) {
          user.value = response.data
          return { success: true, data: response.data }
        }
        return { success: false, message: response.message || '获取用户信息失败' }
      } catch (error) {
        console.error('获取用户信息错误:', error)
        return { success: false, message: (error as Error).message || '获取用户信息失败' }
      }
    }

    /** 检查登录状态（token 存在且未过期） */
    function checkAuth(): boolean {
      if (!token.value || !user.value) {
        return false
      }

      const expiry = user.value.tokenExpiry
      if (expiry && new Date() > new Date(expiry)) {
        clearAuthData()
        return false
      }
      return true
    }

    return {
      // state
      token,
      user,
      // getters
      isAuthenticated,
      currentUser,
      // actions
      initializeAuth,
      login,
      register,
      logout,
      fetchUserInfo,
      checkAuth,
      clearAuthData
    }
  },
  {
    persist: {
      key: 'campus-auth',
      storage: splitStorage,
      pick: ['token', 'user']
    }
  }
)

/**
 * 兼容层：保持旧版 useAuthStore() 的调用契约不变。
 * - storeToRefs 保证解构出来的 isAuthenticated / currentUser 等仍是 ref（模板自动解包、脚本用 .value）
 * - actions 直接透传（setup store 的 action 是闭包函数，解构安全）
 * 这样现有 20+ 个组件的调用点无需任何修改。
 */
let authInitialized = false
export function useAuthStore() {
  const store = useAuthPiniaStore()

  // 首次使用时执行登录态校验（此时持久化插件已完成 state 恢复）
  if (!authInitialized) {
    authInitialized = true
    store.initializeAuth()
  }

  const { token, user, isAuthenticated, currentUser } = storeToRefs(store)

  return {
    // state / getters（refs，保持 .value 用法兼容）
    token,
    user,
    isAuthenticated,
    currentUser,
    // actions
    login: store.login,
    register: store.register,
    logout: store.logout,
    fetchUserInfo: store.fetchUserInfo,
    checkAuth: store.checkAuth,
    clearAuthData: store.clearAuthData
  }
}
