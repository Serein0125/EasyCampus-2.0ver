<template>
  <div class="messages-page">
    <!-- 顶部标题栏 -->
    <header class="page-header">
      <h1 class="page-title">消息</h1>
      <span v-if="unreadTotal > 0" class="unread-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
    </header>

    <!-- 消息列表 -->
    <main class="messages-list">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="skeleton-item" v-for="i in 5" :key="i">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-content">
            <div class="skeleton-line long"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="conversations.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <h3 class="empty-title">暂无消息</h3>
        <p class="empty-desc">去商品详情页联系卖家吧~</p>
        <button @click="$router.push('/')" class="empty-btn">去逛逛</button>
      </div>

      <!-- 会话列表 -->
      <div v-else class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.userId"
          @click="openChat(conv)"
          class="conversation-item"
        >
          <!-- 头像 -->
          <div class="avatar-wrapper">
            <img
              :src="conv.avatar || defaultAvatar"
              :alt="conv.username"
              class="avatar"
            />
            <!-- 在线状态指示器 -->
            <span v-if="conv.isOnline" class="online-indicator"></span>
          </div>

          <!-- 内容区 -->
          <div class="conv-content">
            <div class="conv-header">
              <h4 class="username">{{ conv.username }}</h4>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>

            <div class="conv-body">
              <p class="last-message line-clamp-1">{{ conv.lastMessage || '暂无消息' }}</p>
              <!-- 未读数量 -->
              <span v-if="conv.unreadCount > 0" class="unread-count">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </span>
            </div>
          </div>

          <!-- 商品预览卡片（如果有） -->
          <div v-if="conv.productInfo" class="product-preview">
            <img :src="conv.productInfo.coverImage || conv.productInfo.imageUrl" alt="" class="product-thumb" />
            <div class="product-info-mini">
              <p class="product-name-mini">{{ conv.productInfo.name }}</p>
              <span class="product-price-mini">¥{{ conv.productInfo.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '../services/api'

const router = useRouter()

// 数据状态
const conversations = ref([])
const loading = ref(true)
const unreadTotal = ref(0)

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+'

/** 会话列表轮询定时器 */
let convPollTimer = null

onMounted(async () => {
  await loadConversations()
  // 轮询刷新会话列表（每10秒，仅在页面可见时）
  convPollTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      loadConversations()
    }
  }, 10000)
})

onUnmounted(() => {
  if (convPollTimer) {
    clearInterval(convPollTimer)
    convPollTimer = null
  }
})

// 加载会话列表
async function loadConversations() {
  try {
    loading.value = true

    const response = await messageApi.getContacts()

    if (response.code === 200) {
      const rawData = response.data || []

      conversations.value = (Array.isArray(rawData) ? rawData : []).map(conv => ({
        userId: conv.otherUser?.id || conv.otherUserId || conv.userId,
        username: conv.otherUser?.nickname || conv.otherUser?.username || conv.username || '未知用户',
        avatar: conv.otherUser?.avatar || conv.avatar || null,
        lastMessage: conv.lastMessage || '',
        lastMessageTime: conv.lastMessageAt || conv.lastMessageTime || new Date().toISOString(),
        unreadCount: conv.unreadCount || 0,
        isOnline: conv.otherUser?.isOnline || false,
        productInfo: conv.productInfo || null,
        id: conv.id,
        otherUserId: conv.otherUser?.id || conv.otherUserId || conv.userId,
        productId: conv.productInfo?.id || conv.productId || null
      })).filter(c => c.userId)

      unreadTotal.value = conversations.value.reduce(
        (total, conv) => total + (conv.unreadCount || 0),
        0
      )
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    conversations.value = []
  } finally {
    loading.value = false
  }
}

// 打开聊天室
function openChat(conv) {
  const uid = conv.userId || conv.otherUserId
  if (!uid) return
  router.push({
    path: `/chat/${uid}`,
    query: { productId: conv.productId || undefined }
  })
}

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) {
    return '刚刚'
  } else if (diffMin < 60) {
    return `${diffMin}分钟前`
  } else if (diffHour < 24) {
    return `${diffHour}小时前`
  } else if (diffDay < 7) {
    return `${diffDay}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'numeric',
      day: 'numeric'
    })
  }
}

</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: var(--color-bg-page);
}

.page-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2_5);
  padding: var(--space-4);
  background: var(--color-bg-primary);
  box-shadow: var(--shadow-sm);
}

.page-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.unread-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 var(--space-1_5);
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
  color: var(--color-text-inverse);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-green);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

.loading-state {
  padding: var(--space-4);
  background: var(--color-bg-primary);
}

.skeleton-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3_5) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.skeleton-avatar {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-2_5);
}

.skeleton-line {
  height: 16px;
  border-radius: var(--radius-xs);
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line.long {
  width: 70%;
}

.skeleton-line.short {
  width: 45%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-20) var(--space-8);
  text-align: center;
}

.empty-icon {
  font-size: 72px;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-2);
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-7);
}

.empty-btn {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
  color: var(--color-text-inverse);
  padding: var(--space-3) var(--space-9);
  border-radius: var(--radius-xl);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  border: none;
  cursor: pointer;
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
}

.empty-btn:active {
  transform: scale(0.95);
}

.conversation-list {
  background: var(--color-bg-primary);
}

.conversation-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
  transition: background-color var(--duration-fast) var(--ease-out);
  cursor: pointer;
  position: relative;
}

.conversation-item:active {
  background: var(--color-bg-secondary);
}

.avatar-wrapper {
  position: relative;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-full);
  object-fit: cover;
  background: var(--color-gray-300);
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: var(--color-primary-500);
  border: 2px solid var(--color-bg-primary);
  border-radius: var(--radius-full);
}

.conv-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1_5);
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.username {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 65%;
}

.conv-time {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.conv-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.last-message {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-count {
  min-width: 18px;
  height: 18px;
  padding: 0 var(--space-1);
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
  color: var(--color-text-inverse);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-green);
}

.product-preview {
  display: flex;
  gap: var(--space-2_5);
  padding: var(--space-2_5);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-left: 64px;
}

.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-gray-300);
  flex-shrink: 0;
}

.product-info-mini {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-1);
  min-width: 0;
}

.product-name-mini {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price-mini {
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
  color: var(--color-rose-500);
}
</style>
