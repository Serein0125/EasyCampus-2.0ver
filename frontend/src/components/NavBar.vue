<template>
  <nav class="tab-bar" v-if="showTabBar">
    <div class="tab-bar-container">
      <template v-for="tab in allTabs" :key="tab.path || tab.key">
        <button
          v-if="tab.key === 'publish'"
          class="tab-item publish-tab-item"
          @click="showPublishSheet = true"
        >
          <div class="publish-btn">
            <svg class="publish-icon" viewBox="0 0 24 24" fill="none">
              <path d="M12 8v8M8 12h8" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="tab-label">发布</span>
        </button>

        <router-link
          v-else
          :to="tab.path"
          class="tab-item"
          :class="{ active: isActive(tab.path) }"
        >
          <div class="tab-icon-wrapper">
            <span
              v-if="tab.badge && unreadCount > 0"
              class="tab-badge"
            >
              {{ unreadCount > 99 ? '99+' : unreadCount }}
              <span class="badge-pulse"></span>
            </span>

            <svg
              v-if="tab.icon === 'home'"
              class="tab-icon"
              viewBox="0 0 24 24"
              fill="none"
            >
              <path
                d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"
                :fill="isActive(tab.path) ? 'var(--color-primary-500)' : 'none'"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
              />
              <polyline
                points="9,22 9,12 15,12 15,22"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                fill="none"
              />
            </svg>

            <svg
              v-else-if="tab.icon === 'community'"
              class="tab-icon"
              viewBox="0 0 24 24"
              fill="none"
            >
              <path
                d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <circle
                cx="9" cy="7" r="4"
                :fill="isActive(tab.path) ? 'var(--color-primary-500)' : 'none'"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
              />
              <path
                d="M23 21v-2a4 4 0 00-3-3.87"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M16 3.13a4 4 0 010 7.75"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>

            <svg
              v-else-if="tab.icon === 'product'"
              class="tab-icon"
              viewBox="0 0 24 24"
              fill="none"
            >
              <path
                d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"
                :fill="isActive(tab.path) ? 'var(--color-primary-500)' : 'none'"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <line
                x1="3" y1="6" x2="21" y2="6"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
              />
              <path
                d="M16 10a4 4 0 01-8 0"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>

            <svg
              v-else-if="tab.icon === 'activity'"
              class="tab-icon"
              viewBox="0 0 24 24"
              fill="none"
            >
              <rect
                x="3" y="4" width="18" height="18" rx="2" ry="2"
                :fill="isActive(tab.path) ? 'var(--color-primary-500)' : 'none'"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
              />
              <line
                x1="16" y1="2" x2="16" y2="6"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
              />
              <line
                x1="8" y1="2" x2="8" y2="6"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
                stroke-linecap="round"
              />
              <line
                x1="3" y1="10" x2="21" y2="10"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
              />
            </svg>

            <svg
              v-else-if="tab.icon === 'message'"
              class="tab-icon"
              viewBox="0 0 24 24"
              fill="none"
            >
              <path
                d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"
                :fill="isActive(tab.path) ? 'var(--color-primary-500)' : 'none'"
                :stroke="isActive(tab.path) ? 'var(--color-primary-500)' : 'currentColor'"
                stroke-width="1.8"
              />
            </svg>
          </div>
          <span class="tab-label">{{ tab.label }}</span>
        </router-link>
      </template>
    </div>
  </nav>

  <PublishActionSheet :visible="showPublishSheet" @close="showPublishSheet = false" />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'
import PublishActionSheet from './PublishActionSheet.vue'

const route = useRoute()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const showPublishSheet = ref(false)

const allTabs = [
  {
    path: '/',
    label: '首页',
    icon: 'home',
    badge: false
  },
  {
    path: '/community',
    label: '社区',
    icon: 'community',
    badge: false
  },
  {
    path: '/products',
    label: '商品',
    icon: 'product',
    badge: false
  },
  {
    key: 'publish'
  },
  {
    path: '/activities',
    label: '活动',
    icon: 'activity',
    badge: false
  },
  {
    path: '/messages',
    label: '消息',
    icon: 'message',
    badge: true
  }
]

/** 从 notification store 读取聊天未读数 */
const unreadCount = computed(() => notificationStore.chatUnreadCount.value)

const showTabBar = computed(() => {
  return route.meta.showTabBar !== false
})

function isActive(path) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}

watch(
  () => authStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      // 登录时从 store 拉取最新未读数
      notificationStore.fetchChatUnreadCount()
    }
  },
  { immediate: true }
)


</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: var(--z-sticky, 200);
  background-color: var(--color-bg-primary, #ffffff);
  border-top: 1px solid var(--color-border-light, #e5e7eb);
  box-shadow: 0 -1px 12px rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar-container {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  max-width: 500px;
  margin: 0 auto;
  height: var(--tabbar-height, 56px);
  padding: 0 12px;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1;
  height: 100%;
  text-decoration: none;
  color: var(--color-gray-400, #9ca3af);
  position: relative;
  padding: 6px 0;
  border: none;
  background: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.tab-item:active .tab-icon {
  transform: scale(0.85);
}

.tab-item.active {
  color: var(--color-primary-500, #10b981);
}

.tab-item.active .tab-icon {
  transform: scale(1.08);
}

.tab-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.tab-icon {
  width: 22px;
  height: 22px;
  transition: transform var(--duration-normal, 200ms) var(--ease-spring, cubic-bezier(0.34, 1.56, 0.64, 1));
}

.tab-label {
  font-size: 9px;
  font-weight: var(--font-medium, 500);
  letter-spacing: 0.02em;
  line-height: 1;
  transition: color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.tab-item.active .tab-label {
  font-weight: var(--font-semibold, 600);
  color: var(--color-primary-500, #10b981);
}

.publish-tab-item {
  position: relative;
  z-index: 1;
}

.publish-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full, 9999px);
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
  margin-top: -14px;
  transition: transform var(--duration-normal, 200ms) var(--ease-spring, cubic-bezier(0.34, 1.56, 0.64, 1)),
              box-shadow var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.publish-tab-item:active .publish-btn {
  transform: scale(0.9);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);
}

.publish-icon {
  width: 24px;
  height: 24px;
}

.publish-tab-item .tab-label {
  margin-top: 2px;
  color: var(--color-gray-400, #9ca3af);
}

.tab-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: var(--color-rose-500, #f43f5e);
  color: white;
  font-size: 10px;
  font-weight: var(--font-semibold, 600);
  font-family: var(--font-sans, inherit);
  border-radius: var(--radius-full, 9999px);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--color-bg-primary, #ffffff);
  line-height: 1;
  z-index: 2;
}

.badge-pulse {
  position: absolute;
  inset: -2px;
  border-radius: var(--radius-full, 9999px);
  background-color: var(--color-rose-500, #f43f5e);
  opacity: 0;
  z-index: -1;
  animation: badgePulse 2s ease-out infinite;
}

@keyframes badgePulse {
  0% {
    transform: scale(1);
    opacity: 0.5;
  }
  100% {
    transform: scale(1.8);
    opacity: 0;
  }
}

@media (min-width: 769px) {
  .tab-bar {
    display: none;
  }
}
</style>
