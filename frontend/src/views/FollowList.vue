<template>
  <div class="follow-list-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">{{ targetUser?.nickname || targetUser?.username || '关注列表' }}</span>
      <div class="nav-spacer"></div>
    </header>

    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'following' }" @click="switchTab('following')">
        关注
        <span v-if="stats.followingCount !== null" class="tab-count">{{ stats.followingCount }}</span>
      </button>
      <div class="tab-divider"></div>
      <button class="tab-btn" :class="{ active: activeTab === 'followers' }" @click="switchTab('followers')">
        粉丝
        <span v-if="stats.followerCount !== null" class="tab-count">{{ stats.followerCount }}</span>
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <div v-for="i in 5" :key="i" class="skeleton-user"></div>
    </div>

    <div v-else-if="users.length === 0" class="empty-state">
      <div class="empty-icon">{{ activeTab === 'following' ? '👀' : '🌱' }}</div>
      <p>{{ activeTab === 'following' ? '还没有关注任何人' : '还没有粉丝' }}</p>
    </div>

    <div v-else class="user-list">
      <div v-for="user in users" :key="user.id" class="user-item" @click="goToProfile(user.id)">
        <img :src="user.avatar || defaultAvatar" class="user-avatar" @error="onAvatarError" loading="lazy" />
        <div class="user-info">
          <h3 class="user-name">{{ user.nickname || user.username }}</h3>
          <p class="user-id">@{{ user.username }}</p>
          <p v-if="user.bio" class="user-bio">{{ user.bio }}</p>
          <p v-if="user.school" class="user-school">{{ user.school }}</p>
        </div>
        <div v-if="showFollowBtn(user.id)" class="user-action" @click.stop>
          <button class="follow-btn" :class="{ followed: isUserFollowed(user.id) }" @click="handleToggleFollow(user)">
            {{ isUserFollowed(user.id) ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>

      <div v-if="hasMore && !loadingMore" class="load-more">
        <button @click="loadMore" :disabled="loadingMore" class="load-more-btn">加载更多</button>
      </div>
      <div v-if="!hasMore && users.length > 0" class="no-more">没有更多了</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { followApi, userApi } from '../services/api'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser } = useAuthStore()

const activeTab = ref(route.query.tab || 'following')
const users = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
const targetUser = ref(null)
const stats = reactive({ followingCount: null, followerCount: null })
const followedSet = ref(new Set())

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

const currentUserId = computed(() => currentUser.value?.id)

function showFollowBtn(userId) {
  if (!isAuthenticated.value) return false
  return Number(currentUserId.value) !== Number(userId)
}

function isUserFollowed(userId) {
  return followedSet.value.has(Number(userId))
}

onMounted(() => {
  loadTargetUser()
  loadStats()
  loadData()
})

watch(activeTab, () => {
  page.value = 1
  hasMore.value = true
  users.value = []
  loadData()
})

async function loadTargetUser() {
  const userId = String(route.params.userId || route.query.userId || '')
  if (!userId) return
  try {
    const res = await userApi.getUserPublic(userId)
    if (res.code === 200) targetUser.value = res.data
  } catch {}
}

async function loadStats() {
  const userId = (String(route.params.userId || route.query.userId || '') || currentUserId.value) as string | number
  if (!userId) return
  try {
    const res = await followApi.getFollowStats(userId)
    if (res.code === 200) {
      stats.followingCount = res.data.followingCount || 0
      stats.followerCount = res.data.followerCount || 0
    }
  } catch {}
}

async function loadData(isLoadMore = false) {
  if (isLoadMore) loadingMore.value = true
  else loading.value = true

  const userId = String(route.params.userId || route.query.userId || '')
  try {
    let res
    if (activeTab.value === 'following') {
      res = await followApi.getFollowingList(userId, page.value, pageSize)
    } else {
      res = await followApi.getFollowerList(userId, page.value, pageSize)
    }

    if (res.code === 200) {
      const list = res.data || []
      if (isLoadMore) {
        users.value.push(...list)
      } else {
        users.value = list
      }
      hasMore.value = list.length === pageSize

      if (isAuthenticated.value && currentUserId.value) {
        for (const u of list) {
          checkIfFollowing(u.id)
        }
      }
    }
  } catch (e) {
    console.error('加载关注列表失败:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function checkIfFollowing(targetId) {
  try {
    const res = await followApi.checkFollowing(targetId)
    if (res.code === 200 && res.data.isFollowing) {
      followedSet.value.add(Number(targetId))
    }
  } catch {}
}

function switchTab(tab) {
  activeTab.value = tab
}

function loadMore() {
  page.value++
  loadData(true)
}

async function handleToggleFollow(user) {
  try {
    const res = await followApi.toggleFollow(user.id)
    if (res.code === 200) {
      if (res.data.isFollowing) {
        followedSet.value.add(Number(user.id))
        if (activeTab.value === 'followers') stats.followerCount++
      } else {
        followedSet.value.delete(Number(user.id))
        if (activeTab.value === 'following') stats.followingCount--
      }
    }
  } catch {}
}

function goToProfile(userId) {
  router.push(`/users/${userId}`)
}

function onAvatarError(e) {
  e.target.src = defaultAvatar
}
</script>

<style scoped>
.follow-list-page {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.page-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E8ECF0;
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: #333333;
  cursor: pointer;
  flex-shrink: 0;
}

.nav-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #333333;
}

.nav-spacer { width: 32px; flex-shrink: 0; }

.tab-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 0 24px;
  position: sticky;
  top: 56px;
  z-index: 99;
}

.tab-btn {
  flex: 1;
  padding: 14px 0;
  text-align: center;
  font-size: 15px;
  font-weight: 500;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  transition: color 0.25s ease;
}

.tab-btn.active {
  color: var(--color-primary-500, #10b981);
  font-weight: 700;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 3px;
  background: var(--color-primary-500, #10b981);
  border-radius: 2px;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 4px;
  background: #f0f0f0;
  color: #999;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
}

.tab-btn.active .tab-count {
  background: #E8F5E9;
  color: var(--color-primary-500, #10b981);
}

.tab-divider {
  width: 1px;
  height: 20px;
  background: #E8ECF0;
}

.loading-state { padding: 16px; }

.skeleton-user {
  height: 72px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 12px;
  margin-bottom: 10px;
}

@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #999;
}

.empty-icon { font-size: 48px; margin-bottom: 12px; }

.user-list { padding: 8px 16px 32px; }

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.user-item:active { transform: scale(0.98); }

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 2px;
}

.user-id {
  font-size: 12px;
  color: #bbb;
  margin: 0 0 4px;
}

.user-bio {
  font-size: 13px;
  color: #666;
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-school {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.user-action { flex-shrink: 0; }

.follow-btn {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff;
  box-shadow: 0 2px 8px rgba(16,185,129,0.25);
  transition: all 0.2s ease;
}

.follow-btn.followed {
  background: #F0F2F5;
  color: #999;
  box-shadow: none;
  border: 1px solid #E0E0E0;
}

.load-more { text-align: center; padding: 12px 0; }

.load-more-btn {
  background: #F5F7FA;
  color: #999;
  padding: 8px 28px;
  border-radius: 20px;
  font-size: 13px;
  border: none;
  cursor: pointer;
}

.load-more-btn:disabled { opacity: 0.6; }

.no-more {
  text-align: center;
  padding: 16px 0;
  color: #ccc;
  font-size: 13px;
}
</style>
