<template>
  <div class="chat-room">
    <header class="chat-header">
      <BackButton />
      <div class="header-info">
        <h3 class="contact-name">{{ contactInfo.nickname || contactInfo.username || '用户' }}</h3>
        <span v-if="isOnline" class="online-status">在线</span>
      </div>
      <button @click="showMoreOptions = !showMoreOptions" class="more-btn">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/>
        </svg>
      </button>
      <div v-if="showMoreOptions" class="more-menu">
        <button @click="viewContactProfile" class="menu-item">查看资料</button>
        <button @click="clearMessages" class="menu-item danger">清空聊天记录</button>
      </div>
    </header>

    <div v-if="productInfo" class="product-card-inline">
      <img :src="productInfo.coverImage || (productInfo.imageUrls && productInfo.imageUrls[0])" :alt="productInfo.name" class="product-thumb" @error="onProductImgError" />
      <div class="product-detail">
        <h4 class="product-name">{{ productInfo.name }}</h4>
        <p class="product-price">¥{{ productInfo.price }}</p>
      </div>
    </div>

    <main class="messages-container" ref="messagesContainer">
      <div v-if="loadingHistory" class="loading-more-messages">
        <div class="mini-spinner"></div><span>加载中...</span>
      </div>

      <div v-for="(timeGroup, date) in groupedMessages" :key="date" class="message-group">
        <div class="date-divider"><span class="date-text">{{ formatDate(date) }}</span></div>
        <div v-for="message in timeGroup" :key="message.id"
          :class="['message-item', message.senderId === currentUserId ? 'sent' : 'received']">
          <div v-if="message.senderId !== currentUserId" class="avatar-wrapper">
            <img :src="contactInfo.avatar || defaultAvatar" :alt="contactInfo.username" class="avatar" />
          </div>
          <div class="bubble-wrapper">
            <div class="message-bubble"><p class="message-text">{{ message.content }}</p></div>
            <div class="message-meta">
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <span v-if="message.senderId === currentUserId" class="read-status">{{ message.isRead ? '已读' : '未读' }}</span>
              <!-- 发送状态指示 -->
              <span v-if="message._status === 'sending'" class="send-status sending">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" class="spin-icon"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
              </span>
              <button v-if="message._status === 'failed'" class="send-status failed" @click="retryMessage(message)" title="发送失败，点击重试">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              </button>
            </div>
          </div>
          <div v-if="message.senderId === currentUserId" class="avatar-wrapper self">
            <img :src="currentUserAvatar || defaultAvatar" alt="我" class="avatar" />
          </div>
        </div>
      </div>

      <div v-if="!loadingHistory && messages.length === 0" class="empty-chat">
        <div class="empty-icon">💬</div><p>发送消息开始聊天吧~</p>
      </div>
    </main>

    <footer class="input-area">
      <div class="toolbar">
        <button @click="toggleEmojiPicker" class="tool-btn" :class="{ active: showEmojiPicker }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
        </button>
      </div>
      <div class="input-wrapper">
        <textarea ref="inputRef" v-model="newMessage" class="message-input" placeholder="输入消息..." rows="1"
          @input="autoResize" @keydown.enter.exact.prevent="sendMessage"></textarea>
        <button @click="sendMessage" :disabled="!newMessage.trim() || sending" class="send-btn" :class="{ active: newMessage.trim() }">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
        </button>
      </div>
      <div v-if="showEmojiPicker" class="emoji-picker">
        <div class="emoji-grid">
          <span v-for="emoji in commonEmojis" :key="emoji" @click="insertEmoji(emoji)" class="emoji-item">{{ emoji }}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { messageApi, wsManager, productApi, userApi } from '../services/api'
import { useToast } from '../use/useToast'
import BackButton from '../components/BackButton.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

/** 移动端虚拟键盘适配：键盘弹出时将输入区域固定在键盘上方 */
function setupKeyboardAdapter() {
  // 仅移动端生效。window.visualViewport 会随键盘弹出而缩小，是判断键盘的关键 API。
  // 传统做法监听 resize 不可靠：iOS Safari 的 layout viewport 不会随键盘变化。
  if (!window.visualViewport || window.innerWidth > 768) return null

  const viewportHandler = () => {
    const vv = window.visualViewport
    const inputArea = document.querySelector<HTMLElement>('.chat-room .input-area')
    if (!inputArea) return

    if (vv.height < window.innerHeight * 0.85) {
      // 键盘弹出：visualViewport 高度明显小于窗口高度（85% 为经验阈值）
      // 把输入区 position:fixed 到「窗口底部 - 可视高度 - 偏移」= 键盘正上方
      inputArea.style.position = 'fixed'
      inputArea.style.bottom = (window.innerHeight - vv.height - vv.offsetTop) + 'px'
      inputArea.style.left = '0'
      inputArea.style.right = '0'
      inputArea.style.zIndex = '10'
    } else {
      // 键盘收起：恢复原始样式
      inputArea.style.position = ''
      inputArea.style.bottom = ''
      inputArea.style.left = ''
      inputArea.style.right = ''
      inputArea.style.zIndex = ''
    }
  }

  window.visualViewport.addEventListener('resize', viewportHandler)
  window.visualViewport.addEventListener('scroll', viewportHandler)

  // 返回清理函数，供 onUnmounted 调用，避免组件销毁后仍修改 DOM
  return () => {
    window.visualViewport.removeEventListener('resize', viewportHandler)
    window.visualViewport.removeEventListener('scroll', viewportHandler)
  }
}

/** 键盘适配清理函数 */
let cleanupKeyboard = null

const messages = ref<any[]>([])
const loadingHistory = ref(false)
const sending = ref(false)
const newMessage = ref('')
const showEmojiPicker = ref(false)
const showMoreOptions = ref(false)
const isOnline = ref(false)
const messagesContainer = ref(null)
const inputRef = ref(null)

const currentUserId = computed(() => authStore.currentUser.value?.id)
const currentUserAvatar = computed(() => String(authStore.currentUser.value?.avatar || ''))
const contactInfo = ref<any>({})
const productInfo = ref(null)
const conversationId = ref(null)
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+'

const commonEmojis = ['😀','😂','🥰','😎','🤔','😢','😡','👍','❤️','🎉','🔥','✨','👏','🙏','💪','🤝','💰','📚','🎁','☀️','🌙','⭐','🌈','🎈']

function onProductImgError(e) {
  e.target.src = defaultAvatar
}

const groupedMessages = computed(() => {
  const groups: Record<string, any[]> = {}
  messages.value.forEach(msg => {
    const date = new Date(msg.createdAt).toDateString()
    if (!groups[date]) groups[date] = []
    groups[date].push(msg)
  })
  return groups
})

/** 点击外部关闭表情面板和更多菜单 */
function handleDocumentClick(event) {
  // 关闭更多菜单：点击不在菜单和触发按钮内时关闭
  if (showMoreOptions.value && !event.target.closest('.more-menu') && !event.target.closest('.more-btn')) {
    showMoreOptions.value = false
  }
  // 关闭表情面板：点击不在面板和触发按钮内时关闭
  if (showEmojiPicker.value && !event.target.closest('.emoji-picker') && !event.target.closest('.tool-btn')) {
    showEmojiPicker.value = false
  }
}

/** 消息轮询定时器 */
let messagePollTimer = null

onMounted(async () => {
  await initChat()
  // 订阅全局 WS 的 chat_message 事件：服务端推送新消息时回调 handleWsMessage
  // 必须在 onUnmounted 里 off 掉，否则离开聊天页后仍会收到推送造成内存泄漏/重复处理
  wsManager.on('chat_message', handleWsMessage)
  nextTick(() => inputRef.value?.focus())
  document.addEventListener('click', handleDocumentClick)
  // 轮询刷新消息（每5秒）。WS 是主通道，轮询是「兜底」补偿丢包/未连上的情况。
  // document.visibilityState === 'visible'：页面切到后台时跳过轮询，省电省流量
  messagePollTimer = setInterval(() => {
    if (document.visibilityState === 'visible' && conversationId.value) {
      loadMessages()
    }
  }, 5000)
  // 移动端键盘适配
  cleanupKeyboard = setupKeyboardAdapter()
})

onUnmounted(() => {
  wsManager.off('chat_message', handleWsMessage)
  document.removeEventListener('click', handleDocumentClick)
  if (messagePollTimer) {
    clearInterval(messagePollTimer)
    messagePollTimer = null
  }
  // 清理键盘适配监听
  if (cleanupKeyboard) {
    cleanupKeyboard()
    cleanupKeyboard = null
  }
})

// 进入聊天页的初始化串行流程：取对方信息 → 找会话 → 取商品信息 → 拉历史 → 标已读
// 串行是因为后一步依赖前一步的结果（如 loadMessages 需要 conversationId）
async function initChat() {
  const rawId = route.params.userId
  const otherUserId = Number(rawId)
  if (!rawId || isNaN(otherUserId) || otherUserId <= 0) {
    contactInfo.value = { username: '用户不存在' }
    return
  }
  await loadContactInfo(otherUserId)
  await findConversation(otherUserId)
  await loadProductInfo()
  if (conversationId.value) {
    await loadMessages()
    await markAsRead()
  }
}

async function loadContactInfo(userId) {
  try {
    const res = await userApi.getUserPublic(userId)
    if (res.code === 200) {
      contactInfo.value = res.data
      if (res.data.lastActiveAt) {
        // 在线判断：最近 5 分钟内有活跃即视为在线
        // 这是一种「伪在线」近似，真正的在线状态需要心跳/WS 在线列表支撑
        const lastActive = new Date(res.data.lastActiveAt)
        const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000)
        isOnline.value = lastActive > fiveMinutesAgo
      }
    }
  } catch {
    contactInfo.value = { id: userId, username: '未知用户' }
  }
}

async function findConversation(otherUserId) {
  try {
    const res = await messageApi.getContacts()
    if (res.code === 200) {
      const list = res.data || []
      const conv = list.find(c => c.otherUser && Number(c.otherUser.id) === otherUserId)
      if (conv) conversationId.value = conv.id
    }
  } catch {}
}

async function loadProductInfo() {
  const productId = String(route.query.productId || '')
  if (!productId) return
  try {
    const res = await productApi.getProductDetail(productId)
    if (res.code === 200) productInfo.value = res.data
  } catch {}
}

async function loadMessages() {
  if (!conversationId.value) return
  try {
    loadingHistory.value = true
    const res = await messageApi.getConversation(conversationId.value, { page: 1, size: 50 })
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      messages.value = Array.isArray(list) ? list : []
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载消息失败:', e)
  } finally {
    loadingHistory.value = false
  }
}

async function sendMessage() {
  const content = newMessage.value.trim()
  if (!content || sending.value) return
  const rawId = route.params.userId
  const otherUserId = Number(rawId)
  if (!rawId || isNaN(otherUserId) || otherUserId <= 0) return
  sending.value = true

  // 乐观更新（Optimistic Update）：不等服务器确认就先把消息显示在界面上，
  // 并用临时 id + _status:'sending' 标记「发送中」。这样用户感知「秒回」。
  // 失败时把 _status 改为 'failed' 并提供重试，避免消息凭空消失。
  const tempId = Date.now()
  const optimisticMsg = {
    id: tempId,
    senderId: currentUserId.value,
    content,
    createdAt: new Date().toISOString(),
    isRead: false,
    _status: 'sending'
  }
  messages.value.push(optimisticMsg)
  newMessage.value = ''
  autoResize()
  scrollToBottom()

  try {
    const payload: Record<string, unknown> = { receiverId: otherUserId, content }
    if (route.query.productId) payload.productId = Number(route.query.productId) || undefined

    const res = await messageApi.sendMessage(payload)
    if (res.code === 200) {
      // 发送成功，更新状态
      const msg = messages.value.find(m => m.id === tempId)
      if (msg) {
        msg.id = res.data?.id || tempId
        msg._status = 'sent'
      }
      if (!conversationId.value) await findConversation(otherUserId)
    } else {
      // 发送失败
      const msg = messages.value.find(m => m.id === tempId)
      if (msg) msg._status = 'failed'
    }
  } catch {
    // 发送失败
    const msg = messages.value.find(m => m.id === tempId)
    if (msg) msg._status = 'failed'
  } finally {
    sending.value = false
  }
}

async function retryMessage(message) {
  const rawId = route.params.userId
  const otherUserId = Number(rawId)
  if (!rawId || isNaN(otherUserId) || otherUserId <= 0) return

  message._status = 'sending'
  try {
    const payload: Record<string, unknown> = { receiverId: otherUserId, content: message.content }
    if (route.query.productId) payload.productId = Number(route.query.productId) || undefined

    const res = await messageApi.sendMessage(payload)
    if (res.code === 200) {
      message.id = res.data?.id || message.id
      message._status = 'sent'
    } else {
      message._status = 'failed'
    }
  } catch {
    message._status = 'failed'
  }
}

// 处理服务端通过 WS 推送过来的新消息
// 关键是「过滤」：全局 wsManager 的事件是所有聊天页共用的，
// 必须只接收「当前对话对方发给我的」消息，否则会在 A 聊天页收到 B 的消息
function handleWsMessage(data) {
  const rawId = route.params.userId
  const otherUserId = Number(rawId)
  if (!rawId || isNaN(otherUserId)) return
  // 跳过自己发的消息（服务端可能回显），避免重复显示
  if (Number(data.senderId) === currentUserId.value) return
  // 只处理「对方发的 且 我是接收者」的消息，过滤掉其他会话的推送
  if (Number(data.senderId) !== otherUserId && Number(data.receiverId) !== currentUserId.value) return
  messages.value.push({
    id: data.id || Date.now(),
    senderId: Number(data.senderId),
    content: data.content,
    createdAt: data.timestamp || new Date().toISOString(),
    isRead: true
  })
  scrollToBottom()
}

async function markAsRead() {
  if (!conversationId.value) return
  try {
    await messageApi.markConversationAsRead(conversationId.value)
  } catch {}
}

function viewContactProfile() { showMoreOptions.value = false; router.push(`/users/${route.params.userId}`) }
async function clearMessages() { const ok = await toast.showConfirm('确定要清空聊天记录吗？'); if (ok) { messages.value = []; showMoreOptions.value = false; toast.showToast('聊天记录已清空（仅本地）') } }
function toggleEmojiPicker() { showEmojiPicker.value = !showEmojiPicker.value }
function insertEmoji(emoji) { newMessage.value += emoji; inputRef.value?.focus(); showEmojiPicker.value = false }
function autoResize() { const tx = inputRef.value; if (!tx) return; tx.style.height = 'auto'; tx.style.height = Math.min(tx.scrollHeight, 120) + 'px' }
function scrollToBottom() { nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTo({ top: messagesContainer.value.scrollHeight, behavior: 'smooth' }) }) }
function formatDate(d) { const dt = new Date(d); const today = new Date(); const yest = new Date(today); yest.setDate(yest.getDate()-1); if (dt.toDateString()===today.toDateString()) return '今天'; if (dt.toDateString()===yest.toDateString()) return '昨天'; return `${dt.getMonth()+1}月${dt.getDate()}日` }
function formatTime(ts) { if (!ts) return ''; return new Date(ts).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'}) }
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg-page);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--z-modal);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
  position: relative;
}

.header-info {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.contact-name {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.online-status {
  font-size: var(--text-xs);
  color: var(--color-primary-500);
}

.more-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  border-radius: var(--radius-full);
  border: none;
  background: none;
  cursor: pointer;
}

.more-btn:active {
  background: var(--color-bg-secondary);
}

.more-menu {
  position: absolute;
  top: 56px;
  right: var(--space-4);
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  min-width: 140px;
  z-index: var(--z-dropdown);
}

.menu-item {
  width: 100%;
  padding: var(--space-3_5) var(--space-5);
  font-size: var(--text-base);
  color: var(--color-text-primary);
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
}

.menu-item:active {
  background: var(--color-bg-secondary);
}

.menu-item.danger {
  color: var(--color-error);
}

.product-card-inline {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
  align-items: center;
}

.product-thumb {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  object-fit: cover;
  background: var(--color-bg-secondary);
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1);
}

.product-price {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-rose-500);
  margin: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6) var(--space-6);
}

.loading-more-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-4);
  color: var(--color-text-tertiary);
  font-size: var(--text-xs);
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-gray-200);
  border-top-color: var(--color-primary-500);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.date-divider {
  text-align: center;
  margin: var(--space-5) 0;
}

.date-text {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.message-item {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
  max-width: 85%;
}

.message-item.sent {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-item.received {
  align-self: flex-start;
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar-wrapper.self {
  visibility: hidden;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-full);
  object-fit: cover;
  background: var(--color-gray-300);
}

.bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-1_5);
}

.message-item.sent .bubble-wrapper {
  align-items: flex-end;
}

.message-item.received .bubble-wrapper {
  align-items: flex-start;
}

.message-bubble {
  padding: var(--space-4) var(--space-4_5);
  border-radius: var(--radius-md);
  max-width: 280px;
  word-break: break-word;
}

.message-item.sent .message-bubble {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
  color: var(--color-text-inverse);
}

.message-item.received .message-bubble {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
}

.message-text {
  margin: 0;
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
}

.message-meta {
  display: flex;
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  padding-top: var(--space-1_5);
}

.read-status {
  color: var(--color-primary-500);
}

.send-status {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.send-status.sending {
  color: var(--color-text-tertiary);
}

.send-status.failed {
  color: var(--color-error);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  line-height: 1;
}

.spin-icon {
  animation: spin 0.8s linear infinite;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  color: var(--color-gray-400);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--space-3);
  opacity: 0.6;
}

.empty-chat p {
  font-size: var(--text-sm);
  margin: 0;
}

.input-area {
  background: var(--color-bg-primary);
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-2_5) var(--space-4);
  padding-bottom: calc(var(--space-2_5) + env(safe-area-inset-bottom, 0px));
}

.toolbar {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.tool-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
  border: none;
  background: none;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.tool-btn:active,
.tool-btn.active {
  background: var(--color-primary-50);
  color: var(--color-primary-500);
}

.tool-btn svg {
  width: 22px;
  height: 22px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2_5);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  padding: var(--space-2) var(--space-3);
  border: 1.5px solid transparent;
  transition: all var(--duration-normal) var(--ease-out);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary-400);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
  background: var(--color-bg-primary);
}

.message-input {
  flex: 1;
  border: none;
  background: none;
  font-size: var(--text-base);
  color: var(--color-text-primary);
  resize: none;
  outline: none;
  max-height: 120px;
  line-height: var(--leading-normal);
  font-family: inherit;
}

.message-input::placeholder {
  color: var(--color-text-tertiary);
}

.send-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-gray-300);
  border-radius: var(--radius-full);
  color: var(--color-text-inverse);
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-out);
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.send-btn.active {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-400));
  box-shadow: var(--shadow-green);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.emoji-picker {
  position: absolute;
  bottom: 80px;
  left: var(--space-4);
  right: var(--space-4);
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  box-shadow: var(--shadow-xl);
  z-index: var(--z-dropdown);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: var(--space-2);
}

.emoji-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.emoji-item:active {
  background: var(--color-bg-secondary);
  transform: scale(0.9);
}
</style>
