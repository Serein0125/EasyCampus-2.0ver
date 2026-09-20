<template>
  <!-- ElConfigProvider：给 Element Plus 组件注入中文语言包（确认框、校验提示等文案） -->
  <el-config-provider :locale="zhCn">
    <!-- app-root 作为整个应用的挂载根节点，背景色与最小高度在这里统一控制 -->
    <div id="app" class="app-root">
    <!-- 顶部导航栏：只在「主 tab 页」（路由 meta.showHeader=true）渲染 -->
    <AppHeader v-if="showAppHeader" @toggle-sidebar="showSideMenu = !showSideMenu" />

    <!-- 侧边抽屉菜单，受控显隐（v-if 级别由组件内部控制，这里只传状态） -->
    <SideMenu :visible="showSideMenu" @close="showSideMenu = false" />

    <main class="main-content" :class="{ 'no-header': !showAppHeader }">
      <!-- is-full-bleed：登录页等整屏页面解除 max-width 限制（用 max-width:none 而非 100vw，避免滚动条宽度造成横向溢出） -->
      <div class="content-wrapper" :class="{ 'is-full-bleed': isFullBleed }">
        <!-- router-view 用作用域插槽拿到当前路由组件与路由对象，
            再配合 <transition> 实现页面切换动画：
            - 动画名取自路由 meta.transition（如详情页用 'slide' 左右滑入），缺省用 'page' 淡入淡出
            - mode="out-in" 保证先离开再进入，避免两个页面同时存在重叠
            - :key 绑定 path，路由变化时强制重新挂载组件，触发过渡动画 -->
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition :name="currentRoute.meta.transition || 'page'" mode="out-in">
            <component :is="Component" :key="currentRoute.path" />
          </transition>
        </router-view>
      </div>
    </main>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onUnmounted, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useAuthStore } from './store/auth'
import { useNotificationStore } from './store/notification'
import { wsManager } from './services/api'
import AppHeader from './components/AppHeader.vue'
import SideMenu from './components/SideMenu.vue'

const route = useRoute()
const { isAuthenticated } = useAuthStore()
const notificationStore = useNotificationStore()
const showSideMenu = ref(false)

// 顶部导航栏只在「主 tab 页」显示，依据路由 meta.showHeader 判断
const showAppHeader = computed(() => route.meta.showHeader === true)

// 整屏页面（如登录页）解除内容容器的最大宽度限制
const isFullBleed = computed(() => route.meta.fullBleed === true)

// chat_message 的固定回调引用：off/on 需要同一个函数引用才能正确解绑
function handleChatMessage() {
  notificationStore.fetchChatUnreadCount()
}

// 初始化 WebSocket：仅在已登录、且连接未建立时建立长连接。
// WS 全局单例（wsManager）在 services/api.ts 中创建，整个应用共享一条连接。
function initWebSocket() {
  if (isAuthenticated.value) {
    const token = localStorage.getItem('token')
    if (token && !wsManager.isConnected) {
      wsManager.connect(token)

      // 订阅服务端推送的 chat_message 事件：收到新消息就刷新角标未读数。
      // 先 off 再 on：disconnect() 不会清空 listeners，多次登录会让监听累积，
      // 导致每条消息触发多次未读数请求。App.vue 与应用同生命周期，无需在卸载时 off。
      wsManager.off('chat_message', handleChatMessage)
      wsManager.on('chat_message', handleChatMessage)
    }
  }
}

// 用 watch 监听登录状态，实现「登录即建连、登出即断连」的响应式生命周期：
// - immediate: true 保证页面刷新（已是登录态）时立即触发一次
// - 这是把「状态变化」作为「副作用触发器」的典型 Composition API 写法
watch(
  isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      initWebSocket()
      notificationStore.startPolling() // 登录后启动 30s 未读数轮询
    } else {
      wsManager.disconnect()          // 登出断开 WS
      notificationStore.stopPolling() // 停止轮询，避免对已登出用户发请求
    }
  },
  { immediate: true }
)

// 上面 watch(isAuthenticated, ..., { immediate: true }) 已覆盖首次挂载，
// 这里不再重复调用 initWebSocket，否则同一帧内会注册两次 chat_message 监听
onUnmounted(() => {
  // App.vue 一般不会卸载，这里属于防御性清理，避免 HMR/SPA 重建时残留定时器
  notificationStore.stopPolling()
})
</script>

<style>
@import './assets/css/design-system.css';

.app-root {
  min-height: 100vh;
  background-color: var(--color-bg-page, #f0fdf4);
  position: relative;
}

.main-content {
  min-height: 100vh;
  padding-top: 64px;
  padding-bottom: 0;
  transition: padding 0.3s ease;
}

.main-content.no-header {
  padding-top: 0;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.content-wrapper.is-full-bleed {
  max-width: none;
}

.page-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 详情页左右滑入动画 */
.slide-enter-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.3s ease;
}

.slide-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.slide-enter-from {
  transform: translateX(30%);
  opacity: 0;
}

.slide-leave-to {
  transform: translateX(-20%);
  opacity: 0;
}

* {
  -webkit-tap-highlight-color: transparent;
}

::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.15);
  border-radius: 20px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.25);
}

img {
  max-width: 100%;
  height: auto;
  display: block;
}

a {
  color: inherit;
  text-decoration: none;
}

button {
  font-family: inherit;
  cursor: pointer;
  border: none;
  background: none;
  padding: 0;
  margin: 0;
}

input,
textarea,
select {
  font-family: inherit;
  font-size: inherit;
}
</style>
