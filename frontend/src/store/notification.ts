import { ref, computed } from 'vue'
import { defineStore, storeToRefs } from 'pinia'
import { notificationApi, messageApi } from '../services/api'
import { useAuthStore } from './auth'

/**
 * 通知 Store（Pinia setup store）
 * - state: notificationUnreadCount（系统通知未读）/ chatUnreadCount（聊天未读）
 * - getter: totalUnreadCount / hasUnread（用于头像红点、TabBar 角标）
 * - 未读数为临时状态，每次登录/轮询重新拉取，不做持久化
 */
export const useNotificationPiniaStore = defineStore('notification', () => {
  /** 通知未读数 */
  const notificationUnreadCount = ref(0)
  /** 聊天未读数 */
  const chatUnreadCount = ref(0)

  /** 合并未读数（通知 + 聊天） */
  const totalUnreadCount = computed(
    () => notificationUnreadCount.value + chatUnreadCount.value
  )
  /** 是否有未读（用于头像红点显示） */
  const hasUnread = computed(() => totalUnreadCount.value > 0)

  /** 轮询定时器（非响应式，闭包持有即可） */
  let pollTimer: ReturnType<typeof setInterval> | null = null

  /** 获取通知未读数 */
  async function fetchNotificationUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      if (res.code === 200) {
        notificationUnreadCount.value = res.data?.count || 0
      }
    } catch {
      // 静默失败，不影响用户体验
    }
  }

  /** 获取聊天未读数 */
  async function fetchChatUnreadCount() {
    try {
      const res = await messageApi.getUnreadCount()
      if (res.code === 200) {
        // Chat API 返回 data 直接是数字，不是 {count: N}
        chatUnreadCount.value =
          typeof res.data === 'number' ? res.data : (res.data?.count || 0)
      }
    } catch {
      // 静默失败
    }
  }

  /** 获取所有未读数（未登录时直接跳过） */
  async function fetchAllUnreadCounts() {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated.value) return
    await Promise.allSettled([
      fetchNotificationUnreadCount(),
      fetchChatUnreadCount()
    ])
  }

  /** 启动轮询（30秒间隔） */
  function startPolling() {
    stopPolling()
    fetchAllUnreadCounts()
    pollTimer = setInterval(fetchAllUnreadCounts, 30000)
  }

  /** 停止轮询 */
  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  /** 通知已读后减少计数 */
  function decrementNotificationCount() {
    if (notificationUnreadCount.value > 0) {
      notificationUnreadCount.value--
    }
  }

  /** 全部标记已读后清零通知计数 */
  function clearNotificationCount() {
    notificationUnreadCount.value = 0
  }

  return {
    // state
    notificationUnreadCount,
    chatUnreadCount,
    // getters
    totalUnreadCount,
    hasUnread,
    // actions
    fetchAllUnreadCounts,
    fetchNotificationUnreadCount,
    fetchChatUnreadCount,
    startPolling,
    stopPolling,
    decrementNotificationCount,
    clearNotificationCount
  }
})

/**
 * 兼容层：保持旧版 useNotificationStore() 的调用契约不变。
 * storeToRefs 返回 ref（解构后仍保持响应式，可 .value），actions 直接透传。
 */
export function useNotificationStore() {
  const store = useNotificationPiniaStore()
  const {
    notificationUnreadCount,
    chatUnreadCount,
    totalUnreadCount,
    hasUnread
  } = storeToRefs(store)

  return {
    notificationUnreadCount,
    chatUnreadCount,
    totalUnreadCount,
    hasUnread,
    fetchAllUnreadCounts: store.fetchAllUnreadCounts,
    fetchNotificationUnreadCount: store.fetchNotificationUnreadCount,
    fetchChatUnreadCount: store.fetchChatUnreadCount,
    startPolling: store.startPolling,
    stopPolling: store.stopPolling,
    decrementNotificationCount: store.decrementNotificationCount,
    clearNotificationCount: store.clearNotificationCount
  }
}
