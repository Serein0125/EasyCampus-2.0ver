<template>
  <div class="notifications-page">
    <!-- 顶部导航 -->
    <header class="page-header">
      <BackButton />
      <h1 class="page-title">通知</h1>
      <div class="header-actions">
        <button v-if="notifications.length > 0 && activeFilter !== 'CHAT'" class="action-btn" @click="handleMarkAllRead" :disabled="markingAllRead">
          全部已读
        </button>
      </div>
    </header>

    <!-- 类型筛选标签 -->
    <div class="filter-tabs">
      <button
        v-for="tab in filterTabs"
        :key="tab.key"
        class="filter-tab"
        :class="{ active: activeFilter === tab.key }"
        @click="switchFilter(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading && notifications.length === 0" class="loading-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 通知列表 -->
    <div v-else-if="notifications.length > 0" class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleNotificationClick(item)"
      >
        <!-- 操作者头像 -->
        <div class="actor-avatar-wrap">
          <img
            v-if="item.actorAvatar"
            :src="item.actorAvatar"
            class="actor-avatar"
            @error="handleAvatarError($event)"
          />
          <div v-else class="actor-avatar-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <!-- 类型小图标 -->
          <span class="type-badge" :class="item.type.toLowerCase()">
            {{ typeIcon(item.type) }}
          </span>
        </div>

        <!-- 通知内容 -->
        <div class="notification-content">
          <p class="notification-text">
            <span class="actor-name">{{ item.actorNickname || '用户' }}</span>
            <template v-if="item.type === 'CHAT'">：{{ item.content }}</template>
            <template v-else>{{ item.content }}</template>
          </p>
          <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
        </div>

        <!-- 未读标记 -->
        <div v-if="!item.isRead" class="unread-dot"></div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <span class="empty-emoji">🔔</span>
      <p class="empty-text">{{ activeFilter === 'CHAT' ? '暂无私信' : '暂无通知' }}</p>
      <p class="empty-sub">{{ activeFilter === 'CHAT' ? '有人给你发私信时，会在这里显示' : '有人点赞、评论或关注你时，会在这里显示' }}</p>
    </div>

    <!-- 底部操作栏 -->
    <div v-if="notifications.length > 0 && activeFilter !== 'CHAT'" class="bottom-bar">
      <button class="clear-btn" @click="handleClearAll">
        清空所有通知
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '../services/api'
import { useNotificationStore } from '../store/notification'
import { useToast } from '../use/useToast'
import BackButton from '../components/BackButton.vue'

const router = useRouter()
const toast = useToast()
const notificationStore = useNotificationStore()

const notifications = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const markingAllRead = ref(false)
const currentPage = ref(1)
const hasMore = ref(false)
const activeFilter = ref('ALL')

const filterTabs = [
  { key: 'ALL', label: '全部' },
  { key: 'LIKE', label: '点赞' },
  { key: 'COMMENT', label: '评论' },
  { key: 'FOLLOW', label: '关注' },
  { key: 'CHAT', label: '私信' }
]

/** 切换筛选类型 */
function switchFilter(type) {
  if (activeFilter.value === type) return
  activeFilter.value = type
  currentPage.value = 1
  notifications.value = []
  fetchNotifications()
}

/** 获取通知列表 */
async function fetchNotifications(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: 20
    }

    let items = []
    if (activeFilter.value === 'CHAT') {
      // 私信走独立接口
      const res = await notificationApi.getChatNotifications(params)
      if (res.code === 200 && res.data) {
        items = res.data.map(item => ({
          ...item,
          actorAvatar: item.actorAvatar || '',
          actorNickname: item.actorNickname || '用户'
        }))
      }
    } else {
      // 普通通知
      if (activeFilter.value !== 'ALL') {
        params.type = activeFilter.value
      }
      const res = await notificationApi.getNotifications(params)
      if (res.code === 200 && res.data) {
        items = res.data.map(item => ({
          ...item,
          actorAvatar: item.actorAvatar || '',
          actorNickname: item.actorNickname || '用户'
        }))
      }
    }

    if (isLoadMore) {
      notifications.value.push(...items)
    } else {
      notifications.value = items
    }
    hasMore.value = items.length >= 20
  } catch {
    toast.showToast('获取通知失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/** 加载更多 */
function loadMore() {
  currentPage.value++
  fetchNotifications(true)
}

/** 点击通知：跳转对应内容 + 标记已读 */
async function handleNotificationClick(item) {
  // 私信类型：跳转到聊天会话
  if (item.type === 'CHAT') {
    router.push(`/chat/${item.actorId}`)
    return
  }

  // 标记已读
  if (!item.isRead) {
    try {
      await notificationApi.markAsRead(item.id)
      item.isRead = true
      notificationStore.decrementNotificationCount()
    } catch {
      // 静默失败
    }
  }

  // 根据类型跳转
  if (item.type === 'FOLLOW') {
    router.push(`/users/${item.actorId}`)
  } else if (item.targetType === 'POST') {
    router.push(`/community/posts/${item.targetId}`)
  } else if (item.targetType === 'PRODUCT') {
    router.push(`/products/${item.targetId}`)
  }
}

/** 全部标记已读 */
async function handleMarkAllRead() {
  markingAllRead.value = true
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    notificationStore.clearNotificationCount()
    toast.showToast('已全部标记为已读')
  } catch {
    toast.showToast('操作失败')
  } finally {
    markingAllRead.value = false
  }
}

/** 清空所有通知 */
async function handleClearAll() {
  if (!confirm('确定清空所有通知吗？此操作不可恢复。')) return
  try {
    await notificationApi.clearAll()
    notifications.value = []
    notificationStore.clearNotificationCount()
    toast.showToast('已清空所有通知')
  } catch {
    toast.showToast('操作失败')
  }
}

/** 通知类型图标 */
function typeIcon(type) {
  switch (type) {
    case 'LIKE': return '❤️'
    case 'COMMENT': return '💬'
    case 'FOLLOW': return '👤'
    case 'CHAT': return '✉️'
    default: return '🔔'
  }
}

/** 格式化时间 */
function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

/** 头像加载失败时显示默认头像 */
function handleAvatarError(event) {
  event.target.style.display = 'none'
  event.target.nextElementSibling?.classList.remove('hidden')
}

onMounted(() => {
  fetchNotifications()
})

onUnmounted(() => {
  // 组件销毁时无需特殊清理
})
</script>

<style scoped>
.notifications-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f0fdf4);
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--gradient-glass, linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.7) 100%));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
}

.page-title {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
  margin: 0;
}

.header-actions {
  flex-shrink: 0;
}

.action-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary-600, #059669);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
}

.action-btn:disabled {
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  gap: 0;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  overflow-x: auto;
}

.filter-tab {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary, #6b7280);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s, border-color 0.2s;
}

.filter-tab.active {
  color: var(--color-primary-600, #059669);
  font-weight: 600;
  border-bottom-color: var(--color-primary-500, #10b981);
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: var(--color-text-tertiary, #9ca3af);
  font-size: 14px;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-border-light, #e5e7eb);
  border-top-color: var(--color-primary-500, #10b981);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 通知列表 */
.notification-list {
  padding-bottom: 70px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  cursor: pointer;
  transition: background 0.15s;
}

.notification-item:active {
  background: var(--color-gray-50, #f9fafb);
}

.notification-item.unread {
  background: var(--color-primary-50, #ecfdf5);
}

/* 头像区域 */
.actor-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.actor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.actor-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-gray-100, #f3f4f6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary, #9ca3af);
}

.type-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  background: #fff;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.08);
}

/* 通知内容 */
.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary, #111827);
  line-height: 1.5;
}

.actor-name {
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}

.notification-time {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary, #9ca3af);
}

/* 未读圆点 */
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-rose-500, #f43f5e);
  flex-shrink: 0;
  margin-top: 6px;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 16px;
}

.load-more-btn {
  font-size: 14px;
  color: var(--color-primary-600, #059669);
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px 16px;
}

.load-more-btn:disabled {
  color: var(--color-text-tertiary, #9ca3af);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.empty-emoji {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  margin: 0 0 8px;
}

.empty-sub {
  font-size: 14px;
  color: var(--color-text-tertiary, #9ca3af);
  margin: 0;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  border-top: 1px solid var(--color-border-light, #e5e7eb);
  z-index: 10;
}

.clear-btn {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-rose-500, #f43f5e);
  background: none;
  border: 1px solid var(--color-rose-200, #fecdd3);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.clear-btn:active {
  background: #fff1f2;
}
</style>
