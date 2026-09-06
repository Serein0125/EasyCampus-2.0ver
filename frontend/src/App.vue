<template>
  <!-- app-root 作为整个应用的挂载根节点，背景色与最小高度在这里统一控制 -->
  <div id="app" class="app-root">
    <!-- ToastProvider 包裹整棵组件树，在其内部通过 provide() 注入全局 toast 能力，
        任何子组件用 useToast() 即可调用 showToast/showConfirm，无需层层透传 props -->
    <ToastProvider>
      <!-- 顶部导航栏：只在「需要显示 TabBar」的路由（首页/商品/社区等）渲染 -->
      <AppHeader v-if="showAppHeader" @toggle-sidebar="showSideMenu = !showSideMenu" />
      
      <!-- 侧边抽屉菜单，受控显隐（v-if 级别由组件内部控制，这里只传状态） -->
      <SideMenu :visible="showSideMenu" @close="showSideMenu = false" />

      <main class="main-content" :class="{ 'no-header': !showAppHeader }">
        <div class="content-wrapper">
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

      <!-- 底部 TabBar，CSS 默认 display:none，仅在 ≤768px 移动端显示（见样式区 .mobile-navbar） -->
      <NavBar class="mobile-navbar" />
    </ToastProvider>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './store/auth'
import { useNotificationStore } from './store/notification'
import { wsManager } from './services/api'
import NavBar from './components/NavBar.vue'
import AppHeader from './components/AppHeader.vue'
import SideMenu from './components/SideMenu.vue'
import ToastProvider from './components/ToastProvider.vue'

const route = useRoute()
const { isAuthenticated } = useAuthStore()
const notificationStore = useNotificationStore()
const showSideMenu = ref(false)

// 顶部导航栏只在「主 tab 页」显示，依据路由 meta.showTabBar 判断
const showAppHeader = computed(() => route.meta.showTabBar === true)

// 初始化 WebSocket：仅在已登录、且连接未建立时建立长连接。
// WS 全局单例（wsManager）在 services/api.ts 中创建，整个应用共享一条连接。
function initWebSocket() {
  if (isAuthenticated.value) {
    const token = localStorage.getItem('token')
    if (token && !wsManager.isConnected) {
      wsManager.connect(token)

      // 订阅服务端推送的 chat_message 事件：收到新消息就刷新角标未读数
      // 注意：这里没有在 onUnmounted 里 off，因为 App.vue 与应用同生命周期，
      // 全局监听随应用销毁一起结束；页面级监听（如 ChatRoom）才需要单独 off
      wsManager.on('chat_message', () => {
        notificationStore.fetchChatUnreadCount()
      })
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

onMounted(() => {
  // 动态注入 viewport meta：禁止缩放 + viewport-fit=cover（适配刘海屏安全区）
  // 这种「运行时改 head」常见于需要根据环境定制移动端 viewport 的场景
  const viewport = document.querySelector('meta[name=viewport]')
  if (!viewport) {
    const meta = document.createElement('meta')
    meta.name = 'viewport'
    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover'
    document.head.appendChild(meta)
  }

  initWebSocket()
})

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

.mobile-navbar {
  display: none;
}

@media (max-width: 768px) {
  .main-content {
    padding-top: 52px;
    padding-bottom: calc(56px + env(safe-area-inset-bottom, 0px));
  }

  .main-content.no-header {
    padding-top: 0;
  }

  .mobile-navbar {
    display: block;
  }
}

* {
  -webkit-tap-highlight-color: transparent;
  -webkit-touch-callout: none;
}

body {
  -webkit-user-select: none;
  user-select: none;
  overscroll-behavior-y: contain;
}

input,
textarea {
  -webkit-user-select: auto;
  user-select: auto;
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

@media (max-width: 768px) {
  .hide-on-mobile {
    display: none !important;
  }
}

@media (min-width: 769px) {
  .hide-on-desktop {
    display: none !important;
  }
}
</style>
