<template>
  <div class="profile-page">
    <div class="profile-container">
      <header class="profile-header" v-if="isAuthenticated">
        <div class="header-bg">
          <div class="header-bg-pattern"></div>
        </div>
        <div class="user-info-card">
          <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" @change="handleAvatarChange" hidden />
          <div class="avatar-section" @click="handleAvatarClick">
            <img
              :src="userInfo.avatar || defaultAvatar"
              :alt="userInfo.nickname || '用户'"
              class="user-avatar"
            />
            <span class="edit-avatar-badge">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </span>
          </div>
          <h2 class="user-name">{{ userInfo.nickname || userInfo.username || '未设置昵称' }}</h2>
          <p class="user-id">ID: {{ userInfo.id || '---' }}</p>
          <div v-if="myOrgs.length > 0" class="user-orgs">
            <span
              v-for="org in myOrgs"
              :key="org.id"
              class="org-tag"
              @click="router.push(`/orgs/${org.id}`)"
            >{{ org.name }}</span>
          </div>
          <p v-if="userInfo.bio" class="user-bio">{{ userInfo.bio }}</p>

          <div class="stats-row">
            <div class="stat-item" @click="router.push('/my-products')">
              <span class="stat-value">
                <span v-if="statsLoading" class="stat-skeleton"></span>
                <template v-else>{{ stats.published }}</template>
              </span>
              <span class="stat-label">发布</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="router.push('/favorites')">
              <span class="stat-value">
                <span v-if="statsLoading" class="stat-skeleton"></span>
                <template v-else>{{ stats.favorites }}</template>
              </span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="router.push({ path: '/follows', query: { userId: userInfo.id, tab: 'following' } })">
              <span class="stat-value">
                <span v-if="statsLoading" class="stat-skeleton"></span>
                <template v-else>{{ stats.following }}</template>
              </span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="router.push({ path: '/follows', query: { userId: userInfo.id, tab: 'followers' } })">
              <span class="stat-value">
                <span v-if="statsLoading" class="stat-skeleton"></span>
                <template v-else>{{ stats.followers }}</template>
              </span>
              <span class="stat-label">粉丝</span>
            </div>
          </div>
        </div>
      </header>

      <section v-if="!isAuthenticated" class="auth-welcome">
        <div class="auth-card">
          <div class="auth-card-bg"></div>
          <div class="auth-card-content">
            <div class="auth-logo">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <rect width="48" height="48" rx="14" fill="url(#auth-grad)"/>
                <path d="M24 14c-4.42 0-8 2.69-8 6v2h16v-2c0-3.31-3.58-6-8-6z" fill="white" fill-opacity="0.9"/>
                <circle cx="24" cy="14" r="4" fill="white" fill-opacity="0.9"/>
                <path d="M16 24v4c0 3.31 3.58 6 8 6s8-2.69 8-6v-4H16z" fill="white" fill-opacity="0.7"/>
                <defs>
                  <linearGradient id="auth-grad" x1="0" y1="0" x2="48" y2="48">
                    <stop stop-color="#10b981"/>
                    <stop offset="1" stop-color="#047857"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <h2 class="auth-title">易校EasyCampus</h2>
            <p class="auth-desc">登录你的账号，开启校园闲置交易之旅</p>
            <div class="auth-actions">
              <button @click="goToLogin" class="auth-btn login-btn">登录</button>
              <button @click="goToRegister" class="auth-btn register-btn">注册</button>
            </div>
          </div>
        </div>
      </section>

      <main class="menu-list" v-if="isAuthenticated">
        <section class="menu-group">
          <div class="menu-group-title">快捷操作</div>
          <div class="menu-group-body">
            <div class="menu-item highlight" @click="showPublishSheet = true">
              <span class="menu-icon publish-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </span>
              <span class="menu-text">发布闲置</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </div>
          </div>
        </section>

        <section class="menu-group">
          <div class="menu-group-title">我的交易</div>
          <div class="menu-group-body">
            <router-link to="/my-products" class="menu-item">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                  <line x1="12" y1="22.08" x2="12" y2="12"/>
                </svg>
              </span>
              <span class="menu-text">商品与帖子</span>
              <span class="menu-badge" v-if="stats.published > 0">{{ stats.published }}</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </router-link>

            <router-link to="/favorites" class="menu-item">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </span>
              <span class="menu-text">我的收藏</span>
              <span class="menu-badge" v-if="stats.favorites > 0">{{ stats.favorites }}</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </router-link>

            <router-link to="/dashboard" class="menu-item">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="20" x2="18" y2="10"/>
                  <line x1="12" y1="20" x2="12" y2="4"/>
                  <line x1="6" y1="20" x2="6" y2="14"/>
                  <line x1="3" y1="4" x2="21" y2="4"/>
                </svg>
              </span>
              <span class="menu-text">经营看板</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </router-link>
          </div>
        </section>

        <section class="menu-group">
          <div class="menu-group-title">其他</div>
          <div class="menu-group-body">
            <button @click="goToSettings" class="menu-item">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
              </span>
              <span class="menu-text">账号设置</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </button>

            <a href="#" @click.prevent="showAbout = true" class="menu-item">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              </span>
              <span class="menu-text">关于我们</span>
              <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9,18 15,12 9,6"/>
              </svg>
            </a>
          </div>
        </section>

        <section class="menu-group">
          <div class="menu-group-body">
            <button @click="handleLogout" class="menu-item danger">
              <span class="menu-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                  <polyline points="16 17 21 12 16 7"/>
                  <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
              </span>
              <span class="menu-text">退出登录</span>
            </button>
          </div>
        </section>

        <p class="version-info">易校EasyCampus v1.0.0</p>
      </main>
    </div>

    <Teleport to="body">
      <div v-if="showLogoutConfirm" class="modal-overlay" @click="cancelLogout">
        <div class="modal-content" @click.stop>
          <div class="modal-icon-wrap modal-icon-warn">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <h3 class="modal-title">确定要退出登录吗？</h3>
          <p class="modal-desc">退出后需要重新登录才能使用完整功能</p>
          <div class="modal-actions">
            <button @click="cancelLogout" class="modal-btn cancel-btn">取消</button>
            <button @click="confirmLogout" class="modal-btn confirm-btn">确定退出</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showAbout" class="modal-overlay" @click="showAbout = false">
        <div class="modal-content about-modal" @click.stop>
          <div class="about-logo">
            <svg width="56" height="56" viewBox="0 0 56 56" fill="none">
              <rect width="56" height="56" rx="16" fill="url(#about-grad)"/>
              <path d="M28 16c-5.52 0-10 3.36-10 7.5v2.5h20v-2.5c0-4.14-4.48-7.5-10-7.5z" fill="white" fill-opacity="0.9"/>
              <circle cx="28" cy="16" r="5" fill="white" fill-opacity="0.9"/>
              <path d="M18 26v5c0 4.14 4.48 7.5 10 7.5s10-3.36 10-7.5v-5H18z" fill="white" fill-opacity="0.7"/>
              <defs>
                <linearGradient id="about-grad" x1="0" y1="0" x2="56" y2="56">
                  <stop stop-color="#10b981"/>
                  <stop offset="1" stop-color="#047857"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h2 class="about-title">易校EasyCampus</h2>
          <p class="about-version">版本 1.0.0</p>
          <div class="about-desc">
            <p>一个专为高校学生打造的二手物品交易平台，让闲置物品流转起来，倡导绿色环保的校园生活方式。</p>
          </div>
          <div class="about-features">
            <div class="feature-item">
              <span class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </span>
              <span>安全交易</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </span>
              <span>即时沟通</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                  <line x1="12" y1="18" x2="12.01" y2="18"/>
                </svg>
              </span>
              <span>便捷发布</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66.95-2.3c.48.17.98.3 1.34.3C19 20 22 3 22 3c-1 2-8 2.25-13 3.25S2 11.5 2 13.5s1.75 3.75 1.75 3.75"/>
                </svg>
              </span>
              <span>绿色环保</span>
            </div>
          </div>
          <div class="about-info">
            <p>如有问题或建议，请联系：support@campus2c.com</p>
          </div>
          <button @click="showAbout = false" class="about-close-btn">我知道了</button>
        </div>
      </div>
    </Teleport>

    <PublishActionSheet :visible="showPublishSheet" @close="showPublishSheet = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { productApi, postApi, favoriteApi, uploadApi, userApi, followApi, organizationApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import PublishActionSheet from '../components/PublishActionSheet.vue'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const isAuthenticated = computed(() => authStore.isAuthenticated.value)

const defaultAvatar = `data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+`

const userInfo = computed(() => {
  if (authStore.currentUser.value) {
    return authStore.currentUser.value
  }
  const storedUser = localStorage.getItem('user')
  if (storedUser) {
    try {
      return JSON.parse(storedUser)
    } catch {
      return {}
    }
  }
  return {}
})

const stats = reactive({
  published: 0,
  favorites: 0,
  following: 0,
  followers: 0
})

const myOrgs = ref([])

const statsLoading = ref(true)
const showAbout = ref(false)
const showLogoutConfirm = ref(false)
const showPublishSheet = ref(false)
const avatarInput = ref(null)

/** 页面可见性变化时刷新统计数据（从后台切回前台时） */
function handleVisibilityChange() {
  if (document.visibilityState === 'visible' && authStore.isAuthenticated.value) {
    fetchStats()
  }
}

onMounted(async () => {
  if (authStore.isAuthenticated.value) {
    await fetchUserInfo()
    await fetchStats()
    fetchMyOrgs()
  }
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

watch(() => authStore.isAuthenticated.value, async (val) => {
  if (val) {
    await fetchUserInfo()
    await fetchStats()
    fetchMyOrgs()
  }
})

async function fetchUserInfo() {
  try {
    await authStore.fetchUserInfo()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

/** 获取当前用户已加入的组织列表 */
async function fetchMyOrgs() {
  try {
    const res = await organizationApi.getMyOrgs()
    if (res.code === 200 && Array.isArray(res.data)) {
      myOrgs.value = res.data
    }
  } catch {
    /* 静默失败，组织信息非核心数据 */
  }
}

async function fetchStats() {
  try {
    const userId = authStore.currentUser.value?.id
    const [myProductsRes, myPostsRes, favCountRes] = await Promise.allSettled([
      productApi.getMyProducts(),
      userId ? postApi.getUserPosts(userId) : Promise.resolve({ code: 200, data: [] }),
      favoriteApi.getFavoriteCount()
    ])

    let productCount = 0
    let postCount = 0

    if (myProductsRes.status === 'fulfilled' && myProductsRes.value.code === 200) {
      productCount = (myProductsRes.value.data || []).length
    }

    if (myPostsRes.status === 'fulfilled' && myPostsRes.value.code === 200) {
      postCount = (myPostsRes.value.data || []).length
    }

    stats.published = productCount + postCount

    if (favCountRes.status === 'fulfilled' && favCountRes.value.code === 200) {
      stats.favorites = favCountRes.value.data || 0
    }

    const followStatsRes = await Promise.allSettled([
      userId ? followApi.getFollowStats(userId) : Promise.resolve({ code: 200, data: { followingCount: 0, followerCount: 0 } })
    ])
    if (followStatsRes[0].status === 'fulfilled' && followStatsRes[0].value.code === 200) {
      const data = followStatsRes[0].value.data || {}
      stats.following = data.followingCount || 0
      stats.followers = data.followerCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    /* 统计数据加载完成，关闭loading态 */
    statsLoading.value = false
  }
}

function handleAvatarClick() {
  avatarInput.value?.click()
}

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.showToast('仅支持 JPG/PNG/WEBP 格式', 'error')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    toast.showToast('头像大小不能超过5MB', 'error')
    return
  }

  try {
    toast.showToast('上传中...', 'info')
    const res = await uploadApi.uploadAvatar(file)
    if (res.code === 200 && res.data?.url) {
      await userApi.updateProfile({ avatar: res.data.url })
      await authStore.fetchUserInfo()
      toast.showToast('头像更新成功', 'success')
    } else {
      toast.showToast(res.message || '上传失败', 'error')
    }
  } catch (err) {
    toast.showToast(err.message || '头像上传失败', 'error')
  }

  e.target.value = ''
}

function goToSettings() {
  router.push('/settings')
}

function handleLogout() {
  showLogoutConfirm.value = true
}

function confirmLogout() {
  showLogoutConfirm.value = false
  authStore.logout()
}

function cancelLogout() {
  showLogoutConfirm.value = false
}

function goToLogin() {
  router.push({ path: '/login', query: { redirect: '/profile' } })
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  padding-bottom: var(--space-8);
}

.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .profile-container {
    max-width: 100%;
  }
}

.profile-header {
  position: relative;
  padding-bottom: var(--space-6);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: var(--gradient-hero);
  overflow: hidden;
}

.header-bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.12) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 40%),
    radial-gradient(circle at 60% 80%, rgba(255,255,255,0.06) 0%, transparent 40%);
}

.user-info-card {
  position: relative;
  z-index: 1;
  padding-top: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-section {
  position: relative;
  margin: 0 auto var(--space-4);
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-spring);
}

.avatar-section:hover {
  transform: scale(1.05);
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--color-primary-400);
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.3);
  background-color: var(--color-gray-100);
}

.edit-avatar-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  color: white;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: var(--shadow-sm);
}

.user-name {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1);
}

.user-id {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-1);
  font-family: var(--font-mono);
  letter-spacing: 0.02em;
}

/* 用户已加入的组织标签 */
.user-orgs {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.org-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  border: 1px solid var(--color-primary-200);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.org-tag:hover {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  border-color: var(--color-primary-300);
}

.user-bio {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-5);
  max-width: 280px;
  text-align: center;
  line-height: var(--leading-relaxed);
}

.stats-row {
  display: flex;
  align-items: center;
  background-color: var(--color-bg-primary);
  padding: var(--space-5) var(--space-8);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  cursor: pointer;
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-md);
  transition: background-color var(--duration-fast) var(--ease-out);
}

.stat-item:hover {
  background-color: var(--color-primary-50);
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--font-extrabold);
  color: var(--color-primary-600);
  line-height: 1;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

/* 统计数字加载中的skeleton占位 */
.stat-skeleton {
  display: inline-block;
  width: 40px;
  height: 20px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-primary-50) 37%, var(--color-gray-100) 63%);
  background-size: 400% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.stat-divider {
  width: 1px;
  height: 28px;
  background-color: var(--color-border-light);
  flex-shrink: 0;
}

.auth-welcome {
  display: flex;
  justify-content: center;
  padding: var(--space-20) var(--space-5) var(--space-10);
}

.auth-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.auth-card-bg {
  position: absolute;
  inset: 0;
  background: var(--gradient-hero);
  z-index: 0;
}

.auth-card-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 30% 40%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 70% 70%, rgba(255,255,255,0.1) 0%, transparent 40%);
}

.auth-card-content {
  position: relative;
  z-index: 1;
  padding: var(--space-12) var(--space-8);
  text-align: center;
}

.auth-logo {
  margin-bottom: var(--space-5);
  display: flex;
  justify-content: center;
}

.auth-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: white;
  margin-bottom: var(--space-2);
}

.auth-desc {
  font-size: var(--text-sm);
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: var(--space-8);
  line-height: var(--leading-relaxed);
}

.auth-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.auth-btn {
  width: 100%;
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  cursor: pointer;
  border: none;
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.auth-btn.login-btn {
  background: white;
  color: var(--color-primary-700);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.auth-btn.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.auth-btn.login-btn:active {
  transform: scale(0.97);
}

.auth-btn.register-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 1.5px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
}

.auth-btn.register-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.6);
}

.auth-btn.register-btn:active {
  transform: scale(0.97);
}

.menu-list {
  padding: var(--space-3) var(--space-4);
}

@media (max-width: 768px) {
  .menu-list {
    padding: var(--space-3) var(--space-3);
  }
}

.menu-group {
  margin-bottom: var(--space-4);
}

.menu-group-title {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0 var(--space-4);
  margin-bottom: var(--space-2);
}

.menu-group-body {
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-4);
  text-decoration: none;
  color: var(--color-text-primary);
  transition: all var(--duration-fast) var(--ease-out);
  cursor: pointer;
  width: 100%;
  background: none;
  border: none;
  border-bottom: 1px solid var(--color-gray-50);
  text-align: left;
  font-size: var(--text-base);
  font-family: var(--font-sans);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background-color: var(--color-primary-50);
}

.menu-item:active {
  background-color: var(--color-primary-100);
}

.menu-item.highlight {
  background: linear-gradient(135deg, var(--color-primary-50) 0%, var(--color-bg-primary) 100%);
  font-weight: var(--font-semibold);
}

.menu-item.highlight:hover {
  background: linear-gradient(135deg, var(--color-primary-100) 0%, var(--color-primary-50) 100%);
}

.menu-item.highlight .menu-text {
  color: var(--color-primary-700);
}

.publish-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  color: white;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-green);
}

.menu-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.menu-item.highlight .menu-icon {
  color: white;
}

.menu-text {
  flex: 1;
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.menu-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 var(--space-2);
  background: var(--gradient-primary);
  color: white;
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-green);
}

.arrow-right {
  width: 16px;
  height: 16px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out);
}

.menu-item:hover .arrow-right {
  transform: translateX(2px);
  color: var(--color-primary-500);
}

.menu-item.danger .menu-icon {
  color: var(--color-rose-500);
}

.menu-item.danger .menu-text {
  color: var(--color-rose-500);
  font-weight: var(--font-semibold);
}

.menu-item.danger:hover {
  background-color: var(--color-rose-50);
}

.version-info {
  text-align: center;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin-top: var(--space-8);
  padding-bottom: var(--space-4);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  padding: var(--space-5);
  animation: fadeIn var(--duration-normal) var(--ease-out);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-2xl);
  padding: var(--space-8) var(--space-6);
  max-width: 360px;
  width: 100%;
  text-align: center;
  box-shadow: var(--shadow-xl);
  animation: slideUp var(--duration-slow) var(--ease-spring);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(24px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-5);
}

.modal-icon-warn {
  background-color: var(--color-amber-50);
  color: var(--color-amber-500);
}

.modal-title {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2);
}

.modal-desc {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-6);
  line-height: var(--leading-relaxed);
}

.modal-actions {
  display: flex;
  gap: var(--space-3);
}

.modal-btn {
  flex: 1;
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  border: none;
  font-family: var(--font-sans);
}

.cancel-btn {
  background-color: var(--color-gray-100);
  color: var(--color-text-secondary);
}

.cancel-btn:hover {
  background-color: var(--color-gray-200);
}

.cancel-btn:active {
  transform: scale(0.97);
}

.confirm-btn {
  background: var(--gradient-rose);
  color: white;
  box-shadow: var(--shadow-rose);
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(244, 63, 94, 0.3);
}

.confirm-btn:active {
  transform: scale(0.97);
}

.about-modal {
  max-width: 400px;
  padding: var(--space-8) var(--space-6);
}

.about-logo {
  margin-bottom: var(--space-4);
  display: flex;
  justify-content: center;
}

.about-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1);
}

.about-version {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-4);
}

.about-desc {
  text-align: left;
  margin-bottom: var(--space-5);
}

.about-desc p {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  margin: 0;
}

.about-features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2_5) var(--space-3);
  background: var(--color-primary-50);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-primary-700);
  font-weight: var(--font-medium);
}

.feature-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-500);
}

.about-info {
  margin-bottom: var(--space-5);
}

.about-info p {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin: 0;
}

.about-close-btn {
  width: 100%;
  padding: var(--space-3) var(--space-5);
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  cursor: pointer;
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.about-close-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-green-lg);
}

.about-close-btn:active {
  transform: scale(0.97);
}
</style>
