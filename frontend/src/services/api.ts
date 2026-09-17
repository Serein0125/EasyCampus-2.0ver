import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

// 获取API基础URL（支持多环境配置）
export function getBaseURL(): string {
  // 优先使用环境变量（仅在有实际值时使用）
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL
  if (envBaseUrl && envBaseUrl.trim() !== '') {
    return `${envBaseUrl.trim()}/api`
  }

  // 开发环境：由 Vite proxy 把 /api 转发到后端 8080
  // 生产环境：由 Nginx 反向代理把 /api 转发到后端，实现同源请求
  return '/api'
}

// 创建axios实例
const instance = axios.create({
  baseURL: getBaseURL(),
  timeout: 20000, // 增加超时时间到20秒
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 解析图片URL为完整可访问地址
 * - 如果图片URL已是完整HTTP(S)地址，直接返回
 * - 如果设置了 VITE_IMAGE_BASE_URL 环境变量，拼接前缀（用于CDN/OSS等场景）
 * - 否则直接返回相对路径（默认情况下 /uploads/xxx.png 通过 nginx/vite-proxy 可访问）
 * @param url - 图片URL（如 "/uploads/2026/06/07/uuid.png"）
 * @returns 完整的图片访问地址
 */
export function resolveImageUrl(url: string | null | undefined): string {
  if (!url) return ''
  // 已经是完整URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  // 通过环境变量配置图片资源基础URL（CDN/OSS场景）
  const imageBaseUrl = import.meta.env.VITE_IMAGE_BASE_URL
  if (imageBaseUrl && imageBaseUrl.trim() !== '') {
    const base = imageBaseUrl.trim().replace(/\/+$/, '')
    return base + url
  }
  return url
}

// 请求拦截器 - 添加JWT令牌
instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理错误（增强版）
instance.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      let message = '请求失败'
      let shouldLogout = false

      // 根据状态码生成友好的错误消息
      if (data && data.message) {
        message = data.message
      } else if (status === 400) {
        message = '请求参数错误，请检查输入'
      } else if (status === 401) {
        message = '登录已过期，请重新登录'
        shouldLogout = true
      } else if (status === 403) {
        message = '权限不足，无法执行此操作'
      } else if (status === 404) {
        message = '请求的资源不存在'
      } else if (status === 429) {
        message = '操作太频繁，请稍后再试'
      } else if (status >= 500 && status < 504) {
        // 服务器错误（包括502 Bad Gateway, 503 Service Unavailable）
        const serverErrors: Record<number, string> = {
          500: '服务器内部错误',
          502: '网关错误，服务暂时不可用',
          503: '服务维护中，请稍后重试',
          504: '网关超时，请稍后重试'
        }
        message = serverErrors[status] || '服务器繁忙，请稍后重试'
      }

      // 如果需要登出（如token过期）
      if (shouldLogout) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        // 不在这里直接跳转，让组件自行决定如何处理
      }

      return Promise.reject({ message, status, code: status })
    } else if (error.request) {
      // 请求已发出但没有收到响应（网络问题）
      // 区分不同网络错误类型
      if (error.message.includes('timeout')) {
        return Promise.reject({
          message: '请求超时，请检查网络后重试',
          status: 0,
          code: 'TIMEOUT'
        })
      } else if (error.message.includes('Network Error')) {
        return Promise.reject({
          message: '网络连接失败，请检查网络设置',
          status: 0,
          code: 'NETWORK_ERROR'
        })
      }

      return Promise.reject({
        message: '网络连接失败，请检查网络设置',
        status: 0,
        code: 'NETWORK_ERROR'
      })
    } else {
      // 请求配置出错
      return Promise.reject({
        message: '请求配置错误',
        status: 0,
        code: 'CONFIG_ERROR'
      })
    }
  }
)

// 响应拦截器返回 response.data，因此这里将 axios 实例重新声明为“返回已解包数据”的客户端
type ApiClient = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const api = instance as unknown as ApiClient

// ============================================
// 用户相关API
// ============================================
export const userApi = {
  register(data: Record<string, unknown>) {
    return api.post('/v2/users/register', data)
  },

  login(data: Record<string, unknown>) {
    return api.post('/v2/users/login', data)
  },

  getUserInfo() {
    return api.get('/v2/users/info')
  },

  getUserPublic(userId: number | string) {
    return api.get(`/v2/users/${userId}`)
  },

  updateProfile(data: Record<string, unknown>) {
    return api.put('/v2/users/profile', data)
  },

  changePassword(data: Record<string, unknown>) {
    return api.put('/v2/users/password', data)
  },

  sendResetCode(account: string) {
    return api.post('/v2/users/reset-password/send-code', { account })
  },

  verifyAndResetPassword(account: string, verifyCode: string, newPassword: string) {
    return api.post('/v2/users/reset-password/verify', { account, verifyCode, newPassword })
  },

  searchUsers(keyword: string) {
    return api.get('/v2/users/search', { params: { keyword, size: 20 } })
  }
}

// ============================================
// 商品相关API
// ============================================
export const productApi = {
  getProducts(params?: Record<string, unknown>) {
    return api.get('/v2/products', { params })
  },

  getProductDetail(id: number | string) {
    return api.get(`/v2/products/${id}`)
  },

  createProduct(data: Record<string, unknown>) {
    return api.post('/v2/products', data)
  },

  updateProduct(id: number | string, data: Record<string, unknown>) {
    return api.put(`/v2/products/${id}`, data)
  },

  deleteProduct(id: number | string) {
    return api.delete(`/v2/products/${id}`)
  },

  toggleProductStatus(id: number | string, status: number | string) {
    return api.put(`/v2/products/${id}/status`, null, { params: { status } })
  },

  markAsSold(id: number | string) {
    return api.put(`/v2/products/${id}/sold`)
  },

  getMyProducts() {
    return api.get('/v2/products/my')
  },

  searchProducts(keyword: string) {
    return api.get('/v2/products/search', { params: { keyword } })
  }
}

// ============================================
// 消息/聊天相关API（增强版）
// ============================================
export const messageApi = {
  // 发送消息
  sendMessage(data: Record<string, unknown>) {
    return api.post('/v2/chat/messages', data)
  },

  // 获取对话记录（分页）
  getConversation(conversationId: number | string, params: Record<string, unknown> = {}) {
    return api.get(`/v2/chat/conversations/${conversationId}/messages`, { params })
  },

  // 获取联系人/会话列表
  getContacts() {
    return api.get('/v2/chat/conversations')
  },

  // 获取未读消息列表
  getUnreadMessages() {
    return api.get('/v2/chat/conversations')
  },

  // 标记单条消息为已读
  markAsRead(messageId: number | string) {
    return api.put(`/v2/chat/messages/${messageId}/read`)
  },

  // 标记整个对话为已读
  markConversationAsRead(conversationId: number | string) {
    return api.put(`/v2/chat/conversations/${conversationId}/read`)
  },

  // 获取未读消息数量
  getUnreadCount() {
    return api.get('/v2/chat/unread/count')
  },

  // 删除消息
  deleteMessage(messageId: number | string) {
    return api.delete(`/v2/chat/messages/${messageId}`)
  },

  // 撤回消息（发送后2分钟内）
  recallMessage(messageId: number | string) {
    return api.put(`/v2/chat/messages/${messageId}/recall`)
  },

  // 获取最近的消息预览（用于首页展示）
  getRecentMessages(limit = 10) {
    return api.get('/v2/chat/conversations', { params: { limit } })
  },

  // 创建会话（如果不存在则创建）
  createConversation(userId: number | string, productId: number | string | null) {
    return api.post('/v2/chat/messages', { userId, productId })
  }
}

// ============================================
// 收藏相关API
// ============================================
export const favoriteApi = {
  addFavorite(productId: number | string) {
    return api.post(`/v2/favorites/${productId}`)
  },
  addPostFavorite(postId: number | string) {
    return api.post(`/v2/favorites/${postId}?targetType=POST`)
  },

  removeFavorite(productId: number | string) {
    return api.delete(`/v2/favorites/${productId}`)
  },
  removePostFavorite(postId: number | string) {
    return api.delete(`/v2/favorites/${postId}?targetType=POST`)
  },

  checkFavorited(productId: number | string) {
    return api.get(`/v2/favorites/${productId}/check`)
  },
  checkPostFavorited(postId: number | string) {
    return api.get(`/v2/favorites/${postId}/check?targetType=POST`)
  },

  getMyFavorites(params?: Record<string, unknown>) {
    return api.get('/v2/favorites', { params })
  },

  getFavoriteCount() {
    return api.get('/v2/favorites/count')
  },

  getFavoriteCounts() {
    return api.get('/v2/favorites/counts')
  }
}

// ============================================
// 分类相关API
// ============================================
export const categoryApi = {
  getCategories(params?: Record<string, unknown>) {
    return api.get('/v2/categories', { params })
  },

  getCategoryTree() {
    return api.get('/v2/categories/tree')
  },

  getCategoryDetail(id: number | string) {
    return api.get(`/v2/categories/${id}`)
  },

  createCategory(data: Record<string, unknown>) {
    return api.post('/v2/categories', data)
  },

  updateCategory(id: number | string, data: Record<string, unknown>) {
    return api.put(`/v2/categories/${id}`, data)
  },

  deleteCategory(id: number | string) {
    return api.delete(`/v2/categories/${id}`)
  },

  getCategoryProducts(categoryId: number | string, params?: Record<string, unknown>) {
    return api.get(`/v2/categories/${categoryId}/products`, { params })
  }
}

// ============================================
// WebSocket连接管理（用于实时聊天）
// 能力：心跳保活 / 指数退避重连(+抖动) / 消息ACK确认 / 离线消息队列 / 消息去重
// 说明：后端 WS 端点不可用时会自动进入退避重连，聊天业务仍有 HTTP 轮询兜底，互不影响
// ============================================

/** WS 通信消息体 */
interface WsMessage {
  type: string
  /** 客户端生成的唯一消息ID，用于 ACK 确认与接收端去重 */
  clientMsgId?: string
  [key: string]: unknown
}

type WsStatus = 'disconnected' | 'connecting' | 'connected'

/** 生成唯一消息ID */
function generateMsgId(): string {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

class WebSocketManager {
  private ws: WebSocket | null = null
  private token = ''
  /** 连接状态机：disconnected / connecting / connected */
  private status: WsStatus = 'disconnected'
  /** 对外暴露的连接标记（保持与旧版 API 兼容） */
  isConnected = false

  // ---------- 重连：指数退避 + 随机抖动 ----------
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 10
  private readonly baseReconnectDelay = 1000
  private readonly maxReconnectDelay = 30000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  /** 是否为主动关闭（登出/切换账号）；主动关闭不触发自动重连 */
  private manualClose = false

  // ---------- 心跳保活 ----------
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private readonly heartbeatInterval = 25000
  /** 连续未收到任何服务端流量的心跳周期数 */
  private missedHeartbeats = 0
  private readonly maxMissedHeartbeats = 3

  // ---------- 消息 ACK ----------
  /** 等待服务端确认的消息：clientMsgId -> 超时定时器 */
  private pendingAcks = new Map<string, ReturnType<typeof setTimeout>>()
  private readonly ackTimeout = 5000

  // ---------- 离线消息队列 ----------
  private offlineQueue: WsMessage[] = []
  private readonly queueStorageKey = 'ws_offline_queue'
  private readonly maxQueueSize = 50
  private isFlushingQueue = false

  // ---------- 消息去重（滑动窗口，防止重连/重发导致重复展示） ----------
  private recentMsgIds = new Set<string>()
  private readonly maxRecentMsgIds = 200

  private listeners = new Map<string, Array<(data: any) => void>>()

  constructor() {
    this.loadOfflineQueue()
    // 页面从后台切回前台时：若处于断开状态，立即尝试重连（不必等退避定时器到期）
    document.addEventListener('visibilitychange', () => {
      if (
        document.visibilityState === 'visible' &&
        !this.manualClose &&
        this.token &&
        this.status === 'disconnected'
      ) {
        this.reconnectAttempts = 0
        this.doConnect()
      }
    })
  }

  connect(token: string) {
    this.token = token
    this.manualClose = false
    // 已连接或正在连接中，避免重复建连
    if (this.status === 'connected' || this.status === 'connecting') {
      return
    }
    this.doConnect()
  }

  /** 实际建立 WebSocket 连接 */
  private doConnect() {
    this.clearReconnectTimer()
    this.status = 'connecting'

    const hostname = window.location.hostname
    const isDev = hostname === 'localhost' || hostname === '127.0.0.1'
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'

    // 开发环境：直连 localhost:8080
    // 生产环境：使用当前域名的 wss/ws 协议（前后端一体化部署，由 Nginx 反代）
    const wsUrl =
      import.meta.env.VITE_WS_URL ||
      (isDev ? `ws://localhost:8080/ws/chat` : `${protocol}://${hostname}/ws/chat`)

    try {
      this.ws = new WebSocket(`${wsUrl}?token=${encodeURIComponent(this.token)}`)

      this.ws.onopen = () => {
        this.status = 'connected'
        this.isConnected = true
        this.reconnectAttempts = 0
        this.missedHeartbeats = 0
        this.emit('connected')
        this.startHeartbeat()
        this.flushOfflineQueue()
      }

      this.ws.onmessage = (event) => {
        // 任意入站流量都代表连接存活，重置心跳计数
        this.missedHeartbeats = 0
        try {
          const data = JSON.parse(event.data)

          // pong：心跳响应，无需分发
          if (data.type === 'pong') return

          // ack：服务端确认消息已收到
          if (data.type === 'ack' && data.clientMsgId) {
            this.handleAck(String(data.clientMsgId), true)
            return
          }
          // error：服务端拒绝该消息
          if (data.type === 'error' && data.clientMsgId) {
            this.handleAck(String(data.clientMsgId), false)
            return
          }

          // 消息去重：优先用 clientMsgId，其次用服务端消息 id
          const dedupKey = data.clientMsgId
            ? String(data.clientMsgId)
            : data.id != null
              ? `msg_${data.id}`
              : null
          if (dedupKey && !this.rememberMsgId(dedupKey)) return

          this.emit('message', data)
          // 根据消息类型再分发一次（如 chat_message / notification 等）
          if (data.type) {
            this.emit(data.type, data)
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.emit('error', error)
      }

      this.ws.onclose = () => {
        this.status = 'disconnected'
        this.isConnected = false
        this.stopHeartbeat()
        this.clearAllPendingAcks()
        this.emit('disconnected')

        // 非主动关闭 → 自动重连
        if (!this.manualClose) {
          this.scheduleReconnect()
        }
      }
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
      this.status = 'disconnected'
      this.scheduleReconnect()
    }
  }

  /** 指数退避重连：delay = min(上限, 基础 * 2^n) + 随机抖动，避免服务端重启时客户端同时重连 */
  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn(
        `[WS] 已达到最大重连次数(${this.maxReconnectAttempts})，停止重连；页面重新可见时会再尝试`
      )
      return
    }
    const exponential = this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts)
    const delay = Math.min(this.maxReconnectDelay, exponential) + Math.random() * 1000
    this.reconnectAttempts++
    console.info(`[WS] ${Math.round(delay)}ms 后进行第 ${this.reconnectAttempts} 次重连`)
    this.reconnectTimer = setTimeout(() => this.doConnect(), delay)
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  // ---------- 心跳 ----------
  private startHeartbeat() {
    this.stopHeartbeat()
    this.missedHeartbeats = 0
    this.heartbeatTimer = setInterval(() => {
      if (this.status !== 'connected') return
      // 连续多个周期没有收到任何服务端流量，判定连接已死（NAT 超时/代理断连），主动关闭以触发重连
      if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
        console.warn('[WS] 心跳超时，连接可能已断开，主动关闭以触发重连')
        try {
          this.ws?.close()
        } catch {
          /* ignore */
        }
        return
      }
      this.missedHeartbeats++
      this.rawSend({ type: 'ping', timestamp: Date.now() })
    }, this.heartbeatInterval)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  // ---------- 发送 ----------
  /** 底层发送（不做状态判断，供心跳/内部使用） */
  private rawSend(data: unknown): boolean {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(data))
        return true
      } catch (e) {
        console.error('[WS] 发送失败:', e)
        return false
      }
    }
    return false
  }

  send(data: unknown): boolean {
    if (this.status === 'connected' && this.rawSend(data)) {
      return true
    }
    console.warn('WebSocket is not connected')
    return false
  }

  /**
   * 发送聊天消息（带 clientMsgId）：
   * - 在线：立即发送并等待服务端 ACK，超时宽容处理（兼容暂不支持 ACK 的后端）
   * - 离线：进入离线队列并持久化，重连成功后自动补发
   */
  sendMessage(
    content: string,
    receiverId: number | string,
    productId: number | string | null = null
  ): boolean {
    const msg: WsMessage = {
      type: 'chat',
      clientMsgId: generateMsgId(),
      content,
      receiverId,
      productId,
      timestamp: Date.now()
    }

    if (this.status === 'connected') {
      this.sendWithAck(msg)
    } else {
      this.enqueueOffline(msg)
    }
    return true
  }

  /** 发送消息并挂 ACK 超时定时器 */
  private sendWithAck(msg: WsMessage) {
    if (!msg.clientMsgId) {
      this.rawSend(msg)
      return
    }
    this.rawSend(msg)
    this.pendingAcks.set(
      msg.clientMsgId,
      setTimeout(() => {
        this.pendingAcks.delete(msg.clientMsgId!)
        // 宽容策略：后端可能暂不支持 ACK，不重发（避免重复消息），仅发出可观测事件
        console.warn('[WS] 消息未收到 ACK（后端可能暂不支持），按已发送处理:', msg.clientMsgId)
        this.emit('ack_timeout', msg)
      }, this.ackTimeout)
    )
  }

  /** 处理服务端 ACK / 错误回执 */
  private handleAck(clientMsgId: string, ok: boolean) {
    const timer = this.pendingAcks.get(clientMsgId)
    if (timer) {
      clearTimeout(timer)
      this.pendingAcks.delete(clientMsgId)
    }
    this.emit(ok ? 'ack' : 'message_failed', { clientMsgId })
  }

  private clearAllPendingAcks() {
    this.pendingAcks.forEach((timer) => clearTimeout(timer))
    this.pendingAcks.clear()
  }

  // ---------- 离线队列 ----------
  private enqueueOffline(msg: WsMessage) {
    this.offlineQueue.push(msg)
    // 队列上限：超出丢弃最旧的消息，避免 localStorage 膨胀
    if (this.offlineQueue.length > this.maxQueueSize) {
      this.offlineQueue.shift()
    }
    this.persistOfflineQueue()
    console.info(`[WS] 当前未连接，消息已进入离线队列（${this.offlineQueue.length} 条），重连后自动补发`)
  }

  /** 重连成功后按序补发离线消息 */
  private flushOfflineQueue() {
    if (this.isFlushingQueue || this.offlineQueue.length === 0) return
    this.isFlushingQueue = true
    // 取出快照并立即清空持久化，补发失败的消息靠接收端 clientMsgId 去重兜底
    const queued = this.offlineQueue
    this.offlineQueue = []
    this.persistOfflineQueue()

    queued.forEach((msg) => this.sendWithAck(msg))
    console.info(`[WS] 离线队列补发完成，共 ${queued.length} 条`)
    this.isFlushingQueue = false
  }

  private loadOfflineQueue() {
    try {
      const raw = localStorage.getItem(this.queueStorageKey)
      if (raw) {
        const parsed = JSON.parse(raw)
        this.offlineQueue = Array.isArray(parsed) ? parsed : []
      }
    } catch {
      this.offlineQueue = []
    }
  }

  private persistOfflineQueue() {
    try {
      if (this.offlineQueue.length > 0) {
        localStorage.setItem(this.queueStorageKey, JSON.stringify(this.offlineQueue))
      } else {
        localStorage.removeItem(this.queueStorageKey)
      }
    } catch (e) {
      console.error('[WS] 离线队列持久化失败:', e)
    }
  }

  // ---------- 去重窗口 ----------
  /** 记录消息ID，返回 false 表示重复消息 */
  private rememberMsgId(id: string): boolean {
    if (this.recentMsgIds.has(id)) return false
    // 窗口满了淘汰最早的一条（Set 按插入顺序迭代）
    if (this.recentMsgIds.size >= this.maxRecentMsgIds) {
      const first = this.recentMsgIds.values().next().value
      if (first) this.recentMsgIds.delete(first)
    }
    this.recentMsgIds.add(id)
    return true
  }

  disconnect() {
    this.manualClose = true
    this.clearReconnectTimer()
    this.stopHeartbeat()
    this.clearAllPendingAcks()
    if (this.ws) {
      try {
        this.ws.close()
      } catch {
        /* ignore */
      }
      this.ws = null
    }
    this.status = 'disconnected'
    this.isConnected = false
  }

  on(event: string, callback: (data: any) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)!.push(callback)
  }

  off(event: string, callback: (data: any) => void) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)!
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  emit(event: string, data?: unknown) {
    if (this.listeners.has(event)) {
      this.listeners.get(event)!.forEach((callback) => {
        try {
          callback(data)
        } catch (e) {
          console.error(`Error in event handler for ${event}:`, e)
        }
      })
    }
  }
}

// ==================== 全局搜索 API ====================
export const searchApi = {
  search(params: Record<string, unknown>) { return api.get('/v2/search', { params }) }
}

// ==================== 社区帖子 API ====================
export const postApi = {
  getPosts(params: Record<string, unknown>) { return api.get('/v2/posts', { params }) },
  getPostDetail(id: number | string) { return api.get(`/v2/posts/${id}`) },
  createPost(data: Record<string, unknown>) { return api.post('/v2/posts', data) },
  updatePost(id: number | string, data: Record<string, unknown>) { return api.put(`/v2/posts/${id}`, data) },
  deletePost(id: number | string) { return api.delete(`/v2/posts/${id}`) },
  getUserPosts(userId: number | string) { return api.get(`/v2/posts/user/${userId}`) },
  addComment(postId: number | string, data: Record<string, unknown>) { return api.post(`/v2/posts/${postId}/comments`, data) },
  getComments(postId: number | string, params: Record<string, unknown> = {}) { return api.get(`/v2/posts/${postId}/comments`, { params }) },
  deleteComment(commentId: number | string) { return api.delete(`/v2/posts/comments/${commentId}`) },
  togglePin(id: number | string, isPinned: boolean) { return api.put(`/v2/posts/${id}/pin`, null, { params: { isPinned } }) },
  toggleEssence(id: number | string, isEssence: boolean) { return api.put(`/v2/posts/${id}/essence`, null, { params: { isEssence } }) }
}

// ==================== 商品评论 API ====================
export const productCommentApi = {
  addComment(productId: number | string, data: Record<string, unknown>) { return api.post(`/v2/products/${productId}/comments`, data) },
  getComments(productId: number | string, params: Record<string, unknown> = {}) { return api.get(`/v2/products/${productId}/comments`, { params }) },
  deleteComment(commentId: number | string) { return api.delete(`/v2/products/comments/${commentId}`) }
}

// ==================== 点赞 API ====================
export const likeApi = {
  toggleLike(targetType: string, targetId: number | string) { return api.post('/v2/likes/toggle', null, { params: { targetType, targetId } }) },
  checkLiked(targetType: string, targetId: number | string) { return api.get('/v2/likes/check', { params: { targetType, targetId } }) },
  getLikeCount(targetType: string, targetId: number | string) { return api.get('/v2/likes/count', { params: { targetType, targetId } }) }
}

// ==================== 关注 API ====================
export const followApi = {
  toggleFollow(followeeId: number | string) { return api.post('/v2/follows/toggle', null, { params: { followeeId } }) },
  checkFollowing(followeeId: number | string) { return api.get('/v2/follows/check', { params: { followeeId } }) },
  getFollowStats(userId: number | string) { return api.get('/v2/follows/stats', { params: { userId } }) },
  getFollowingList(userId: number | string, page = 1, size = 20) { return api.get('/v2/follows/following', { params: { userId, page, size } }) },
  getFollowerList(userId: number | string, page = 1, size = 20) { return api.get('/v2/follows/followers', { params: { userId, page, size } }) }
}

// ==================== 信息流 API ====================
export const feedApi = {
  getFeed(params: Record<string, unknown>) { return api.get('/v2/feed', { params }) },
  getRecommendations(limit = 6) { return api.get('/v2/feed/recommend', { params: { limit } }) },
  recordBehavior(targetType: string, targetId: number | string) { return api.post('/v2/feed/behavior', { targetType, targetId }) }
}

// ==================== 活动 API ====================
export const activityApi = {
  getActivities(params: Record<string, unknown>) { return api.get('/v2/activities', { params }) },
  getActivityDetail(id: number | string) { return api.get(`/v2/activities/${id}`) },
  createActivity(data: Record<string, unknown>) { return api.post('/v2/activities', data) },
  getMyActivities(params: Record<string, unknown>) { return api.get('/v2/activities/my', { params }) },
  joinActivity(id: number | string) { return api.post(`/v2/activities/${id}/join`) },
  cancelJoin(id: number | string) { return api.delete(`/v2/activities/${id}/join`) },
  deleteActivity(id: number | string) { return api.delete(`/v2/activities/${id}`) }
}

// ==================== 商品故事 API ====================
export const storyApi = {
  getStoryFeed(params?: Record<string, unknown>) { return api.get('/v2/stories/feed', { params }) },
  getUserStories(userId: number | string) { return api.get(`/v2/stories/user/${userId}`) }
}

// ==================== 通知 API ====================
export const notificationApi = {
  /** 获取通知列表，支持按类型筛选 */
  getNotifications(params: Record<string, unknown> = {}) {
    return api.get('/v2/notifications', { params })
  },
  /** 获取私信通知列表 */
  getChatNotifications(params: Record<string, unknown> = {}) {
    return api.get('/v2/notifications/chats', { params })
  },
  /** 获取未读通知数 */
  getUnreadCount() {
    return api.get('/v2/notifications/unread/count')
  },
  /** 标记单条通知为已读 */
  markAsRead(id: number | string) {
    return api.put(`/v2/notifications/${id}/read`)
  },
  /** 标记所有通知为已读 */
  markAllAsRead() {
    return api.put('/v2/notifications/read/all')
  },
  /** 清空所有通知 */
  clearAll() {
    return api.delete('/v2/notifications/clear')
  }
}

// ==================== 广告 API ====================
export const adApi = {
  createAd(data: Record<string, unknown>) { return api.post('/v2/ads', data) },
  getPackages() { return api.get('/v2/ads/packages') },
  simulatePayment(postId: number | string, packageId: number | string) { return api.post(`/v2/ads/${postId}/pay`, { packageId }) }
}

// ==================== 文件上传 API ====================
export const uploadApi = {
  // 上传单张图片
  uploadImage(file: File, onProgress?: (evt: any) => void) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/v2/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
      timeout: 60000
    })
  },

  // 批量上传图片
  uploadImages(files: File[], onProgress?: (evt: any) => void) {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))
    return api.post('/v2/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
      timeout: 120000
    })
  },

  // 上传头像
  uploadAvatar(file: File, onProgress?: (evt: any) => void) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/v2/upload/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
      timeout: 60000
    })
  },

  // 上传分片
  uploadChunk(chunk: Blob, fileId: string, chunkIndex: number, totalChunks: number, fileName: string) {
    const formData = new FormData()
    formData.append('chunk', chunk)
    formData.append('fileId', fileId)
    formData.append('chunkIndex', String(chunkIndex))
    formData.append('totalChunks', String(totalChunks))
    formData.append('fileName', fileName)
    return api.post('/v2/upload/chunk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })
  },

  // 合并分片
  mergeChunks(fileId: string, fileName: string, totalChunks: number) {
    return api.post('/v2/upload/merge', null, {
      params: { fileId, fileName, totalChunks }
    })
  }
}

// ==================== 组织 API ====================
export const organizationApi = {
  create(data: Record<string, unknown>) { return api.post('/v2/organizations', data) },
  getDetail(id: number | string) { return api.get(`/v2/organizations/${id}`) },
  getMyOrgs() { return api.get('/v2/organizations/my') },
  getList(params: Record<string, unknown>) { return api.get('/v2/organizations', { params }) },
  invite(orgId: number | string, inviteeId: number | string) { return api.post(`/v2/organizations/${orgId}/invite`, { inviteeId }) },
  acceptInvite(code: string) { return api.post(`/v2/organizations/invitations/${code}/accept`) },
  rejectInvite(code: string) { return api.post(`/v2/organizations/invitations/${code}/reject`) },
  getMyInvitations() { return api.get('/v2/organizations/invitations/my') },
  applyJoin(orgId: number | string, message: string) { return api.post(`/v2/organizations/${orgId}/apply`, { message }) },
  getPendingRequests(orgId: number | string) { return api.get(`/v2/organizations/${orgId}/requests`) },
  approveRequest(requestId: number | string) { return api.put(`/v2/organizations/requests/${requestId}/approve`) },
  rejectRequest(requestId: number | string) { return api.put(`/v2/organizations/requests/${requestId}/reject`) },
  getMembers(orgId: number | string, params: Record<string, unknown>) { return api.get(`/v2/organizations/${orgId}/members`, { params }) },
  removeMember(orgId: number | string, userId: number | string) { return api.delete(`/v2/organizations/${orgId}/members/${userId}`) },
  changeRole(orgId: number | string, userId: number | string, role: string) { return api.put(`/v2/organizations/${orgId}/members/${userId}/role`, { role }) },
  getMyRole(orgId: number | string) { return api.get(`/v2/organizations/${orgId}/my-role`) },
  leaveOrg(orgId: number | string) { return api.post(`/v2/organizations/${orgId}/leave`) },
  getAuditLogs(orgId: number | string, limit: number) { return api.get(`/v2/organizations/${orgId}/audit-logs`, { params: { limit } }) },
  approveOrg(id: number | string) { return api.put(`/v2/organizations/${id}/approve`) },
  rejectOrg(id: number | string) { return api.put(`/v2/organizations/${id}/reject`) }
}

// ==================== 数据看板 API ====================
export const dashboardApi = {
  getStats() { return api.get('/v2/dashboard/stats') }
}

// 创建全局WebSocket实例
export const wsManager = new WebSocketManager()

export default api