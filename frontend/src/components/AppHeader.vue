<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <div class="user-area">
          <template v-if="isAuthenticated">
            <button class="user-avatar-link" @click="toggleSidebar" title="打开菜单">
              <img
                v-if="userAvatar && !avatarLoadError"
                :src="userAvatar"
                class="user-avatar"
                alt="用户头像"
                @error="avatarLoadError = true"
              />
              <div v-else class="user-avatar-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </div>
              <!-- 未读红点（通知+聊天合并） -->
              <span v-if="hasUnread" class="unread-badge"></span>
            </button>
          </template>
          <template v-else>
            <router-link to="/login" class="login-btn">
              登录
            </router-link>
          </template>
        </div>
        <router-link to="/" class="logo-link">
          <span class="logo-text hide-on-mobile">易校EasyCampus</span>
          <span class="logo-text hide-on-desktop">易校</span>
          <span class="logo-sub hide-on-mobile">EasyCampus</span>
        </router-link>
      </div>

      <nav class="header-center">
        <div class="desktop-nav hide-on-mobile">
          <router-link
            v-for="item in desktopNavItems"
            :key="item.path"
            :to="item.path"
            class="desktop-nav-item"
            :class="{ active: isDesktopActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </div>
        <div class="mobile-nav hide-on-desktop">
          <button
            v-for="tab in headerTabs"
            :key="tab.key"
            class="mobile-nav-tab"
            :class="{ active: activeTab === tab.key }"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>
      </nav>

      <div class="header-right">
        <div
          class="search-box"
          :class="{ expanded: searchExpanded }"
          @click="activateSearch"
        >
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          <input
            ref="searchInput"
            type="text"
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索商品、社区..."
            @keyup.enter="handleSearch"
            @focus="searchExpanded = true"
            @blur="handleSearchBlur"
          />
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'

const emit = defineEmits(['toggleSidebar'])
function toggleSidebar() {
  emit('toggleSidebar')
}

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser } = useAuthStore()
const { hasUnread } = useNotificationStore()

const headerTabs = [
  { key: 'following', label: '关注' },
  { key: 'discover', label: '发现' }
]

const desktopNavItems = [
  { label: '首页', path: '/' },
  { label: '社区', path: '/community' },
  { label: '商品', path: '/products' },
  { label: '活动', path: '/activities' }
]

const searchKeyword = ref('')
const searchExpanded = ref(false)
const searchInput = ref(null)
const avatarLoadError = ref(false)

const userAvatar = computed(() => currentUser.value?.avatar || '')

const activeTab = computed(() => {
  if (route.path === '/') {
    return route.query.tab || 'discover'
  }
  return 'discover'
})

function isDesktopActive(path) {
  if (path === '/') {
    return route.path === '/' && !route.query.tab
  }
  return route.path.startsWith(path)
}

function switchTab(key) {
  router.push({ path: '/', query: { tab: key } })
}

function activateSearch() {
  searchExpanded.value = true
  searchInput.value?.focus()
}

function handleSearchBlur() {
  if (!searchKeyword.value.trim()) {
    searchExpanded.value = false
  }
}

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searchKeyword.value = ''
  searchExpanded.value = false
  searchInput.value?.blur()
  router.push({ path: '/search', query: { keyword: kw } })
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-sticky, 200);
  height: var(--header-height, 64px);
  background: var(--gradient-glass, linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.7) 100%));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04));
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: var(--container-xl, 1200px);
  margin: 0 auto;
  padding: 0 var(--space-6, 1.5rem);
  height: 100%;
}

.header-left {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-link {
  display: flex;
  align-items: baseline;
  gap: var(--space-2, 0.5rem);
  text-decoration: none;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.logo-sub {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  font-weight: var(--font-normal, 400);
  letter-spacing: 0.02em;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: var(--space-1, 0.25rem);
}

.desktop-nav-item {
  position: relative;
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  font-size: var(--text-sm, 0.8125rem);
  font-weight: var(--font-medium, 500);
  color: var(--color-text-secondary, #6b7280);
  text-decoration: none;
  border-radius: var(--radius-md, 10px);
  transition: color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              background-color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
  cursor: pointer;
}

.desktop-nav-item:hover {
  color: var(--color-primary-600, #059669);
  background-color: var(--color-primary-50, #ecfdf5);
}

.desktop-nav-item.active {
  color: var(--color-primary-600, #059669);
  font-weight: var(--font-semibold, 600);
}

.desktop-nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  border-radius: var(--radius-full, 9999px);
  animation: slideUnderline var(--duration-slow, 350ms) var(--ease-spring, cubic-bezier(0.34, 1.56, 0.64, 1));
}

@keyframes slideUnderline {
  from {
    width: 0;
    opacity: 0;
  }
  to {
    width: 24px;
    opacity: 1;
  }
}

.mobile-nav {
  display: flex;
  align-items: center;
}

.mobile-nav-tab {
  padding: var(--space-2, 0.5rem) var(--space-5, 1.25rem);
  font-size: 15px;
  font-weight: var(--font-medium, 500);
  color: var(--color-text-secondary, #6b7280);
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  transition: color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.mobile-nav-tab.active {
  color: var(--color-primary-600, #059669);
  font-weight: var(--font-bold, 700);
}

.mobile-nav-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 3px;
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  border-radius: var(--radius-full, 9999px);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-shrink: 0;
  min-width: 0;
}

.search-box {
  display: flex;
  align-items: center;
  gap: var(--space-1_5, 0.375rem);
  background: var(--color-gray-100, #f3f4f6);
  border-radius: var(--radius-full, 9999px);
  padding: var(--space-1_5, 0.375rem) var(--space-3, 0.75rem);
  cursor: pointer;
  transition: width var(--duration-slow, 350ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              background-color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              box-shadow var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              border-color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
  width: 160px;
  border: 1.5px solid transparent;
}

.search-box.expanded {
  width: 260px;
  background: var(--color-bg-primary, #ffffff);
  border-color: var(--color-primary-400, #34d399);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.search-icon {
  color: var(--color-text-tertiary, #9ca3af);
  flex-shrink: 0;
  pointer-events: none;
  transition: color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.search-box.expanded .search-icon {
  color: var(--color-primary-500, #10b981);
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-size: var(--text-sm, 0.8125rem);
  font-family: var(--font-sans, inherit);
  color: var(--color-text-primary, #111827);
  outline: none;
  min-width: 60px;
}

.search-input::placeholder {
  color: var(--color-text-tertiary, #9ca3af);
}

.user-area {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-right: var(--space-2, 0.5rem);
}

.user-avatar-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  cursor: pointer;
  border: none;
  background: none;
  padding: 0;
  position: relative;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full, 9999px);
  object-fit: cover;
  border: 2px solid var(--color-primary-100, #d1fae5);
  transition: border-color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              box-shadow var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.user-avatar:hover {
  border-color: var(--color-primary-400, #34d399);
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
}

.user-avatar-placeholder {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-primary-50, #ecfdf5);
  border: 2px solid var(--color-primary-100, #d1fae5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-500, #10b981);
  transition: border-color var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              box-shadow var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.user-avatar-placeholder:hover {
  border-color: var(--color-primary-400, #34d399);
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
}

/* 头像右下角未读红点 */
.unread-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-rose-500, #f43f5e);
  border: 2px solid #fff;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(244, 63, 94, 0.4); }
  50% { box-shadow: 0 0 0 4px rgba(244, 63, 94, 0); }
}

.login-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-1_5, 0.375rem) var(--space-5, 1.25rem);
  font-size: var(--text-sm, 0.8125rem);
  font-weight: var(--font-semibold, 600);
  font-family: var(--font-sans, inherit);
  color: white;
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  border-radius: var(--radius-full, 9999px);
  text-decoration: none;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
  transition: box-shadow var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
              transform var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
  cursor: pointer;
}

.login-btn:hover {
  box-shadow: var(--shadow-green-lg, 0 8px 25px rgba(16,185,129,0.3));
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0) scale(0.97);
}

@media (max-width: 768px) {
  .app-header {
    height: 52px;
  }

  .header-inner {
    padding: 0 var(--space-3, 0.75rem);
  }

  .logo-text {
    font-size: 17px;
  }

  .search-box.expanded {
    width: 160px;
  }

  .user-avatar,
  .user-avatar-placeholder {
    width: 30px;
    height: 30px;
  }

  .login-btn {
    padding: var(--space-1, 0.25rem) var(--space-3, 0.75rem);
    font-size: var(--text-xs, 0.6875rem);
  }
}
</style>
