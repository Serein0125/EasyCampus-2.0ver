<template>
  <div class="user-profile-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">{{ profile?.nickname || profile?.username || '用户主页' }}</span>
      <div class="nav-spacer"></div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="skeleton-avatar"></div>
      <div class="skeleton-line short"></div>
      <div class="skeleton-line medium"></div>
      <div class="skeleton-line long"></div>
    </div>

    <div v-else-if="error" class="error-state">
      <span class="error-icon">😕</span>
      <p>{{ error }}</p>
      <button @click="loadProfile" class="retry-btn">重试</button>
    </div>

    <main v-else-if="profile" class="profile-content">
      <section class="profile-card">
        <img :src="profile.avatar || defaultAvatar" class="profile-avatar" @error="onAvatarError" loading="lazy" />
        <h2 class="profile-name">{{ profile.nickname || profile.username }}</h2>
        <p class="profile-id">@{{ profile.username }}</p>
        <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
        <div v-if="profile.school" class="profile-meta">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>
          <span>{{ profile.school }}{{ profile.major ? ' · ' + profile.major : '' }}</span>
        </div>

        <div class="stats-row">
          <div class="stat-item" @click="goToPosts">
            <span class="stat-num">{{ stats.postCount }}</span>
            <span class="stat-lbl">帖子</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item clickable" @click="goToFollowList('following')">
            <span class="stat-num">{{ stats.followingCount }}</span>
            <span class="stat-lbl">关注</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item clickable" @click="goToFollowList('followers')">
            <span class="stat-num">{{ stats.followerCount }}</span>
            <span class="stat-lbl">粉丝</span>
          </div>
        </div>

        <div v-if="isSelf" class="self-actions">
          <button @click="$router.push('/profile')" class="action-btn edit-btn">编辑资料</button>
        </div>
        <div v-else class="visit-actions">
          <button class="action-btn chat-btn" @click="startChat">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            私聊
          </button>
          <button class="action-btn follow-btn" :class="{ followed: isFollowing }" @click="toggleFollow">
            {{ isFollowing ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </section>

      <section class="posts-section">
        <h3 class="section-title">{{ isSelf ? '我的帖子' : 'TA的帖子' }}</h3>

        <div v-if="postsLoading" class="posts-loading">
          <div v-for="i in 3" :key="i" class="skeleton-post"></div>
        </div>
        <div v-else-if="posts.length === 0" class="posts-empty">暂无帖子</div>
        <div v-else class="posts-list">
          <PostCard v-for="post in posts" :key="post.id" :post="post" @click="goToPost(post.id)" />
        </div>

        <div v-if="!postsLoading && postsMore && posts.length > 0" class="load-more">
          <button @click="loadMorePosts" :disabled="postsLoadingMore" class="load-more-btn">
            {{ postsLoadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
        <div v-if="!postsMore && posts.length > 0" class="no-more">
          <span class="no-more-line"></span><span class="no-more-text">已经到底啦</span><span class="no-more-line"></span>
        </div>
      </section>

      <section class="products-section">
        <h3 class="section-title">{{ isSelf ? '我的商品' : 'TA 的商品' }}</h3>

        <div v-if="productsLoading" class="products-loading">
          <div v-for="i in 4" :key="i" class="skeleton-product"></div>
        </div>
        <div v-else-if="products.length === 0" class="products-empty">暂无商品</div>
        <div v-else class="products-grid">
          <ProductCard v-for="product in products" :key="product.id" :product="product" @click="goToProduct(product.id)" />
        </div>

        <div v-if="!productsLoading && productsMore && products.length > 0" class="load-more">
          <button @click="loadMoreProducts" :disabled="productsLoadingMore" class="load-more-btn">
            {{ productsLoadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
        <div v-if="!productsMore && products.length > 0" class="no-more">
          <span class="no-more-line"></span><span class="no-more-text">已经到底啦</span><span class="no-more-line"></span>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userApi, postApi, productApi, followApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import PostCard from '../components/PostCard.vue'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser } = useAuthStore()

const profile = ref(null)
const loading = ref(true)
const error = ref(null)
const isFollowing = ref(false)

const posts = ref([])
const postsLoading = ref(false)
const postsLoadingMore = ref(false)
const postsMore = ref(true)
const pageSize = 10
const allPosts = ref([])
const displayedCount = ref(pageSize)

const products = ref([])
const productsLoading = ref(false)
const productsLoadingMore = ref(false)
const productsMore = ref(true)
const productPageSize = 10
const allProducts = ref([])
const displayedProductCount = ref(productPageSize)

const stats = ref({ postCount: 0, followingCount: 0, followerCount: 0, productCount: 0 })

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

const isSelf = computed(() => {
  return isAuthenticated.value && currentUser.value && Number(currentUser.value.id) === Number(profile.value?.id)
})

onMounted(() => { loadProfile() })

async function loadProfile() {
  loading.value = true
  error.value = null
  try {
    const userId = String(route.params.id)
    const [userRes, statsRes] = await Promise.allSettled([
      userApi.getUserPublic(userId),
      followApi.getFollowStats(userId)
    ])
    if (userRes.status === 'fulfilled' && userRes.value.code === 200) {
      profile.value = userRes.value.data
    } else {
      error.value = '用户不存在或加载失败'
      return
    }
    if (statsRes.status === 'fulfilled' && statsRes.value.code === 200) {
      stats.value = statsRes.value.data || {}
    }
    if (profile.value && isAuthenticated.value && !isSelf.value) {
      checkFollowStatus(profile.value.id)
    }
    loadPosts()
    loadProducts()
  } catch {
    error.value = '网络错误，请检查网络连接'
  } finally {
    loading.value = false
  }
}

async function checkFollowStatus(userId) {
  try {
    const res = await followApi.checkFollowing(userId)
    if (res.code === 200) isFollowing.value = res.data.isFollowing
  } catch {}
}

async function toggleFollow() {
  if (!profile.value) return
  try {
    const res = await followApi.toggleFollow(profile.value.id)
    if (res.code === 200) isFollowing.value = res.data.isFollowing
    stats.value.followerCount += isFollowing.value ? 1 : -1
  } catch {}
}

async function loadPosts(isLoadMore = false) {
  if (!profile.value) return
  try {
    if (isLoadMore) postsLoadingMore.value = true
    else postsLoading.value = true

    const res = await postApi.getUserPosts(profile.value.id)
    if (res.code === 200) {
      const list = res.data || []
      allPosts.value = Array.isArray(list) ? list : []
      stats.value.postCount = allPosts.value.length
      displayedCount.value = pageSize
      posts.value = allPosts.value.slice(0, displayedCount.value)
      postsMore.value = allPosts.value.length > displayedCount.value
    }
  } catch {
    posts.value = []
  } finally {
    postsLoading.value = false
    postsLoadingMore.value = false
  }
}

function loadMorePosts() {
  displayedCount.value += pageSize
  posts.value = allPosts.value.slice(0, displayedCount.value)
  postsMore.value = allPosts.value.length > displayedCount.value
}

async function loadProducts(isLoadMore = false) {
  if (!profile.value) return
  try {
    if (isLoadMore) productsLoadingMore.value = true
    else productsLoading.value = true

    const res = await productApi.getProducts({ sellerId: profile.value.id, sortBy: 'time_desc', size: productPageSize })
    if (res.code === 200) {
      const data = res.data || {}
      // 兼容两种返回格式：分页对象 {list, total} 和直接数组
      const list = Array.isArray(data) ? data : (data.list || [])
      allProducts.value = list
      stats.value.productCount = allProducts.value.length
      displayedProductCount.value = productPageSize
      products.value = allProducts.value.slice(0, displayedProductCount.value)
      productsMore.value = allProducts.value.length > displayedProductCount.value
    }
  } catch {
    products.value = []
  } finally {
    productsLoading.value = false
    productsLoadingMore.value = false
  }
}

function loadMoreProducts() {
  displayedProductCount.value += productPageSize
  products.value = allProducts.value.slice(0, displayedProductCount.value)
  productsMore.value = allProducts.value.length > displayedProductCount.value
}

function startChat() {
  if (!isAuthenticated.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push({ path: `/chat/${profile.value.id}` })
}

function goToFollowList(tab) {
  const userId = profile.value?.id
  if (!userId) return
  router.push({ path: '/follows', query: { userId, tab } })
}

function goToPosts() {
  document.querySelector('.posts-section')?.scrollIntoView({ behavior: 'smooth' })
}

function goToPost(postId) {
  router.push(`/community/posts/${postId}`)
}

function goToProduct(productId) {
  router.push(`/products/${productId}`)
}

function onAvatarError(e) {
  e.target.src = defaultAvatar
}
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background-color: #F5F7FA;
  padding-bottom: 32px;
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

.profile-content {
  padding: 16px;
  max-width: 750px;
  margin: 0 auto;
}

.profile-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 32px 24px 24px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  margin-bottom: 16px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
  margin-bottom: 14px;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  color: #333333;
  margin: 0 0 4px;
}

.profile-id {
  font-size: 13px;
  color: #999999;
  margin: 0 0 8px;
}

.profile-bio {
  font-size: 14px;
  color: #666666;
  line-height: 1.5;
  margin: 0 0 12px;
  padding: 0 16px;
}

.profile-meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
  margin-bottom: 16px;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  padding: 16px 0;
  margin-bottom: 16px;
  border-top: 1px solid #F5F7FA;
  border-bottom: 1px solid #F5F7FA;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-item.clickable {
  cursor: pointer;
}

.stat-item.clickable:active { opacity: 0.6; }

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #333333;
}

.stat-lbl {
  font-size: 12px;
  color: #999999;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: #E8ECF0;
}

.visit-actions, .self-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 32px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
}

.chat-btn {
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff;
  box-shadow: 0 4px 12px rgba(16,185,129,0.25);
}

.chat-btn:active { transform: scale(0.96); }

.follow-btn {
  background: #F0F2F5;
  color: #666666;
  border: 1px solid #DDE1E6;
}

.follow-btn.followed {
  background: #E8F4FD;
  border-color: #B3D8F5;
  color: #1890FF;
}

.edit-btn {
  background: #F0F2F5;
  color: #666666;
  border: 1px solid #DDE1E6;
  width: 100%;
}

.posts-section {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 20px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #333333;
  margin: 0 0 16px;
}

.posts-loading, .posts-empty {
  text-align: center;
  padding: 32px 0;
  color: #999999;
  font-size: 14px;
}

.skeleton-post {
  height: 120px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 12px;
  margin-bottom: 12px;
}

.loading-state {
  padding: 40px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.skeleton-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line.short { width: 120px; }
.skeleton-line.medium { width: 200px; }
.skeleton-line.long { width: 280px; }

@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.error-state {
  padding: 80px 32px;
  text-align: center;
  color: #999999;
}

.error-icon { font-size: 64px; display: block; margin-bottom: 16px; }

.retry-btn {
  margin-top: 16px;
  padding: 10px 32px;
  background: var(--color-primary-500, #10b981);
  color: #fff;
  border-radius: 4px;
  font-size: 14px;
  border: none;
  cursor: pointer;
}

.load-more { text-align: center; padding: 16px 0 8px; }
.load-more-btn {
  background: #F5F7FA;
  color: #999;
  padding: 8px 32px;
  border-radius: 20px;
  font-size: 14px;
  border: none;
  cursor: pointer;
}
.load-more-btn:disabled { opacity: 0.6; }

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 0;
  color: #ccc;
  font-size: 13px;
}
.no-more-line { flex: 1; height: 1px; background: #E8ECF0; }

/* 帖子瀑布流布局 - 小红书风格 */
.posts-list {
  column-count: 4;
  column-gap: 8px;
}

/* 瀑布流子项需阻止被列分割 */
.posts-list > * {
  break-inside: avoid;
  margin-bottom: 8px;
}

/* 商品区域 */
.products-section {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 20px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.products-loading, .products-empty {
  text-align: center;
  padding: 32px 0;
  color: #999999;
  font-size: 14px;
}

/* 商品瀑布流布局 - 小红书风格 */
.products-grid {
  column-count: 4;
  column-gap: 8px;
}

/* 瀑布流子项需阻止被列分割 */
.products-grid > * {
  break-inside: avoid;
  margin-bottom: 8px;
}

/* 响应式：平板3列 */
@media (max-width: 1024px) {
  .posts-list,
  .products-grid {
    column-count: 3;
  }
}

/* 响应式：手机2列 */
@media (max-width: 600px) {
  .posts-list,
  .products-grid {
    column-count: 2;
  }
}

.skeleton-product {
  height: 180px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 12px;
}
</style>
