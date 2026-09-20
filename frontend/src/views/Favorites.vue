<template>
  <div class="favorites-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">我的收藏</h1>
      <span class="count-badge" v-if="total > 0">{{ total }}</span>
    </header>

    <main class="main-content">
      <div class="fav-tabs">
        <el-tabs :model-value="activeType" @tab-change="switchType">
          <el-tab-pane name="PRODUCT">
            <template #label>
              商品收藏<span v-if="productCount > 0"> ({{ productCount }})</span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="POST">
            <template #label>
              帖子收藏<span v-if="postCount > 0"> ({{ postCount }})</span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="loadFavorites" class="retry-btn">重试</button>
      </div>

      <el-empty v-else-if="items.length === 0" :description="activeType === 'PRODUCT' ? '还没有收藏商品' : '还没有收藏帖子'">
        <p v-if="activeType === 'PRODUCT'" class="empty-sub">去逛逛，收藏感兴趣的商品吧</p>
        <p v-else class="empty-sub">去社区看看，收藏感兴趣的帖子吧</p>
        <router-link :to="activeType === 'PRODUCT' ? '/products' : '/community'">
          <el-button type="primary" round>{{ activeType === 'PRODUCT' ? '逛一逛' : '去社区' }}</el-button>
        </router-link>
      </el-empty>

      <div v-else-if="activeType === 'PRODUCT'" class="product-list">
        <el-card
          v-for="product in items"
          :key="product.id"
          shadow="hover"
          :body-style="{ padding: '0px' }"
          @click="goToDetail(product.id)"
        >
          <div class="product-card">
            <div class="product-image">
              <img
                v-if="product.coverImage"
                :src="product.coverImage"
                alt=""
                loading="lazy"
              />
              <div v-else class="image-placeholder">
                <span>📦</span>
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <div class="product-meta">
                <span class="product-price">¥{{ product.price }}</span>
                <span v-if="product.originalPrice" class="product-original">¥{{ product.originalPrice }}</span>
              </div>
              <div class="product-footer">
                <span class="product-status" :class="{ 'on-sale': product.status === 1 }">
                  {{ getStatusText(product.status) }}
                </span>
                <span class="fav-time">{{ formatTime(product.favoritedAt) }}</span>
              </div>
            </div>
            <button @click.stop="removeFavoriteItem(product.id)" class="unfav-btn" title="取消收藏">
              ❤️
            </button>
          </div>
        </el-card>
      </div>

      <div v-else-if="activeType === 'POST'" class="post-list">
        <el-card
          v-for="post in items"
          :key="post.id"
          shadow="hover"
          :body-style="{ padding: '0px' }"
          @click="goToPost(post.id)"
        >
          <div class="post-card">
            <div class="post-info">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-excerpt">{{ truncate(post.content, 80) }}</p>
              <div class="post-footer">
                <span class="post-type">{{ getPostTypeText(post.postType) }}</span>
                <span class="fav-time">{{ formatTime(post.favoritedAt) }}</span>
              </div>
            </div>
            <button @click.stop="removeFavoriteItem(post.id)" class="unfav-btn" title="取消收藏">
              ❤️
            </button>
          </div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { favoriteApi } from '../services/api'
import { useToast } from '../use/useToast'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const items = ref([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const activeType = ref('PRODUCT')
const productCount = ref(0)
const postCount = ref(0)

function getPostTypeText(postType) {
  const map = { DISCUSSION: '讨论', SHOWCASE: '展示', HELP: '求助', ACTIVITY: '活动' }
  return map[postType] || '讨论'
}

function truncate(str, len) {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '...' : str
}

function getStatusText(status) {
  const map = { 0: '已下架', 1: '在售', 2: '已售出', 3: '预约中' }
  return map[status] || '未知'
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚收藏'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前收藏'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前收藏'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前收藏'
  return date.toLocaleDateString('zh-CN')
}

function goToDetail(id) {
  router.push(`/products/${id}`)
}

function goToPost(id) {
  router.push(`/community/posts/${id}`)
}

function switchType(type) {
  if (activeType.value === type) return
  activeType.value = type
  loadFavorites()
}

async function removeFavoriteItem(targetId) {
  try {
    if (activeType.value === 'POST') {
      await favoriteApi.removePostFavorite(targetId)
    } else {
      await favoriteApi.removeFavorite(targetId)
    }
    items.value = items.value.filter(item => item.id !== targetId)
    total.value = Math.max(0, total.value - 1)
  } catch (e) {
    toast.showToast(e?.message || '取消收藏失败', 'error')
  }
}

async function loadFavorites() {
  loading.value = true
  error.value = ''
  try {
    const res = await favoriteApi.getMyFavorites({ targetType: activeType.value })
    if (res.code === 200) {
      const data = res.data || {}
      items.value = data.list || []
      total.value = data.total || 0
    } else {
      error.value = res.message || '加载失败'
    }
  } catch (e) {
    console.error('加载收藏失败:', e)
    // 401：登录态已过期（响应拦截器已清空本地凭证），引导重新登录而不是误报网络问题
    if (e?.status === 401) {
      error.value = '登录已过期，请重新登录'
      toast.showToast('登录已过期，请重新登录', 'warning')
      setTimeout(() => {
        router.replace({ path: '/login', query: { redirect: route.fullPath } })
      }, 800)
      return
    }
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFavorites()
  loadCounts()
})

document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') {
    loadFavorites()
    loadCounts()
  }
})

/** 加载收藏分类统计（商品数 + 帖子数） */
async function loadCounts() {
  try {
    const res = await favoriteApi.getFavoriteCounts()
    if (res.code === 200 && res.data) {
      productCount.value = res.data.productCount || 0
      postCount.value = res.data.postCount || 0
    }
  } catch {
    // 静默失败，不影响主流程
  }
}
</script>

<style scoped>
.favorites-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
}

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
}

.count-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 7px;
  background-color: #FF4D4F;
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-content {
  padding: 12px 16px 80px;
}

.fav-tabs {
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 0 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.fav-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: #999;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top-color: var(--color-primary-500, #10b981);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-state {
  text-align: center;
  padding: 60px 0;
}

.error-icon { font-size: 48px; margin-bottom: 12px; }
.error-state p { color: #666; margin-bottom: 16px; }

.retry-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.empty-sub {
  margin: 0 0 12px;
  font-size: 14px;
  color: #999;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
}

.product-card:active {
  transform: scale(0.98);
}

.product-image {
  width: 90px;
  height: 90px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: #f8f8f8;
}

.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.product-price {
  font-size: 17px;
  font-weight: 700;
  color: #FF4D4F;
}

.product-original {
  font-size: 13px;
  color: #ccc;
  text-decoration: line-through;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-status {
  font-size: 12px;
  color: #999;
  padding: 2px 6px;
  background: #f5f5f5;
  border-radius: 4px;
}

.product-status.on-sale {
  color: #52c41a;
  background: #f6ffed;
}

.fav-time {
  font-size: 12px;
  color: #ccc;
}

.unfav-btn {
  align-self: center;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.unfav-btn:active {
  transform: scale(1.3);
}

/* 帖子收藏列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
}

.post-card:active {
  transform: scale(0.98);
}

.post-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-excerpt {
  font-size: 13px;
  color: #999;
  margin: 4px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.post-type {
  font-size: 12px;
  color: #999;
  padding: 2px 6px;
  background: #f5f5f5;
  border-radius: 4px;
}
</style>
